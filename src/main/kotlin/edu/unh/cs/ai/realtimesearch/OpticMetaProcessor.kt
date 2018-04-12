package edu.unh.cs.ai.realtimesearch

import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.util.AdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.Indexable
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JSON
import kotlinx.serialization.list
import java.io.File

/**
 * @author Bence Cserna (bence@cserna.net)
 */
fun main(args: Array<String>) {
    if (args.size != 2) {
        println("Usage: <search_tree.json> <processed_tree.json>")
    }

    var searchTreePath = "/Users/bencecserna/Downloads/testingstatic/searchtree.json"
    var targetTreePath = "/Users/bencecserna/Downloads/testingstatic/processed_tree.json"

    if (args.size == 2) {
        searchTreePath = args[0]
        targetTreePath = args[1]
    }

    if (!File(searchTreePath).canRead() || !File(targetTreePath).canWrite()) {
        throw MetronomeException("Can't read/write source.")
    }

    val jsonTree = File(searchTreePath).readText()

    val loader = JsonOpticNode.serializer().list
    val jsonOpticNodes = JSON.parse(loader, jsonTree)
    println("Json node parsing was successful.")

    val opticNodes = jsonOpticNodes.map(JsonOpticNode::toOpticNode)

    opticNodes
            .parallelStream()
            .forEach {
                aStar(opticNodes, it)
                System.out.flush()
                it.minGoalDistance = it.expansionsToGoals?.min()
            }

    println("\nNode count: ${jsonOpticNodes.size}")

    val processedTree = JSON.plain.stringify(OpticNode.serializer().list, opticNodes)
    File(targetTreePath).writeText(processedTree)
}

@Serializable
data class JsonOpticNode(
        private val id: String,
        @Optional
        private val duplicates: List<String> = listOf(),
        private val tag: NodeTag,
        private val gValue: Int,
        val distanceToGo: String,
        val latestStartTimeUB: String,
        private val latestStartTimeEstimate: String,
        private val successors: List<String>
) {
    fun toOpticNode(): OpticNode = OpticNode(
            id.removePrefix("0x").toLong(16),
            duplicates.map { it.removePrefix("0x").toLong(16) },
            tag,
            gValue,
            distanceToGo.toIntOrNull(),
            latestStartTimeUB.toDoubleOrNull(),
            latestStartTimeEstimate.toDoubleOrNull(),
            successors.map { it.removePrefix("0x").toLong(16) }
    )

}

enum class NodeTag {
    frontier, expanded, goal
}

@Serializable
data class OpticNode(val id: Long,
                     @Transient
                     val duplicates: List<Long>,
                     val tag: NodeTag,
                     val gValue: Int,
                     val distanceToGo: Int?,
                     val latestStartTimeUB: Double?,
                     val latestStartTimeEstimate: Double?,
                     @Transient
                     val successors: List<Long>,
                     var expansionsToGoals: List<Int>? = null,
                     var minGoalDistance: Int? = null)


fun aStar(nodes: List<OpticNode>, sourceNode: OpticNode) {
    data class Node(val opticNode: OpticNode, var closed: Boolean = false, override var index: Int = -1) : Indexable

    // Add
    val localNodeMap = nodes.associate { it.id to Node(it) }.toMutableMap()
    nodes.forEach { opticNode -> opticNode.duplicates.forEach { duplicate -> localNodeMap[duplicate] = localNodeMap[opticNode.id]!! } }

    val openList = AdvancedPriorityQueue<Node>(arrayOfNulls(nodes.size), compareBy {
        it.opticNode.gValue + (it.opticNode.distanceToGo?.times(5) ?: Int.MAX_VALUE)
    })

    openList.add(localNodeMap[sourceNode.id] ?: throw MetronomeException("Source node is not found"))

    var expansionCount = 0
    val expansionsToGoals = mutableListOf<Int>()

    while (openList.isNotEmpty()) {
        val currentNode = openList.pop() ?: throw GoalNotReachableException("Open list is empty.")

        expansionCount++

        for (successorId in currentNode.opticNode.successors) {
            val successorNode = localNodeMap[successorId]
                    ?: throw MetronomeException("Invalid NodeId 0x${successorId.toString(16)}")

            if (successorNode.closed) {
                continue
            }

            if (successorNode.opticNode.tag == NodeTag.goal) {
                expansionsToGoals.add(expansionCount)
            }

            successorNode.closed = true
            successorNode.opticNode.duplicates.forEach { localNodeMap[it]?.closed = true }
            // Cost of duplicates? Could it be better than the original?

            if (successorNode.open) {
                openList.update(successorNode)
            } else {
                openList.add(successorNode)
            }
        }
    }

    sourceNode.expansionsToGoals = expansionsToGoals
}


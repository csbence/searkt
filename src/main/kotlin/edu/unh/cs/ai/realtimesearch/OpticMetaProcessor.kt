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

//    var searchTreePath = "/Users/bencecserna/Downloads/testingstatic/searchtree.json"
//    var targetTreePath = "/Users/bencecserna/Downloads/testingstatic/processed_tree.json"

    var searchTreePath = "/home/aifs2/bence/development/projects/optic_metareasoning/test/searchtree.json"
    var targetTreePath = "/home/aifs2/bence/development/projects/optic_metareasoning/test/processed_tree.json"

    if (args.size == 2) {
        searchTreePath = args[0]
        targetTreePath = args[1]
    }

    if (!File(searchTreePath).canRead()) {
        throw MetronomeException("Can't read source tree.")
    }

    val outputTreePath = File(targetTreePath)

    if (!outputTreePath.canWrite() && !outputTreePath.createNewFile()) {
        throw MetronomeException("Can't write target tree.")
    }

    val jsonTree = File(searchTreePath).readText()

    val opticDump = JSON.parse<OpticDump>(jsonTree)
    val jsonOpticNodes = opticDump.nodes
    println("Json node parsing was successful.")

    val opticNodes = jsonOpticNodes.map(JsonOpticNode::toOpticNode)
    val rootNodeId = opticDump.initialState.removePrefix("0x").toLong(16)

    if (opticNodes.none { it.tag == NodeTag.goal }) {
        println("\nGoal not found.")
        return
    }

//    checkInvariant(opticNodes)

    opticNodes
            .parallelStream()
            .forEach {
                aStar(opticNodes, it, rootNodeId)
                System.out.flush()

                var minDistance: Int? = it.expansionsToGoals?.firstOrNull()
                var minDistanceIndex: Int? = if (minDistance != null) 0 else null

                it.expansionsToGoals?.forEachIndexed { index: Int, distance: Int ->
                    if (minDistance == null || distance < minDistance!!) {
                        minDistance = distance
                        minDistanceIndex = index
                    }
                }

                it.minGoalDistance = minDistance
                if (minDistanceIndex != null) {
                    it.firstGoalLatestStartTime = it.latestStartTimesForGoals?.get(minDistanceIndex!!)
                }
            }

    // Remove nodes that can't reach the goal
    val successfulNodes = opticNodes.filter { it.expansionsToGoals?.isNotEmpty() ?: false }

    println("\nNode count: ${jsonOpticNodes.size}")
    println("\nSuccess count: ${successfulNodes.size}")

    // Save results as json
    val processedTree = JSON.plain.stringify(OpticNode.serializer().list, successfulNodes)
    outputTreePath.writeText(processedTree)
}

@Serializable
class OpticDump(
        val initialState: String,
        val expansionsPerSecond: Double,
        val nodes: List<JsonOpticNode>
)

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
                     var minGoalDistance: Int? = null,
                     var firstGoalLatestStartTime: Double? = null,
                     var expansionTime: Int = 0,
                     var generationTime: Int = 0,
                     var latestStartTimesForGoals: List<Double>? = null)


fun aStar(nodes: List<OpticNode>, sourceNode: OpticNode, rootNodeId: Long) {
    data class Node(val opticNode: OpticNode, var closed: Boolean = false, override var index: Int = -1) : Indexable

    // Add
    val localNodeMap = nodes.associate { it.id to Node(it) }.toMutableMap()
    nodes.forEach { opticNode -> opticNode.duplicates.forEach { duplicate -> localNodeMap[duplicate] = localNodeMap[opticNode.id]!! } }

    val openList = AdvancedPriorityQueue<Node>(arrayOfNulls(nodes.size), compareBy {
        it.opticNode.gValue + (it.opticNode.distanceToGo?.times(5) ?: Int.MAX_VALUE)
    })

    openList.add(localNodeMap[sourceNode.id] ?: throw MetronomeException("Source node is not found"))

    var expansionCount = 0
    var generationCount = 0
    val expansionsToGoals = mutableListOf<Int>()
    val latestStartTimesForGoals = mutableListOf<Double>()

    while (openList.isNotEmpty()) {
        val currentNode = openList.pop() ?: throw GoalNotReachableException("Open list is empty.")

        expansionCount++
        if (sourceNode.id == rootNodeId)
            currentNode.opticNode.expansionTime = expansionCount

        for (successorId in currentNode.opticNode.successors) {
            val successorNode = localNodeMap[successorId]
                    ?: throw MetronomeException("Invalid NodeId 0x${successorId.toString(16)}")

            if (successorNode.closed) {
                continue
            }

            if (successorNode.opticNode.tag == NodeTag.goal) {
                expansionsToGoals.add(expansionCount)
                latestStartTimesForGoals.add(successorNode.opticNode.latestStartTimeUB
                        ?: throw MetronomeException("Unknown latest start time for goal"))
            }

            successorNode.closed = true
            successorNode.opticNode.duplicates.forEach { localNodeMap[it]?.closed = true }
            // Cost of duplicates? Could it be better than the original?

            if (successorNode.open) {
                println("\n This should not happen! ***")
                openList.update(successorNode)
            } else {
                generationCount++
                if (sourceNode.id == rootNodeId)
                    successorNode.opticNode.generationTime = generationCount

                openList.add(successorNode)
            }
        }
    }

    sourceNode.expansionsToGoals = expansionsToGoals
    sourceNode.latestStartTimesForGoals = latestStartTimesForGoals
}

fun checkInvariant(nodes: List<OpticNode>) {
    data class Node(val opticNode: OpticNode)

    val localNodeMap = nodes.associate { it.id to Node(it) }.toMutableMap()
    nodes.forEach { opticNode -> opticNode.duplicates.forEach { duplicate -> localNodeMap[duplicate] = localNodeMap[opticNode.id]!! } }


    // Check invariant
    nodes
            .parallelStream()
            .forEach { sourceNode ->
                val nonMonotonic = sourceNode.successors.any {
                    val successorLatestStartTime = localNodeMap[it]!!.opticNode.latestStartTimeUB ?: Double.MAX_VALUE
                    val sourceLatestStartTime = sourceNode?.latestStartTimeUB ?: Double.MIN_VALUE

                    successorLatestStartTime > sourceLatestStartTime
                }

                if (nonMonotonic) throw MetronomeException("Invariant violated.")
            }

}


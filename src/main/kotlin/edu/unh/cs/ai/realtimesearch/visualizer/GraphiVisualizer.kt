package edu.unh.cs.ai.realtimesearch.visualizer

import com.github.kittinunf.fuel.Fuel
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.planner.RealTimeSearchNode
import edu.unh.cs.ai.realtimesearch.planner.SafeRealTimeSearchNode
import edu.unh.cs.ai.realtimesearch.planner.SearchNode
import java.nio.charset.Charset
import kotlin.math.min

/**
 * @author Bence Cserna (bence@cserna.net)
 */

class GraphiVisualizer<StateType : State<StateType>, NodeType : SearchNode<StateType, NodeType>> {
    private val visualizedNodes: MutableSet<SearchNode<StateType, NodeType>> = mutableSetOf()
    private val visualizedEdges: MutableSet<Pair<SearchNode<StateType, NodeType>, SearchNode<StateType, NodeType>>> = mutableSetOf()

    fun clear() {
        val deleteNodeOperations = visualizedNodes.map { deleteNode(it) }
        val deleteEdgeOperations = visualizedEdges.map { deleteEdge(it) }

        publishOperations(deleteEdgeOperations + deleteNodeOperations)

        visualizedNodes.clear()
        visualizedEdges.clear()
    }

    fun visualizeNodes(nodes: Collection<SearchNode<StateType, NodeType>>, planNodes: List<SearchNode<StateType, NodeType>>) {
        val nodesInPlan = planNodes.toSet()

        val nodeOperations = nodes.map { visualizeNode(it, nodesInPlan) }
//        val edgeOperations = nodes.map { visualizeEdge(it.parent, it) }
        val edgeOperations = nodes.flatMap { target -> target.predecessors.map { visualizeEdge(it.node, target, nodesInPlan) } }
        publishOperations(nodeOperations + edgeOperations)
    }

    private fun visualizeNode(node: SearchNode<StateType, NodeType>, nodesInPlan: Set<SearchNode<StateType, NodeType>>): String {
        val operation = if (visualizedNodes.contains(node)) "cn" else "an"
        val nodeId = node.id
        visualizedNodes.add(node)

        var extras = ""
        var group = ""
        if (node is SafeRealTimeSearchNode) {
            extras += ",\"safe\": ${node.safe}"
            if (node.safe) group += "-safe"
            if (node.open) group += "-open"

        }

        if (nodesInPlan.contains(node)) group += "-plan"
        if (node is RealTimeSearchNode) {
            extras += ",\"iteration\": ${node.iteration},\"localDepth\": ${node.localDepth}"

            extras += ",\"y\":${min(node.f, 100000.0)}"
            extras += ",\"x\":${node.localDepth * 100}"
        }
        if (group.isNotBlank()) extras += ",\"group\": \"$group\""

//        extras += ",\"size\":${(5000.0 / node.f).toInt()}"
        extras += ",\"f\":${min(node.f, 100000.0)}"
        return "{\"$operation\":{ \"$nodeId\": {\"label\":${node.f},\"size\": 10 $extras}}}"
    }

    private fun visualizeEdge(sourceNode: SearchNode<StateType, NodeType>, targetNode: SearchNode<StateType, NodeType>, nodesInPlan: Set<SearchNode<StateType, NodeType>>): String {
        val edge = sourceNode to targetNode
        val operation = if (visualizedEdges.contains(edge)) "ce" else "ae"
        val edgeId = "${sourceNode.id}_${targetNode.id}"

        visualizedEdges.add(edge)

        return "{\"$operation\":{ \"$edgeId\": {" +
                "\"source\":\"${sourceNode.id}\"," +
                "\"target\":\"${targetNode.id}\"," +
                "\"label\":\"\"," +
                "\"directed\":true}}}"
    }


    private fun deleteNode(node: SearchNode<StateType, NodeType>): String {
        return "{\"dn\":{ \"${node.id}\": {}}}"
    }

    private fun deleteEdge(edge: Pair<SearchNode<StateType, NodeType>, SearchNode<StateType, NodeType>>): String {
        val edgeId = "${edge.first.id}_${edge.second.id}"

        return "{\"de\":{ \"$edgeId\": {}}}"
    }

    private fun publishOperations(operations: Collection<String>) {
        val body = operations.joinToString("\n\r")

        val (request, response, result1) = Fuel
                .post("http://localhost:5000/workspace1", parameters = listOf("operation" to "updateGraph"))
                .body(body = body, charset = Charset.forName("UTF-8"))
                .response()

    }
}

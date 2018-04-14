package edu.unh.cs.ai.realtimesearch.visualizer

import com.github.kittinunf.fuel.Fuel
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.planner.RealTimeSearchNode
import edu.unh.cs.ai.realtimesearch.planner.SearchNode
import java.nio.charset.Charset

/**
 * @author Bence Cserna (bence@cserna.net)
 */

class GraphiVisualizer<StateType : State<StateType>, NodeType : SearchNode<StateType, NodeType>> {
    val visualizedNodes: MutableSet<SearchNode<StateType, NodeType>> = mutableSetOf()
    val visualizedEdges: MutableSet<Pair<SearchNode<StateType, NodeType>, SearchNode<StateType, NodeType>>> = mutableSetOf()

    fun visualizeNodes(nodes: Collection<SearchNode<StateType, NodeType>>) {
        val nodeOperations = nodes.map { visualizeNode(it) }
//        val edgeOperations = nodes.map { visualizeEdge(it.parent, it) }
        val edgeOperations = nodes.flatMap { target -> target.predecessors.map { visualizeEdge(it.node, target) } }
        publishGraph(nodeOperations + edgeOperations)
    }

    private fun visualizeNode(node: SearchNode<StateType, NodeType>): String {
        val operation = if (visualizedNodes.contains(node)) "cn" else "an"
        val nodeId = node.id
        visualizedNodes.add(node)

        val extras = if (node is RealTimeSearchNode) {
            ",\"iteration\": ${node.iteration}"
        } else ""

        return "{\"$operation\":{ \"$nodeId\": {\"label\":${node.f} $extras}}}"
    }

    private fun visualizeEdge(sourceNode: SearchNode<StateType, NodeType>, targetNode: SearchNode<StateType, NodeType>): String {
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

    private fun publishGraph(operations: Collection<String>) {
        val body = operations.joinToString("\n\r")

        val (request, response, result1) = Fuel
                .post("http://localhost:5000/workspace1", parameters = listOf("operation" to "updateGraph"))
                .body(body = body, charset = Charset.forName("UTF-8"))
                .response()

    }
}

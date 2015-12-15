package edu.unh.cs.ai.realtimesearch.planner.anytime

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import java.util.*

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class AnytimeReparingAStar(val domain: Domain, var inflationFactor: Double) {

    private val openList: Queue<Node> = PriorityQueue()
    private val closedList: MutableMap<State, Node> = hashMapOf()
    private val inconsistentNodes: MutableList<Node> = arrayListOf()

    public var generatedNodes = 0
    public var expandedNodes = 0

    data class Node(val parent: Node? = null, val state: State,
                    val action: Action? = null, val cost: Double = 0.0)

    private fun fValue(state: State): Double {
        return 0.0
    }

    private fun improvePath() {
        // This is analogue to Likhachev's CLOSED list
        val localClosedList: MutableSet<State> = hashSetOf()

        // TODO while loop
        while (true) {
            val nextNode = openList.poll() ?: return // Return if the frontier is empty
            localClosedList.add(nextNode.state)
            domain.successors(nextNode.state).forEach {
                val successorNode = closedList[it.state] // TODO Please validate this

                if (successorNode == null || successorNode.cost > nextNode.cost + it.actionCost) {
                    val updatedSuccessorNode = Node(nextNode, it.state, it.action, nextNode.cost + it.actionCost)
                    closedList[it.state] = updatedSuccessorNode

                    if (localClosedList.contains(it.state)) {
                        inconsistentNodes.add(updatedSuccessorNode)
                    } else {
                        openList.add(updatedSuccessorNode)
                    }
                }
            }
        }
    }

    private fun solve(startState: State) {
        openList.add(Node(state = startState))
        improvePath()

        // calculate e

        while (inflationFactor > 1) {
            // Decrease inflation factor

            // move states from inconsistent to open
            openList.addAll(inconsistentNodes)
            inconsistentNodes.clear()

            // Update all priorities in the open list

            // Clear closed

            improvePath()

            // calculate e

        }
    }

}
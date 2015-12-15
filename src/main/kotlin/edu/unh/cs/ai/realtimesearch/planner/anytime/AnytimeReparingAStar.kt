package edu.unh.cs.ai.realtimesearch.planner.anytime

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import java.util.*

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class AnytimeReparingAStar(val domain: Domain, var inflationFactor: Double) {

    private val openList: Queue<State> = PriorityQueue(compareBy {
        val node = closedList[it]!!
        node.cost + inflationFactor * node.heuristic
    })
    private val closedList: MutableMap<State, Node> = hashMapOf()
    private val inconsistentNodes: MutableList<Node> = arrayListOf()

    public var generatedNodes = 0
    public var expandedNodes = 0

    data class Node(val parent: Node? = null, val state: State, val action: Action? = null, val cost: Double = 0.0, val heuristic: Double = 0.0)

    private fun improvePath() {
        // This is analogue to Likhachev's CLOSED list
        val localClosedList: MutableSet<State> = hashSetOf()

        // TODO while loop
        while (true) {
            val currentState = openList.poll() ?: return // Return if the frontier is empty
            val currentNode = closedList[currentState]!!

            localClosedList.add(currentState)

            domain.successors(currentState).forEach {
                val successorNode = closedList[it.state]

                if (successorNode == null || successorNode.cost > currentNode.cost + it.actionCost) {
                    val updatedSuccessorNode = Node(currentNode, it.state, it.action, currentNode.cost + it.actionCost)
                    closedList[it.state] = updatedSuccessorNode

                    if (localClosedList.contains(it.state)) {
                        inconsistentNodes.add(updatedSuccessorNode)
                    } else {
                        openList.add(it.state)
                    }
                }
            }
        }
    }

    private fun solve(startState: State) {
        closedList[startState] = Node(state = startState, heuristic = domain.heuristic(startState))
        openList.add(startState)
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
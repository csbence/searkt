package edu.unh.cs.ai.realtimesearch.planner.anytime

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import org.slf4j.LoggerFactory
import java.lang.Math.min
import java.util.*

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class AnytimeReparingAStar(val domain: Domain, var inflationFactor: Double) {

    private val logger = LoggerFactory.getLogger(AnytimeReparingAStar::class.java)

    private val openList: Queue<State> = PriorityQueue(compareBy { inflatedFValue(it) })
    private val closedList: MutableMap<State, Node> = hashMapOf()
    private val inconsistentStates: MutableList<State> = arrayListOf()
    private val goal: State? = null

    public var generatedNodes = 0
    public var expandedNodes = 0

    data class Node(val parent: Node? = null, val state: State, val action: Action? = null, val cost: Double = 0.0, val heuristic: Double = 0.0)

    private fun improvePath() {
        // This is analogue to Likhachev's CLOSED list
        val localClosedList: MutableSet<State> = hashSetOf()

        while (goalCost() > inflatedFValue(openList.element())) {
            val currentState = openList.poll() ?: return // Return if the frontier is empty
            val currentNode = closedList[currentState]!!

            localClosedList.add(currentState)

            domain.successors(currentState).forEach {
                val successorNode = closedList[it.state]

                if (successorNode == null || successorNode.cost > currentNode.cost + it.actionCost) {
                    val updatedSuccessorNode = Node(currentNode, it.state, it.action, currentNode.cost + it.actionCost)
                    closedList[it.state] = updatedSuccessorNode

                    if (localClosedList.contains(it.state)) {
                        inconsistentStates.add(it.state)
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
        assert(inconsistentStates.isEmpty())
        updateInflationFactor()

        while (inflationFactor > 1) {
            // 08 Decrease inflation factor ?

            // 09 Move states from inconsistent to open
            // 10 Update all priorities in the open list
            openList.addAll(inconsistentStates)
            inconsistentStates.clear()
            closedList.clear()

            improvePath()
            updateInflationFactor()
        }
    }

    private fun updateInflationFactor() {
        val minimalInconsistentState = inconsistentStates.minBy { fValue(it) }
        val minimalInconsistentFValue = if (minimalInconsistentState != null) fValue(minimalInconsistentState) else Double.POSITIVE_INFINITY
        val minimalOpenStateFValue = fValue(openList.element())

        inflationFactor = goalCost() / min(minimalInconsistentFValue, minimalOpenStateFValue)
    }

    private fun fValue(state: State): Double {
        val node = closedList[state]!!
        return node.cost + node.heuristic
    }

    private fun inflatedFValue(state: State): Double {
        val node = closedList[state]!!
        return node.cost + inflationFactor * node.heuristic
    }

    private fun goalCost(): Double {
        goal ?: return Double.POSITIVE_INFINITY
        return closedList[goal]!!.cost
    }

}
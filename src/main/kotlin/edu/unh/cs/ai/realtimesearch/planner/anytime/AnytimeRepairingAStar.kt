package edu.unh.cs.ai.realtimesearch.planner.anytime

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import org.slf4j.LoggerFactory
import java.lang.Math.min
import java.util.*

class AnytimeRepairingAStar<StateType : State<StateType>>(val domain: Domain<StateType>, var inflationFactor: Double) {

    private val logger = LoggerFactory.getLogger(AnytimeRepairingAStar::class.java)

    private val openList: Queue<StateType> = PriorityQueue(compareBy { inflatedFValue(it) })
    private val closedList: MutableMap<StateType, Node<StateType>> = hashMapOf()
    private val inconsistentStates: MutableList<StateType> = arrayListOf()
    private var goal: StateType? = null

    public var generatedNodes = 0
    public var expandedNodes = 0

    data class Node<State>(val parent: Node<State>? = null, val state: State, val action: Action? = null, val cost: Double = 0.0, val heuristic: Double = 0.0)

    private fun improvePath() {
        // This is analogue to Likhachev's CLOSED list
        val localClosedList: MutableSet<StateType> = hashSetOf()

        while (goalCost() > inflatedFValue(openList.element())) {
            val currentState = openList.poll() ?: return // Return if the frontier is empty
            val currentNode = closedList[currentState]!!

            localClosedList.add(currentState)

            domain.successors(currentState).forEach {
                val successorNode = closedList[it.state]

                if (successorNode == null || successorNode.cost > currentNode.cost + it.actionCost) {
                    val updatedSuccessorNode = Node(currentNode, it.state, it.action, currentNode.cost + it.actionCost)
                    closedList[it.state] = updatedSuccessorNode

                    if (domain.isGoal(it.state)) {
                        goal = it.state
                    }

                    if (localClosedList.contains(it.state)) {
                        inconsistentStates.add(it.state)
                    } else {
                        openList.add(it.state)
                    }
                }
            }
        }
    }

    public fun solve(startState: StateType) {
        closedList[startState] = Node(state = startState, heuristic = domain.heuristic(startState))
        openList.add(startState)
        improvePath()

        // calculate e
        assert(inconsistentStates.isEmpty())
        updateInflationFactor()

        while (inflationFactor > 1) {
            // 08 Decrease inflation factor ?
            inflationFactor /= 1.2

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

    private fun fValue(state: StateType): Double {
        val node = closedList[state]!!
        return node.cost + node.heuristic
    }

    private fun inflatedFValue(state: StateType): Double {
        val node = closedList[state]!!
        return node.cost + inflationFactor * node.heuristic
    }

    private fun goalCost(): Double {
        goal ?: return Double.POSITIVE_INFINITY
        return closedList[goal!!]!!.cost
    }
}
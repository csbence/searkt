package edu.unh.cs.ai.realtimesearch.planner.anytime

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.planner.AnytimePlanner
import org.slf4j.LoggerFactory
import java.lang.Math.min
import java.util.*
import kotlin.comparisons.compareBy

class AnytimeRepairingAStar<StateType : State<StateType>>(domain: Domain<StateType>/*, var inflationFactor: Double*/) : AnytimePlanner<StateType>(domain) {

    private val logger = LoggerFactory.getLogger(AnytimeRepairingAStar::class.java)

    private var inflationFactor = 3.0
    private val openList: Queue<Node<StateType>> = PriorityQueue(compareBy<Node<StateType>> { inflatedFValue(it) })
    private val closedList: MutableMap<StateType, Node<StateType>> = hashMapOf()
    private val inconsistentStates: MutableList<Node<StateType>> = arrayListOf()
    private var goal: StateType? = null
    private var targetgoal: StateType? = null
    private var goalNode: Node<StateType>? = null

    public var generatedNodes = 0
    public var expandedNodes = 0

    data class Node<State>(val parent: Node<State>? = null, val state: State, val action: Action? = null, val cost: Double = 0.0, val heuristic: Double = 0.0)


    private fun improvePath() {
        // This is analogue to Likhachev's CLOSED list
        val localClosedList: MutableSet<StateType> = hashSetOf()

        //println("here " + openList.element())
        while (goalCost() > inflatedFValue(openList.element())) {
            val currentNode = openList.poll() ?: return // Return if the frontier is empty
            val currentState = currentNode.state//closedList[currentState]!!

            localClosedList.add(currentState)

            domain.predecessors(currentState).forEach {
                val predecessorNode = closedList[it.state]
                //println(predecessorNode);

                if (predecessorNode == null || predecessorNode.cost > currentNode.cost + it.actionCost) {
                    val updatedSuccessorNode = Node(currentNode, it.state, it.action, currentNode.cost + it.actionCost)
                    closedList[it.state] = updatedSuccessorNode

                    if (targetgoal!!.equals(it.state)) {
                        goal = it.state
                        goalNode = updatedSuccessorNode
                        //println("clist: "  + closedList[it.state])
                        //println("here")
                    }

                    if (localClosedList.contains(it.state)) {
                        //println("here3")
                        inconsistentStates.add(updatedSuccessorNode)
                    } else {
                        //println("here2")
                        openList.add(updatedSuccessorNode)
                    }
                }
            }
        }
    }

    fun solve(startState: StateType, goalState: StateType) : MutableList<Node<StateType>>{
        //Solving backwards, so flip start and goal states

        //println( " solve ")
        targetgoal = startState
        closedList[goalState] = Node(state = goalState, heuristic = domain.heuristic(goalState, startState))

        //println( " ADD ")
        openList.add(closedList[goalState])
        //println( " added ")
        improvePath()
        //println("made it out")
        // calculate e
        //assert(inconsistentStates.isEmpty())

        val result: MutableList<Node<StateType>> = arrayListOf()
        var cur = goalNode
        while(cur != null){
            //print("" + cur.state + " " + cur.action + " ")
            //print("" + cur.action + " ")
            result.add(cur)
            cur = cur.parent
        }
        //println()

        return result//.asReversed()

        /*updateInflationFactor()

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
        }*/
    }

    fun update() : Double{
        //println( " update ")
        inflationFactor *= 10
        inflationFactor -= 2
        inflationFactor /= 10
        if (inflationFactor < 1)
                return inflationFactor;
        //println( " add all ")
        val tempOpen = openList.toMutableList()
        openList.clear()
        openList.addAll(tempOpen)
        openList.addAll(inconsistentStates)
        //println( " added all ")
        inconsistentStates.clear()
        closedList.clear()
        goal = null
        return inflationFactor;
    }

    private fun updateInflationFactor() {
        val minimalInconsistentState = inconsistentStates.minBy { fValue(it.state) }
        val minimalInconsistentFValue = if (minimalInconsistentState != null) fValue(minimalInconsistentState.state) else Double.POSITIVE_INFINITY
        val minimalOpenStateFValue = fValue(openList.element().state)

        inflationFactor = goalCost() / min(minimalInconsistentFValue, minimalOpenStateFValue)
    }

    private fun fValue(state: StateType): Double {
        val node = closedList[state]!!
        return node.cost + node.heuristic
    }

    private fun inflatedFValue(node: Node<StateType>): Double {
        //println("node: " + node)
        //val node = closedList[state]!!

        //println(node)
        //println("i: " + (node.cost + inflationFactor * node.heuristic))
        return node.cost + inflationFactor * node.heuristic
    }

    private fun goalCost(): Double {
        //println("goal" + goal)
        goal ?: return Double.POSITIVE_INFINITY
        //println("g: " + closedList[goal!!]!!.cost)
        return closedList[goal!!]!!.cost
    }
}
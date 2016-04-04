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
    private val allNodes: MutableMap<StateType, Node<StateType>> = hashMapOf()
    private var iterationCount = 0;

    data class Node<State>(var parent: Node<State>? = null, val state: State, var action: Action? = null, var cost: Double = 0.0, var iteration: Int/*, val heuristic: Double = 0.0*/)


    private fun improvePath() {
        // This is analogue to Likhachev's CLOSED list
        val localClosedList: MutableSet<StateType> = hashSetOf()

        //println("here " + openList.element())
        while (goalCost() > inflatedFValue(openList.element())) {
//            println("loop: " + goalCost() + " " + inflatedFValue(openList.element()))
            val currentNode = openList.poll() ?: return // Return if the frontier is empty
            val currentState = currentNode.state//closedList[currentState]!!

            localClosedList.add(currentState)
            expandedNodeCount++;

            domain.predecessors(currentState).forEach {
                val predecessorNode = closedList[it.state]
//                println("" + it.state + " " + it.state.hashCode() + " " + predecessorNode);

                if (predecessorNode == null || predecessorNode.cost > currentNode.cost + it.actionCost) {
                    generatedNodeCount++
                    var updatedSuccessorNode = allNodes[it.state]
                    if(updatedSuccessorNode == null){
                        updatedSuccessorNode = Node(currentNode, it.state, it.action, currentNode.cost + it.actionCost, iterationCount)
                        allNodes[it.state] = updatedSuccessorNode
//                        println("" + it.state + " " + it.state.hashCode() + " " + updatedSuccessorNode.state);
                    }
                    else if(updatedSuccessorNode.iteration == iterationCount
                            || (updatedSuccessorNode.iteration < iterationCount && updatedSuccessorNode.cost > currentNode.cost + it.actionCost)){
                        updatedSuccessorNode.action = it.action
                        updatedSuccessorNode.parent = currentNode
                        updatedSuccessorNode.cost = currentNode.cost + it.actionCost
                        updatedSuccessorNode.iteration = iterationCount
                        allNodes[it.state] = updatedSuccessorNode
//                        println("" + it.state + " " + it.state.hashCode() + " " + updatedSuccessorNode.state);
                    }

                    closedList[it.state] = updatedSuccessorNode

//                    println(targetgoal)
                    if (targetgoal!!.equals(it.state)) {
                        goal = it.state
                        goalNode = updatedSuccessorNode
                        //println("clist: "  + closedList[it.state])
//                        println("here")
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

    fun solve(startState: StateType, goalState: StateType): MutableList<Action?> {
        //Solving backwards, so flip start and goal states

//        println( " solve ")
        targetgoal = startState
        var tempNode = allNodes[goalState]

        if(tempNode == null) {
            tempNode = Node(state = goalState/*, heuristic = domain.heuristic(goalState, startState)*/, iteration = iterationCount)
            allNodes[goalState] = tempNode
        }
        else{
            tempNode.parent = null
            tempNode.action = null
            tempNode.cost = 0.0
            tempNode.iteration = iterationCount
            allNodes[goalState] = tempNode
        }

        closedList[goalState] = tempNode
        //println( " ADD ")
        openList.add(closedList[goalState])
        //println( " added ")
        improvePath()
        //println("made it out")
        // calculate e
        //assert(inconsistentStates.isEmpty())

        iterationCount++
        val result: MutableList<Action?> = arrayListOf()
        var cur = goalNode
        while (cur != null) {
//            println("looping")
//            print("" + cur.state + " " + cur.action + " ")
//            print("" + cur.action + " ")
            result.add(cur!!.action)
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

    fun update(): Double {
        //println( " update ")
        inflationFactor *= 100
        inflationFactor -= 2
        inflationFactor /= 100

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
        return node.cost + domain.heuristic(node.state, targetgoal!!)
    }

    private fun inflatedFValue(node: Node<StateType>): Double {
        //println("node: " + node)
        //val node = closedList[state]!!

        //println(node)
        //println("i: " + (node.cost + inflationFactor * node.heuristic))
        return node.cost + inflationFactor * domain.heuristic(node.state, targetgoal!!)
    }

    private fun goalCost(): Double {
        //println("goal" + goal)
        goal ?: return Double.POSITIVE_INFINITY
        //println("g: " + closedList[goal!!]!!.cost)
        return closedList[goal!!]!!.cost
    }
}
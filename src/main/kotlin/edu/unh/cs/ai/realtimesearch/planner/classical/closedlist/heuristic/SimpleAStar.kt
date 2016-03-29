package edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import java.util.*

/**
 * Simple fast A* implementation based on Matthew Hatem's code.
 */
class SimpleAStar<StateType : State<StateType>>(val domain: Domain<StateType>) {

    private val closed: MutableMap<StateType, Node> = HashMap<StateType, Node>()

    private val open = PriorityQueue(3) { lhs: Node, rhs: Node ->
        if (lhs.f == rhs.f) {
            when {
                lhs.g > rhs.g -> -1
                lhs.g < rhs.g -> 1
                else -> 0
            }
        } else {
            when {
                lhs.f < rhs.f -> -1
                lhs.f > rhs.f -> 1
                else -> 0
            }
        }
    }

    private val path = ArrayList<StateType>(3)
    private var expanded: Long = 0
    private var generated: Long = 0

    //    private var timestamp = 0L

    fun search(init: StateType) {
        val initNode = Node(init, null, 0)
        open.add(initNode)
        while (!open.isEmpty()) {
            val currentNode = open.poll()
            if (closed.containsKey(currentNode.state)) {
                continue
            }
            if (domain.isGoal(currentNode.state)) {
                var p: Node? = currentNode
                while (p != null) {
                    path.add(p.state)
                    p = p.parent
                }
                break
            }
            closed.put(currentNode.state, currentNode)
            expanded++
            domain.successors(currentNode.state).forEach {
                generated++
                open.add(Node(state = it.state, parent = currentNode, cost = it.actionCost.toInt()))
            }

            //            if (expanded % 100000 == 0L) {
            //                println(System.currentTimeMillis() - timestamp)
            //                timestamp = System.currentTimeMillis()
            //            }
        }
    }

    /**
     * The node class
     */
    private inner class Node(internal val state: StateType, internal val parent: Node?, cost: Int) {
        internal val f: Int
        internal val g: Int

        init {
            this.g = if (parent != null) parent.g + cost else cost
            this.f = g + domain.heuristic(state).toInt()
        }
    }

}

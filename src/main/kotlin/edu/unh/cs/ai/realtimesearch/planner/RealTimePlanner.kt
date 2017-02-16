package edu.unh.cs.ai.realtimesearch.planner

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.realtime.SearchNode

/**
 * A planner for real time search environments, where a constraint is placed
 * on the the amount of time allowed to plan. A RTS planner requires to return
 * a action within a certain time limit
 *
 * @param domain: The domain to plan in
 */
abstract class RealTimePlanner<StateType : State<StateType>>(protected val domain: Domain<StateType>) : Planner<StateType>() {
    /**
     * Data class to store [Action]s along with their execution time.
     *
     * The [duration] is measured in nanoseconds.
     */
    data class ActionBundle(val action: Action, val duration: Long)

    /**
     * Returns an action while abiding the termination checker's criteria.
     *
     * @param sourceState is the sourceState to pick an action for
     * @param terminationChecker provides the termination criteria
     * @return an action for current sourceState
     */
    abstract fun selectAction(sourceState: StateType, terminationChecker: TerminationChecker): List<ActionBundle>

    /**
     * Called before the first [selectAction] call.
     *
     * This call does not count towards the planning time.
     */
    open fun init() {

    }

}

/**
 * Extracts an action sequence that leads from the start state to the target state.
 * The path follows the parent pointers from the target to the start in reversed order.
 *
 * @return path from source to target if exists.
 */
fun <StateType : State<StateType>, NodeType : SearchNode<StateType, NodeType>> extractSourctToTargetPath(targetNode: NodeType?, sourceState: StateType): List<RealTimePlanner.ActionBundle> {
    targetNode ?: return emptyList()

    val actions = ArrayList<RealTimePlanner.ActionBundle>(1000)
    var currentNode: NodeType = targetNode

    if (targetNode.state == sourceState) {
        return emptyList()
    }

    // keep on pushing actions to our queue until source state (our root) is reached
    do {
        actions.add(RealTimePlanner.ActionBundle(currentNode.action, currentNode.actionCost))
        currentNode = currentNode.parent
    } while (currentNode.state != sourceState)

    return actions.reversed()
}

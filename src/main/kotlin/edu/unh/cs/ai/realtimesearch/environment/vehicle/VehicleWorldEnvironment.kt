package edu.unh.cs.ai.realtimesearch.environment.vehicle

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Environment
import edu.unh.cs.ai.realtimesearch.logging.trace
import org.slf4j.LoggerFactory

/**
 * Vehicle environment ? contains domain and current state
 *
 * Created by doylew on 1/17/17.
 */
class VehicleWorldEnvironment(private val domain: VehicleWorld, private val initialWorldState: VehicleWorldState) : Environment<VehicleWorldState> {

    private val logger = LoggerFactory.getLogger(VehicleWorldEnvironment::class.java)
    private var currentState = initialWorldState

    override fun step(action: Action) {
        val successorBundles = domain.successors(currentState)

        // acquire the state by filtering ... should probably change TODO
        currentState = successorBundles.first { it.action == action }.state
        logger.trace { "Action $action leads to state $currentState" }
    }

    override fun getState() = currentState
    override fun getGoal() = domain.getGoal()

    override fun isGoal(): Boolean {
        val goal = domain.isGoal(currentState)
        logger.trace { "State $currentState is ${if (goal) "" else "not"} a goal" }
        return goal
    }

    override fun reset() {
        currentState = initialWorldState
    }

}
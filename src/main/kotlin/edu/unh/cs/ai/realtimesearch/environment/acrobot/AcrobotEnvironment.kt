package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Environment
import edu.unh.cs.ai.realtimesearch.logging.trace
import org.slf4j.LoggerFactory


class AcrobotEnvironment(private val domain: Acrobot, private val initialState: AcrobotState) : Environment<AcrobotState> {
    override fun step(action: Action) {
        throw UnsupportedOperationException()
    }

    override fun getState(): AcrobotState {
        throw UnsupportedOperationException()
    }

    override fun isGoal(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun reset() {
        throw UnsupportedOperationException()
    }
}
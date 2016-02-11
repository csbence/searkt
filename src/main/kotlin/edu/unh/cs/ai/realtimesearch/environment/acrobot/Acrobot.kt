package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import edu.unh.cs.ai.realtimesearch.environment.acrobot.AcrobotAction
import org.slf4j.LoggerFactory


class Acrobot() : Domain<AcrobotState> {
    override fun successors(state: AcrobotState): List<SuccessorBundle<AcrobotState>> {
        throw UnsupportedOperationException()
    }

    override fun heuristic(state: AcrobotState): Double {
        throw UnsupportedOperationException()
    }

    override fun distance(state: AcrobotState): Double {
        throw UnsupportedOperationException()
    }

    override fun isGoal(state: AcrobotState): Boolean {
        throw UnsupportedOperationException()
    }

    override fun print(state: AcrobotState): String {
        throw UnsupportedOperationException()
    }

    override fun randomState(): AcrobotState {
        throw UnsupportedOperationException()
    }

}


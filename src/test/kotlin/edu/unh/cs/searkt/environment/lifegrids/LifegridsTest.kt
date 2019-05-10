package edu.unh.cs.searkt.environment.lifegrids

import edu.unh.cs.searkt.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.searkt.experiment.configuration.realtime.TerminationType
import edu.unh.cs.searkt.experiment.terminationCheckers.StaticExpansionTerminationChecker
import edu.unh.cs.searkt.planner.suboptimal.WeightedAStar
import org.junit.Test
import java.io.File

class LifegridsTest {

    private val dummyConfiguration = ExperimentConfiguration(domainName = "LIFEGRIDS", algorithmName = "WEIGHTED_A_STAR",
            terminationType = TerminationType.EXPANSION, actionDuration = 1L, timeLimit = 1000L,
            expansionLimit = 5000000L,
            weight = 1.0)

    private val lifeGrid = LifegridsIO.parseFromStream(File("src/main/resources/input/lifegrids/lifegrids0.lg").inputStream(), 1L)

    @Test
    fun aStarLifegrids() {
        val weightedAStar = WeightedAStar(lifeGrid.domain, dummyConfiguration)
        weightedAStar.plan(lifeGrid.initialState, StaticExpansionTerminationChecker(dummyConfiguration.expansionLimit!!))
    }
}


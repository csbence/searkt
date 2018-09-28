package edu.unh.cs.ai.realtimesearch.planner.suboptimal

import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleIO
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.StaticExpansionTerminationChecker
import edu.unh.cs.ai.realtimesearch.util.convertNanoUpDouble
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

class ExplicitEstimationSearchVacuumTest {
    private val configuration = ExperimentConfiguration(domainName = "SLIDING_TILE_4", algorithmName = "EES",
            terminationType = TerminationType.EXPANSION, actionDuration = 1L, timeLimit = 1000L, expansionLimit = 100000000L,
            errorModel = "path", weight = 2.0)

    @Test
    fun testEESVacuum() {
        val file = "/home/aifs2/doylew/IdeaProjects/real-time-search/src/main/resources/input/tiles/korf/4/real/1"
        println("File -> $file")
        val instance = File(file).inputStream()
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, actionDuration = configuration.actionDuration)
        val initialState = slidingTilePuzzle.initialState
        val tsAgent = ExplicitEstimationSearchH(slidingTilePuzzle.domain, configuration)
        val plan = tsAgent.plan(initialState, StaticExpansionTerminationChecker(configuration.expansionLimit))
        println("timeTaken:${convertNanoUpDouble(tsAgent.executionNanoTime, TimeUnit.SECONDS)}")
        println("expansions:${tsAgent.expandedNodeCount}")
        println("costOfSolution:${plan.size}")
        println("solution: $plan")
        println()
//        println("dHatExpansions:${tsAgent.dHatExpansions}")
//        println("fHatExpansions:${tsAgent.fHatExpansions}")
//        println("aStarExpansions:${tsAgent.aStarExpansions}")
    }
}
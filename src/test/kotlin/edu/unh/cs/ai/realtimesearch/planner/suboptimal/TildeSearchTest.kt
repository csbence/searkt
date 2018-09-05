package edu.unh.cs.ai.realtimesearch.planner.suboptimal

import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldIO
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.StaticExpansionTerminationChecker
import edu.unh.cs.ai.realtimesearch.util.convertNanoUpDouble
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

class TildeSearchTest {

    private val configuration = ExperimentConfiguration(domainName = "VACUUM_WORLD", algorithmName = "TS",
            terminationType = TerminationType.EXPANSION, actionDuration = 1L, timeLimit = 1000L,
            expansionLimit = 1000000L, weight = 1.1, errorModel = "path")

    @Test
    fun testTS1() {
        val file = "/home/aifs2/doylew/IdeaProjects/real-time-search/src/main/resources/input/vacuum/gen/vacuum49.vw"
        val instance = File(file).inputStream()
        val slidingTilePuzzle = VacuumWorldIO.parseFromStream(instance)
        val initialState = slidingTilePuzzle.initialState
        val tsAgent = TildeSearch(slidingTilePuzzle.domain, configuration)
        val plan = tsAgent.plan(initialState, StaticExpansionTerminationChecker(1000000L))
        println("timeTaken:${convertNanoUpDouble(tsAgent.executionNanoTime, TimeUnit.SECONDS)}")
        println("expansions:${tsAgent.expandedNodeCount}")
        println("costOfSolution:${plan.size}")
    }
}
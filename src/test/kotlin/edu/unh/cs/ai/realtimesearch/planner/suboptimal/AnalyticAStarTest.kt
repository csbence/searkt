package edu.unh.cs.ai.realtimesearch.planner.suboptimal

import edu.unh.cs.ai.realtimesearch.environment.gridworld.GridWorldIO
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.StaticExpansionTerminationChecker
import org.junit.Test
import java.io.File

class AnalyticAStarTest {
    private val configuration = ExperimentConfiguration(domainName = "SLIDING_TILE_4", algorithmName = "EESO",
            terminationType = TerminationType.EXPANSION, actionDuration = 1L, timeLimit = 1000L, expansionLimit = 100000000L,
            errorModel = "path", weight = 2.5)

    @Test
    fun testAnalyticAStar() {
        val num = "34"
//        val file = "/home/aifs2/doylew/IdeaProjects/real-time-search/src/main/resources/input/tiles/korf/4/real/$num"
//        val file = "/home/aifs2/doylew/IdeaProjects/real-time-search/src/main/resources/input/vacuum/gen/vacuum0.vw"
        val file = "/Users/bencecserna/Documents/Development/projects/ai/real-time-search/src/main/resources/input/gridworld/gridworld0.gw"
//        val file = "/home/aifs2/doylew/uniform40/1k8000/uniform1000_8000-0"
        println("File -> $file")
        val instance = File(file).inputStream()
//        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, actionDuration = configuration.actionDuration)
//        val initialState = slidingTilePuzzle.initialState
        val vacuumWorld = GridWorldIO.parseFromStream(instance, 1L)
        val initialState = vacuumWorld.initialState
//        val tsAgent = TentacleSearch(vacuumWorld.domain, configuration)
//        val tsAgent = ExplicitEstimationSearchH(vacuumWorld.domain, configuration)
//        val tsAgent = ExplicitEstimationSearch(vacuumWorld.domain, configuration)
        val tsAgent = AnalyticAStar(vacuumWorld.domain, configuration)
//        val tsAgent = OptimisticSearch(vacuumWorld.domain, configuration)
        val plan = tsAgent.plan(initialState, StaticExpansionTerminationChecker(configuration.expansionLimit))
        println(num)
        println("expansions:${tsAgent.expandedNodeCount}")
        println("costOfSolution:${plan.size}")
        println("reexpansions:${tsAgent.reexpansions}")
       // println("aStarPrimeExpansions:${tsAgent.aStarPrimeExpansions}")
//        println("dHatExpansions:${tsAgent.dHatExpansions}")
//        println("fHatExpansions:${tsAgent.fHatExpansions}")
//        println("aStarExpansions:${tsAgent.aStarExpansions}")
//        println("tentacleExpansions: ${tsAgent.tentacleExpansions}")
        println()
//        println("dHatExpansions:${tsAgent.dHatExpansions}")
//        println("fHatExpansions:${tsAgent.fHatExpansions}")
//        println("aStarExpansions:${tsAgent.aStarExpansions}")
    }
}

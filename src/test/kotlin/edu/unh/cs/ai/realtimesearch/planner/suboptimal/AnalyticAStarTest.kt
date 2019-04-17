package edu.unh.cs.searkt.planner.suboptimal

import edu.unh.cs.searkt.environment.gridworld.GridWorldIO
import edu.unh.cs.searkt.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.searkt.experiment.configuration.realtime.TerminationType
import edu.unh.cs.searkt.experiment.terminationCheckers.StaticExpansionTerminationChecker
import org.junit.Test
import java.io.File

class AnalyticAStarTest {
    private val configuration = ExperimentConfiguration(domainName = "SLIDING_TILE_4", algorithmName = "EESO",
            terminationType = TerminationType.EXPANSION, actionDuration = 1L, timeLimit = 1000L, expansionLimit = 100000000L,
            errorModel = "path", weight = 2.5, variant = "O")

    @Test
    fun testAnalyticAStar() {
        val num = "7"
//        val file = "/Users/bencecserna/Documents/Development/projects/ai/real-time-search/src/main/resources/input/tiles/korf/4/real/$num"
//        val file = "/home/aifs2/doylew/IdeaProjects/real-time-search/src/main/resources/input/vacuum/gen/vacuum0.vw"
        val file = "/Users/bencecserna/Documents/Development/projects/ai/real-time-search/src/main/resources/input/gridworld/uniform40/1k4k/uniform1000_4000-0__"
//        val file = "/home/aifs2/doylew/uniform40/1k8k/uniform1000_8000-0"
        println("File -> $file")
        val instance = File(file).inputStream()
//        val domain = SlidingTilePuzzleIO.parseFromStream(instance, actionDuration = configuration.actionDuration)
        val domain = GridWorldIO.parseFromStream(instance, 1L)
        val initialState = domain.initialState
//        val initialState = vacuumWorld.initialState
//        val tsAgent = TentacleSearch(vacuumWorld.domain, configuration)
//        val tsAgent = ExplicitEstimationSearchH(vacuumWorld.domain, configuration)
        val tsAgent = ExplicitEstimationSearch(domain.domain, configuration)
//        val tsAgent = AnalyticAStar(domain.domain, configuration)
//        val tsAgent = WeightedAStar(domain.domain, configuration)
//        val tsAgent = OptimisticSearch(domain.domain, configuration)
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

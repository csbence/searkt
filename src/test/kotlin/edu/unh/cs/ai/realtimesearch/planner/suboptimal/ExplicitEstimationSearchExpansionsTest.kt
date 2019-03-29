package edu.unh.cs.ai.realtimesearch.planner.suboptimal

import edu.unh.cs.ai.realtimesearch.environment.gridworld.GridWorldIO
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.StaticExpansionTerminationChecker
import org.junit.Test
import java.io.File

class ExplicitEstimationSearchVacuumTest {
    private val configuration = ExperimentConfiguration(domainName = "SLIDING_TILE_4", algorithmName = "WEIGHTED_A_STAR",
            terminationType = TerminationType.EXPANSION, actionDuration = 1L, timeLimit = 1000L, expansionLimit = 100000000L,
            errorModel = "path", weight = 3.0, variant = "O")
    private val configuration2 = ExperimentConfiguration(domainName = "SLIDING_TILE_4", algorithmName = "SXDP",
            terminationType = TerminationType.EXPANSION, actionDuration = 1L, timeLimit = 1000L, expansionLimit = 100000000L,
            errorModel = "path", weight = 1.2, variant = "O")
    private val configuration3 = ExperimentConfiguration(domainName = "SLIDING_TILE_4", algorithmName = "SXUP",
            terminationType = TerminationType.EXPANSION, actionDuration = 1L, timeLimit = 1000L, expansionLimit = 100000000L,
            errorModel = "path", weight = 1.2, variant = "O")

    @Test
    fun testEESVacuum() {
//        val file = "/home/aifs2/doylew/IdeaProjects/real-time-search/src/main/resources/input/gridworld/gridworld1.gw"
//        val file = "/home/aifs2/doylew/IdeaProjects/real-time-search/src/main/resources/input/lifegrids/lifegrids0.lg"
//        val file = "/home/aifs2/doylew/IdeaProjects/real-time-search/src/main/resources/input/uniform40/1k4000/uniform1000_4000-7"
        val file = "/home/doylew/IdeaProjects/real-time-search/src/main/resources/input/uniform40/1k4000/uniform1000_4000-0"
        println("File -> $file")
        val instance = File(file).inputStream()
//        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, actionDuration = configuration.actionDuration)
//        val initialState = slidingTilePuzzle.initialState
//        val tsAgent = TentacleSearch(vacuumWorld.domain, configuration)
//        val tsAgent = ExplicitEstimationSearchH(vacuumWorld.domain, configuration)
//        val tsAgent = ExplicitEstimationSearch(vacuumWorld.domain, configuration)


        // println("aStarPrimeExpansions:${tsAgent.aStarPrimeExpansions}")
        val tsAgent = WeightedAStar(vacuumWorld.domain, configuration)
        val plan = tsAgent.plan(initialState, StaticExpansionTerminationChecker(configuration.expansionLimit))
        println(num)
        println("expansions:${tsAgent.expandedNodeCount}")
        println("costOfSolution:${plan.size}")
        println("reexpansions:${tsAgent.reexpansions}")

        println()
        val xdpAgent = ConvexSearch(vacuumWorld.domain, configuration2)
        val plan2 = xdpAgent.plan(initialState, StaticExpansionTerminationChecker(configuration2.expansionLimit))
        println("expansions:${xdpAgent.expandedNodeCount}")
        println("costOfSolution:${plan2.size}")
        println("reexpansions:${xdpAgent.reexpansions}")

        println()
        val xupAgent = ConvexSearch(vacuumWorld.domain, configuration3)
        val plan3 = xupAgent.plan(initialState, StaticExpansionTerminationChecker(configuration3.expansionLimit))
        println("expansions:${xupAgent.expandedNodeCount}")
        println("costOfSolution:${plan3.size}")
        println("reexpansions:${xupAgent.reexpansions}")

       // println("aStarPrimeExpansions:${tsAgent.aStarPrimeExpansions}")
//        println("dHatExpansions:${tsAgent.dHatExpansions}")
//        println("fHatExpansions:${tsAgent.fHatExpansions}")
//        println("greedyExpansions:${tsAgent.greedyExpansions}")
//        println("aStarExpansions:${tsAgent.aStarExpansions}")
//        println("tentacleExpansions: ${tsAgent.tentacleExpansions}")
//        println("dHatExpansions:${tsAgent.dHatExpansions}")
//        println("fHatExpansions:${tsAgent.fHatExpansions}")
//        println("aStarExpansions:${tsAgent.aStarExpansions}")
    }
}

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

    @Test
    fun testEESVacuum() {
        val numInstances = 20
        for (w in 1..1) {
            var average1 = 0
            var average2 = 0
            var average3 = 0

            for (num in 0 until numInstances) {
//                val file = "/home/aifs2/doylew/IdeaProjects/real-time-search/src/main/resources/input/tiles/korf/4/real/$num"
                val file = "/home/aifs2/doylew/IdeaProjects/real-time-search/src/main/resources/input/uniform40/1k4000/uniform1000_4000-$num"
//                println("File -> $file")
                val instance = File(file).inputStream()
//                val tiles = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
                val gridWorldInstance = GridWorldIO.parseFromStream(instance, 1L)
                val initialState = gridWorldInstance.initialState
                val tsAgent = WeightedAStar(gridWorldInstance.domain, configuration)
                val xdpAgent = XDP(gridWorldInstance.domain, configuration)
                val eesAgent = ExplicitEstimationSearch(gridWorldInstance.domain, configuration)

                println(num)
                val plan = tsAgent.plan(initialState, StaticExpansionTerminationChecker(configuration.expansionLimit))
                val plan2 = xdpAgent.plan(initialState, StaticExpansionTerminationChecker(configuration.expansionLimit))
                val plan3 = eesAgent.plan(initialState, StaticExpansionTerminationChecker(configuration.expansionLimit))


//                println("expansions:${tsAgent.expandedNodeCount}")
//                println("costOfSolution:${plan.size}")
//                println("reexpansions:${tsAgent.reexpansions}")
//
//                println("---")
//
//                println("expansions:${xdpAgent.expandedNodeCount}")
//                println("costOfSolution:${plan2.size}")
//                println("reexpansions:${xdpAgent.reexpansions}")
//
//                println("---")
//
//                println("expansions:${eesAgent.expandedNodeCount}")
//                println("costOfSolution:${plan3.size}")
//                println("reexpansions:${eesAgent.reexpansions}")

                average1 += tsAgent.expandedNodeCount
                average2 += xdpAgent.expandedNodeCount
                average3 += eesAgent.expandedNodeCount
            }

            println("average expansions wA*: ${average1 / numInstances}")
            println("average expansions XDP: ${average2 / numInstances}")
            println("average expansions EES: ${average3 / numInstances}")
        }
//        val file = "/home/aifs2/doylew/IdeaProjects/real-time-search/src/main/resources/input/vacuum/gen/vacuum0.vw"
//        val file = "/home/aifs2/doylew/IdeaProjects/real-time-search/src/main/resources/input/gridworld/gridworld1.gw"
//        val file = "/home/aifs2/doylew/IdeaProjects/real-time-search/src/main/resources/input/lifegrids/lifegrids0.lg"
//        val file = "/home/aifs2/doylew/IdeaProjects/real-time-search/src/main/resources/input/uniform40/1k4000/uniform1000_4000-7"
//        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, actionDuration = configuration.actionDuration)
//        val initialState = slidingTilePuzzle.initialState
//        val tsAgent = TentacleSearch(vacuumWorld.domain, configuration)
//        val tsAgent = ExplicitEstimationSearchH(vacuumWorld.domain, configuration)
//        val tsAgent = ExplicitEstimationSearch(vacuumWorld.domain, configuration)


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

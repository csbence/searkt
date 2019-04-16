package edu.unh.cs.searkt.planner.suboptimal

import edu.unh.cs.searkt.environment.vacuumworld.VacuumWorldIO
import edu.unh.cs.searkt.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.searkt.experiment.configuration.realtime.TerminationType
import edu.unh.cs.searkt.experiment.terminationCheckers.StaticExpansionTerminationChecker
import org.junit.Test
import java.io.File

class ExplicitEstimationSearchVacuumTest {
    private val configuration = ExperimentConfiguration(domainName = "SLIDING_TILE_4", algorithmName = "WEIGHTED_A_STAR",
            terminationType = TerminationType.EXPANSION, actionDuration = 1L, timeLimit = 1000L, expansionLimit = 100000000L,
            errorModel = "path", weight = 1.0, variant = "O")
    private val configuration2 = ExperimentConfiguration(domainName = "SLIDING_TILE_4", algorithmName = "SXDP",
            terminationType = TerminationType.EXPANSION, actionDuration = 1L, timeLimit = 1000L, expansionLimit = 100000000L,
            errorModel = "path", weight = 1.01, variant = "O")
    private val configuration3 = ExperimentConfiguration(domainName = "SLIDING_TILE_4", algorithmName = "SXUP",
            terminationType = TerminationType.EXPANSION, actionDuration = 1L, timeLimit = 1000L, expansionLimit = 100000000L,
            errorModel = "path", weight = 1.01, variant = "O")
    private val configuration4 = ExperimentConfiguration(domainName = "SLIDING_TILE_4", algorithmName = "OPTIMISTIC_DD",
            terminationType = TerminationType.EXPANSION, actionDuration = 1L, timeLimit = 1000L, expansionLimit = 100000000L,
            errorModel = "path", weight = 1.01, variant = "O")

    @Test
    fun testEESVacuum() {
        val num = 7
//        val file = "/home/aifs2/doylew/IdeaProjects/real-time-search/src/main/resources/input/gridworld/gridworld1.gw"
//        val file = "/home/aifs2/doylew/IdeaProjects/real-time-search/src/main/resources/input/lifegrids/lifegrids7.lg"
//        val file = "/home/aifs2/doylew/IdeaProjects/real-time-search/src/main/resources/input/uniform40/1k4000/uniform1000_4000-7"
//        val file = "/home/aifs2/doylew/IdeaProjects/real-time-search/src/main/resources/input/uniform40/1k4000/uniform1000_4000-0"
//        val file = "/home/aifs2/doylew/IdeaProjects/real-time-search/src/main/resources/input/racetrack/hansen-bigger-quad.track"
        val file = "/home/aifs2/doylew/IdeaProjects/real-time-search/src/main/resources/input/vacuum/gen/vacuum3.vw"
        println("File -> $file")
        val instance = File(file).inputStream()
        val vacuumWorld = VacuumWorldIO.parseFromStream(instance)
        val initialState = vacuumWorld.initialState
//        val tsAgent = TentacleSearch(vacuumWorld.domain, configuration)
//        val tsAgent = ExplicitEstimationSearchH(vacuumWorld.domain, configuration)
//        val tsAgent = ExplicitEstimationSearch(vacuumWorld.domain, configuration)

//        val dpsAgent = DynamicPotentialSearchG(vacuumWorld.domain, configuration4)
//        val plan7 = dpsAgent.plan(initialState, StaticExpansionTerminationChecker(configuration.expansionLimit))
//        println(num)
//        println("expansions:${dpsAgent.expandedNodeCount}")
//        println("costOfSolution:${plan7.size}")
//        println("reexpansions:${dpsAgent.reexpansions}")

        // println("aStarPrimeExpansions:${tsAgent.aStarPrimeExpansions}")
        val opAgent = OptimisticSearch(vacuumWorld.domain, configuration4)
        val plan6 = opAgent.plan(initialState, StaticExpansionTerminationChecker(configuration.expansionLimit))
        println(num)
        println("expansions:${opAgent.expandedNodeCount}")
        println("costOfSolution:${plan6.size}")
        println("reexpansions:${opAgent.reexpansions}")


        // println("aStarPrimeExpansions:${tsAgent.aStarPrimeExpansions}")
        val tsAgent = WeightedAStar(vacuumWorld.domain, configuration)
        val plan = tsAgent.plan(initialState, StaticExpansionTerminationChecker(configuration.expansionLimit))
        println(num)
        println("expansions:${tsAgent.expandedNodeCount}")
        println("costOfSolution:${plan.size}")
        println("reexpansions:${tsAgent.reexpansions}")

        println()
        val xdpAgent = ImprovedOptimisticSearch(vacuumWorld.domain, configuration2)
        val plan2 = xdpAgent.plan(initialState, StaticExpansionTerminationChecker(configuration2.expansionLimit))
        println("expansions:${xdpAgent.expandedNodeCount}")
        println("costOfSolution:${plan2.size}")
        println("reexpansions:${xdpAgent.reexpansions}")

        println()
        val xupAgent = ImprovedOptimisticSearch(vacuumWorld.domain, configuration3)
        val plan3 = xupAgent.plan(initialState, StaticExpansionTerminationChecker(configuration3.expansionLimit))
        println("expansions:${xupAgent.expandedNodeCount}")
        println("costOfSolution:${plan3.size}")
        println("reexpansions:${xupAgent.reexpansions}")

        println()
        val eesAgent = ExplicitEstimationSearch(vacuumWorld.domain, configuration)
        val plan4 = eesAgent.plan(initialState, StaticExpansionTerminationChecker(configuration.expansionLimit))
        println("expansions:${eesAgent.expandedNodeCount}")
        println("costOfSolution:${plan4.size}")
        println("reexpansions:${eesAgent.reexpansions}")

        println()
        val eesAgent2 = ExplicitEstimationSearchDD(vacuumWorld.domain, configuration)
        val plan5 = eesAgent2.plan(initialState, StaticExpansionTerminationChecker(configuration.expansionLimit))
        println("expansions:${eesAgent2.expandedNodeCount}")
        println("costOfSolution:${plan5.size}")
        println("reexpansions:${eesAgent2.reexpansions}")



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

package edu.unh.cs.searkt.environment.heavyvacuumworld

import edu.unh.cs.searkt.environment.location.Location
import edu.unh.cs.searkt.environment.vacuumworld.VacuumWorldTest
import edu.unh.cs.searkt.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.searkt.experiment.configuration.realtime.TerminationType
import edu.unh.cs.searkt.experiment.terminationCheckers.StaticExpansionTerminationChecker
import edu.unh.cs.searkt.planner.suboptimal.DynamicPotentialSearch
import edu.unh.cs.searkt.planner.suboptimal.WeightedAStar
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HeavyVacuumWorldTest {
    private val configuration = ExperimentConfiguration(domainName = "VACUUM_WORLD_HEAVY", algorithmName = "WEIGHTED_A_STAR",
            terminationType = TerminationType.EXPANSION, actionDuration = 1L, timeLimit = 1000L,
            expansionLimit = 1000000L, weight = 1.3, errorModel = "path")


    @Test
    fun testGoalChecker() {
        val world = HeavyVacuumWorld(10, 10, emptySet(), 0)
        val l1 = Location(3, 5)
        val l2 = Location(0, 5)

        val goalState1 = HeavyVacuumWorldState(l1, emptyList(), world.calculateHeuristic(l1, emptyList()))
        val goalState2 = HeavyVacuumWorldState(l2, emptyList(), world.calculateHeuristic(l2, emptyList()))
        val notGoalState1 = HeavyVacuumWorldState(l2, listOf(l1), world.calculateHeuristic(l2, listOf(l1)))
        val notGoalState2 = HeavyVacuumWorldState(l2, listOf(l1, l2), world.calculateHeuristic(l2, listOf(l1, l2)))

        assert(world.isGoal(goalState1))
        assert(world.isGoal(goalState2))
        assertFalse(world.isGoal(notGoalState1))
        assertFalse(world.isGoal(notGoalState2))
    }

    @Test
    fun loadFromFileAndPlan() {
        val instancePath = "input/vacuum/toy7.vw"
        val stream = VacuumWorldTest::class.java.classLoader.getResourceAsStream(instancePath)
        val instanceIO = HeavyVacuumWorldIO.parseFromStream(stream)
        val initialState = instanceIO.initialState
        val domain = instanceIO.domain

        val aStarAgent = DynamicPotentialSearch(domain, configuration)
        val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000000L))
        var currentState = initialState
        println(domain.print(currentState))
        println("heuristic: ${currentState.heuristic}")
        println("plan is ${plan.size} long")
        domain.calculateHeuristic(currentState.agentLocation, currentState.dirtyCells)
        plan.forEach { action ->
            currentState = domain.successors(currentState).first { it.action == action }.state
            println(action.toString() + "\n" + domain.print(currentState))
            println("heuristic: ${domain.testHeuristic(currentState.agentLocation, currentState.dirtyCells)}")
        }
        assertTrue { domain.heuristic(currentState) == 0.0 }

    }


    @Test
    fun harderWorld() {
        val instancePath = "input/vacuum/gen/vacuum3.vw"
        val stream = VacuumWorldTest::class.java.classLoader.getResourceAsStream(instancePath)
        val instanceIO = HeavyVacuumWorldIO.parseFromStream(stream)
        val initialState = instanceIO.initialState
        val domain = instanceIO.domain

        val aStarAgent = WeightedAStar(domain, configuration)
        val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(5000000L))
        var currentState = initialState
        println(domain.print(currentState))
        println("heuristic: ${currentState.heuristic}")
        println("plan is ${plan.size} long")
        plan.forEach { action ->
            currentState = domain.successors(currentState).first { it.action == action }.state
            println("heuristic: ${currentState.heuristic}")
        }
        assertTrue { domain.heuristic(currentState) == 0.0 }

    }
}

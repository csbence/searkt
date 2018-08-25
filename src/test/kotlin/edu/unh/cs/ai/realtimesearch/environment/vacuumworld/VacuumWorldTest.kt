package edu.unh.cs.ai.realtimesearch.environment.vacuumworld

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleIO
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleTest
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.StaticExpansionTerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.suboptimal.*
import org.junit.Test
import kotlin.math.roundToInt
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VacuumWorldTest {

    private val configuration = ExperimentConfiguration(domainName = "VACUUM_WORLD", algorithmName = "WEIGHTED_A_STAR",
            terminationType = TerminationType.EXPANSION, actionDuration = 1L, timeLimit = 1000L,
            expansionLimit = 1000000L, weight = 1.0, errorModel = "path")


    @Test
    fun testGoalChecker() {
        val world = VacuumWorld(10, 10, emptySet(), 0)
        val l1 = Location(3, 5)
        val l2 = Location(0, 5)

        val goalState1 = VacuumWorldState(l1, emptyList(), world.calculateHeuristic(l1, emptyList()))
        val goalState2 = VacuumWorldState(l2, emptyList(), world.calculateHeuristic(l2, emptyList()))
        val notGoalState1 = VacuumWorldState(l2, listOf(l1), world.calculateHeuristic(l2, listOf(l1)))
        val notGoalState2 = VacuumWorldState(l2, listOf(l1, l2), world.calculateHeuristic(l2, listOf(l1, l2)))

        assert(world.isGoal(goalState1))
        assert(world.isGoal(goalState2))
        assertFalse(world.isGoal(notGoalState1))
        assertFalse(world.isGoal(notGoalState2))
    }

    @Test
    fun loadFromFileAndPlan() {
        val instancePath = "input/vacuum/toy7.vw"
        val stream = VacuumWorldTest::class.java.classLoader.getResourceAsStream(instancePath)
        val instanceIO = VacuumWorldIO.parseFromStream(stream)
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
        val instancePath = "input/vacuum/slalom.vw"
        val stream = VacuumWorldTest::class.java.classLoader.getResourceAsStream(instancePath)
        val instanceIO = VacuumWorldIO.parseFromStream(stream)
        val initialState = instanceIO.initialState
        val domain = instanceIO.domain

        val aStarAgent = DynamicPotentialSearch(domain, configuration)
        val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000000L))
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

    @Test
    fun solveAllInstancesTest() {
        val startingWeight = 1.01
        val stepSize = 0.00
        for (w in 2..2) {
            val currentWeight = startingWeight + (stepSize * w)
            println("running sub-optimality validation on weight: $currentWeight")
            configuration.weight = currentWeight
            for (i in 0 until 50) {
                println(i.toString())
                val stream = SlidingTilePuzzleTest::class.java.classLoader.getResourceAsStream("input/vacuum/gen/vacuum$i.vw")
                val vacuumWorld = VacuumWorldIO.parseFromStream(stream)
                val initialState = vacuumWorld.initialState
                val aStarAgent = WeightedAStar(vacuumWorld.domain, configuration)
                val plan: List<Action>
                try {
                    plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(5000000))
                    val pathLength  = plan.size.toLong()
                    var currentState = initialState
                    // valid plan check
                    plan.forEach { action ->
                        currentState = vacuumWorld.domain.successors(currentState).first { it.action == action }.state
                    }
                    assertTrue { vacuumWorld.domain.heuristic(currentState) == 0.0 }
                    println("Solved instance $i")
                } catch (e: Exception) {
                    System.err.println(e.message + " on instance $i with weight $currentWeight")
                }
            }
        }
    }


}
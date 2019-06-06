package edu.unh.cs.searkt.environment.pancake

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class PancakeProblemTest {

    private val pancakeProblem = PancakeIO.parseFromStream(File("/home/aifs2/doylew/IdeaProjects/searkt/src/main/resources/input/pancake/toy").inputStream(), 1L)
    private val initialState = byteArrayOf(6, 8, 9, 10, 1, 2, 3, 5, 4, 7, 11)
    private val goalState = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)

    @Test
    fun parseProblemInstance() {
        assertTrue(pancakeProblem.initialState.ordering.contentEquals(initialState), "initial state should be: $initialState")
        assertTrue(pancakeProblem.domain.startOrdering.contentEquals(initialState), "domain beginning ordering should be: $initialState")
        assertTrue(pancakeProblem.domain.endOrdering.contentEquals(goalState), "domain goal state should be: $goalState")
        assertEquals(11, pancakeProblem.initialState.ordering.size, "initial state size should be 11")
        assertEquals(11, pancakeProblem.domain.startOrdering.size, "domain beginning ordering should be size 11")
        assertEquals(11, pancakeProblem.domain.endOrdering.size, "domain goal state should be size 11")
    }

    @Test
    fun successors() {
        pancakeProblem.domain.successors(pancakeProblem.initialState).forEach { successor ->
            println(successor.state.ordering.asList())
        }
    }

    @Test
    fun heuristic() {
        pancakeProblem.domain.successors(pancakeProblem.initialState).forEach { successor ->
            println("${successor.state.ordering.asList()} : ${pancakeProblem.domain.heuristic(successor.state)}")
        }
    }

    @Test
    fun isGoal() {
        assertTrue(pancakeProblem.domain.isGoal(PancakeState(goalState, 0)))
        assertFalse(pancakeProblem.domain.isGoal(pancakeProblem.initialState))
        pancakeProblem.domain.successors(pancakeProblem.initialState).forEach { successor ->
            assertFalse(pancakeProblem.domain.isGoal(successor.state))
        }

    }
}
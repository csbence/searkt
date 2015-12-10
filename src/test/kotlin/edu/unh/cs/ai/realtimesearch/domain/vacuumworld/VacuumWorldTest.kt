package edu.unh.cs.ai.realtimesearch.domain.vacuumworld

import org.junit.Test
import kotlin.test.assertFalse

class VacuumWorldTest {

    @Test
    fun moveTest() {

    }

    @Test
    fun goalTest() {
        val world = VacuumWorld(10,10, emptyList())
        val l1 = VacuumWorldState.Location(3, 5)
        val l2 = VacuumWorldState.Location(0, 5)

        val goalState1 = VacuumWorldState(l1, emptyList())
        val goalState2 = VacuumWorldState(l2, emptyList())
        val notGoalState1 = VacuumWorldState(l2, listOf(l1))
        val notGoalState2 = VacuumWorldState(l2, listOf(l1, l2))

        assert(world.isGoal(goalState1))
        assert(world.isGoal(goalState2))
        assertFalse(world.isGoal(notGoalState1))
        assertFalse(world.isGoal(notGoalState2))
    }

    @Test
    fun legalLocationTest() {
        // TODO: make islegalLocation private to package
    }
}
package edu.unh.cs.ai.realtimesearch.environment.vacuumworld

import org.junit.Test
import kotlin.test.assertTrue

class VacuumWorldIOTest {

    @Test
    fun parseFromStreamCupsTest() {
        val stream = VacuumWorldIOTest::class.java.classLoader.getResourceAsStream("input/vacuum/cups.vw")
        val vacuumWorldInstance = VacuumWorldIO.parseFromStream(stream)
        val startState = vacuumWorldInstance.initialState
        val agentLocation = startState.agentLocation

        assertTrue(startState.dirtyCells.count() == 1)
        assertTrue(agentLocation.x == 6)
        assertTrue(agentLocation.y == 2)

        assertTrue(startState.dirtyCells.first().x == 6)
        assertTrue(startState.dirtyCells.first().y == 8)
    }
}
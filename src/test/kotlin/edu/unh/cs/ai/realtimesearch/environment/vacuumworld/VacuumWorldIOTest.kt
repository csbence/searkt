package edu.unh.cs.ai.realtimesearch.environment.vacuumworld

import org.junit.Test
import java.io.File
import java.io.FileInputStream
import kotlin.test.assertTrue

class VacuumWorldIOTest {

    @Test
    fun parseFromStreamCupsTest() {
        val file = File("input/vacuum/cups.vw")
        val vacuumWorldInstance = VacuumWorldIO.parseFromStream(FileInputStream(file))
        val startState = vacuumWorldInstance.initialState
        val agentLocation = startState.agentLocation

        assertTrue(startState.dirtyCells.count() == 1)
        assertTrue(agentLocation.x == 6)
        assertTrue(agentLocation.y == 2)

        assertTrue(startState.dirtyCells.first().x == 6)
        assertTrue(startState.dirtyCells.first().y == 8)
    }
}
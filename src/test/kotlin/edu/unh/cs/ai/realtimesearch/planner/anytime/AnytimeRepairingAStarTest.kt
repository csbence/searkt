package edu.unh.cs.ai.realtimesearch.planner.anytime

import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorld
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldState
import org.junit.Test

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class AnytimeRepairingAStarTest {

    @Test
    fun solveVacuumWorldTest1() {
        val world = VacuumWorld(2,2, emptyList())
        val location = VacuumWorldState.Location(0, 0)

        val startState = VacuumWorldState(location, setOf(location))

        val anytimeRepairingAStar = AnytimeRepairingAStar(world, 2.0)
        anytimeRepairingAStar.solve(startState)
    }

}
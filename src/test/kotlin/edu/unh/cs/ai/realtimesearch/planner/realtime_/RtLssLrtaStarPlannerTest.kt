package edu.unh.cs.ai.realtimesearch.planner.realtime_

import edu.unh.cs.ai.realtimesearch.environment.gridworld.GridWorld
import edu.unh.cs.ai.realtimesearch.environment.gridworld.GridWorldState
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import org.junit.Test
import kotlin.test.assertTrue

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class RtLssLrtaStarPlannerTest {


    @Test
    fun testAddPredecessor() {
        val gridWorld = GridWorld(2,2,hashSetOf(), Location(1,1))
        val rtLssLrtaStarPlanner = RtLssLrtaStarPlanner(gridWorld)

        val childState = GridWorldState(Location(0, 0))
        val parentState1 = GridWorldState(Location(1, 0))
        val parentState2 = GridWorldState(Location(0, 1))

        rtLssLrtaStarPlanner.addPredecessor(childState, parentState1, 1.0)
        rtLssLrtaStarPlanner.addPredecessor(childState, parentState2, 1.0)

        val clazz = RtLssLrtaStarPlanner::class.java
        val predecessorField = clazz.getDeclaredField("predecessors")
        predecessorField.isAccessible = true

        val predecessors : MutableMap<GridWorldState, MutableList<RtLssLrtaStarPlanner.StateCostPair<GridWorldState>>> = predecessorField.get(rtLssLrtaStarPlanner) as MutableMap<GridWorldState, MutableList<RtLssLrtaStarPlanner.StateCostPair<GridWorldState>>>

        val mutableList = predecessors[childState]
        assertTrue { mutableList?.size == 2 }
    }

}
package edu.unh.cs.ai.realtimesearch.environment.traffic

import edu.unh.cs.ai.realtimesearch.environment.location.Location
import edu.unh.cs.ai.realtimesearch.environment.obstacle.MovingObstacle
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * basic traffic world tests
 * Created by doylew on 1/18/17.
 */
class TrafficWorldTest {

    val world = TrafficWorld(10, 10, emptySet(), Location(9, 9), 1, emptyList())

    @Test
    fun testGoalState() {
        assert(world.isGoal(TrafficWorldState(world.goal, 0)))
        assertFalse(world.isGoal(TrafficWorldState(Location(4, 5), 0)))
        assertFalse(world.isGoal(TrafficWorldState(Location(0, 0), 0)))
    }

    @Test
    fun testHeuristicBasic() {
        val pos1 = TrafficWorldState(Location(0, 0), 0)
        val pos2 = TrafficWorldState(Location(5, 5), 0)

        assert(world.heuristic(pos1) == 19.0)
        assert(world.heuristic(pos2) == 9.0)
    }

    val obstacleWorld = TrafficWorld(10, 10, emptySet(), Location(9,9), 1, listOf(MovingObstacle(3,3,1,1), MovingObstacle(5,5,-1,0),
            MovingObstacle(2,2,0,-1), MovingObstacle(4,4,1,-1), MovingObstacle(6,6,-1,2)))

    @Test
    fun testVisualizeSomeObstacles() {
        val pos1 = TrafficWorldState(Location(0, 0), 0)
        println("showing starting world...")
        println(obstacleWorld.print(pos1))
        println("now successors...")
        obstacleWorld.successors(pos1).forEach { println(obstacleWorld.print(it.state)) }
        println("now successors of successors...")
        obstacleWorld.successors(pos1).forEach {
            obstacleWorld.successors(it.state).forEach {
                println(obstacleWorld.print(it.state))
            }
            println("new parent...")
        }
    }

    val littleObstacleWorld = TrafficWorld(10, 10, emptySet(), Location(9,9), 1, listOf(MovingObstacle(3, 3, 1, 1), MovingObstacle(3, 5, 2, 0)))

    @Test
    fun testDirectSuccessorConsistency() {
        val state = TrafficWorldState(Location(0, 0), 0)
        val successors1 = littleObstacleWorld.successors(state)
        val successors2 = littleObstacleWorld.successors(state)

        // Make sure that the successor generation is consistent
        assertTrue { successors1.all { it in successors2 } }
        assertTrue { successors2.all { it in successors1 } }
    }

    @Test
    fun testIndirectSuccessorConsistency() {
        val state = TrafficWorldState(Location(0, 0), 0)
        var stateA: TrafficWorldState = state
        var stateB: TrafficWorldState = state

        (0..1000).forEach { stateA = littleObstacleWorld.successors(stateA).first().state }
        (0..1000).forEach { stateB = littleObstacleWorld.successors(stateB).first().state }

        assertTrue { stateA == stateB }
    }


//    @Test
//    fun testVisualizeObstacles() {
//        val obstacles = mutableSetOf<Location>()
//        val bunkers = mutableSetOf<Location>()
//        (1..8).forEach { obstacles.add(Location(it,it)) }
//        bunkers.add(Location(3,4))
//        bunkers.add(Location(5,7))
//        bunkers.add(Location(7,9))
//        val obstacleWorld = TrafficWorld(10, 10,  bunkers = bunkers, goal = Location(9, 9), actionDuration = 1)
//
//        val pos1 = TrafficWorldState(Location(0, 0), obstacles)
//
//        println("showing starting world...")
//        println(obstacleWorld.print(pos1))
//        println("now successors...")
////        obstacleWorld.successors(pos1).forEach { println(obstacleWorld.print(it.state)) }
//        println("now successors of successors...")
//        obstacleWorld.successors(pos1).forEach {
//            println("new source...")
//            println(obstacleWorld.print(it.state))
//            obstacleWorld.successors(it.state).forEach {
//                println(obstacleWorld.print(it.state))
//            }
//        }
//    }
}
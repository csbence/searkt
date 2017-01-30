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

    val world = TrafficWorld(10, 10, emptySet(), Location(9, 9), 1)

    @Test
    fun testGoalState() {
        assert(world.isGoal(TrafficWorldState(world.targetLocation, emptySet())))
        assertFalse(world.isGoal(TrafficWorldState(Location(4, 5), emptySet())))
        assertFalse(world.isGoal(TrafficWorldState(Location(0, 0), emptySet())))
    }

    @Test
    fun testHeuristicBasic() {
        val pos1 = TrafficWorldState(Location(0, 0), emptySet())
        val pos2 = TrafficWorldState(Location(5, 5), emptySet())

        assert(world.heuristic(pos1) == 19.0)
        assert(world.heuristic(pos2) == 9.0)
    }

    @Test
    fun testVisualizeNoObstacles() {
        val pos1 = TrafficWorldState(Location(0, 0), emptySet())
        println("showing starting world...")
        println(world.print(pos1))
        println("now successors...")
        world.successors(pos1).forEach { println(world.print(it.state)) }
        println("now successors of successors...")
        world.successors(pos1).forEach {
            world.successors(it.state).forEach {
                println(world.print(it.state))
            }
        }
    }

    @Test
    fun testDirectSuccessorConsistency() {
        val state = TrafficWorldState(Location(0, 0), setOf(MovingObstacle(3, 3, 1, 1), MovingObstacle(3, 5, 2, 0)))
        val successors1 = world.successors(state)
        val successors2 = world.successors(state)

        // Make sure that the successor generation is consistent
        assertTrue { successors1.all { it in successors2 } }
        assertTrue { successors2.all { it in successors1 } }
    }

    @Test
    fun testIndirectSuccessorConsistency() {
        val state = TrafficWorldState(Location(0, 0), setOf(MovingObstacle(3, 3, 1, 1), MovingObstacle(3, 5, 2, 0)))
        var stateA: TrafficWorldState = state
        var stateB: TrafficWorldState = state

        (0..1000).forEach { stateA = world.successors(stateA).first().state }
        (0..1000).forEach { stateB = world.successors(stateB).first().state }

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
//        val obstacleWorld = TrafficWorld(10, 10,  bunkers = bunkers, targetLocation = Location(9, 9), actionDuration = 1)
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
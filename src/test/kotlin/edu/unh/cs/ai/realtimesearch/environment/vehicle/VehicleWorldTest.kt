package edu.unh.cs.ai.realtimesearch.environment.vehicle

import org.junit.Test
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import kotlin.test.assertFalse

/**
 * basic vehicle world tests
 * Created by doylew on 1/18/17.
 */
class VehicleWorldTest {

    val world = VehicleWorld(10, 10, emptySet(),Location(9, 9), 1)

    @Test
    fun testGoalState() {
        assert(world.isGoal(VehicleWorldState(world.targetLocation, emptySet())))
        assertFalse(world.isGoal(VehicleWorldState(Location(4, 5), emptySet())))
        assertFalse(world.isGoal(VehicleWorldState(Location(0, 0), emptySet())))
    }

    @Test
    fun testHeuristicBasic() {
        val pos1 = VehicleWorldState(Location(0, 0), emptySet())
        val pos2 = VehicleWorldState(Location(5, 5), emptySet())

        assert(world.heuristic(pos1) == 19.0)
        assert(world.heuristic(pos2) == 9.0)
    }

    @Test
    fun testVisualizeNoObstacles() {
        val pos1 = VehicleWorldState(Location(0, 0), emptySet())
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
    fun testVisualizeObstacles() {
        val obstacles = mutableSetOf<Location>()
        val bunkers = mutableSetOf<Location>()
        (1..8).forEach { obstacles.add(Location(it,it)) }
        bunkers.add(Location(3,4))
        bunkers.add(Location(5,7))
        bunkers.add(Location(7,9))
        val obstacleWorld = VehicleWorld(10, 10,  bunkers = bunkers, targetLocation = Location(9, 9), actionDuration = 1)

        val pos1 = VehicleWorldState(Location(0, 0), obstacles)

        println("showing starting world...")
        println(obstacleWorld.print(pos1))
        println("now successors...")
//        obstacleWorld.successors(pos1).forEach { println(obstacleWorld.print(it.state)) }
        println("now successors of successors...")
        obstacleWorld.successors(pos1).forEach {
            println("new source...")
            println(obstacleWorld.print(it.state))
            obstacleWorld.successors(it.state).forEach {
                println(obstacleWorld.print(it.state))
            }
        }
    }
}
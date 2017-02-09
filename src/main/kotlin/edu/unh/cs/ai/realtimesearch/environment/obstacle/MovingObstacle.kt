package edu.unh.cs.ai.realtimesearch.environment.obstacle

import edu.unh.cs.ai.realtimesearch.environment.location.Location

/**
 * MovingObstacle data class
 * a basic location class which allows
 * the obstacle to location to have a dx and dy
 * to allow for motion of the location based on time
 * Created by doylew on 1/25/17.
 */

/**
 * @param x the x location
 * @param y the y location
 * @param dx the x-velocity
 * @param dy the y-velocity
 */
data class MovingObstacle(var x: Int, var y: Int, var dx: Int, var dy: Int) {
    override fun equals(other: Any?): Boolean = other is MovingObstacle && other.x == x && other.y == y &&
            other.dx == dx && other.dy == dy

    override fun hashCode(): Int {
        return x xor Integer.rotateLeft(y, 8) xor dx xor Integer.rotateLeft(dy, 24)
    }

    fun invertVelocity() {
        dx *= -1
        dy *= -1
    }
}

fun Iterable<MovingObstacle>.toLocationSet() = mapTo(HashSet()) { Location(it.x, it.y) }
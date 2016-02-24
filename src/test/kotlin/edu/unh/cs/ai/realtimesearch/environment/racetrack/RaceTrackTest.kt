package edu.unh.cs.ai.realtimesearch.environment.racetrack

import edu.unh.cs.ai.realtimesearch.environment.location.Location
import org.junit.Test

import org.junit.Assert.*
import java.util.*

class RaceTrackTest {

    @Test
    fun testSuccessors() {

    }

    @Test
    fun testHeuristic() {
        val track = RaceTrack(HashSet(), HashSet(), HashSet(), 1, 1);

        var state = RaceTrackState(Location(5, 4), 1, 0);
        assertTrue(track.heuristic(state) == 1.0);
    }

    @Test
    fun testDistance() {
        val track = RaceTrack(HashSet(), HashSet(), HashSet(), 1, 1);

        var state = RaceTrackState(Location(5, 4), 1, 0);
        assertTrue(track.distance(state) == 1.0);
    }

    @Test
    fun testIsGoal() {
        val track = RaceTrack(HashSet(), hashSetOf(Location(5,5), Location(5,6)), HashSet(), 10, 10)

        var state = RaceTrackState(Location(5, 5), 1, 0)
        assertTrue(track.isGoal(state))

        state = RaceTrackState(Location(5, 4), 1, 0)
        assertTrue(!track.isGoal(state))

        state = RaceTrackState(Location(5, 6), 1, 0)
        assertTrue(track.isGoal(state))

        state = RaceTrackState(Location(6, 6), 1, 0)
        assertTrue(!track.isGoal(state))
    }

    @Test
    fun testPrint() {

    }
}
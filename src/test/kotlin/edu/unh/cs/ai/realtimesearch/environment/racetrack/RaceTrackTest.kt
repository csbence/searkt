package edu.unh.cs.ai.realtimesearch.environment.racetrack

import org.junit.Test
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import edu.unh.cs.ai.realtimesearch.environment.Domain
import java.lang.Math.*

class RaceTrackTest {

    @Test
    fun testSuccessors() {

    }

//    @Test
//    fun testCalculateDijkstraHeuristic() {
//        val goals = setOf(Location(0,0))
//
//        val track = RaceTrack(10, 10, HashSet(), goals, 1)
//        track.heuristicMap.forEach { k, v ->
//            println("$k\t$v\n")
//            val distanceFunction: (Location) -> Double = {
//                (x, y) -> max(abs(k.x - x) / track.maxXSpeed.toDouble(), abs(k.y - y) / track.maxYSpeed.toDouble())
//            }
//
//            val minDist =  goals.map(distanceFunction).min()
//            println(minDist)
//
//        }
//    }

//    @Test
//    fun testHeuristic() {
//        val track = RaceTrack(HashSet(), HashSet(), HashSet(), 1, 1);
//
//        var state = RaceTrackState(Location(5, 4), 1, 0);
//        assertTrue(track.heuristic(state) == 1.0);
//    }
//
//    @Test
//    fun testDistance() {
//        val track = RaceTrack(HashSet(), HashSet(), HashSet(), 1, 1);
//
//        var state = RaceTrackState(Location(5, 4), 1, 0);
//        assertTrue(track.distance(state) == 1.0);
//    }
//
//    @Test
//    fun testIsGoal() {
//        val track = RaceTrack(HashSet(), hashSetOf(Location(5,5), Location(5,6)), HashSet(), 10, 10)
//
//        var state = RaceTrackState(Location(5, 5), 1, 0)
//        assertTrue(track.isGoal(state))
//
//        state = RaceTrackState(Location(5, 4), 1, 0)
//        assertTrue(!track.isGoal(state))
//
//        state = RaceTrackState(Location(5, 6), 1, 0)
//        assertTrue(track.isGoal(state))
//
//        state = RaceTrackState(Location(6, 6), 1, 0)
//        assertTrue(!track.isGoal(state))
//    }

    @Test
    fun testPrint() {

    }
}
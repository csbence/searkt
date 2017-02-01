package edu.unh.cs.ai.realtimesearch.environment.racetrack

import org.junit.Before
import org.junit.Test

internal class RaceTrackTest {
    val rawRaceTrack: String = "30\n" +
            "33\n" +
            "##########_______#############\n" +
            "######______________##########\n" +
            "##_____________________#######\n" +
            "##________________________####\n" +
            "##________________________####\n" +
            "##________________________####\n" +
            "##__________##____________####\n" +
            "##________#####___________####\n" +
            "##_______#######__________####\n" +
            "##______#####____________#####\n" +
            "##_____####____________#######\n" +
            "##_____####__________#########\n" +
            "#______####________###########\n" +
            "#______####______#############\n" +
            "#______####_____________######\n" +
            "#______###_______________#####\n" +
            "#_____####________________####\n" +
            "#_____####_________________###\n" +
            "#_____####__________________##\n" +
            "#_____#####__________________#\n" +
            "#_____######__________________\n" +
            "#_____################________\n" +
            "______#################_______\n" +
            "______#################_______\n" +
            "______#################_______\n" +
            "______#################_______\n" +
            "______#################_______\n" +
            "______#################_______\n" +
            "______#################_______\n" +
            "______#################_______\n" +
            "______#################_______\n" +
            "______#################_______\n" +
            "__@___#################*******"

    lateinit var raceTrack: RaceTrack
    lateinit var initialState: RaceTrackState

    @Before
    fun setUp() {
        val (raceTrack, initialState) = RaceTrackIO.parseFromStream(rawRaceTrack.byteInputStream(), 100)
        this.raceTrack = raceTrack
        this.initialState = initialState
    }

    @Test
    fun testSuccessors() {

    }

//    @Test
//    fun testCalculateDijkstraHeuristic() {
//        val goals = setOf(Location(0,0))
//        val obstacles = setOf(Location(1, 0), Location(1, 1))
//
//        val track = RaceTrack(10, 10, obstacles, goals, 1)
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
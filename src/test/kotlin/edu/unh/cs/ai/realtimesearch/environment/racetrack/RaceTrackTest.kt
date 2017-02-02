package edu.unh.cs.ai.realtimesearch.environment.racetrack

import edu.unh.cs.ai.realtimesearch.environment.location.Location
import org.junit.Before
import org.junit.Test
import java.util.*
import java.lang.Math.*

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

    @Test
    fun testCalculateDijkstraHeuristic() {
        data class Node(val state: RaceTrackState, val distance: Double)

        val visited = HashSet<RaceTrackState>()
        val nodeComparator = java.util.Comparator<Node> { (_, lhsDistance), (_, rhsDistance) ->
            when {
                lhsDistance < rhsDistance -> -1
                lhsDistance > rhsDistance -> 1
                else -> 0
            }
        }


        // Lower bound estimate of distance from initial state to goal
        val initialLocation = Location(initialState.x, initialState.y)
        val heuristicEstimate = this.raceTrack.heuristicMap[initialLocation]!!

        val stateQueue = PriorityQueue<Node>(nodeComparator)
        stateQueue.add(Node(this.initialState, 0.0))
        while (!stateQueue.isEmpty()) {
            val curNode = stateQueue.poll()
            if (curNode.state in visited) {
                continue
            }

            visited += curNode.state

            if (this.raceTrack.isGoal(curNode.state)) {
                assert(heuristicEstimate <= curNode.distance / max(this.raceTrack.maxXSpeed, this.raceTrack.maxYSpeed))
                break
            }

            this.raceTrack.successors(RaceTrackState(curNode.state.x, curNode.state.y, 0, 0))
                    .filter { Location(it.state.x, it.state.y) !in this.raceTrack.obstacles }
                    .mapTo(stateQueue, { Node(it.state, curNode.distance + this.raceTrack.actionDuration)})
        }


    }

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
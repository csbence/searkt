package edu.unh.cs.searkt.environment.racetrack

import edu.unh.cs.searkt.MetronomeException
import edu.unh.cs.searkt.environment.location.Location
import org.junit.Before
import org.junit.Test
import java.lang.Math.max
import java.util.*
import kotlin.test.assertTrue

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
            "______#####_______#####_______\n" +
            "______#####_______#####_______\n" +
            "______#####_______#####_______\n" +
            "______#####___*____####_______\n" +
            "______########_########_______\n" +
            "______########_########_______\n" +
            "______########_########_______\n" +
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

        fun runTest(initialLocation: Location) {
            val visited = HashSet<RaceTrackState>()
            val nodeComparator = java.util.Comparator<Node> { (_, lhsDistance), (_, rhsDistance) ->
                when {
                    lhsDistance < rhsDistance -> -1
                    lhsDistance > rhsDistance -> 1
                    else -> 0
                }
            }


            // Lower bound estimate of distance from initial state to goal
            val heuristicEstimate = this.raceTrack.heuristicMap[initialLocation]!!

            val stateQueue = PriorityQueue<Node>(nodeComparator)
            stateQueue.add(Node(RaceTrackState(initialLocation.x, initialLocation.y, 0, 0), 0.0))
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
                        .mapTo(stateQueue, { Node(it.state, curNode.distance + this.raceTrack.actionDuration) })
            }

        }

        arrayOf(Location(initialState.x, initialState.y),
                Location(3, 3), Location(5, 5), Location(15, 25)).forEach(::runTest)


    }

    @Test
    fun testDijkstraHeuristicHardcoded() {
        val mFactor = (1.0 * max(raceTrack.maxXSpeed, raceTrack.maxYSpeed)) / raceTrack.actionDuration
        assertTrue { raceTrack.getGoals().all { raceTrack.heuristicMap[Location(it.x, it.y)] == 0.0 } }

        assertTrue { raceTrack.heuristic(RaceTrackState(14, 25, 0, 0)) * mFactor == 2.0 }
        assertTrue { raceTrack.heuristicMap[Location(14, 25)]!! * mFactor == 2.0 }

        assertTrue { raceTrack.heuristic(RaceTrackState(14, 27, 0, 0)) * mFactor == 0.0 } // goal
        assertTrue { raceTrack.heuristic(RaceTrackState(17, 27, 0, 0)) * mFactor == 3.0 } // three right
        assertTrue { raceTrack.heuristic(RaceTrackState(18, 27, 0, 0)) * mFactor == 4.0 } // four right
        assertTrue { raceTrack.heuristic(RaceTrackState(11, 27, 0, 0)) * mFactor == 3.0 } // three left
        assertTrue { raceTrack.heuristic(RaceTrackState(14, 24, 0, 0)) * mFactor == 3.0 } // three up
        assertTrue { raceTrack.heuristic(RaceTrackState(14, 30, 0, 0)) * mFactor == 3.0 } // three down
    }

    @Test
    fun testRandomizedStartState() {
        val goalDistance = raceTrack.heuristicMap[Location(initialState.x, initialState.y)]!!
        for (seed in 0L..1000L) {
            raceTrack.randomizedStartState(initialState, seed).let {
                assertTrue {raceTrack.heuristicMap[Location(it.x, it.y)]!! in (goalDistance * 0.9)..(goalDistance) }
            }
        }
    }

    @Test
    fun testUniformHeuristic() {
        val (uniformTrack, _) = RaceTrackIO.parseFromStream(
                Unit::class.java.classLoader.getResourceAsStream("input/vacuum/empty.vw") ?: throw MetronomeException("Instance file not found.")
                , 100)

        // Picking a point in vacuum world: it should have successors
        uniformTrack.successors(RaceTrackState(7, 10, 0, 0)).forEach { println(it.state) }


    }

    @Test
    fun testCollisionDetection() {
        fun testStraightLineCollisionDetection(rawRaceTrack: String, collisionFree: Boolean) {
            val (raceTrack, _) = RaceTrackIO.parseFromStream(rawRaceTrack.byteInputStream(), 100)

            assertTrue { raceTrack.isCollisionFree(0, 0, 1, 0) == collisionFree }
            assertTrue { raceTrack.isCollisionFree(0, 0, 2, 0) == collisionFree }
            assertTrue { raceTrack.isCollisionFree(0, 0, 3, 0) == collisionFree }
            assertTrue { raceTrack.isCollisionFree(0, 0, 4, 0) == collisionFree }
        }

        testStraightLineCollisionDetection("5\n1\n@#__*", false)
        testStraightLineCollisionDetection("5\n1\n@##_*", false)
        testStraightLineCollisionDetection("5\n1\n@###*", false)
        testStraightLineCollisionDetection("5\n1\n@#_#*", false)

        testStraightLineCollisionDetection("5\n1\n@___*", true)
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

}
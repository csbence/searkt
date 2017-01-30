package edu.unh.cs.ai.realtimesearch.planner.realtime

import edu.unh.cs.ai.realtimesearch.environment.racetrack.RaceTrack
import edu.unh.cs.ai.realtimesearch.environment.racetrack.RaceTrackIO
import edu.unh.cs.ai.realtimesearch.environment.racetrack.RaceTrackState
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.FakeTerminationChecker
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * @author Bence Cserna (bence@cserna.net)
 */
internal class SafeRealTimeSearchTest {
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

    @After
    fun tearDown() {
    }

    @Test
    fun testIsComfortable() {
        assertTrue { isComfortable(initialState, FakeTerminationChecker, raceTrack) }
        assertTrue { isComfortable(RaceTrackState(2, 2, 0, 0), FakeTerminationChecker, raceTrack) }
        assertTrue { isComfortable(RaceTrackState(2, 2, 1, 1), FakeTerminationChecker, raceTrack) }
        // Go up & right
        assertFalse { isComfortable(RaceTrackState(2, 32, 5, 5), FakeTerminationChecker, raceTrack) }
        // Go right
        assertFalse { isComfortable(RaceTrackState(2, 32, 5, 0), FakeTerminationChecker, raceTrack) }
        // Go up
        assertTrue { isComfortable(RaceTrackState(2, 32, 0, 5), FakeTerminationChecker, raceTrack) }
    }


}
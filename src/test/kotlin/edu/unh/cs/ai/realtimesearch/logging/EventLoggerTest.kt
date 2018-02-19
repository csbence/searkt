package edu.unh.cs.ai.realtimesearch.logging

//import com.fasterxml.jackson.core.JsonFactory
//import com.fasterxml.jackson.core.JsonToken
//import com.fasterxml.jackson.databind.ObjectMapper
import edu.unh.cs.ai.realtimesearch.environment.racetrack.RaceTrackAction
import edu.unh.cs.ai.realtimesearch.environment.racetrack.RaceTrackState
import org.junit.Test
import kotlin.test.assertTrue

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class EventLoggerTest {

    @Test
    fun emptyLogger() {
        val eventLogger = EventLogger<RaceTrackState>()

        val json = eventLogger.toJson()
//        val jsonFactory = JsonFactory()
//        val jsonParser = jsonFactory.createParser(json)
//        assertTrue(jsonParser.nextToken() == JsonToken.START_ARRAY)
//        assertTrue(jsonParser.nextToken() == JsonToken.END_ARRAY)
    }

    @Test
    fun updateState() {
        val eventLogger = EventLogger<RaceTrackState>()

        eventLogger.expandState(RaceTrackState(1,2,3,4), 1.0, 2, 3, RaceTrackAction.UP, null)
        val json = eventLogger.toJson()

        // Syntax check
//        ObjectMapper().readTree(json)
    }

    @Test
    fun commitActions() {
        val eventLogger = EventLogger<RaceTrackState>()

        eventLogger.commitActions(listOf(RaceTrackAction.UP, RaceTrackAction.DOWN))
        val json = eventLogger.toJson()

        // Syntax check
//        ObjectMapper().readTree(json)
    }

    @Test
    fun startIteration() {
        val eventLogger = EventLogger<RaceTrackState>()

        eventLogger.startIteration()
        val json = eventLogger.toJson()

        // Syntax check
//        val node = ObjectMapper().readTree(json)
//        assertTrue { node.isArray }
//         The array should contain one item
//        assertTrue { node[0] != null }
//        assertTrue { node[1] == null }
//
//        assertTrue { node[0].get("type") != null }
    }

}
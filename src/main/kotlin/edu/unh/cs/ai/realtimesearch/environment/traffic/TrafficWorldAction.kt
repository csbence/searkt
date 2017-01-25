package edu.unh.cs.ai.realtimesearch.environment.traffic

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.location.Location
/**
 * This is an action in the vehicleworld as an enum:
 * actions for each of the four usual directions in grid-like domains
 *
 * Created by doylew on 1/17/17.
 */
enum class TrafficWorldAction(val index: Int) : Action {
    LEFT(0), DOWN(1), UP(2), RIGHT(3);

    private val relativeLocations = arrayOf(
            Location(-1, 0),
            Location(0, -1),
            Location(0, 1),
            Location(1,0)
    )

    companion object {
        fun getRelativeLocation(trafficWorldAction: TrafficWorldAction) = trafficWorldAction.relativeLocations[trafficWorldAction.index]
    }
}
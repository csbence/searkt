package edu.unh.cs.ai.realtimesearch.environment.racetrack

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.environment.location.Location

/**
 * The racetrack domain is a gridworld with a specific start 'line' and finish 'line'. The
 * agent starts at one of the cells on the starting line, and the goal is to reach one of the
 * cells at the finish line. The shape of the track is variable, and driving of the grid returns
 * the agent to a cell on the starting line.
 *
 * The car can choose to accelerate up to 1 in either x or y direction, reaching a speed of up to
 * 1 (in both directions). The dynamics are as follows:
 *
 * x(t+1) = x(t) + x.(t) + m(x,t)
 * y(t+1) = y(t) + y.(t) + m(y,t)
 * x.(t+1) = x.(t) + m(x,t)
 * y.(t+1) = y.(t) + m(y,t)
 *
 * The parameter 'p' introduces stochasticity to the problem: with probability p the car will
 * fail its action and maintain its speed.
 *
 */
class RaceTrack(val width: Int,
                val height: Int,
                val track: Set<Location>,
                val finish_line: Set<Location>
                ) : Domain<RaceTrackState> {
    //private val logger = LoggerFactory.getLogger(RaceTrack::class.java)

    override fun successors(state: RaceTrackState): List<SuccessorBundle<RaceTrackState>> {
        val successors: MutableList<SuccessorBundle<RaceTrackState>> = arrayListOf()

        for (action in RaceTrackAction.values()) {
            val new_x_speed = state.x_speed + action.getAcceleration().x
            val new_y_speed = state.y_speed + action.getAcceleration().y

            var x = state.x.toDouble()
            var y = state.y.toDouble()
            val dt = 0.1
            val nSteps = 10
            var valid = true

            for (i in 0..nSteps-1) {
                x += new_x_speed * dt;
                y += new_y_speed * dt;

                if (!isLegalLocation(x, y)) {
                    valid = false;
                    break;
                }
            }

            // filter on legal moves (not too fast and on the track)
//            if (new_x_speed <= 3 && new_x_speed >= -3 &&
//                    new_y_speed <= 3 && new_x_speed >= -3 &&
//                    valid) {
             if(valid){

                successors.add(SuccessorBundle(
                        RaceTrackState(state.x + new_x_speed, state.y + new_y_speed, new_x_speed, new_y_speed),
                        action,
                        actionCost = 1.0))
            }
        }

        return successors
    }

    /**
     * Returns whether location within boundaries and not a blocked cell.
     *
     * @param location the location to test
     * @return true if location is legal
     */
    fun isLegalLocation( x : Double, y : Double): Boolean {
        return x >= 0 && y >= 0 && x < width &&
                y < height && Location(Math.round(x.toFloat()), Math.round(y.toFloat())) !in track
    }

    /**
     * TODO: think of an heuristic. The regular manhattanDistance only works if you have only one goal
     */
    /*
    * Max Acceleration, area under the curve to get the time
    * */
    override fun heuristic(state: RaceTrackState) = 1.0


    // distance is equal to heuristic, since each step has cost of 1
    override fun distance(state: RaceTrackState) = heuristic(state)


    override fun isGoal(state: RaceTrackState): Boolean {
        val newLocation = Location(state.x, state.y)
        return newLocation in finish_line
    }

    /**
     * agent = @
     * blocked cell = ' '
     * track = #
     * finish line = $
     * start line = %
     */
    override fun print(state: RaceTrackState): String {
        val description = StringBuilder()
        for (h in 1..height) {
            for (w in 1..width) {
                val charCell = when (Location(w, h)) {
                    //state.agentLocation -> '@'
                    in finish_line -> '$'
                    //in start_line -> '%'
                    in track -> '*'
                    else -> ' '
                }
                description.append(charCell)
            }
            description.append("\n")
        }

        return description.toString()
    }

    /**
     * TODO: implement racetrack.randomState()
     */
    override fun randomState(): RaceTrackState {
        throw UnsupportedOperationException("Random state not implemented for racetrack domain")
    }

    override fun actionDuration(action: Action<RaceTrackState>) {
        throw UnsupportedOperationException()
    }
}


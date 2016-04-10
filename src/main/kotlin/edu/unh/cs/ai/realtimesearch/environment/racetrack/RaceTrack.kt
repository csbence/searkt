package edu.unh.cs.ai.realtimesearch.environment.racetrack

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
                val finish_line: Set<Location>,
                val actionDuration: Long) : Domain<RaceTrackState> {

    //private val logger = LoggerFactory.getLogger(RaceTrack::class.java)

    val maxXSpeed = getXSpeed()
    val maxYSpeed = getYSpeed()

    fun getXSpeed(): Int {
        var w = 1
        var xSpeed = 0

        while (w <= width) {
            w += xSpeed
            xSpeed++
        }

        return xSpeed - 1
    }

    fun getYSpeed(): Int {
        var h = 1
        var ySpeed = 0

        while (h <= height) {
            h += ySpeed
            ySpeed++
        }

        return ySpeed - 1
    }

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

            for (i in 0..nSteps - 1) {
                x += new_x_speed * dt;
                y += new_y_speed * dt;

                if (!isLegalLocation(x, y)) {
                    valid = false;
                    break;
                }
            }

            //filter on legal moves (not too fast and on the track)
            if (valid) {

                successors.add(SuccessorBundle(
                        RaceTrackState(state.x + new_x_speed, state.y + new_y_speed, new_x_speed, new_y_speed),
                        action,
                        actionCost = actionDuration))
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
    fun isLegalLocation(x: Double, y: Double): Boolean {
        return x >= 0 && y >= 0 && x < width &&
                y < height && Location(Math.round(x.toFloat()), Math.round(y.toFloat())) !in track
    }

    /*
    * Heuristic is the distance divided by the max speed
    * */
    override fun heuristic(state: RaceTrackState): Double {
        var h = distance(state)
        //        return 0.0
        if (maxXSpeed > maxYSpeed)
            return h / maxYSpeed
        return h / maxYSpeed
    }

    override fun heuristic(startState: RaceTrackState, endState: RaceTrackState): Double{
        var h = distance(startState, endState)
        //        return 0.0
        if (maxXSpeed > maxYSpeed)
            return h / maxYSpeed * actionDuration
        return h / maxYSpeed * actionDuration
    }

    // Distance is the max(min(dx), min(dy))
    override fun distance(state: RaceTrackState): Double {
        var dx = Double.MAX_VALUE
        var dy = Double.MAX_VALUE

        for (it in finish_line) {
            val xdist = Math.abs(state.x - it.x)
            if (xdist < dx)
                dx = xdist.toDouble()
            val ydist = Math.abs(state.y - it.y)
            if (ydist < dy)
                dy = ydist.toDouble()
        }
        val retval = Math.max(dx.toDouble(), dy.toDouble())
        return retval;
    }

    // Distance is the max(min(dx), min(dy))
    fun distance(startState: RaceTrackState, endState: RaceTrackState): Double {
        val xdist = Math.abs(startState.x - endState.x)
        val ydist = Math.abs(startState.y - endState.y)

        val retval = Math.max(xdist.toDouble(), ydist.toDouble())
        return retval;
    }

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
                    in finish_line -> '$'
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

    override fun getGoal(): List<RaceTrackState> {
        val list: MutableList<RaceTrackState> = arrayListOf()
        for (it in finish_line) {
            for (xS in 0..maxXSpeed) {
                for (yS in 0..maxYSpeed) {
                    if (xS == 0 && yS == 0) {
                        list.add(RaceTrackState(it.x, it.y, xS, yS))
                    } else if (xS == 0) {
                        list.add(RaceTrackState(it.x, it.y, xS, yS))
                        list.add(RaceTrackState(it.x, it.y, xS, -yS))
                    } else if (yS == 0) {
                        list.add(RaceTrackState(it.x, it.y, xS, yS))
                        list.add(RaceTrackState(it.x, it.y, -xS, yS))
                    } else {
                        list.add(RaceTrackState(it.x, it.y, xS, yS))
                        list.add(RaceTrackState(it.x, it.y, -xS, yS))
                        list.add(RaceTrackState(it.x, it.y, xS, -yS))
                        list.add(RaceTrackState(it.x, it.y, -xS, -yS))
                    }
                }
            }
        }

        return list
    }

    override fun predecessors(state: RaceTrackState): List<SuccessorBundle<RaceTrackState>> {
        val predecessors: MutableList<SuccessorBundle<RaceTrackState>> = arrayListOf()

        for (action in RaceTrackAction.values()) {
            val new_x_speed = state.x_speed
            val new_y_speed = state.y_speed

            var x: Double
            var y: Double
            val dt = 0.1
            val nSteps = 10
            var valid = true

            for (i in 1..nSteps) {
                x = state.x - (new_x_speed * (dt * i));
                y = state.y - (new_y_speed * (dt * i));

                if (!isLegalLocation(x, y)) {
                    valid = false;
                    break;
                }
            }

            //filter on legal moves (not too fast and on the track)
            if (valid) {
                predecessors.add(SuccessorBundle(
                        RaceTrackState(state.x - new_x_speed, state.y - new_y_speed, new_x_speed - action.getAcceleration().x, new_y_speed - action.getAcceleration().y),
                        action,
                        actionCost = actionDuration))
            }
        }
        return predecessors
    }
}
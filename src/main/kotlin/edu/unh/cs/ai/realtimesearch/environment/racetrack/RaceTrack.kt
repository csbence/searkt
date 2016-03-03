package edu.unh.cs.ai.realtimesearch.environment.racetrack

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import edu.unh.cs.ai.realtimesearch.environment.pointrobot.PointRobotAction
import edu.unh.cs.ai.realtimesearch.environment.pointrobot.PointRobotState
import java.util.*

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
                        RaceTrackState(state.x + state.x_speed, state.y + state.y_speed, new_x_speed, new_y_speed),
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
    override fun heuristic(state: RaceTrackState): Double {
        //Distance Formula
        //        var a = -1 * distance(state)
        //        var b = pythagorean(state.xdot, state.ydot)
        //        var result1 = quadraticFormula(a, b, 0.5)
        //        println("" + a + " " + b + " "+ 0.5 + " " + state.loc.x + " " + state.loc.y + " " + result1)
        //        var result2 = quadraticFormula(a, b, -0.5)
        //        println("" + a + " " + b + " -"+ 0.5 + " " + state.loc.x + " " + state.loc.y + " " + result2)
        //
        //        return Math.max(result1, result2)
//        var bx = state.xdot
//        var cx = state.loc.x - endLocation.x
//
//        var by = state.ydot
//        var cy = state.loc.y - endLocation.y
//
//        var resultx1 = quadraticFormula(0.5, bx, cx)
//        var resultx2 = quadraticFormula(-0.5, bx, cx)
//
//        var resulty1 = quadraticFormula(0.5, by, cy)
//        var resulty2 = quadraticFormula(-0.5, by, cy)
//
//        //        println("" + resultx1 + " " + resultx2 + " "+ resulty1 + " " + resulty2 + " "
//        //                + Math.max(Math.min(resultx1, resultx2), Math.min(resulty1, resulty2)))
//
//        return Math.max(Math.min(resultx1, resultx2), Math.min(resulty1, resulty2))
        return 0.0;
    }

    //    fun pythagorean(a : Double, b : Double) : Double{
    //        var result = Math.pow(a, 2.0) + Math.pow(b, 2.0)
    //        return Math.sqrt(result)
    //    }

    fun quadraticFormula(a : Double, b : Double, c : Double) : Double{
        if(Math.pow(b, 2.0) - 4 * a * c < 0.0)
            return Double.MAX_VALUE

        var result1 = -1 * b + Math.sqrt(Math.pow(b, 2.0) - 4 * a * c)
        result1 /= (2 * a)

        var result2 = -1 * b - Math.sqrt(Math.pow(b, 2.0) - 4 * a * c)
        result2 /= (2 * a)
        if(result1 < 0.0 && result2 >= 0.0)
            return result2;
        if(result2 < 0.0 && result1 >= 0.0)
            return result1;

        if(result1 < 0.0 && result2 < 0.0)
            return Double.MAX_VALUE;

        return Math.max(result1, result2)
    }

    override fun distance(state: RaceTrackState): Double {
        //Distance Formula
//        return (Math.sqrt(
//                Math.pow((endLocation.x) - state.loc.x, 2.0)
//                        + Math.pow((endLocation.y) - state.loc.y, 2.0)) - goalRadius)
        return 0.0;
    }

    override fun isGoal(state: RaceTrackState): Boolean {
        //        val curXLoc = (state.x * 2).toInt() / 2.0
        //        val curYLoc = (state.y * 2).toInt() / 2.0
        //
        //        //        println("" + state.x + " " + curXLoc + " " + state.y + " " + curYLoc)
        //
        //
        //
        //        return (endLocation.x + 0.5) == curXLoc && (endLocation.y + 0.5) == curYLoc
        //        return endLocation.x == state.x && (endLocation.y + 0.5) == curYLoc
        return distance(state) <= 0 //&& state.xdot == 0.0 && state.ydot == 0.0;
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
}


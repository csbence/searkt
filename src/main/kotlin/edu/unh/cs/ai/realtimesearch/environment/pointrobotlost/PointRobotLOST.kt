package edu.unh.cs.ai.realtimesearch.environment.pointrobotlost

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.environment.location.DoubleLocation
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import java.util.*

/**
 * Double Integrator Domain
 */
class PointRobotLOST(val width: Int, val height: Int, val blockedCells: Set<Location>,
                     val endLocation: DoubleLocation, val goalRadius: Double, val actionDuration: Long) : Domain<PointRobotLOSTState> {

    //    private val logger = LoggerFactory.getLogger(DoubleIntegrator::class.java)
    private var actions = getAllActions()
    var maxXSpeed = 0
    var maxYSpeed = 0

    private fun getAllActions(): ArrayList<PointRobotLOSTAction> {
        val a = ArrayList<PointRobotLOSTAction>()
        maxXSpeed = width
        maxYSpeed = height
        val rangeXSpeed = width * 2
        val rangeYSpeed = height * 2
        var itX = 0
        while (itX <= rangeXSpeed) {
            var itY = 0
            while (itY <= rangeYSpeed) {
                val xDot = ((itX) - width)
                val yDot = ((itY) - height)
                //                println("" + xdot + " " + ydot)
                a.add(PointRobotLOSTAction(xDot.toDouble(), yDot.toDouble()))
                itY++
            }
            itX++
        }
        return a
    }

    override fun successors(state: PointRobotLOSTState): List<SuccessorBundle<PointRobotLOSTState>> {
        // to return
        val successors: MutableList<SuccessorBundle<PointRobotLOSTState>> = arrayListOf()

        for (it in actions) {
            //            println(it)
            val nSteps = 1000
            val dt = 1.0 / nSteps
            var valid = true

            for (i in 1..nSteps) {
                val x = state.x + (it.xdot * (dt * i))
                val y = state.y + (it.ydot * (dt * i))
                //                x += it.xdot * dt;
                //                y += it.ydot * dt;

                if (!isLegalLocation(x, y)) {
                    valid = false
                    break
                }
            }

            if (valid) {
                successors.add(SuccessorBundle(
                        PointRobotLOSTState(state.x + it.xdot, state.y + it.ydot),
                        PointRobotLOSTAction(it.xdot, it.ydot),
                        actionDuration))
            }
        }
        return successors
    }

    /**
     * Returns whether location within boundaries and not a blocked cell.
     *
     * @param x coordinate of the location
     * @param y coordinate of the location
     * @return true if location is legal
     */
    private fun isLegalLocation(x: Double, y: Double): Boolean {
        return x >= 0 && y >= 0 && x < width &&
                y < height && Location(x.toInt(), y.toInt()) !in blockedCells
    }

    /*
    * eight way - octile distance
    * max(min(dX), min(dy))/3
    * euclidiean distance
    * */
    override fun heuristic(state: PointRobotLOSTState): Double {
        //Distance Formula
        //                return 0.0
        val h = distance(state)

        if (maxXSpeed < maxYSpeed)
            return h / maxYSpeed * actionDuration
        return h / maxXSpeed * actionDuration
    }

    override fun heuristic(startState: PointRobotLOSTState, endState: PointRobotLOSTState): Double {
        //Distance Formula
        //        return 0.0
        val h = distance(startState, endState)

        if (maxXSpeed < maxYSpeed)
            return h / maxYSpeed * actionDuration
        return h / maxXSpeed * actionDuration
    }

    override fun distance(state: PointRobotLOSTState): Double {
        //Distance Formula
        return (Math.sqrt(
                Math.pow((endLocation.x) - state.x, 2.0)
                        + Math.pow((endLocation.y) - state.y, 2.0)) - goalRadius)
    }

    fun distance(startState: PointRobotLOSTState, endState: PointRobotLOSTState): Double {
        //Distance Formula
        return (Math.sqrt(
                Math.pow((endState.x) - startState.x, 2.0)
                        + Math.pow((endState.y) - startState.y, 2.0)) - goalRadius)
    }

    override fun isGoal(state: PointRobotLOSTState): Boolean = distance(state) <= 0
            //        val curXLoc = (state.x * 2).toInt() / 2.0
            //        val curYLoc = (state.y * 2).toInt() / 2.0
            //
            //        //        println("" + state.x + " " + curXLoc + " " + state.y + " " + curYLoc)
            //
            //
            //
            //        return (endLocation.x + 0.5) == curXLoc && (endLocation.y + 0.5) == curYLoc
            //        return endLocation.x == state.x && (endLocation.y + 0.5) == curYLoc

    override fun print(state: PointRobotLOSTState): String {
        val description = StringBuilder()

        description.append("State: at (")
        description.append(state.x)
        description.append(", ")
        description.append(state.y)
        description.append(")")

        return description.toString()
    }

    override fun getGoals(): List<PointRobotLOSTState> = listOf(PointRobotLOSTState(endLocation.x, endLocation.y))

    override fun predecessors(state: PointRobotLOSTState): List<SuccessorBundle<PointRobotLOSTState>> {
        val predecessors: MutableList<SuccessorBundle<PointRobotLOSTState>> = arrayListOf()

        for (it in actions) {
            val nSteps = 1000
            val dt = 1.0 / nSteps
            var valid = true

            for (i in 1..nSteps) {
                val x = state.x - (it.xdot * (dt * i))
                val y = state.y - (it.ydot * (dt * i))

                if (!isLegalLocation(x, y)) {
                    valid = false
                    break
                }
            }

            if (valid) {
                predecessors.add(SuccessorBundle(
                        PointRobotLOSTState(state.x - it.xdot, state.y - it.ydot),
                        PointRobotLOSTAction(it.xdot, it.ydot),
                        actionDuration))
            }
        }
        return predecessors
    }
}

package edu.unh.cs.ai.realtimesearch.environment.pointrobot

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.environment.location.DoubleLocation
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import java.lang.Math.pow
import java.lang.Math.sqrt
import java.util.*

/**
 * Double Integrator Domain
 */
class PointRobot(val width: Int, val height: Int, val blockedCells: Set<Location>,
                 val endLocation: DoubleLocation, val goalRadius: Double, val actionDuration: Long) : Domain<PointRobotState> {

    //    private val logger = LoggerFactory.getLogger(DoubleIntegrator::class.java)
    private var actions = getAllActions()

    fun getAllActions(): ArrayList<PointRobotAction> {
        val actions = ArrayList<PointRobotAction>()
        for (x in 0..6) {
            for (y in 0..6) {
                val xdot = ((x) - 3.0);
                val ydot = ((y) - 3.0);
                actions.add(PointRobotAction(xdot, ydot))
            }
        }
        return actions
    }

    override fun successors(state: PointRobotState): List<SuccessorBundle<PointRobotState>> {
        val successors: MutableList<SuccessorBundle<PointRobotState>> = arrayListOf()

        for ((xdot, ydot) in actions) {
            val dt = 0.1
            val nSteps = 10
            var valid = true

            for (i in 1..nSteps) {
                val x = state.x + (xdot * (dt * i));
                val y = state.y + (ydot * (dt * i));

                if (!isLegalLocation(x, y)) {
                    valid = false
                    break
                }
            }

            if (valid) {
                successors.add(SuccessorBundle(
                        PointRobotState(state.x + xdot, state.y + ydot),
                        PointRobotAction(xdot, ydot),
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
    fun isLegalLocation(x: Double, y: Double): Boolean {
        return x >= 0 && y >= 0 && x < width &&
                y < height && Location(x.toInt(), y.toInt()) !in blockedCells
    }

    /*
    * eight way - octile distance
    * max(min(dX), min(dy))/3
    * euclidiean distance
    * */
    override fun heuristic(state: PointRobotState): Double {
        //Distance Formula
        return (distance(state) / 3) * actionDuration
    }

    override fun heuristic(startState: PointRobotState, endState: PointRobotState): Double {
        //Distance Formula
        return ((sqrt(pow((endState.x) - startState.x, 2.0) + pow((endState.y) - startState.y, 2.0)) - goalRadius) / 3) * actionDuration
    }

    override fun distance(state: PointRobotState): Double {
        //Distance Formula
        return (sqrt(pow((endLocation.x) - state.x, 2.0) + pow((endLocation.y) - state.y, 2.0)) - goalRadius)
    }

    override fun isGoal(state: PointRobotState): Boolean {
        return distance(state) <= 0
    }

    override fun print(state: PointRobotState): String {
        val description = StringBuilder()

        description.append("State: at (")
        description.append(state.x)
        description.append(", ")
        description.append(state.y)
        description.append(")")

        return description.toString()
    }

    override fun getGoals(): List<PointRobotState> {
        return listOf(PointRobotState(endLocation.x, endLocation.y))
    }

    override fun predecessors(state: PointRobotState): List<SuccessorBundle<PointRobotState>> {
        val predecessors: MutableList<SuccessorBundle<PointRobotState>> = arrayListOf()

        for ((xdot, ydot) in actions) {
            val dt = 0.1
            val nSteps = 10
            var valid = true

            for (i in 1..nSteps) {
                val x = state.x - (xdot * (dt * i))
                val y = state.y - (ydot * (dt * i))

                if (!isLegalLocation(x, y)) {
                    valid = false
                    break
                }
            }

            if (valid) {
                predecessors.add(SuccessorBundle(
                        PointRobotState(state.x - xdot, state.y - ydot),
                        PointRobotAction(xdot, ydot),
                        actionDuration))
            }
        }
        return predecessors
    }
}


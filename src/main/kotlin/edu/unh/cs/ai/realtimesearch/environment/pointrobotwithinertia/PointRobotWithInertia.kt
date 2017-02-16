package edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.environment.location.DoubleLocation
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import java.lang.Math.*
import java.util.*

/**
 * Double Integrator Domain
 */
class PointRobotWithInertia(val width: Int, val height: Int, val blockedCells: Set<Location>,
                            val endLocation: DoubleLocation, val goalRadius: Double,
                            val numActions: Int = defaultNumActions,
                            val actionFraction: Double = defaultActionFraction,
                            val stateFraction: Double = defaultStateFraction,
                            val actionDuration: Long) : Domain<PointRobotWithInertiaState> {
    companion object {
        val defaultNumActions = 3
        val defaultActionFraction = 1.0
        val defaultStateFraction = 0.5
    }

    val numAction = numActions // total number of accelerations avaliable in one direction
    val fractions = actionFraction // number of values between whole numbers i.e. How many actions should there be in the range [0,1)?
    var maxAcc = -1.0
    private var actions = getAllActions()

    fun getAllActions(): ArrayList<PointRobotWithInertiaAction> {
        val actions = ArrayList<PointRobotWithInertiaAction>()
        //        for (itX in 0..4) {
        //            for (itY in 0..4) {
        //                var xDoubleDot = ((itX) - 2.0) / 4.0;
        //                var yDoubleDot = ((itY) - 2.0) / 4.0;
        for (x in 0..numAction - 1) {
            for (y in 0..numAction - 1) {
                val xDoubleDot = ((x) - ((numAction - (1.0)) / 2)) / fractions
                val yDoubleDot = ((y) - ((numAction - (1.0)) / 2)) / fractions
                if (maxAcc < 0)
                    maxAcc = -1 * xDoubleDot
                //                                println("" + xDoubleDot + " " + yDoubleDot)
                actions.add(PointRobotWithInertiaAction(xDoubleDot, yDoubleDot))
                //                println(maxAcc);
            }
        }

        return actions
    }

    override fun successors(state: PointRobotWithInertiaState): List<SuccessorBundle<PointRobotWithInertiaState>> {
        // to return
        val successors: MutableList<SuccessorBundle<PointRobotWithInertiaState>> = ArrayList(9)

        //        println(state)
        for ((xDoubleDot, yDoubleDot) in actions) {
            val nSteps = 100
            val dt = 1.0 / nSteps
            var valid = true
            var x = state.x
            var y = state.y
            var xdot = state.xdot
            var ydot = state.ydot

            for (i in 1..nSteps) {
                xdot += xDoubleDot * dt
                ydot += yDoubleDot * dt
                x += xdot * dt
                y += ydot * dt

                if (!isLegalLocation(x, y)) {
                    valid = false
                    break
                }
            }

            if (valid) {
                //                println0("" + x + " " + y + " " + (state.loc.x + state.xdot) + " " + (state.loc.y + state.ydot))
                //                println("\t" + PointRobotWithInertiaState(x, y, state.xdot + it.xDoubleDot, state.ydot + it.yDoubleDot))
                successors.add(SuccessorBundle(
                        PointRobotWithInertiaState(x, y, state.xdot + xDoubleDot, state.ydot + yDoubleDot, stateFraction),
                        PointRobotWithInertiaAction(xDoubleDot, yDoubleDot),
                        actionDuration))
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
        val inBounds = x >= 0 && y >= 0 && x < width &&
                y < height
        val notBlocked = Location(x.toInt(), y.toInt()) !in blockedCells
        return inBounds && notBlocked

        //        return x >= 0 && y >= 0 && x < width &&
        //                y < height && Location(x.toInt(), y.toInt()) !in blockedCells
    }

    /*
    * eight way - octile distance
    * max(min(dX), min(dy))/3
    * euclidiean distance
    * */
    override fun heuristic(state: PointRobotWithInertiaState): Double {
        //Distance Formula
        val bx = state.xdot
        val cx = state.x - endLocation.x

        val by = state.ydot
        val cy = state.y - endLocation.y

        val resultx1 = quadraticFormula(0.5 * maxAcc, bx, cx)
        val resultx2 = quadraticFormula(-0.5 * maxAcc, bx, cx)

        val resulty1 = quadraticFormula(0.5 * maxAcc, by, cy)
        val resulty2 = quadraticFormula(-0.5 * maxAcc, by, cy)

        //        println("" + resultx1 + " " + resultx2 + " "+ resulty1 + " " + resulty2 + " "
        //                + Math.max(Math.min(resultx1, resultx2), Math.min(resulty1, resulty2)))

        val minx = min(resultx1, resultx2)
        val miny = min(resulty1, resulty2)

        val retval: Double

        if (minx == Double.MAX_VALUE && miny != Double.MAX_VALUE)
            retval = miny
        else if (minx != Double.MAX_VALUE && miny == Double.MAX_VALUE)
            retval = minx
        else if (minx == Double.MAX_VALUE && miny == Double.MAX_VALUE)
            retval = 0.0
        else
            retval = max(minx, miny)
        //        println(retval)
        return retval * actionDuration
    }

    override fun heuristic(startState: PointRobotWithInertiaState, endState: PointRobotWithInertiaState): Double {
        //Distance Formula
        val bx = startState.xdot
        val cx = startState.x - endState.x

        val by = startState.ydot
        val cy = startState.y - endState.y

        val resultx1 = quadraticFormula(0.5, bx, cx)
        val resultx2 = quadraticFormula(-0.5, bx, cx)

        val resulty1 = quadraticFormula(0.5, by, cy)
        val resulty2 = quadraticFormula(-0.5, by, cy)

        val minx = min(resultx1, resultx2)
        val miny = min(resulty1, resulty2)

        return actionDuration * when {
            minx == Double.MAX_VALUE && miny != Double.MAX_VALUE -> miny
            minx != Double.MAX_VALUE && miny == Double.MAX_VALUE -> minx
            minx == Double.MAX_VALUE && miny == Double.MAX_VALUE -> 0.0
            else -> max(minx, miny)
        }
    }

    fun quadraticFormula(a: Double, b: Double, c: Double): Double {
        if (pow(b, 2.0) - 4 * a * c < 0.0)
            return Double.MAX_VALUE

        var result1 = -1 * b + sqrt(pow(b, 2.0) - 4 * a * c)
        result1 /= (2 * a)

        var result2 = -1 * b - sqrt(pow(b, 2.0) - 4 * a * c)
        result2 /= (2 * a)

        return when {
            result1 < 0.0 && result2 >= 0.0 -> result2
            result2 < 0.0 && result1 >= 0.0 -> result1
            result1 < 0.0 && result2 < 0.0 -> Double.MAX_VALUE
            else -> max(result1, result2)
        }
    }

    override fun distance(state: PointRobotWithInertiaState): Double {
        //Distance Formula
        return (sqrt(
                pow((endLocation.x) - state.x, 2.0)
                        + pow((endLocation.y) - state.y, 2.0)) - goalRadius)
    }

    override fun isGoal(state: PointRobotWithInertiaState): Boolean {
        return distance(state) <= 0 && state.xdot == 0.0 && state.ydot == 0.0
    }

    override fun print(state: PointRobotWithInertiaState): String {
        val description = StringBuilder()

        description.append("State: at (")
        description.append(state.x)
        description.append(", ")
        description.append(state.y)
        description.append(") going ")
        description.append(state.xdot)
        description.append(" in the ")
        description.append(state.ydot)
        description.append("direction.")

        return description.toString()
    }

    fun randomState(): PointRobotWithInertiaState {
        throw UnsupportedOperationException()
    }

    override fun getGoals(): List<PointRobotWithInertiaState> {
        return listOf(PointRobotWithInertiaState(endLocation.x, endLocation.y, 0.0, 0.0, stateFraction))
    }

    override fun predecessors(state: PointRobotWithInertiaState): List<SuccessorBundle<PointRobotWithInertiaState>> {
        val predecessors: MutableList<SuccessorBundle<PointRobotWithInertiaState>> = arrayListOf()

        for (it in actions) {
            val nSteps = 100
            val dt = 1.0 / nSteps
            var valid = true
            var x = state.x
            var y = state.y
            var xdot = state.xdot
            var ydot = state.ydot

            for (i in 1..nSteps) {
                x -= xdot * dt
                y -= ydot * dt
                xdot -= it.xDoubleDot * dt
                ydot -= it.yDoubleDot * dt

                if (!isLegalLocation(x, y)) {
                    valid = false
                    break
                }
            }

            if (valid) {
                //                println("" + x + " " + y + " " + (state.loc.x + state.xdot) + " " + (state.loc.y + state.ydot))
                predecessors.add(SuccessorBundle(
                        PointRobotWithInertiaState(x, y, state.xdot - it.xDoubleDot, state.ydot - it.yDoubleDot, stateFraction),
                        PointRobotWithInertiaAction(it.xDoubleDot, it.yDoubleDot),
                        actionDuration))
            }
        }
        return predecessors
    }
}


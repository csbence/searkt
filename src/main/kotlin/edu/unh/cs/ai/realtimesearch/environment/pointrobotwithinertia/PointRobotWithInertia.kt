package edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.environment.location.DoubleLocation
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import java.util.*

/**
 * Double Integrator Domain
 */
class PointRobotWithInertia(val width: Int, val height: Int, val blockedCells: Set<Location>,
                            val endLocation: DoubleLocation, val goalRadius: Double) : Domain<PointRobotWithInertiaState> {

    private var actions = getAllActions()

    fun getAllActions(): ArrayList<PointRobotWithInertiaAction> {
        var a = ArrayList<PointRobotWithInertiaAction>()
        //        for (itX in 0..4) {
        //            for (itY in 0..4) {
        //                var xDoubleDot = ((itX) - 2.0) / 4.0;
        //                var yDoubleDot = ((itY) - 2.0) / 4.0;
        for (itX in 0..2) {
            for (itY in 0..2) {
                var xDoubleDot = (itX) - 1.0;
                var yDoubleDot = (itY) - 1.0;
                //                                println("" + xDoubleDot + " " + yDoubleDot)
                a.add(PointRobotWithInertiaAction(xDoubleDot, yDoubleDot))
            }
        }
        return a
    }

    override fun successors(state: PointRobotWithInertiaState): List<SuccessorBundle<PointRobotWithInertiaState>> {
        // to return
        val successors: MutableList<SuccessorBundle<PointRobotWithInertiaState>> = arrayListOf()


        for (it in actions) {
            val nSteps = 100
            val dt = 1.0 / nSteps
            var valid = true
            var x = state.loc.x
            var y = state.loc.y
            var xdot = state.xdot
            var ydot = state.ydot

            for (i in 1..nSteps) {
                xdot += it.xDoubleDot * dt
                ydot += it.yDoubleDot * dt
                x += xdot * dt;
                y += ydot * dt;

                if (!isLegalLocation(x, y)) {
                    valid = false;
                    break;
                }
            }


            if (valid) {
                //                println("" + x + " " + y + " " + (state.loc.x + state.xdot) + " " + (state.loc.y + state.ydot))
                successors.add(SuccessorBundle(
                        PointRobotWithInertiaState(DoubleLocation(x, y), state.xdot + it.xDoubleDot, state.ydot + it.yDoubleDot),
                        PointRobotWithInertiaAction(it.xDoubleDot, it.yDoubleDot),
                        1));
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
                y < height && Location(x.toInt(), y.toInt()) !in blockedCells
    }

    /*
    * eight way - octile distance
    * max(min(dx), min(dy))/3
    * euclidiean distance
    * */
    override fun heuristic(state: PointRobotWithInertiaState): Double {
        //Distance Formula
        var bx = state.xdot
        var cx = state.loc.x - endLocation.x

        var by = state.ydot
        var cy = state.loc.y - endLocation.y

        var resultx1 = quadraticFormula(0.5, bx, cx)
        var resultx2 = quadraticFormula(-0.5, bx, cx)

        var resulty1 = quadraticFormula(0.5, by, cy)
        var resulty2 = quadraticFormula(-0.5, by, cy)

        //        println("" + resultx1 + " " + resultx2 + " "+ resulty1 + " " + resulty2 + " "
        //                + Math.max(Math.min(resultx1, resultx2), Math.min(resulty1, resulty2)))

        var minx = Math.min(resultx1, resultx2)
        var miny = Math.min(resulty1, resulty2)

        var retval: Double

        if (minx == Double.MAX_VALUE && miny != Double.MAX_VALUE)
            retval = miny;
        else if (minx != Double.MAX_VALUE && miny === Double.MAX_VALUE)
            retval = minx
        else if (minx == Double.MAX_VALUE && miny == Double.MAX_VALUE)
            retval = 0.0;
        else
            retval = Math.max(minx, miny)
        //        println(retval)
        return retval
    }


    override fun heuristic(startState: PointRobotWithInertiaState, endState: PointRobotWithInertiaState): Double {
        //Distance Formula
        var bx = startState.xdot
        var cx = startState.loc.x - endState.loc.x

        var by = startState.ydot
        var cy = startState.loc.y - endState.loc.y

        var resultx1 = quadraticFormula(0.5, bx, cx)
        var resultx2 = quadraticFormula(-0.5, bx, cx)

        var resulty1 = quadraticFormula(0.5, by, cy)
        var resulty2 = quadraticFormula(-0.5, by, cy)

        //        println("" + resultx1 + " " + resultx2 + " "+ resulty1 + " " + resulty2 + " "
        //                + Math.max(Math.min(resultx1, resultx2), Math.min(resulty1, resulty2)))

        var minx = Math.min(resultx1, resultx2)
        var miny = Math.min(resulty1, resulty2)

        var retval: Double

        if (minx == Double.MAX_VALUE && miny != Double.MAX_VALUE)
            retval = miny;
        else if (minx != Double.MAX_VALUE && miny === Double.MAX_VALUE)
            retval = minx
        else if (minx == Double.MAX_VALUE && miny == Double.MAX_VALUE)
            retval = 0.0;
        else
            retval = Math.max(minx, miny)
        //        println(retval)
        return retval
    }

    fun quadraticFormula(a: Double, b: Double, c: Double): Double {
        if (Math.pow(b, 2.0) - 4 * a * c < 0.0)
            return Double.MAX_VALUE

        var result1 = -1 * b + Math.sqrt(Math.pow(b, 2.0) - 4 * a * c)
        result1 /= (2 * a)

        var result2 = -1 * b - Math.sqrt(Math.pow(b, 2.0) - 4 * a * c)
        result2 /= (2 * a)
        if (result1 < 0.0 && result2 >= 0.0)
            return result2;
        if (result2 < 0.0 && result1 >= 0.0)
            return result1;

        if (result1 < 0.0 && result2 < 0.0)
            return Double.MAX_VALUE;

        return Math.max(result1, result2)
    }

    override fun distance(state: PointRobotWithInertiaState): Double {
        //Distance Formula
        return (Math.sqrt(
                Math.pow((endLocation.x) - state.loc.x, 2.0)
                        + Math.pow((endLocation.y) - state.loc.y, 2.0)) - goalRadius)
    }

    override fun isGoal(state: PointRobotWithInertiaState): Boolean {
        return distance(state) <= 0 && state.xdot == 0.0 && state.ydot == 0.0;
    }

    override fun print(state: PointRobotWithInertiaState): String {
        val description = StringBuilder()

        description.append("State: at (")
        description.append(state.loc.x)
        description.append(", ")
        description.append(state.loc.y)
        description.append(") going ")
        description.append(state.xdot)
        description.append(" in the ")
        description.append(state.ydot)
        description.append("direction.")

        return description.toString()
    }

    override fun randomState(): PointRobotWithInertiaState {
        throw UnsupportedOperationException()
    }

    override fun getGoal(): PointRobotWithInertiaState {
        throw UnsupportedOperationException()
    }

    override fun predecessors(state: PointRobotWithInertiaState): List<SuccessorBundle<PointRobotWithInertiaState>> {
        throw UnsupportedOperationException()
    }
}


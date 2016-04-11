package edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.environment.location.DoubleLocation
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import edu.unh.cs.ai.realtimesearch.util.doubleNearGreaterThanOrEquals
import edu.unh.cs.ai.realtimesearch.util.doubleNearLessThanOrEquals
import java.util.*

/**
 * Double Integrator Domain
 *
 * @param width the width of the map in # of cells
 * @param height the height of the map in # of cells
 * @param blockedCells the set of blocked cells on the map
 * @param endLocation the center of the goal cell
 * @param goalRadius the radius of the goal region around the end location
 * @param actionDuration the duration for which an action is applied
 */
class PointRobotWithInertia(val width: Int, val height: Int, val blockedCells: Set<Location>,
                            val endLocation: DoubleLocation, val goalRadius: Double, val actionDuration: Long) : Domain<PointRobotWithInertiaState> {

    /**
     * total number of accelerations available in one direction
     */
    val numAction = 3
    /**
     * number of values between whole numbers i.e. How many actions should there be in the range [0,1)?
     */
    val fractions = 1

    /**
     * The goal radius around 0.0
     */
    val velocityGoalRadius = 0.005
    private val goalState = PointRobotWithInertiaState(endLocation.x, endLocation.y, 0.0, 0.0)
    //    private val blockedCellEdges = getAllBlockedCellEdges(blockedCells)
    private val actions = getAllActions()

    fun getAllActions(): ArrayList<PointRobotWithInertiaAction> {
        var actions = ArrayList<PointRobotWithInertiaAction>()
        //        for (itX in 0..4) {
        //            for (itY in 0..4) {
        //                var xDoubleDot = ((itX) - 2.0) / 4.0;
        //                var yDoubleDot = ((itY) - 2.0) / 4.0;
        for (x in 0..numAction - 1) {
            for (y in 0..numAction - 1) {
                var xDoubleDot = ((x) - ((numAction - (1.0)) / 2)) / fractions
                var yDoubleDot = ((y) - ((numAction - (1.0)) / 2)) / fractions
                //                println("" + xDoubleDot + " " + yDoubleDot)
                actions.add(PointRobotWithInertiaAction(xDoubleDot, yDoubleDot))
            }
        }

        return actions
    }

    //    data class Line(val start: DoubleLocation, val end: DoubleLocation) {
    //        val slope = (end.y - start.y) / (end.x - start.x)
    //        val lineEquationY = { x: Double -> slope * (x - end.x) + end.y }
    //        val lineEquationX = { y: Double -> (y - end.y) / slope + end.x }
    //        val b = slope * (-end.x) + (end.y)
    //    }
    //
    //    data class CellEdges(val topLeftPoint: DoubleLocation) {
    //        val bottomRightPoint = DoubleLocation(bottomRightPoint.x + 1, bottomRightPoint.y + 1)
    //        val topRightPoint = DoubleLocation(bottomRightPoint.x, topLeftPoint.y)
    //        val bottomLeftPoint = DoubleLocation(topLeftPoint.x, bottomRightPoint.y)
    //
    //        val topLine = Line(topLeftPoint, topRightPoint)
    //        val rightLine = Line(topRightPoint, bottomRightPoint)
    //        val leftLine = Line(topLeftPoint, bottomLeftPoint)
    //        val bottomLine = Line(bottomLeftPoint, bottomRightPoint)
    //
    //        val points = listOf(
    //                topLeftPoint,
    //                topRightPoint,
    //                bottomLeftPoint,
    //                bottomRightPoint
    //        )
    //
    //        val lines = listOf(
    //                topLine,
    //                rightLine,
    //                bottomLine,
    //                leftLine
    //        )
    //    }
    //
    //    fun getAllBlockedCellEdges(blockedCells: Set<Location>): Set<CellEdges> {
    //        val cellSet = mutableSetOf<CellEdges>()
    //        for (blockedCell in blockedCells) {
    //            cellSet.add(CellEdges(DoubleLocation(blockedCell.x.toDouble(), blockedCell.y.toDouble())))
    //        }
    //        return cellSet
    //    }

    /**
     * Calculate the next state given the current state and an action
     */
    internal fun calculateNextState(currentState: PointRobotWithInertiaState, action: PointRobotWithInertiaAction): PointRobotWithInertiaState {
        return currentState.calculateNextState(action, actionDuration)
    }

    override fun successors(state: PointRobotWithInertiaState): List<SuccessorBundle<PointRobotWithInertiaState>> {
        // to return
        val successors: MutableList<SuccessorBundle<PointRobotWithInertiaState>> = ArrayList(9)

        //        println(state)
        for (action in actions) {
            //            val nSteps = 100
            //            val dt = 1.0 / nSteps
            //            var valid = true

            val nextState = calculateNextState(state, action)

            //    println("($state) ($action) ~ $actionDuration -> ($nextState), h: ${heuristic(nextState)}")
            if (!isLegalAction(state.x, state.y, nextState.x, nextState.y)) {
                //    println("ILLEGAL")
                continue
            }

            //            var x = state.x
            //            var y = state.y
            //            var xdot = state.xdot
            //            var ydot = state.ydot
            //
            //            for (i in 1..nSteps) {
            //                xdot += it.xDoubleDot * dt
            //                ydot += it.yDoubleDot * dt
            //                x += xdot * dt
            //                y += ydot * dt
            //
            //                if (!isLegalLocation(x, y)) {
            //                    valid = false;
            //                    break;
            //                }
            //            }
            //
            //            if (valid) {
            //                println0("" + x + " " + y + " " + (state.loc.x + state.xdot) + " " + (state.loc.y + state.ydot))
            //                println("\t" + PointRobotWithInertiaState(x, y, state.xdot + it.xDoubleDot, state.ydot + it.yDoubleDot))
            successors.add(SuccessorBundle(nextState, action, actionCost = actionDuration))
            //            }
        }

        return successors
    }

    fun pointInBounds(x: Double, y: Double) = x >= 0 && y >= 0 && x < width && y < height

    /**
     * Returns whether location within boundaries and not a blocked cell.
     *
     * @param location the location to test
     * @return true if location is legal
     */
    fun isLegalLocation(x: Double, y: Double): Boolean {
        val inBounds = pointInBounds(x, y)
        val notBlocked = { Location(x.toInt(), y.toInt()) !in blockedCells }
        return inBounds && notBlocked()

        //        return x >= 0 && y >= 0 && x < width &&
        //                y < height && Location(x.toInt(), y.toInt()) !in blockedCells
    }

    /**
     * Perform raytracing to find all cells the line connecting the two given points pass through.
     * Implementation adapted from {@link http://playtechs.blogspot.ca/2007/03/raytracing-on-grid.html}
     */
    fun raytrace(x0: Double, y0: Double, x1: Double, y1: Double): Set<Location> {
        val visitedCells = mutableSetOf<Location>()
        val dx = Math.abs(x1 - x0)
        val dy = Math.abs(y1 - y0)

        var x = Math.floor(x0).toInt()
        var y = Math.floor(y0).toInt()

        var n = 1
        var x_inc: Int
        var y_inc: Int
        var error: Double

        if (dx == 0.0) {
            x_inc = 0
            error = Double.POSITIVE_INFINITY
        } else if (x1 > x0) {
            x_inc = 1
            n += Math.floor(x1).toInt() - x
            error = (Math.floor(x0) + 1 - x0) * dy
        } else {
            x_inc = -1;
            n += x - Math.floor(x1).toInt()
            error = (x0 - Math.floor(x0)) * dy
        }

        if (dy == 0.0) {
            y_inc = 0
            error -= Double.POSITIVE_INFINITY
        } else if (y1 > y0) {
            y_inc = 1;
            n += Math.floor(y1).toInt() - y
            error -= (Math.floor(y0) + 1 - y0) * dx
        } else {
            y_inc = -1;
            n += y - Math.floor(y1).toInt()
            error -= (y0 - Math.floor(y0)) * dx
        }

        while (n > 0) {
            visitedCells.add(Location(x, y))

            if (error > 0) {
                y += y_inc
                error -= dx
            } else {
                x += x_inc
                error += dy
            }

            n -= 1
        }

        return visitedCells
    }

    fun isLegalAction(initialX: Double, initialY: Double, newX: Double, newY: Double): Boolean {
        if (blockedCells.isNotEmpty()) {
            val visitedCells = raytrace(initialX, initialY, newX, newY)
            for (visistedCell in visitedCells) {
                // TODO optimize
                if (visistedCell in blockedCells)
                    return false
            }
        }
        return pointInBounds(newX, newY)
        //        val actionLine = Line(DoubleLocation(initialX, initialY), DoubleLocation(newX, newY))
        //        val verticalLine = initialX == newX
        //        val horizontalLine = initialY == newY
        //
        //        for (cellEdges in blockedCellEdges) {
        //            // Check bounding boxes
        //            val inBound = false
        //            for (point in cellEdges.points) {
        //                if (verticalLine) {
        //                    if (point.x != initialX)
        //                        continue
        //                }
        //                val minX = Math.min(initialX, newX)
        //                val maxX = if (minX == initialX) newX else initialX
        //                //
        //                if (point.x > initialX && point.x < newX)
        //            }
        //
        //            if (!inBound && !verticalLine)
        //                return false
        //        }
    }

    private fun octileDistance(state: PointRobotWithInertiaState, endState: PointRobotWithInertiaState): Double {
        var bx = state.xdot
        var cx = state.x - endLocation.x

        var by = state.ydot
        var cy = state.y - endLocation.y

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
            retval = miny
        else if (minx != Double.MAX_VALUE && miny === Double.MAX_VALUE)
            retval = minx
        else if (minx == Double.MAX_VALUE && miny == Double.MAX_VALUE)
            retval = 0.0
        else
            retval = Math.max(minx, miny)
        //        println(retval)
        return retval * actionDuration
    }

    /*
    * eight way - octile distance
    * max(min(dx), min(dy))/3
    * euclidiean distance
    * */
    override fun heuristic(state: PointRobotWithInertiaState): Double {
        //Distance Formula
        var bx = state.xdot
        var cx = state.x - endLocation.x

        var by = state.ydot
        var cy = state.y - endLocation.y

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
            retval = miny
        else if (minx != Double.MAX_VALUE && miny === Double.MAX_VALUE)
            retval = minx
        else if (minx == Double.MAX_VALUE && miny == Double.MAX_VALUE)
            retval = 0.0
        else
            retval = Math.max(minx, miny)
        //        println(retval)
        return retval * actionDuration
    }

    override fun heuristic(startState: PointRobotWithInertiaState, endState: PointRobotWithInertiaState): Double {
        //Distance Formula
        var bx = startState.xdot
        var cx = startState.x - endState.x

        var by = startState.ydot
        var cy = startState.y - endState.y

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
        return retval * actionDuration
    }

    fun quadraticFormula(a: Double, b: Double, c: Double): Double {
        if (Math.pow(b, 2.0) - 4 * a * c < 0.0)
            return Double.MAX_VALUE

        var result1 = -1 * b + Math.sqrt(Math.pow(b, 2.0) - 4 * a * c)
        result1 /= (2 * a)

        var result2 = -1 * b - Math.sqrt(Math.pow(b, 2.0) - 4 * a * c)
        result2 /= (2 * a)
        if (result1 < 0.0 && result2 >= 0.0)
            return result2
        if (result2 < 0.0 && result1 >= 0.0)
            return result1

        if (result1 < 0.0 && result2 < 0.0)
            return Double.MAX_VALUE

        return Math.max(result1, result2)
    }

    /**
     * Retrieve the Euclidean distance between the given state and the goal radius.
     */
    override fun distance(state: PointRobotWithInertiaState): Double {
        //Distance Formula
        val distance = Math.sqrt(
                Math.pow((endLocation.x) - state.x, 2.0)
                        + Math.pow((endLocation.y) - state.y, 2.0))
        return if (doubleNearGreaterThanOrEquals(distance, goalRadius)) distance - goalRadius else 0.0
    }

    override fun isGoal(state: PointRobotWithInertiaState): Boolean {
        // distance will return exactly 0.0 if within goal radius
        val inGoalRadius = distance(state) == 0.0
        val xInGoalVelocityRadius = doubleNearGreaterThanOrEquals(state.xdot, -velocityGoalRadius) && doubleNearLessThanOrEquals(state.xdot, velocityGoalRadius)
        val yInGoalVelocityRadius = doubleNearGreaterThanOrEquals(state.ydot, -velocityGoalRadius) && doubleNearLessThanOrEquals(state.ydot, velocityGoalRadius)
        return inGoalRadius && xInGoalVelocityRadius && yInGoalVelocityRadius
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

    override fun randomState(): PointRobotWithInertiaState {
        throw UnsupportedOperationException()
    }

    override fun getGoal(): List<PointRobotWithInertiaState> {
        return listOf(goalState)
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
                x -= xdot * dt;
                y -= ydot * dt;
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
                        PointRobotWithInertiaState(x, y, state.xdot - it.xDoubleDot, state.ydot - it.yDoubleDot),
                        PointRobotWithInertiaAction(it.xDoubleDot, it.yDoubleDot),
                        actionDuration))
            }
        }
        return predecessors
    }
}


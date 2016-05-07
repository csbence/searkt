package edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.environment.location.DoubleLocation
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import edu.unh.cs.ai.realtimesearch.util.doubleNearGreaterThan
import edu.unh.cs.ai.realtimesearch.util.doubleNearGreaterThanOrEqual
import edu.unh.cs.ai.realtimesearch.util.doubleNearLessThanOrEqual
import edu.unh.cs.ai.realtimesearch.util.raytrace
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
     * Number of accelerations available in one direction
     */
    val numActions: Int = 1
    /**
     * number of values between whole numbers i.e. How many actions should there be in the range [0,1)?
     */
    val actionStepSize: Double = 1.0

    val totalActionCount = Math.pow(numActions * 2.0 + 1, 2.0).toInt() // positive and negative, x and y, plus 0

    /**
     * The goal radius around 0.0
     */
    val velocityGoalRadius = 0.005
    private val goalState = PointRobotWithInertiaState(endLocation.x, endLocation.y, 0.0, 0.0)
    //    private val blockedCellEdges = getAllBlockedCellEdges(blockedCells)

    val actions: List<PointRobotWithInertiaAction> = {
        val actions = ArrayList<PointRobotWithInertiaAction>()
        val maxAction = actionStepSize * numActions
        var xAcceleration = -maxAction
        while (!doubleNearGreaterThan(xAcceleration, maxAction)) {
            var yAcceleration = -maxAction
            while (!doubleNearGreaterThan(yAcceleration, maxAction)) {
                actions.add(PointRobotWithInertiaAction(xAcceleration, yAcceleration))
                yAcceleration += actionStepSize
            }
            xAcceleration += actionStepSize
        }
        assert(actions.size == totalActionCount,
                { "Calculated action quantity (${actions.size}) != expected ($totalActionCount)" })

        actions.toList()
    }()

    /**
     * Calculate the next state given the current state and an action
     */
    internal fun calculateNextState(currentState: PointRobotWithInertiaState, action: PointRobotWithInertiaAction): PointRobotWithInertiaState {
        return currentState.calculateNextState(action, actionDuration)
    }

    internal fun calculatePreviousState(currentState: PointRobotWithInertiaState, previousAction: PointRobotWithInertiaAction): PointRobotWithInertiaState {
        return currentState.calculatePreviousState(previousAction, actionDuration)
    }

    override fun successors(state: PointRobotWithInertiaState): List<SuccessorBundle<PointRobotWithInertiaState>> {
        val successors: MutableList<SuccessorBundle<PointRobotWithInertiaState>> = ArrayList(totalActionCount)
        //        println("SUCCESSORS FOR: $state")
        for (action in actions) {
            val nextState = calculateNextState(state, action)

            //                println("($state) ($action) ~ $actionDuration -> ($nextState), h: ${heuristic(nextState)}")
            if (!isLegalAction(state.x, state.y, nextState.x, nextState.y)) {
                //                    println("ILLEGAL")
                continue
            }

            successors.add(SuccessorBundle(nextState, action, actionCost = actionDuration))
        }

        return successors
    }

    override fun predecessors(state: PointRobotWithInertiaState): List<SuccessorBundle<PointRobotWithInertiaState>> {
        val predecessors: MutableList<SuccessorBundle<PointRobotWithInertiaState>> = ArrayList(totalActionCount)
        //        println("PREDECESSORS FOR: $state")
        for (action in actions) {
            val previousState = calculatePreviousState(state, action)

            //            println("($previousState) ($action) ~ $actionDuration -> ($state)")
            if (!pointInBounds(previousState.x, previousState.y) ||
                    !isLegalAction(previousState.x, previousState.y, state.x, state.y)) {
                //                println("ILLEGAL")
                continue
            }

            predecessors.add(SuccessorBundle(previousState, action, actionCost = actionDuration))
        }

        return predecessors
    }

    fun pointInBounds(x: Double, y: Double)
            = doubleNearGreaterThanOrEqual(x, 0.0)
            && doubleNearGreaterThanOrEqual(y, 0.0)
            && doubleNearLessThanOrEqual(x, width.toDouble())
            && doubleNearLessThanOrEqual(y, height.toDouble())

    /**
     * Returns whether location within boundaries and not a blocked cell.
     *
     * @param location the location to test
     * @return true if location is legal
     */
    fun isLegalLocation(location: DoubleLocation): Boolean {
        val inBounds = pointInBounds(location.x, location.y)
        val notBlocked = { Location(location.x.toInt(), location.y.toInt()) !in blockedCells }
        return inBounds && notBlocked()
    }

    fun isLegalAction(initialX: Double, initialY: Double, newX: Double, newY: Double): Boolean {
        if (!pointInBounds(newX, newY))
            return false
        if (blockedCells.isNotEmpty()) {
            val visitedCells = raytrace(initialX, initialY, newX, newY)
            for (visitedCell in visitedCells) {
                if (visitedCell in blockedCells)
                    return false
            }
        }
        return true
    }

    override fun heuristic(state: PointRobotWithInertiaState): Double //= octileDistance(state, goalState) * actionDuration
            = distance(state) * actionDuration

    override fun heuristic(startState: PointRobotWithInertiaState, endState: PointRobotWithInertiaState): Double
            //            = octileDistance(startState, endState) * actionDuration
            = distance(startState, endState) * actionDuration


    /*
    * eight way - octile distance
    * max(min(dx), min(dy))/3
    * euclidiean distance
    * */
    private fun octileDistance(state: PointRobotWithInertiaState, endState: PointRobotWithInertiaState): Double {
        val bx = state.xdot
        val cx = state.x - endState.x

        val by = state.ydot
        val cy = state.y - endState.y

        val resultx1 = quadraticFormula(0.5, bx, cx)
        val resultx2 = quadraticFormula(-0.5, bx, cx)

        val resulty1 = quadraticFormula(0.5, by, cy)
        val resulty2 = quadraticFormula(-0.5, by, cy)

        val minx = Math.min(resultx1, resultx2)
        val miny = Math.min(resulty1, resulty2)

        var retval: Double

        if (minx == Double.MAX_VALUE && miny != Double.MAX_VALUE)
            retval = miny
        else if (minx != Double.MAX_VALUE && miny === Double.MAX_VALUE)
            retval = minx
        else if (minx == Double.MAX_VALUE && miny == Double.MAX_VALUE)
            retval = 0.0
        else
            retval = Math.max(minx, miny)

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
    override fun distance(state: PointRobotWithInertiaState): Double = distance(state, goalState)

    fun distance(startState: PointRobotWithInertiaState, endState: PointRobotWithInertiaState): Double {
        //Distance Formula
        val distance = Math.sqrt(
                Math.pow((endState.x) - startState.x, 2.0)
                        + Math.pow((endState.y) - startState.y, 2.0))
        return if (doubleNearGreaterThanOrEqual(distance, goalRadius)) distance - goalRadius else 0.0
    }

    override fun isGoal(state: PointRobotWithInertiaState): Boolean {
        // distance will return exactly 0.0 if within goal radius
        val inGoalRadius = distance(state) == 0.0
        val xInGoalVelocityRadius = doubleNearGreaterThanOrEqual(state.xdot, -velocityGoalRadius) && doubleNearLessThanOrEqual(state.xdot, velocityGoalRadius)
        val yInGoalVelocityRadius = doubleNearGreaterThanOrEqual(state.ydot, -velocityGoalRadius) && doubleNearLessThanOrEqual(state.ydot, velocityGoalRadius)
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
}


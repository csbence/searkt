package edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldAction
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldState
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Double Integrator Domain
 */
class PointRobotWithInertia(val width: Int, val height: Int, val blockedCells: Set<Location>, val endLocation: Location) : Domain<PointRobotWithInertiaState> {

//    private val logger = LoggerFactory.getLogger(DoubleIntegrator::class.java)
    private var actions = getAllActions()

    fun getAllActions() : ArrayList<PointRobotWithInertiaAction>{
        var a = ArrayList<PointRobotWithInertiaAction>()
        for (itX in 0..4) {
            for (itY in 0..4) {
                var xDoubleDot = ((itX) - 2.0) / 4.0;
                var yDoubleDot = ((itY) - 2.0) / 4.0;
                //                println("" + xDoubleDot + " " + yDoubleDot)
                a.add(PointRobotWithInertiaAction(xDoubleDot, yDoubleDot))
            }
        }
        return a
    }

    override fun successors(state: PointRobotWithInertiaState): List<SuccessorBundle<PointRobotWithInertiaState>> {
        // to return
        val successors: MutableList<SuccessorBundle<PointRobotWithInertiaState>> = arrayListOf()
        val maxSpeed = 3
        val minSpeed = maxSpeed * -1

        for (it in actions) {
            if(it.xDoubleDot + state.xdot > maxSpeed || it.xDoubleDot + state.xdot < minSpeed
                    || it.xDoubleDot + state.xdot == 0.0
                    || it.yDoubleDot + state.ydot > maxSpeed || it.yDoubleDot + state.ydot < minSpeed
                    || it.yDoubleDot + state.ydot == 0.0) {
                continue;
            }

            var x = state.x
            var y = state.y
            val xdot = it.xDoubleDot + state.xdot
            val ydot = it.yDoubleDot + state.ydot
            val dt = 0.1
            val nSteps = 10
            var valid = true

            for (i in 0..nSteps-1) {
                x += xdot * dt;
                y += ydot * dt;

                if (!isLegalLocation(x, y)) {
                    valid = false;
                    break;
                }
            }

            if (valid) {

//                    println("" + state.x + " " + state.y + " " + state.xdot + " " + state.ydot)
//                    println("" + x + " " + y + " " + (state.xdot + it.xDoubleDot) + " " + (state.ydot + it.yDoubleDot) + " " + it.xDoubleDot + " " + it.yDoubleDot)
//                println("" + x + " " + y + " " + (state.x + state.xdot) + " " + (state.y + state.ydot))

                successors.add(SuccessorBundle(
                        PointRobotWithInertiaState(x, y, xdot, ydot),
                        PointRobotWithInertiaAction(it.xDoubleDot, it.yDoubleDot),
                        1.0));
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
                y < height && Location(x.toInt(), y.toInt()) !in blockedCells
    }

    override fun heuristic(state: PointRobotWithInertiaState): Double {
        //Distance Formula
        return Math.sqrt(
                Math.pow((endLocation.x + 0.5) - state.x, 2.0)
                        + Math.pow((endLocation.y + 0.5) - state.y, 2.0)) / 3
    }

    override fun distance(state: PointRobotWithInertiaState) = heuristic(state)

    override fun isGoal(state: PointRobotWithInertiaState): Boolean {
        val curXLoc = (state.x * 2).toInt() / 2.0
        val curYLoc = (state.y * 2).toInt() / 2.0

//        println("" + state.x + " " + curXLoc + " " + state.y + " " + curYLoc)



        return (endLocation.x + 0.5) == curXLoc && (endLocation.y + 0.5) == curYLoc
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

}


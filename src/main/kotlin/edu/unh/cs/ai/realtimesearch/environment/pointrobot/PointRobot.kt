package edu.unh.cs.ai.realtimesearch.environment.pointrobot

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
class PointRobot(val width: Int, val height: Int, val blockedCells: Set<Location>, val endLocation: Location) : Domain<PointRobotState> {

//    private val logger = LoggerFactory.getLogger(DoubleIntegrator::class.java)
    private var actions = getAllActions()

    fun getAllActions() : ArrayList<PointRobotAction>{
        var a = ArrayList<PointRobotAction>()
        for (itX in 0..6) {
            for (itY in 0..6) {
                var xdot = ((itX) - 3.0);
                var ydot = ((itY) - 3.0);
//                println("" + xdot + " " + ydot)
                a.add(PointRobotAction(xdot, ydot))
            }
        }
        return a
    }

    override fun successors(state: PointRobotState): List<SuccessorBundle<PointRobotState>> {
        // to return
        val successors: MutableList<SuccessorBundle<PointRobotState>> = arrayListOf()

        for (it in actions) {
            if (isLegalLocation(state.x + it.xdot, state.y + it.ydot)) {

//                    println("" + state.x + " " + state.y)
//                    println("" + state.x + it.xdot + " " + state.y + it.ydot);
                successors.add(SuccessorBundle(
                        PointRobotState(state.x + it.xdot, state.y + it.ydot),
                        PointRobotAction(it.xdot, it.ydot),
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

    override fun heuristic(state: PointRobotState): Double {
        //Distance Formula
        return Math.sqrt(
                Math.pow(endLocation.x - state.x, 2.0)
                        + Math.pow(endLocation.y - state.y, 2.0)) / 3
    }

    override fun distance(state: PointRobotState) = heuristic(state)

    override fun isGoal(state: PointRobotState): Boolean {
        return endLocation.x == state.x.toInt() && endLocation.y == state.y.toInt()
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

    override fun randomState(): PointRobotState {
        throw UnsupportedOperationException()
    }

}


package edu.unh.cs.ai.realtimesearch.environment.doubleintegrator

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldAction
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldState
import org.slf4j.LoggerFactory

/**
 * Double Integrator Domain
 */
class DoubleIntegrator(val width: Int, val height: Int, val blockedCells: Set<Location>, val endLocation: Location) : Domain<DoubleIntegratorState> {

//    private val logger = LoggerFactory.getLogger(DoubleIntegrator::class.java)

    override fun successors(state: DoubleIntegratorState): List<SuccessorBundle<DoubleIntegratorState>> {
        // to return
        val successors: MutableList<SuccessorBundle<DoubleIntegratorState>> = arrayListOf()

        for (it in DoubleIntegratorAction.values()) {
//            val newLocation = state.agentLocation + it.getRelativeLocation()

            var theta = state.theta
            var speed = state.speed
            var x = state.x
            var y = state.y
            val dt = 0.1
            val nSteps = 10

            // add the legal movement actions
            if (it == DoubleIntegratorAction.ACC) {
                val v = 1
                val t = 0

                for(i in 0..nSteps){
                    theta += t * dt;
                    theta %= (2*Math.PI);
                    if(theta < 0){
                        theta += (2*Math.PI);
                    }

                    speed += v * dt;

                    x += Math.cos(theta) * speed * dt;
                    y += Math.sin(theta) * speed * dt;
                }

                val newLocation = Location(x.toInt(), y.toInt())
                if(isLegalLocation(newLocation)) {
                    successors.add(SuccessorBundle(
                            DoubleIntegratorState(x, y, speed, theta),
                            it,
                            1.0));
                }
            } else if (it == DoubleIntegratorAction.DEC) {
                val v = -1
                val t = 0

                for(i in 0..nSteps){
                    theta += t * dt;
                    theta %= (2*Math.PI);
                    if(theta < 0){
                        theta += (2*Math.PI);
                    }

                    speed += v * dt;

                    x += Math.cos(theta) * speed * dt;
                    y += Math.sin(theta) * speed * dt;
                }

                val newLocation = Location(x.toInt(), y.toInt())
                if(isLegalLocation(newLocation)) {
                    successors.add(SuccessorBundle(
                            DoubleIntegratorState(x, y, speed, theta),
                            it,
                            1.0));
                }
            } else {
                val v = 0
                val t = (Math.PI / 180) * (it.index * 2)

                for(i in 0..nSteps){
                    theta += t * dt;
                    theta %= (2*Math.PI);
                    if(theta < 0){
                        theta += (2*Math.PI);
                    }

                    speed += v * dt;

                    x += Math.cos(theta) * speed * dt;
                    y += Math.sin(theta) * speed * dt;
                }

                val newLocation = Location(x.toInt(), y.toInt())
                if(isLegalLocation(newLocation)) {
                    successors.add(SuccessorBundle(
                            DoubleIntegratorState(x, y, speed, theta),
                            it,
                            1.0));
                }
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
    fun isLegalLocation(location: Location): Boolean {
        return location.x >= 0 && location.y >= 0 && location.x < width &&
                location.y < height && location !in blockedCells
    }

    override fun heuristic(state: DoubleIntegratorState): Double {
        //TODO Implement heuristic!

        return 0.0
    }

    override fun distance(state: DoubleIntegratorState) = heuristic(state)

    override fun isGoal(state: DoubleIntegratorState): Boolean {
        return endLocation.x == state.x.toInt() && endLocation.y == state.y.toInt()
    }

    override fun print(state: DoubleIntegratorState): String {
        val description = StringBuilder()

        description.append("State: at (")
        description.append(state.x)
        description.append(", ")
        description.append(state.y)
        description.append(") going ")
        description.append(state.speed)
        description.append(" in the ")
        description.append(state.theta)
        description.append("direction.")

        return description.toString()
    }

    override fun randomState(): DoubleIntegratorState {
        throw UnsupportedOperationException()
    }

}


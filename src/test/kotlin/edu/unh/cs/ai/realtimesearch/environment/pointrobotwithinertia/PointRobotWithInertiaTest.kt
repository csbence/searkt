package edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia

import edu.unh.cs.ai.realtimesearch.environment.DiscretizedDomain
import edu.unh.cs.ai.realtimesearch.environment.DiscretizedState
import edu.unh.cs.ai.realtimesearch.environment.location.DoubleLocation
import edu.unh.cs.ai.realtimesearch.util.doubleNearEqual
import edu.unh.cs.ai.realtimesearch.util.doubleNearGreaterThan
import org.junit.Test
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.test.assertTrue

/**
 * @author Mike Bogochow (mgp36@unh.edu)
 * @since 4/12/16
 */
class PointRobotWithInertiaTest {

    private val logger = LoggerFactory.getLogger(PointRobotWithInertiaTest::class.java)

    private val pointRobotWithInertia1 = PointRobotWithInertia(5, 5, setOf(), DoubleLocation(4.5, 4.5), 0.5, 320000000)
    private val pointRobotWithInertia2 = PointRobotWithInertia(2, 2, setOf(), DoubleLocation(1.5, 1.5), 0.5, 320000000)

    @Test
    fun testGetGoal() {
        val goalStates = pointRobotWithInertia1.getGoal()

        assertTrue { goalStates.size == 1 }
    }

    @Test
    fun testGoalHeuristic() {
        val pointRobotWithInertia = pointRobotWithInertia1
        val goalState = pointRobotWithInertia.getGoal().first()
        val heuristic1 = pointRobotWithInertia.heuristic(goalState)
        val heuristic2 = pointRobotWithInertia.heuristic(
                PointRobotWithInertiaState(goalState.x - pointRobotWithInertia.goalRadius, goalState.y, goalState.xdot, goalState.ydot))
        val heuristic3 = pointRobotWithInertia.heuristic(
                PointRobotWithInertiaState(goalState.x + pointRobotWithInertia.goalRadius, goalState.y, goalState.xdot, goalState.ydot))
        val heuristic4 = pointRobotWithInertia.heuristic(
                PointRobotWithInertiaState(goalState.x, goalState.y - pointRobotWithInertia.goalRadius, goalState.xdot, goalState.ydot))
        val heuristic5 = pointRobotWithInertia.heuristic(
                PointRobotWithInertiaState(goalState.x, goalState.y + pointRobotWithInertia.goalRadius, goalState.xdot, goalState.ydot))

        assertTrue { doubleNearEqual(heuristic1, 0.0) }
        assertTrue { doubleNearEqual(heuristic2, 0.0) }
        assertTrue { doubleNearEqual(heuristic3, 0.0) }
        assertTrue { doubleNearEqual(heuristic4, 0.0) }
        assertTrue { doubleNearEqual(heuristic5, 0.0) }
    }

    @Test
    fun testHeuristic1() {
        val pointRobotWithInertia = pointRobotWithInertia1
        val goalState = pointRobotWithInertia.getGoal().first()
        val heuristic1 = pointRobotWithInertia.heuristic(
                PointRobotWithInertiaState(goalState.x - pointRobotWithInertia.goalRadius * 2, goalState.y, goalState.xdot, goalState.ydot))
        val heuristic2 = pointRobotWithInertia.heuristic(
                PointRobotWithInertiaState(goalState.x + pointRobotWithInertia.goalRadius * 2, goalState.y, goalState.xdot, goalState.ydot))
        val heuristic3 = pointRobotWithInertia.heuristic(
                PointRobotWithInertiaState(goalState.x, goalState.y - pointRobotWithInertia.goalRadius * 2, goalState.xdot, goalState.ydot))
        val heuristic4 = pointRobotWithInertia.heuristic(
                PointRobotWithInertiaState(goalState.x, goalState.y + pointRobotWithInertia.goalRadius * 2, goalState.xdot, goalState.ydot))

        assertTrue { doubleNearGreaterThan(heuristic1, 0.0) }
        assertTrue { doubleNearGreaterThan(heuristic2, 0.0) }
        assertTrue { doubleNearGreaterThan(heuristic3, 0.0) }
        assertTrue { doubleNearGreaterThan(heuristic4, 0.0) }
    }

    @Test
    fun testSuccessors1() {
        val pointRobotWithInertia = pointRobotWithInertia1
        val actions = pointRobotWithInertia.actions
        val state = PointRobotWithInertiaState(pointRobotWithInertia.width / 2.0, pointRobotWithInertia.height / 2.0, 0.0, 0.0)
        val successors = pointRobotWithInertia.successors(state)

        assertTrue { successors.size == actions.size }
    }

    @Test
    fun testPredecessors1() {
        val pointRobotWithInertia = pointRobotWithInertia1
        val actions = pointRobotWithInertia.actions
        val state = PointRobotWithInertiaState(pointRobotWithInertia.width / 2.0, pointRobotWithInertia.height / 2.0, 0.0, 0.0)
        val predecessors = pointRobotWithInertia.predecessors(state)

        assertTrue { predecessors.size == actions.size }
    }

    @Test
    fun testPredecessors2() {
        val pointRobotWithInertia = pointRobotWithInertia1
        val actions = pointRobotWithInertia.actions
        val state = PointRobotWithInertiaState(pointRobotWithInertia.width / 2.0, pointRobotWithInertia.height / 2.0, 0.0, 0.0)
        val successors = pointRobotWithInertia.successors(state)

        for (successor in successors) {
            val predecessors = pointRobotWithInertia.predecessors(successor.state)
            assertTrue { predecessors.size == actions.size }

            var foundState = false
            for (predecessor in predecessors) {
                if (predecessor.state.equals(state)) {
                    foundState = true
                    break
                }
            }

            assertTrue { foundState }
        }

        assertTrue { successors.size == actions.size }
    }

    /**
     * Tests all possible successors for predecessors
     */
    private fun testAllSuccessors(state: DiscretizedState<PointRobotWithInertiaState>,
                                  pointRobotWithInertia: DiscretizedDomain<PointRobotWithInertiaState, PointRobotWithInertia>,
                                  visited: HashSet<DiscretizedState<PointRobotWithInertiaState>>) {
        val successors = pointRobotWithInertia.successors(state)
        for (successor in successors) {
            if (successor.state in visited)
                continue

            visited.add(successor.state)

            val predecessors = pointRobotWithInertia.predecessors(successor.state)
            var foundState = false
            for (predecessor in predecessors) {
                if (predecessor.state.equals(state)) {
                    foundState = true
                    break
                }
            }

            assertTrue("Did not find state '$state' in predecessors list of '$successor'", { foundState })

            testAllSuccessors(successor.state, pointRobotWithInertia, visited)
        }
    }

    @Test
    fun testPredecessors3() {
        val pointRobotWithInertia = DiscretizedDomain(pointRobotWithInertia2)
        val state = PointRobotWithInertiaState(pointRobotWithInertia.domain.width / 2.0, pointRobotWithInertia.domain.height / 2.0, 0.0, 0.0)
        val discretizedState = DiscretizedState(state)

        testAllSuccessors(discretizedState, pointRobotWithInertia, setOf(discretizedState).toHashSet())
    }

    @Test
    fun testGoal1() {
        val pointRobotWithInertia = pointRobotWithInertia1
        val goalState = pointRobotWithInertia.getGoal().first()
        val state1 = PointRobotWithInertiaState(goalState.x - pointRobotWithInertia.goalRadius, goalState.y, goalState.xdot, goalState.ydot)
        val state2 = PointRobotWithInertiaState(goalState.x + pointRobotWithInertia.goalRadius, goalState.y, goalState.xdot, goalState.ydot)
        val state3 = PointRobotWithInertiaState(goalState.x, goalState.y - pointRobotWithInertia.goalRadius, goalState.xdot, goalState.ydot)
        val state4 = PointRobotWithInertiaState(goalState.x, goalState.y + pointRobotWithInertia.goalRadius, goalState.xdot, goalState.ydot)

        assertTrue { pointRobotWithInertia.isGoal(goalState) }
        assertTrue { pointRobotWithInertia.isGoal(state1) }
        assertTrue { pointRobotWithInertia.isGoal(state2) }
        assertTrue { pointRobotWithInertia.isGoal(state3) }
        assertTrue { pointRobotWithInertia.isGoal(state4) }
    }
}
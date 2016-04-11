package edu.unh.cs.ai.realtimesearch.visualizer.gridbased

import edu.unh.cs.ai.realtimesearch.environment.DiscretizedDomain
import edu.unh.cs.ai.realtimesearch.environment.DiscretizedEnvironment
import edu.unh.cs.ai.realtimesearch.environment.DiscretizedState
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia.PointRobotWithInertia
import edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia.PointRobotWithInertiaAction
import edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia.PointRobotWithInertiaState
import edu.unh.cs.ai.realtimesearch.util.convertNanoUpDouble
import edu.unh.cs.ai.realtimesearch.visualizer.ThemeColors
import javafx.animation.Interpolator
import javafx.animation.PathTransition
import javafx.animation.SequentialTransition
import javafx.animation.Timeline
import javafx.scene.shape.Circle
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.util.Duration
import java.util.concurrent.TimeUnit

/**
 * Created by Stephen on 2/29/16.
 */
class PointInertiaVisualizer : PointVisualizer() {
    private var domain: DiscretizedDomain<PointRobotWithInertiaState, PointRobotWithInertia>? = null
    private var environment: DiscretizedEnvironment<PointRobotWithInertiaState, Domain<DiscretizedState<PointRobotWithInertiaState>>>? = null

    override fun setupDomain() {
        domain = DiscretizedDomain(PointRobotWithInertia(
                mapInfo.columnCount,
                mapInfo.rowCount,
                mapInfo.blockedCells.toSet(),
                mapInfo.endCells.first().toDoubleLocation(),
                goalRadius,
                actionDuration
        ))
        environment = DiscretizedEnvironment(domain!!, DiscretizedState(PointRobotWithInertiaState(startX, startY, 0.0, 0.0)))
    }

    override fun playAnimation(transitions: List<PathTransition>) {
        val sequentialTransition = SequentialTransition()
        for (pathTransition in transitions) {
            sequentialTransition.children.add(pathTransition)
        }
        sequentialTransition.cycleCount = Timeline.INDEFINITE
        sequentialTransition.play()
    }

    override fun buildAnimation(): List<PathTransition> {
        /* Create the path that the robot will travel */
        if (displayLine) {
            val path = Path()
            path.elements.add(MoveTo(agentView.agent.x, agentView.agent.y))
            path.stroke = ThemeColors.PATH.stroke
            grid.children.add(path)
        }

        val pathTransitions = mutableListOf<PathTransition>()
        val actionIterator = actionList.iterator()
        while (actionIterator.hasNext()) {
            val x = actionIterator.next()
            assert(actionIterator.hasNext(), { "Action has no matching y value" })
            val y = actionIterator.next()
            var pathTransition = animate(x, y)
            pathTransitions.addAll(pathTransition)
        }

        return pathTransitions
    }

    override fun animate(x: String, y: String): MutableList<PathTransition> {
        val agent = agentView.agent
        val width = tileSize
        val retval: MutableList<PathTransition> = arrayListOf()
        val xAcceleration = x.toDouble()
        val yAcceleration = y.toDouble()
        val action = PointRobotWithInertiaAction(xAcceleration, yAcceleration)

//        domain.calculateNextState(PointRobotWithInertiaState(x, y))
        val previousState = environment!!.getState()
        environment!!.step(action)
        val newState = environment!!.getState()
//println("Animating from $previousState to $newState ($action)")
        val xChange = (newState.state.x - previousState.state.x) * tileSize
        val yChange = (newState.state.y - previousState.state.y) * tileSize
//println("x: ${agent.translateX} | y: ${agent.translateY}")
//println("xChange: $xChange | yChange: $yChange")

        val path = Path()
        path.elements.add(MoveTo(agent.translateX, agent.translateY))
        path.elements.add(LineTo(agent.translateX + xChange, agent.translateY + yChange))
        agent.translateX += xChange
        agent.translateY += yChange

        if(displayLine){
            path.stroke = ThemeColors.PATH.stroke
            grid.children.add(path)
            val actionCircle = Circle(agent.translateX, agent.translateY, width / 10.0)
            grid.children.add(actionCircle)
        }

        /* Animate the robot */
        val pathTransition = PathTransition()
        pathTransition.duration = Duration.millis(convertNanoUpDouble(actionDuration, TimeUnit.MILLISECONDS))
        pathTransition.path = path
        pathTransition.node = agent
        pathTransition.interpolator = Interpolator.LINEAR
        retval.add(pathTransition)

        return retval
    }
}

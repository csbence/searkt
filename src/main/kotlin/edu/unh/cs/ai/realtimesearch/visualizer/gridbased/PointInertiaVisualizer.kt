package edu.unh.cs.ai.realtimesearch.visualizer.gridbased

import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
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
 * Visualizer for the point robot with inertia domain.
 *
 * @author Stephen Chambers, Mike Bogochow
 * @since 2/29/16
 */
class PointInertiaVisualizer : PointVisualizer() {
    private var xDot = 0.0
    private var yDot = 0.0

    override fun playAnimation(transitions: List<PathTransition>) {
        val sequentialTransition = SequentialTransition()
        for (pathTransition in transitions) {
            sequentialTransition.children.add(pathTransition)
        }
        sequentialTransition.cycleCount = Timeline.INDEFINITE

        // Delay startup of animation to simulate idle planning time
        Thread({
            val delayTime = convertNanoUpDouble(experimentResult.idlePlanningTime, TimeUnit.MILLISECONDS) * animationTime / convertNanoUpDouble(experimentResult.experimentConfiguration[Configurations.ACTION_DURATION.toString()] as Long, TimeUnit.MILLISECONDS)
            println("Delay:  $delayTime")
            Thread.sleep(delayTime.toLong())
            sequentialTransition.play()
        }).start()
    }

    override fun buildAnimation(): List<PathTransition> {
        animationX = initialAgentXLocation
        animationY = initialAgentYLocation

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

    override fun animate(xAcceleration: String, yAcceleration: String): MutableList<PathTransition> {
        val robot = agentView.agent
        val width = tileSize
        val retval: MutableList<PathTransition> = arrayListOf()

        val xDDot = xAcceleration.toDouble() * width
        val yDDot = yAcceleration.toDouble() * width

        val nSteps = 100
        val dt = 1.0 / nSteps

        for (i in 0..nSteps - 1) {
            val path = Path()
            path.elements.add(MoveTo(animationX, animationY))

            var xdot = xDot + xDDot * (dt * i)
            var ydot = yDot + yDDot * (dt * i)

            path.elements.add(LineTo(animationX + (xdot * dt), animationY + (ydot * dt)))
            animationX += xdot * dt
            animationY += ydot * dt

            if (displayLine) {
                path.stroke = ThemeColors.PATH.stroke
                grid.children.add(path)
            }

            /* Animate the robot */
            val pathTransition = PathTransition()
            pathTransition.duration = Duration.millis(10.0)
            pathTransition.path = path
            pathTransition.node = robot
            pathTransition.interpolator = Interpolator.LINEAR
            retval.add(pathTransition)
        }

        xDot += xDDot
        yDot += yDDot

        if (displayLine) {
            val action = Circle(animationX, animationY, width / 10.0)
            grid.children.add(action)
        }
        return retval
    }
}

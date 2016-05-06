package edu.unh.cs.ai.realtimesearch.visualizer.gridbased

import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.planner.Planners
import edu.unh.cs.ai.realtimesearch.util.convertNanoUpDouble
import edu.unh.cs.ai.realtimesearch.util.roundToNearestDecimal
import edu.unh.cs.ai.realtimesearch.util.roundUpToDecimal
import edu.unh.cs.ai.realtimesearch.visualizer.ThemeColors
import edu.unh.cs.ai.realtimesearch.visualizer.delayPlay
import groovyjarjarcommonscli.CommandLine
import groovyjarjarcommonscli.Options
import javafx.animation.Animation
import javafx.animation.Interpolator
import javafx.animation.PathTransition
import javafx.animation.Timeline
import javafx.scene.Scene
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.scene.shape.PathElement
import javafx.stage.Stage
import javafx.util.Duration
import java.util.concurrent.TimeUnit

/**
 * Visualizer for the vacuum world and grid world domains.
 *
 * @author Stephen Chambers, Mike Bogochow
 * @since 2/11/16
 */
class VacuumVisualizer : GridBasedVisualizer() {
    /**
     * The current x position of the agent in the animation that is being built.
     */
    protected var animationX = 0.0

    /**
     * The current y position of the agent in the animation that is being built.
     */
    protected var animationY = 0.0

    /**
     * The animation time for a single transition in the animation in milliseconds.
     */
    private val animationStepDuration = 200.0

    override fun getAnimationStepDuration(configuration: Map<String, Any?>): Double = animationStepDuration

    override fun getOptions(): Options = super.getOptions()

    override fun processOptions(cmd: CommandLine) = super.processOptions(cmd)

    override fun start(primaryStage: Stage) {
        processCommandLine(parameters.raw.toTypedArray())

        visualizerSetup()

        primaryStage.title = "RTS Visualizer"
        primaryStage.scene = Scene(grid, tileSize * mapInfo.columnCount, tileSize * mapInfo.rowCount,
                ThemeColors.BACKGROUND.color)
        primaryStage.show()

        val animation = buildAnimation()

        // Delay startup of animation to simulate idle planning time
        delayPlay(animation, roundToNearestDecimal(animationIdlePlanningTime, 1.0).toLong())
    }

    /**
     * Build a path for the agent to follow from the action list.
     */
    private fun buildAnimation(): Animation {
        animationX = initialAgentAnimationLocation.x
        animationY = initialAgentAnimationLocation.y

        val path = Path()
        path.elements.add(MoveTo(animationX, animationY))

        for (action in actionList) {
            path.elements.add(animate(action))
            path.elements.add(MoveTo(animationX, animationY))
        }

        /* Display the path */
        if (displayLine) {
            path.stroke = ThemeColors.PATH.stroke
            grid.children.add(path)
        }

        /* Animate the robot */
        val timeToRun = actionList.size * animationStepDuration
        val pathTransition = PathTransition()
        pathTransition.duration = Duration.millis(timeToRun)
        pathTransition.path = path
        pathTransition.node = agentView.agent
        pathTransition.interpolator = Interpolator.LINEAR
        pathTransition.cycleCount = Timeline.INDEFINITE

        return pathTransition
    }

    /**
     * Get the PathElement for the given action.
     *
     * @param action the action to animate
     * @return the path element for animating the given action
     */
    private fun animate(action: String): PathElement {
        val width = tileSize
        val height = tileSize

        when (action) {
            "UP" -> {
                animationY += height
            }
            "RIGHT" -> {
                animationX += width
            }
            "DOWN" -> {
                animationY -= height
            }
            "LEFT" -> {
                animationX -= width
            }
        }

        return LineTo(animationX, animationY)
    }
}

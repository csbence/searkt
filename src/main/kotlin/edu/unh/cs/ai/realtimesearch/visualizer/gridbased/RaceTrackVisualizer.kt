package edu.unh.cs.ai.realtimesearch.visualizer.gridbased

import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.util.convertNanoUpDouble
import edu.unh.cs.ai.realtimesearch.visualizer.ThemeColors
import groovyjarjarcommonscli.CommandLine
import groovyjarjarcommonscli.Options
import javafx.animation.Interpolator
import javafx.animation.PathTransition
import javafx.animation.SequentialTransition
import javafx.animation.Timeline
import javafx.scene.Scene
import javafx.scene.shape.Circle
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.stage.Stage
import javafx.util.Duration
import java.util.concurrent.TimeUnit

/**
 * Visualizer for the racetrack domain.
 *
 * @author Stephen Chambers, Mike Bogochow
 * @since 2/29/16
 */
class RacetrackVisualizer : GridBasedVisualizer() {
    /**
     * The current x position of the agent in the animation that is being built.
     */
    protected var animationX = 0.0

    /**
     * The current y position of the agent in the animation that is being built.
     */
    protected var animationY = 0.0

    /**
     * The current x velocity of the agent in the animation that is being built.
     */
    private var xDot = 0

    /**
     * The current y velocity of the agent in the animation that is being built.
     */
    private var yDot = 0

    /**
     * The animation time for a single transition in the animation in milliseconds.
     */
    private var animationStepDuration = 1000.0

    override var robotScale: Double = 4.0

    override fun getOptions(): Options = super.getOptions()

    override fun processOptions(cmd: CommandLine) = super.processOptions(cmd)

    override fun start(primaryStage: Stage) {
        processCommandLine(parameters.raw.toTypedArray())

        visualizerSetup()

        primaryStage.title = "RTS RaceTrack Visualizer: ${experimentResult.experimentConfiguration["algorithmName"]}"
        primaryStage.scene = Scene(grid, tileSize * mapInfo.columnCount, tileSize * mapInfo.rowCount, ThemeColors.BACKGROUND.color)
        primaryStage.show()

        val sequentialTransition = buildAnimation()
        sequentialTransition.cycleCount = Timeline.INDEFINITE

        // Delay startup of animation to simulate idle planning time
        Thread({
            val delayTime = convertNanoUpDouble(experimentResult.idlePlanningTime, TimeUnit.MILLISECONDS) * animationStepDuration / convertNanoUpDouble(experimentResult.experimentConfiguration[Configurations.ACTION_DURATION.toString()] as Long, TimeUnit.MILLISECONDS)
            println("Relative IPT: $delayTime ms")
            Thread.sleep(delayTime.toLong())
            sequentialTransition.play()
        }).start()
    }

    /**
     * Create a sequential transition from the action list.
     */
    private fun buildAnimation(): SequentialTransition {
        val sequentialTransition = SequentialTransition()

        animationX = initialAgentXLocation
        animationY = initialAgentYLocation

        for (action in actionList)
            sequentialTransition.children.add(animate(action))

        return sequentialTransition
    }

    /**
     * Create a transition from the action.
     */
    private fun animate(action: String): PathTransition {
        val robot = agentView.agent
        val width = tileSize
        val path = Path()

        when (action) {
            "UP" -> {
                yDot++
            }
            "RIGHT" -> {
                xDot++
            }
            "DOWN" -> {
                yDot--
            }
            "LEFT" -> {
                xDot--
            }
            "RIGHTUP" -> {
                xDot++
                yDot++
            }
            "RIGHTDOWN" -> {
                xDot++
                yDot--
            }
            "LEFTDOWN" -> {
                xDot--
                yDot--
            }
            "LEFTUP" -> {
                xDot--
                yDot++
            }
            "NOOP" -> {

            }
        }
        path.elements.add(MoveTo(animationX, animationY))
        path.elements.add(LineTo(animationX + (xDot * width), animationY + (yDot * width)))
        animationX += xDot * width
        animationY += yDot * width

        if (displayLine) {
            path.stroke = ThemeColors.PATH.stroke
            grid.children.add(path)
            val actionPoint = Circle(animationX, animationY, width / 10.0)
            grid.children.add(actionPoint)
        }

        /* Animate the robot */
        val pathTransition = PathTransition()
        pathTransition.duration = Duration.millis(animationStepDuration)
        pathTransition.path = path
        pathTransition.node = robot
        pathTransition.interpolator = Interpolator.LINEAR
        return pathTransition
    }
}

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
 * Created by Stephen on 2/29/16.
 */
class RacetrackVisualizer : GridBasedVisualizer() {
    private var animationX = 0.0
    private var animationY = 0.0
    private var xDot = 0
    private var yDot = 0
    override var robotScale: Double = 4.0
    private var animationStepDuration = 1000.0 // ms

    override fun getOptions(): Options = super.getOptions()

    override fun processOptions(cmd: CommandLine) = super.processOptions(cmd)

    override fun start(primaryStage: Stage) {
        processCommandLine(parameters.raw.toTypedArray())

        visualizerSetup()

        primaryStage.title = "RTS Visualizer"
        primaryStage.scene = Scene(grid, tileSize * mapInfo.columnCount, tileSize * mapInfo.rowCount, ThemeColors.BACKGROUND.color)
        primaryStage.show()

        val sequentialTransition = buildAnimation()
        sequentialTransition.cycleCount = Timeline.INDEFINITE
        Thread({
            val delayTime = convertNanoUpDouble(experimentResult.idlePlanningTime, TimeUnit.MILLISECONDS) * animationStepDuration / convertNanoUpDouble(experimentResult.experimentConfiguration[Configurations.ACTION_DURATION.toString()] as Long, TimeUnit.MILLISECONDS)
            println("Delay:  $delayTime")
            Thread.sleep(delayTime.toLong())
            sequentialTransition.play()
        }).start()
    }

    private fun buildAnimation(): SequentialTransition {
        val sequentialTransition = SequentialTransition()

        animationX = initialAgentXLocation
        animationY = initialAgentYLocation

        for (action in actionList)
            sequentialTransition.children.add(animate(action))

        return sequentialTransition
    }

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

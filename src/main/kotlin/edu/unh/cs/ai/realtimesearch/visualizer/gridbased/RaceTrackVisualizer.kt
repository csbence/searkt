package edu.unh.cs.ai.realtimesearch.visualizer.gridbased

import groovyjarjarcommonscli.CommandLine
import groovyjarjarcommonscli.Options
import javafx.animation.Interpolator
import javafx.animation.PathTransition
import javafx.animation.SequentialTransition
import javafx.animation.Timeline
import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.stage.Stage
import javafx.util.Duration

/**
 * Created by Stephen on 2/29/16.
 */
class RacetrackVisualizer : GridBasedVisualizer() {
    private var xDot = 0
    private var yDot = 0
    override var robotScale: Double = 4.0

    override fun getOptions(): Options = super.getOptions()

    override fun processOptions(cmd: CommandLine) = super.processOptions(cmd)

    override fun start(primaryStage: Stage) {
        processCommandLine(parameters.raw.toTypedArray())

        visualizerSetup()

        primaryStage.title = "RTS Visualizer"
        primaryStage.scene = Scene(grid, tileSize * mapInfo.columnCount, tileSize * mapInfo.rowCount, Color.LIGHTSLATEGRAY)
        primaryStage.show()

        val sequentialTransition = buildAnimation()
        sequentialTransition.cycleCount = Timeline.INDEFINITE
        sequentialTransition.play()
    }

    private fun buildAnimation(): SequentialTransition {
        val sequentialTransition = SequentialTransition()
        for (action in actionList)
            sequentialTransition.children.add(animate(action))

        /* Display the path */
        if (displayLine) {
            val path = Path()
            path.elements.add(MoveTo(robotView.robot.x, robotView.robot.y))
            path.stroke = Color.ORANGE
            grid.children.add(path)
        }

        return sequentialTransition
    }

    private fun animate(action: String): PathTransition {
        val robot = robotView.robot
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
        path.elements.add(MoveTo(robot.translateX, robot.translateY))
        path.elements.add(LineTo(robot.translateX + (xDot * width), robot.translateY + (yDot * width)))
        robot.translateX += xDot * width
        robot.translateY += yDot * width

        if (displayLine) {
            path.stroke = Color.RED
            grid.children.add(path)
            val actionPoint = Circle(robot.translateX, robot.translateY, width / 10.0)
            grid.children.add(actionPoint)
        }

        /* Animate the robot */
        val pathTransition = PathTransition()
        pathTransition.duration = Duration.millis(1000.0)
        pathTransition.path = path
        pathTransition.node = robot
        pathTransition.interpolator = Interpolator.LINEAR
        return pathTransition
    }
}

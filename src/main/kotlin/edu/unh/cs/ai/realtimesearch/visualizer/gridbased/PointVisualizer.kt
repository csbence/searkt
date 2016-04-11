package edu.unh.cs.ai.realtimesearch.visualizer.gridbased

import edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia.PointRobotWithInertia
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
import java.util.*

/**
 * Created by Stephen on 2/29/16.
 */
open class PointVisualizer : GridBasedVisualizer() {
    var startX: Double = 0.0
    var startY: Double = 0.0
    var goalX: Double = 0.0
    var goalY: Double = 0.0
    var goalRadius: Double = 0.0
    var actionDuration: Long = 0

    override var robotScale: Double = 4.0

    override fun getOptions(): Options = super.getOptions()

    override fun processOptions(cmd: CommandLine) = super.processOptions(cmd)

    open protected fun setupDomain() {}

    override fun parseMapHeader(inputScanner: Scanner): GridDimensions {
        val dimensions = super.parseMapHeader(inputScanner)
        startX = inputScanner.nextLine().toDouble()
        startY = inputScanner.nextLine().toDouble()
        goalX = inputScanner.nextLine().toDouble()
        goalY = inputScanner.nextLine().toDouble()
        goalRadius = inputScanner.nextLine().toDouble()
        return dimensions
    }

    override fun parseActions(): MutableList<String> {
        val actionList: MutableList<String> = arrayListOf()
        for (action in experimentResult!!.actions) {
            var xStart = action.indexOf('(') + 1
            var xEnd = action.indexOf(',')
            var yStart = xEnd + 2
            var yEnd = action.indexOf(')')

            val x = action.substring(xStart, xEnd)
            val y = action.substring(yStart, yEnd)
            actionList.add(x)
            actionList.add(y)
        }
        return actionList
    }

    override fun start(primaryStage: Stage) {
        processCommandLine(parameters.raw.toTypedArray())

        visualizerSetup()
        actionDuration = experimentResult!!.experimentConfiguration["actionDuration"] as Long

        setupDomain()

        /* the goal radius */
        val goalCircle = Circle(goalX * tileSize, goalY * tileSize, goalRadius * tileSize)
        goalCircle.stroke = ThemeColors.GOAL_CIRCLE.stroke
        goalCircle.fill = ThemeColors.GOAL_CIRCLE.color
        goalCircle.opacity = ThemeColors.GOAL_CIRCLE.opacity
        grid.children.add(goalCircle)

        primaryStage.title = "RTS Visualizer"
        primaryStage.scene = Scene(grid, tileSize * mapInfo.columnCount, tileSize * mapInfo.rowCount, ThemeColors.BACKGROUND.color)
        //primaryStage.scene = Scene(root, WIDTH, HEIGHT)
        primaryStage.show()

        playAnimation(buildAnimation())
    }

    open protected fun playAnimation(transitions: List<PathTransition>) {
        val sequentialTransition = SequentialTransition()
        for (pathTransition in transitions) {
            sequentialTransition.children.add(pathTransition)
        }
        sequentialTransition.cycleCount = Timeline.INDEFINITE
        sequentialTransition.play()
    }

    open protected fun buildAnimation(): List<PathTransition> {
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

    open protected fun animate(x: String, y: String): MutableList<PathTransition> {
        val path = Path()
        val robot = agentView.agent
        val width = tileSize

        val xDot = x.toDouble() * width
        val yDot = y.toDouble() * width

        path.elements.add(MoveTo(robot.translateX, robot.translateY))
        path.elements.add(LineTo(robot.translateX + xDot, robot.translateY + yDot))
        robot.translateX += xDot
        robot.translateY += yDot

        if (displayLine) {
            path.stroke = ThemeColors.PATH.stroke
            grid.children.add(path)
            val action = Circle(robot.translateX, robot.translateY, width / 10.0)
            grid.children.add(action)
        }

        /* Animate the robot */
        val pathTransition = PathTransition()
        pathTransition.duration = Duration.millis(2000.0)
        pathTransition.path = path
        pathTransition.node = robot
        pathTransition.interpolator = Interpolator.LINEAR
        return mutableListOf(pathTransition)
    }
}

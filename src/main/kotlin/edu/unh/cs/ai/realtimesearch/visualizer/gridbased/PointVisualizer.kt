package edu.unh.cs.ai.realtimesearch.visualizer.gridbased

import edu.unh.cs.ai.realtimesearch.environment.pointrobot.PointRobotHeader
import edu.unh.cs.ai.realtimesearch.environment.pointrobot.PointRobotIO
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
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by Stephen on 2/29/16.
 */
open class PointVisualizer : GridBasedVisualizer() {
    var startX: Double = 0.0
    var startY: Double = 0.0
    var goalX: Double = 0.0
    var goalY: Double = 0.0
    var header: PointRobotHeader? = null
    var actionDuration: Long = 0
    var animationTime: Double = 0.0
    val minimumAnimationTime: Double = 500.0
    var animationX = 0.0
    var animationY = 0.0

    override var robotScale: Double = 4.0

    override fun getOptions(): Options = super.getOptions()

    override fun processOptions(cmd: CommandLine) = super.processOptions(cmd)

    open protected fun setupDomain() {
    }

    override fun parseMapHeader(inputScanner: Scanner): GridDimensions {
        header = PointRobotIO.parseHeader(inputScanner)
        return GridDimensions(header!!.rowCount, header!!.columnCount)
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
        actionDuration = experimentResult!!.experimentConfiguration[Configurations.ACTION_DURATION.toString()] as Long
        animationTime = convertNanoUpDouble(actionDuration, TimeUnit.MILLISECONDS)
        //        if (animationTime < minimumAnimationTime)
        animationTime = minimumAnimationTime

        startX = mapInfo.startCells.first().x + header!!.startLocationOffset.x
        startY = mapInfo.startCells.first().y + header!!.startLocationOffset.y
        goalX = mapInfo.goalCells.first().x + header!!.goalLocationOffset.x
        goalY = mapInfo.goalCells.first().y + header!!.goalLocationOffset.y

        setupDomain()

        /* the goal radius */
        val goalCircle = Circle(goalX * tileSize, goalY * tileSize, header!!.goalRadius * tileSize)
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

        Thread({
            val delayTime = convertNanoUpDouble(experimentResult.idlePlanningTime, TimeUnit.MILLISECONDS) * animationTime / convertNanoUpDouble(experimentResult.experimentConfiguration[Configurations.ACTION_DURATION.toString()] as Long, TimeUnit.MILLISECONDS)
            println("Delay:  $delayTime")
            Thread.sleep(delayTime.toLong())
            sequentialTransition.play()
        }).start()
    }

    open protected fun buildAnimation(): List<PathTransition> {
        val pathTransitions = mutableListOf<PathTransition>()

        animationX = initialAgentXLocation
        animationY = initialAgentYLocation

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

        path.elements.add(MoveTo(animationX, animationY))
        path.elements.add(LineTo(animationX + xDot, animationY + yDot))
        animationX += xDot
        animationY += yDot

        if (displayLine) {
            path.stroke = ThemeColors.PATH.stroke
            grid.children.add(path)
            val action = Circle(animationX, animationY, width / 10.0)
            grid.children.add(action)
        }

        /* Animate the robot */
        val pathTransition = PathTransition()
        pathTransition.duration = Duration.millis(animationTime)
        pathTransition.path = path
        pathTransition.node = robot
        pathTransition.interpolator = Interpolator.LINEAR
        return mutableListOf(pathTransition)
    }
}

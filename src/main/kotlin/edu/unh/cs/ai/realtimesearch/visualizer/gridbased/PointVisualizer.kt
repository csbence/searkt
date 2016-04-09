package edu.unh.cs.ai.realtimesearch.visualizer.gridbased

import groovyjarjarcommonscli.CommandLine
import groovyjarjarcommonscli.Options
import javafx.animation.Interpolator
import javafx.animation.PathTransition
import javafx.animation.SequentialTransition
import javafx.animation.Timeline
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.*
import javafx.stage.Stage
import javafx.util.Duration
import java.util.*

/**
 * Created by Stephen on 2/29/16.
 */
class PointVisualizer : GridBasedVisualizer() {
    var startX: Double = 0.0
    var startY: Double = 0.0
    var goalX: Double = 0.0
    var goalY: Double = 0.0
    var goalRadius: Double = 0.0

    override var robotScale: Double = 4.0

    override fun getOptions(): Options = super.getOptions()

    override fun processOptions(cmd: CommandLine) = super.processOptions(cmd)

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

        //        /* the dirty cell */
        //        val dirtyCell = Circle(goalX * TILE_SIZE, goalY * TILE_SIZE, TILE_SIZE / 4.0)
        //        dirtyCell.fill = Color.BLUE
        //        root.children.add(dirtyCell)

        /* the goal radius */
        val goalCircle = Circle(goalX * tileSize, goalY * tileSize, goalRadius * tileSize)
        goalCircle.stroke = Color.BLUE
        goalCircle.fill = Color.WHITE
        goalCircle.opacity = 0.5
        grid.children.add(goalCircle)

        primaryStage.title = "RTS Visualizer"
        primaryStage.scene = Scene(grid, tileSize * mapInfo.columnCount, tileSize * mapInfo.rowCount)
        //primaryStage.scene = Scene(root, WIDTH, HEIGHT)
        primaryStage.show()

        val path = buildAnimation()

        /* Display the path */
        if (displayLine)
            grid.children.add(path)

        val sq = SequentialTransition()
        var count = 0
        while (count != actionList.size) {
            val x = actionList[count]
            val y = actionList[count + 1]
            var pathTransition = animate(grid, x, y, displayLine, robotView.robot, tileSize)
            sq.children.add(pathTransition)
            count += 2
        }
        sq.cycleCount = Timeline.INDEFINITE
        sq.play()
    }

    private fun buildAnimation(): Path {
        /* Create the path that the robot will travel */
        val path = Path()
        path.elements.add(MoveTo(robotView.robot.x, robotView.robot.y))
        path.stroke = Color.ORANGE
        return path
    }

    private fun animate(root: Pane, x: String, y: String, displayLine: Boolean, robot: Rectangle, width: Double): PathTransition {
        val path = Path()

        val xDot = x.toDouble() * width
        val yDot = y.toDouble() * width

        path.elements.add(MoveTo(robot.translateX, robot.translateY))
        path.elements.add(LineTo(robot.translateX + xDot, robot.translateY + yDot))
        robot.translateX += xDot
        robot.translateY += yDot

        if (displayLine) {
            path.stroke = Color.RED
            root.children.add(path)
            val action = Circle(robot.translateX, robot.translateY, width / 10.0)
            root.children.add(action)
        }

        /* Animate the robot */
        val pathTransition = PathTransition()
        pathTransition.duration = Duration.millis(2000.0)
        pathTransition.path = path
        pathTransition.node = robot
        pathTransition.interpolator = Interpolator.LINEAR
        return pathTransition
    }
}

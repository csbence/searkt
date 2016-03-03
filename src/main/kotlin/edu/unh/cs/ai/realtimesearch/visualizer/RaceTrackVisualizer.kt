package edu.unh.cs.ai.realtimesearch.visualizer

import javafx.animation.Interpolator
import javafx.animation.PathTransition
import javafx.animation.SequentialTransition
import javafx.animation.Timeline
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.*
import javafx.stage.Stage
import javafx.util.Duration
import java.util.*
import kotlin.system.exitProcess

/**
 * Created by Stephen on 2/29/16.
 */

var xDot = 0
var yDot = 0

class RacetrackVisualizer : Application() {
    override fun start(primaryStage: Stage) {

        val DISPLAY_LINE = true

        /* Get domain from Application */
        val parameters = getParameters()!!
        val raw = parameters.getRaw()!!
        if (raw.isEmpty()) {
            println("Cannot visualize without a domain!")
            exitProcess(1)
        }
        val rawDomain = raw.first()

        /* Get action list from Application */
        val actionList: MutableList<String> = arrayListOf()
        for (i in 1..raw.size - 1) {
            actionList.add(raw.get(i))
        }

        primaryStage.title = "RTS Visualizer"
        val inputScanner = Scanner(rawDomain.byteInputStream())

        val rowCount: Int
        val columnCount: Int

        columnCount = inputScanner.nextLine().toInt()
        rowCount = inputScanner.nextLine().toInt()

        val root = Pane()

        /* Graphical parameters */
        val WIDTH = 1400.0
        val HEIGHT = 800.0
        val TILE_WIDTH: Double = (WIDTH / columnCount)
        val TILE_HEIGHT: Double = (HEIGHT / rowCount)
        var TILE_SIZE = Math.min(TILE_WIDTH, TILE_HEIGHT)

        while(((TILE_SIZE * columnCount) > WIDTH) || ((TILE_SIZE * rowCount) > HEIGHT)){
            TILE_SIZE /= 1.05
        }

        /* The robot */
        val robotWidth = TILE_SIZE / 4.0
        val robot = Rectangle(robotWidth, robotWidth)
        robot.fill = Color.ORANGE
        root.children.add(robot)

        /* The robots starting location, needs to be drawn later */
        var startX: Double? = null
        var startY: Double? = null


        for (y in 0..rowCount - 1) {
            val line = inputScanner.nextLine()
            for (x in 0..columnCount - 1) {
                when (line[x]) {
                    '#' -> {
                        val blocked = Rectangle(x*TILE_SIZE, y*TILE_SIZE, TILE_SIZE, TILE_SIZE)
                        blocked.fill = Color.BLACK
                        blocked.stroke = Color.BLACK
                        root.children.add(blocked)
                    }
                    '_' -> {
                        val free = Rectangle(x*TILE_SIZE, y*TILE_SIZE, TILE_SIZE, TILE_SIZE)
                        free.fill = Color.LIGHTSLATEGRAY
                        free.stroke = Color.WHITE
                        free.opacity = 0.5
                        root.children.add(free)
                    }
                    '*' -> {
                        val radius = TILE_WIDTH / 10.0
                        val dirtyLocX = x * TILE_SIZE + ((TILE_SIZE) / 2.0)
                        val dirtyLocY = y * TILE_SIZE + ((TILE_SIZE) / 2.0)

                        val dirtyCell = Circle(dirtyLocX, dirtyLocY, radius)
                        dirtyCell.fill = Color.BLUE
                        root.children.add(dirtyCell)
                    }
                    '@' -> {
                        startX = x * 1.0
                        startY = y * 1.0
                    }
                }
            }
        }

        if (startX == null || startY == null) {
            println("Start location must be defined")
            exitProcess(1)
        }

        primaryStage.scene = Scene(root, TILE_SIZE * columnCount, TILE_SIZE * rowCount)
        //primaryStage.scene = Scene(root, WIDTH, HEIGHT)
        primaryStage.show()


        /* Create the path that the robot will travel */
        robot.toFront()
        val path = Path()
        val xLoc = startX * TILE_SIZE + (TILE_SIZE / 2.0)
        val yLoc = startY * TILE_SIZE + (TILE_SIZE / 2.0)
        robot.x = xLoc
        robot.y = yLoc
        robot.translateX = xLoc
        robot.translateY = yLoc
        path.elements.add(MoveTo(xLoc, yLoc))
        path.stroke = Color.ORANGE

        /* Display the path */
        if (DISPLAY_LINE)
            root.children.add(path)

        val sq = SequentialTransition()
        for(action in actionList){
            var pt = animate(root, action, DISPLAY_LINE, robot, TILE_SIZE)
            sq.children.add(pt)
        }
        sq.setCycleCount(Timeline.INDEFINITE);
        sq.play()
    }

    private fun animate(root: Pane, action: String, dispLine: Boolean, robot: Rectangle, width: Double): PathTransition {
        val path = Path()

        when (action){
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

        if(dispLine){
            path.stroke = Color.RED
            root.children.add(path)
            val action = Circle(robot.translateX, robot.translateY, width / 10.0)
            root.children.add(action)
        }

        /* Animate the robot */
        val pathTransition = PathTransition()
        pathTransition.setDuration(Duration.millis(1000.0))
        pathTransition.setPath(path)
        pathTransition.setNode(robot)
        pathTransition.setInterpolator(Interpolator.LINEAR);
        return pathTransition
    }
}

package edu.unh.cs.ai.realtimesearch.visualizer

/**
 * Created by Stephen on 2/11/16.
 */

import javafx.animation.PathTransition
import javafx.animation.Timeline
import javafx.application.Application
import javafx.animation.Interpolator
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.input.MouseEvent
import javafx.stage.Stage
import javafx.scene.paint.Color
import java.util.*
import javafx.scene.layout.Pane
import javafx.scene.shape.*
import javafx.util.Duration
import kotlin.system.exitProcess

class VaccumVisualizer : Application() {
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
        val TIME_TO_RUN = actionList.size * 200.0
        //val TIME_TO_RUN = 2000.0

        /* Assuming the domain is correct because the experiment was already run */
        primaryStage.title = "RTS Visualizer"
        val inputScanner = Scanner(rawDomain.byteInputStream())

        val rowCount: Int
        val columnCount: Int

        columnCount = inputScanner.nextLine().toInt()
        rowCount = inputScanner.nextLine().toInt()

        val root = Pane()

        /* Graphical parameters */
        val WIDTH = 1700.0
        val HEIGHT = 1400.0
        val TILE_WIDTH: Double = (WIDTH / columnCount)
        val TILE_HEIGHT: Double = (HEIGHT / rowCount)
        val TILE_SIZE = Math.min(TILE_WIDTH, TILE_HEIGHT) / 1.19

        /* The robot */
        val robotWidth = TILE_SIZE / 2.0
        val robot = Rectangle(robotWidth, robotWidth)
        robot.fill = Color.ORANGE

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
                        //blocked.widthProperty().bind(root.widthProperty().divide(columnCount))
                        //blocked.heightProperty().bind(root.heightProperty().divide(rowCount))

                        root.children.add(blocked)
                    }
                    '_' -> {
                        val free = Rectangle(x*TILE_SIZE, y*TILE_SIZE, TILE_SIZE, TILE_SIZE)
                        free.fill = Color.LIGHTSLATEGRAY

                        /* resize width and height */
                        //free.widthProperty().bind(root.widthProperty().divide(columnCount))
                        //free.heightProperty().bind(root.heightProperty().divide(rowCount))

                        free.stroke = Color.WHITE
                        root.children.add(free)
                    }
                    '*' -> {
                        val radius = TILE_WIDTH / 10.0
                        val dirtyLocX = x * TILE_SIZE + ((TILE_SIZE) / 2.0)
                        val dirtyLocY = y * TILE_SIZE + ((TILE_SIZE) / 2.0)
                        println(dirtyLocX)
                        println(dirtyLocY)

                        val dirtyCell = Circle(dirtyLocX, dirtyLocY, radius)
                        dirtyCell.fill = Color.BLUE
                        root.children.add(dirtyCell)
                    }
                    '@' -> {
                        startX = x * 1.0
                        startY = y * 1.0
                        root.children.add(robot)
                    }
                }
            }
        }

        primaryStage.scene = Scene(root, TILE_SIZE * columnCount, TILE_SIZE * rowCount)
        //primaryStage.scene = Scene(root, WIDTH, HEIGHT)
        primaryStage.show()

        if (startX == null || startY == null) {
            println("Start location must be defined")
            exitProcess(1)
        }


        /* Create the path that the robot will travel */
        robot.toFront()
        val path = Path()
        val xLoc = startX * TILE_SIZE + ((TILE_SIZE) / 2.0)
        val yLoc = startY * TILE_SIZE + ((TILE_SIZE) / 2.0)
        robot.x = xLoc
        robot.y = yLoc
        robot.translateX = xLoc
        robot.translateY = yLoc
        path.elements.add(MoveTo(xLoc, yLoc))
        path.stroke = Color.ORANGE

        /* Display the path */
        if(DISPLAY_LINE)
            root.children.add(path)

        for (action in actionList) {
           animate(root, action, path, robot, TILE_SIZE, TILE_SIZE)
        }

        /* Animate the robot */
        val pathTransition = PathTransition()
        pathTransition.setDuration(Duration.millis(TIME_TO_RUN))
        pathTransition.setPath(path)
        pathTransition.setNode(robot)
        pathTransition.setInterpolator(Interpolator.LINEAR);
        pathTransition.setCycleCount(Timeline.INDEFINITE);
        pathTransition.play()


    }

    private fun animate(root: Pane, action: String, path: Path, robot: Rectangle, width: Double, height: Double) {
        when (action) {
            "UP" -> {
                path.elements.add(LineTo(robot.translateX, robot.translateY + height))
                robot.translateY = robot.translateY + height
            }
            "RIGHT" -> {
                path.elements.add(LineTo(robot.translateX + width, robot.translateY))
                robot.translateX = robot.translateX + width
            }
            "DOWN" -> {
                path.elements.add(LineTo(robot.translateX, robot.translateY - height))
                robot.translateY = robot.translateY - height
            }
            "LEFT" -> {
                path.elements.add(LineTo(robot.translateX - width, robot.translateY))
                robot.translateX = robot.translateX - width
            }
        }
    }
}

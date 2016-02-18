package edu.unh.cs.ai.realtimesearch.visualizer

/**
 * Created by Stephen on 2/11/16.
 */

import javafx.animation.PathTransition
import javafx.animation.Timeline
import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import javafx.scene.paint.Color
import java.util.*
import javafx.scene.layout.TilePane
import javafx.scene.shape.*
import javafx.util.Duration
import kotlin.system.exitProcess

class VaccumVisualizer : Application() {
    override fun start(primaryStage: Stage) {
        /* Get domain from Application */
        val parameters = getParameters()!!
        val raw = parameters.getRaw()!!
        if(raw.isEmpty()){
            println("Cannot visualize without a domain!")
            exitProcess(1)
        }
        val rawDomain = raw.first()

        /* Get action list from Application */
        val actionList: MutableList<String> = arrayListOf()
        for (i in 1..raw.size - 1){
            actionList.add(raw.get(i))
        }
        println(actionList)

        /* Assuming the domain is correct because the experiment was already run */
        primaryStage.title = "RTS Visualizer"
        val inputScanner = Scanner(rawDomain.byteInputStream())

        val rowCount: Int
        val columnCount: Int

        columnCount = inputScanner.nextLine().toInt()
        rowCount = inputScanner.nextLine().toInt()

        /* Graphical parameters */
        val WIDTH = 1200.0
        val HEIGHT = 700.0
        val TILE_WIDTH: Double = (WIDTH/columnCount)
        println(WIDTH)
        println(TILE_WIDTH)
        val TILE_HEIGHT: Double = (HEIGHT/rowCount)
        println(HEIGHT)
        println(TILE_HEIGHT)

        val root = TilePane(0.0, 0.0)
        root.isSnapToPixel = true
        /* The robot */
        val robotWidth = TILE_WIDTH/4.0
        val robot = Rectangle(robotWidth, robotWidth)
        //robot.stroke = Color.ORANGE
        robot.fill = Color.ORANGE

        /* The robots starting location, needs to be drawn later */
        var startX: Double? = null
        var startY: Double? = null


        for (y in 0..rowCount - 1) {
            val line = inputScanner.nextLine()

            for (x in 0..columnCount - 1) {
                when (line[x]) {
                    '#' -> {
                        val blocked = Rectangle(TILE_WIDTH, TILE_HEIGHT)
                        blocked.fill = Color.BLACK
                        //blocked.stroke = Color.BLACK
                        root.children.add(blocked)
                    }
                    '_' -> {
                        val free = Rectangle(TILE_WIDTH, TILE_HEIGHT)
                        free.fill = Color.LIGHTSLATEGRAY
                        //free.stroke = Color.LIGHTSLATEGRAY
                        free.opacity = .5
                        root.children.add(free)
                    }
                    '*' -> {
                        val radius = TILE_WIDTH/10.0
                        val dirtyCell = Circle(radius)
                        //dirtyCell.stroke = Color.BLUE
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

        primaryStage.scene = Scene(root, WIDTH + columnCount, HEIGHT + rowCount)
        primaryStage.show()

        if(startX == null || startY == null){
            println("Start location must be defined")
            exitProcess(1)
        }


        /* Create the path that the robot will travel */
        val path = Path()
        val xLoc = startX*TILE_WIDTH + ((TILE_WIDTH - robotWidth)/2.0)
        val yLoc = startY*TILE_HEIGHT + ((TILE_HEIGHT - robotWidth)/2.0)
        robot.x = xLoc
        robot.y = yLoc
        robot.translateX = xLoc
        robot.translateY = yLoc
        path.elements.add(MoveTo(xLoc, yLoc))
        //robot.translateX = xLoc
        //robot.translateY = yLoc

        for(action in actionList){
            animate(action, path, robot, TILE_WIDTH, TILE_HEIGHT)
        }
        val pathTransition =  PathTransition()
        pathTransition.setDuration(Duration.millis(10000.0))
        pathTransition.setPath(path)
        pathTransition.setNode(robot)
        pathTransition.setAutoReverse(true)
        pathTransition.play()
    }

    private fun animate(action: String, path: Path, robot: Rectangle, width: Double, height: Double){
        when(action){
            "UP" -> {
                path.elements.add(LineTo(robot.translateX,robot.translateY + height + 1))
                robot.translateY = robot.translateY + height + 1
            }
            "RIGHT" -> {
                path.elements.add(LineTo(robot.translateX + width +1,robot.translateY))
                robot.translateX = robot.translateX + width + 1
            }
            "DOWN" -> {
                path.elements.add(LineTo(robot.translateX,robot.translateY - height -1))
                robot.translateY = robot.translateY - height - 1
            }
            "LEFT" -> {
                path.elements.add(LineTo(robot.translateX - width -1,robot.translateY))
                robot.translateX = robot.translateX - width - 1
            }
        }
    }

}

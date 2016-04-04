package edu.unh.cs.ai.realtimesearch.visualizer.gridbased

import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.visualizer.BaseVisualizer
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
class PointInertiaVisualizer : BaseVisualizer() {
    private var xDot = 0.0
    private var yDot = 0.0

    override fun getOptions(): Options = Options()

    override fun processOptions(cmd: CommandLine) {}

    override fun start(primaryStage: Stage) {
        processCommandLine(parameters.raw.toTypedArray())

        val DISPLAY_LINE = true

        val rawDomain = experimentResult!!.experimentConfiguration[Configurations.RAW_DOMAIN.toString()] as String

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

        primaryStage.title = "RTS Visualizer"
        val inputScanner = Scanner(rawDomain.byteInputStream())

        val rowCount: Int
        val columnCount: Int
        val startX: Double
        val startY: Double
        val goalX: Double
        val goalY: Double
        val goalRadius: Double

        columnCount = inputScanner.nextLine().toInt()
        rowCount = inputScanner.nextLine().toInt()
        startX = inputScanner.nextLine().toDouble()
        startY = inputScanner.nextLine().toDouble()
        goalX = inputScanner.nextLine().toDouble()
        goalY = inputScanner.nextLine().toDouble()
        goalRadius = inputScanner.nextLine().toDouble()

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

        /* the dirty cell */
        val dirtyCell = Circle(goalX * TILE_SIZE, goalY * TILE_SIZE, TILE_SIZE / 4.0)
        dirtyCell.fill = Color.BLUE
        dirtyCell.toFront()
        root.children.add(dirtyCell)

        /* the goal radius */
        val goalCircle = Circle(goalX * TILE_SIZE, goalY * TILE_SIZE, goalRadius * TILE_SIZE)
        goalCircle.stroke = Color.BLUE
        goalCircle.fill = Color.WHITE
        goalCircle.opacity = 0.5
        root.children.add(goalCircle)


        for (y in 0..rowCount - 1) {
            val line = inputScanner.nextLine()
            for (x in 0..columnCount - 1) {
                when (line[x]) {
                    '#' -> {
                        val blocked = Rectangle(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE)
                        blocked.fill = Color.BLACK
                        blocked.stroke = Color.BLACK
                        root.children.add(blocked)
                    }
                    '_' -> {
                        val free = Rectangle(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE)
                        free.fill = Color.LIGHTSLATEGRAY
                        free.stroke = Color.WHITE
                        free.opacity = 0.5
                        root.children.add(free)
                    }
                }
            }
        }

        primaryStage.scene = Scene(root, TILE_SIZE * columnCount, TILE_SIZE * rowCount)
        //primaryStage.scene = Scene(root, WIDTH, HEIGHT)
        primaryStage.show()


        /* Create the path that the robot will travel */
        robot.toFront()
        val path = Path()
        val xLoc = startX * TILE_SIZE
        val yLoc = startY * TILE_SIZE
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
        var count = 0
        while (count != actionList.size) {
            val x = actionList.get(count)
            val y = actionList.get(count + 1)
            val ptList = animate(root, x, y, DISPLAY_LINE, robot, TILE_SIZE)
            for(pt in ptList)
                sq.children.add(pt)
            count+=2
        }
        sq.setCycleCount(Timeline.INDEFINITE);
        sq.play()
    }


    private fun animate(root: Pane, x: String, y: String, dispLine: Boolean, robot: Rectangle, width: Double): MutableList<PathTransition> {
        val retval: MutableList<PathTransition> = arrayListOf()

        val xDDot = x.toDouble() * width
        val yDDot = y.toDouble() * width

        val nSteps = 100
        val dt = 1.0 / nSteps

        for (i in 0..nSteps-1) {
            val path = Path()
            path.elements.add(MoveTo(robot.translateX, robot.translateY))

            var xdot = xDot + xDDot * (dt * i)
            var ydot = yDot + yDDot * (dt * i)

            path.elements.add(LineTo(robot.translateX + (xdot * dt), robot.translateY + (ydot * dt)))
            robot.translateX += xdot * dt;
            robot.translateY += ydot * dt;

            if(dispLine){
                path.stroke = Color.RED
                root.children.add(path)
            }
            /* Animate the robot */
            val pathTransition = PathTransition()
            pathTransition.setDuration(Duration.millis(10.0))
            pathTransition.setPath(path)
            pathTransition.setNode(robot)
            pathTransition.setInterpolator(Interpolator.LINEAR);
            retval.add(pathTransition)
        }

        xDot += xDDot
        yDot += yDDot

        if(dispLine){
            val action = Circle(robot.translateX, robot.translateY, width / 10.0)
            root.children.add(action)
        }
        return retval
    }
}

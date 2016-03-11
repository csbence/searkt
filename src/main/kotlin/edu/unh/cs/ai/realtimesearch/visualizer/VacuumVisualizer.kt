package edu.unh.cs.ai.realtimesearch.visualizer

/**
 * Created by Stephen on 2/11/16.
 */

import edu.unh.cs.ai.realtimesearch.experiment.configuration.json.experimentResultFromJson
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import groovyjarjarcommonscli.GnuParser
import groovyjarjarcommonscli.HelpFormatter
import groovyjarjarcommonscli.Option
import groovyjarjarcommonscli.Options
import javafx.animation.Interpolator
import javafx.animation.PathTransition
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

class VacuumVisualizer : Application() {
    private var experimentResult: ExperimentResult? = null

    private fun processCommandLine(args: Array<String>) {
        val options = Options()

        val helpOption = Option("h", "help", false, "Print help and exit")

        options.addOption(helpOption)

        /* parse command line arguments */
        val parser = GnuParser()
        val cmd = parser.parse(options, args)

        /* print help if help option was specified*/
        val formatter = HelpFormatter()
        if (cmd.hasOption("h")) {
            formatter.printHelp("real-time-search", options)
            exitProcess(1)
        }

        if (cmd.args.size < 1) {
            throw IllegalArgumentException("Error: Must pass results to visualizer")
        }

        experimentResult = experimentResultFromJson(cmd.args.first())
    }

    override fun start(primaryStage: Stage) {
        processCommandLine(parameters.raw.toTypedArray())

        val DISPLAY_LINE = true

        val rawDomain = experimentResult!!.experimentConfiguration["rawDomain"] as String

        /* Get action list from Application */
        val actionList: MutableList<String> = arrayListOf()
        for (action in experimentResult!!.actions) {
            actionList.add(action)
        }

        val TIME_TO_RUN = actionList.size * 200.0

        /* Assuming the domain is correct because the experiment was already run */
        primaryStage.title = "RTS Visualizer"
        val inputScanner = Scanner(rawDomain.byteInputStream())

        val rowCount: Int
        val columnCount: Int

        columnCount = inputScanner.nextLine().toInt()
        rowCount = inputScanner.nextLine().toInt()

        val root = Pane()

        /* Graphical parameters */
        val WIDTH = 1600.0
        val HEIGHT = 800.0
        val TILE_WIDTH: Double = (WIDTH / columnCount)
        val TILE_HEIGHT: Double = (HEIGHT / rowCount)
        var TILE_SIZE = Math.min(TILE_WIDTH, TILE_HEIGHT)

        while(((TILE_SIZE * columnCount) > WIDTH) || ((TILE_SIZE * rowCount) > HEIGHT)){
            TILE_SIZE /= 1.05
        }


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

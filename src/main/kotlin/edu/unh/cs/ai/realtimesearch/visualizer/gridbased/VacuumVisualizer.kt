package edu.unh.cs.ai.realtimesearch.visualizer.gridbased

import edu.unh.cs.ai.realtimesearch.environment.location.Location
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.visualizer.BaseVisualizer
import groovyjarjarcommonscli.CommandLine
import groovyjarjarcommonscli.Options
import javafx.animation.Interpolator
import javafx.animation.PathTransition
import javafx.animation.Timeline
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.scene.shape.Shape
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.util.Duration
import java.util.*


/**
 * Info for a grid world map.
 *
 * @author Mike Bogochow (mgp36@unh.edu)
 * @since April 8, 2016
 */
data class MapInfo(
        val rowCount: Int,
        val columnCount: Int,
        val blockedCells: MutableList<Location> = mutableListOf(),
        val startCells: MutableList<Location> = mutableListOf(),
        val endCells: MutableList<Location> = mutableListOf())

/**
 * A canvas which displays the grid specified in the provided {@link MapInfo}.  All cells except start cells are
 * displayed automatically.
 *
 * @author Mike Bogochow (mgp36@unh.edu)
 * @since April 8, 2016
 */
class VacuumGrid(val mapInfo: MapInfo, val tileSize: Double) : Pane() {
    val canvas: Canvas = Canvas()
    val gridWidth = mapInfo.columnCount * tileSize
    val gridHeight = mapInfo.rowCount * tileSize

    init {
        children.add(canvas)
    }

    override fun layoutChildren() {
        val top = snappedTopInset()
        val right = snappedRightInset()
        val bottom = snappedBottomInset()
        val left = snappedLeftInset()
        val layoutWidth = width - left - right
        val layoutHeight = height - top - bottom
        canvas.layoutX = left
        canvas.layoutY = top

        if (layoutWidth != canvas.width || layoutHeight != canvas.height) {
            canvas.width = layoutWidth
            canvas.height = layoutHeight
            val g: GraphicsContext = canvas.graphicsContext2D

            g.stroke = Color.WHITE
            g.fill = Color.LIGHTSLATEGRAY
            g.lineWidth = 0.1

            // Set background color
            g.fillRect(canvas.layoutX, canvas.layoutY, canvas.width, canvas.height)

            // Add row lines
            for (row in 1..mapInfo.rowCount) {
                val yPosition = row * tileSize
                g.strokeLine(0.0, yPosition, gridWidth, yPosition)
            }

            // Add column lines
            for (column in 1..mapInfo.columnCount) {
                val xPosition = column * tileSize
                g.strokeLine(xPosition, 0.0, xPosition, gridHeight)
            }

            // Add blocked cells
            g.fill = Color.BLACK
            for (cell in mapInfo.blockedCells) {
                g.fillRect(cell.x.toDouble() * tileSize, cell.y.toDouble() * tileSize, tileSize, tileSize)
            }

            // Add goal cells
            g.fill = Color.BLUE
            val radius = tileSize / 10.0
            val diameter = radius * 2
            for (cell in mapInfo.endCells) {
                val dirtyLocX = cell.x * tileSize + tileSize / 2.0 - radius
                val dirtyLocY = cell.y * tileSize + tileSize / 2.0 - radius

                g.fillOval(dirtyLocX, dirtyLocY, diameter, diameter)
            }
        }
    }
}

/**
 * Created by Stephen on 2/11/16.
 */
class VacuumVisualizer : BaseVisualizer() {
    var moveRobot = true
    var trackRobot = true
    var arastarXOriginal = 0.0
    var arastarYOriginal = 0.0
    var arastarX = 0.0
    var arastarY = 0.0
    var count = 0

    var tileSize = 0.0
    var tileHeight = 0.0
    var tileWidth = 0.0

    override fun getOptions(): Options = Options()

    override fun processOptions(cmd: CommandLine) {
    }

    /**
     * Parse map; fill {@link MapInfo}
     */
    fun parseGrid(rawDomain: String): MapInfo {
        val inputScanner = Scanner(rawDomain.byteInputStream())
        val columnCount = inputScanner.nextLine().toInt()
        val rowCount = inputScanner.nextLine().toInt()
        val mapInfo = MapInfo(rowCount, columnCount)
        for (y in 0..rowCount - 1) {
            val line = inputScanner.nextLine()
            for (x in 0..columnCount - 1) {
                when (line[x]) {
                    '#' -> {
                        mapInfo.blockedCells.add(Location(x, y))
                    }
                    '_' -> {
                    }
                    '*' -> {
                        mapInfo.endCells.add(Location(x, y))
                    }
                    '@' -> {
                        mapInfo.startCells.add(Location(x, y))
                    }
                    else -> {
                        throw IllegalArgumentException("Invalid character ${line[x]} found in map")
                    }
                }
            }
        }
        return mapInfo
    }

    override fun start(primaryStage: Stage) {
        processCommandLine(parameters.raw.toTypedArray())

        val DISPLAY_LINE = true

        val rawDomain = experimentResult!!.experimentConfiguration[Configurations.RAW_DOMAIN.toString()] as String

        /* Get action list from Application */
        val actionList: MutableList<String> = arrayListOf()
        for (action in experimentResult!!.actions) {
            actionList.add(action)
        }

        val isARAStar = false
        if (isARAStar)
            moveRobot = false

        val TIME_TO_RUN = actionList.size * 200.0

        /* Assuming the domain is correct because the experiment was already run */
        primaryStage.title = "RTS Visualizer"

        val mapInfo = parseGrid(rawDomain)

        if (mapInfo.startCells.size != 1) {
            throw IllegalArgumentException("${mapInfo.startCells.size} start cells found in map; required 1")
        }

        /* Graphical parameters */
        val primaryScreenBounds = Screen.getPrimary().visualBounds
        val WIDTH = primaryScreenBounds.width - 100
        val HEIGHT = primaryScreenBounds.height - 100
        tileWidth = WIDTH / mapInfo.columnCount
        tileHeight = HEIGHT / mapInfo.rowCount
        tileSize = Math.min(tileWidth, tileHeight)

        while (((tileSize * mapInfo.columnCount) > WIDTH) || ((tileSize * mapInfo.rowCount) > HEIGHT)) {
            tileSize /= 1.05
        }

        /* The robot */
        val robotWidth = tileSize / 2.0
        val robotView = RobotView(robotWidth)
        robotView.trackingEnabled = trackRobot

        val grid = VacuumGrid(mapInfo, tileSize)
        grid.children.add(robotView.robot)
        grid.children.add(robotView.tracker)

        primaryStage.scene = Scene(grid, tileSize * mapInfo.columnCount, tileSize * mapInfo.rowCount)
        //primaryStage.scene = Scene(root, WIDTH, HEIGHT)
        primaryStage.show()

        val robotStartX = mapInfo.startCells.first().x
        val robotStartY = mapInfo.startCells.first().y

        /* Create the path that the robot will travel */
        robotView.toFront()

        val robotLocationX = robotStartX * tileSize + ((tileSize) / 2.0)
        val robotLocationY = robotStartY * tileSize + ((tileSize) / 2.0)
        robotView.setLocation(robotLocationX, robotLocationY)
        //path.elements.add(MoveTo(xLoc, yLoc))
        //path.stroke = Color.ORANGE
        if (isARAStar) {
            arastarXOriginal = robotLocationX
            arastarYOriginal = robotLocationY
            arastarX = robotLocationX
            arastarY = robotLocationY
        }

        /* Display the path */
        //if(DISPLAY_LINE)
        //root.children.add(path)

        val paths: MutableList<Path> = arrayListOf()
        //if(isARAStar){
        val p = Path()
        p.elements.add(MoveTo(robotLocationX, robotLocationY))
        paths.add(p)
        //}
        var pIndex = 0

        for (action in actionList) {
            val path = paths[pIndex]

            if (action.contains(".")) {
                arastarX = arastarXOriginal
                arastarY = arastarYOriginal
                //path.stroke = Color.RED

                val newPath = Path()
                //println("" + arastarX + " " + arastarY)
                newPath.elements.add(MoveTo(arastarX, arastarY))
                paths.add(newPath)
                pIndex++
                count = 0;
            } else if (!action.equals("UP")
                    && !action.equals("DOWN")
                    && !action.equals("LEFT")
                    && !action.equals("RIGHT")) {
                //                println(action)
                moveRobot = true
                val newP = Path()
                newP.elements.add(MoveTo(robotLocationX, robotLocationY))
                paths.add(newP)
                pIndex++
            } else {
                //println(action)
                animate(action, path, robotView.robot, tileSize, tileSize)
            }
        }

        //for(it in paths) {
        if (DISPLAY_LINE) {
            grid.children.add(paths[pIndex])
        }
        //}

        //        if(isARAStar) {
        //            paths.get(0).stroke = Color.RED
        //            paths.get(1).stroke = Color.YELLOW
        //            paths.get(2).stroke = Color.BLACK
        //            paths.get(3).stroke = Color.CYAN
        //            paths.get(4).stroke = Color.BLUE
        //            paths.get(5).stroke = Color.MAGENTA
        //            paths.get(6).stroke = Color.GREEN
        //            paths.get(7).stroke = Color.WHITE
        //            paths.get(8).stroke = Color.GOLD
        //            paths.get(9).stroke = Color.PLUM
        //        }

        paths[pIndex].stroke = Color.RED

        /* Animate the robot */
        val pathTransition = PathTransition()
        pathTransition.duration = Duration.millis(TIME_TO_RUN)
        pathTransition.path = paths[pIndex]
        pathTransition.node = robotView.robot
        pathTransition.interpolator = Interpolator.LINEAR
        pathTransition.cycleCount = Timeline.INDEFINITE
        pathTransition.play()
    }

    private fun animate(action: String, path: Path, robot: Shape, width: Double, height: Double) {
        //path.elements.add(MoveTo(arastarX, arastarY))
        count++;
        when (action) {
            "UP" -> {
                if (moveRobot) {
                    path.elements.add(LineTo(robot.translateX, robot.translateY + height))
                    robot.translateY = robot.translateY + height
                } else {
                    path.elements.add(LineTo(arastarX, arastarY + height))
                    arastarY += height
                    if (count <= 3) {
                        arastarYOriginal = arastarY
                    }
                }
            }
            "RIGHT" -> {
                if (moveRobot) {
                    path.elements.add(LineTo(robot.translateX + width, robot.translateY))
                    robot.translateX = robot.translateX + width
                } else {
                    path.elements.add(LineTo(arastarX + width, arastarY))
                    arastarX += width
                    if (count <= 3) {
                        arastarXOriginal = arastarX
                    }
                }
            }
            "DOWN" -> {
                if (moveRobot) {
                    path.elements.add(LineTo(robot.translateX, robot.translateY - height))
                    robot.translateY = robot.translateY - height
                } else {
                    path.elements.add(LineTo(arastarX, arastarY - height))
                    arastarY -= height
                    if (count <= 3) {
                        arastarYOriginal = arastarY
                    }
                }
            }
            "LEFT" -> {
                if (moveRobot) {
                    path.elements.add(LineTo(robot.translateX - width, robot.translateY))
                    robot.translateX = robot.translateX - width
                } else {
                    path.elements.add(LineTo(arastarX - width, arastarY))
                    arastarX -= width
                    if (count <= 3) {
                        arastarXOriginal = arastarX
                    }
                }
            }
        }
    }
}

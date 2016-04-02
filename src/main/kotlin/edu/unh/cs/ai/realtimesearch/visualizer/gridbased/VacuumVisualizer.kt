package edu.unh.cs.ai.realtimesearch.visualizer.gridbased

/**
 * Created by Stephen on 2/11/16.
 */

import edu.unh.cs.ai.realtimesearch.visualizer.BaseVisualizer
import groovyjarjarcommonscli.CommandLine
import groovyjarjarcommonscli.Options
import javafx.animation.Interpolator
import javafx.animation.PathTransition
import javafx.animation.Timeline
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.*
import javafx.stage.Stage
import javafx.util.Duration
import java.util.*
import kotlin.system.exitProcess

class VacuumVisualizer : BaseVisualizer() {
    var moverobot = true;
    var arastarXOrig = 0.0;
    var arastarYOrig = 0.0;
    var arastarX = 0.0;
    var arastarY = 0.0;
    var count = 0;

    override fun getOptions(): Options = Options()

    override fun processOptions(cmd: CommandLine) {}

    override fun start(primaryStage: Stage) {
//        processCommandLine(parameters.raw.toTypedArray())
//
//        val DISPLAY_LINE = true
//
//        val rawDomain = experimentResult!!.experimentConfiguration["rawDomain"] as String
//
//        /* Get action list from Application */
//        val actionList: MutableList<String> = arrayListOf()
//        for (action in experimentResult!!.actions) {
//            actionList.add(action)
//        }
        val DISPLAY_LINE = true
        val isARAStar = true;
        if(isARAStar)
            moverobot = false

        val parameters = getParameters()
        val raw = parameters.raw
        if(raw.isEmpty()){
            println("Cannot visualize without a domain!")
            //exitProcess(1);
        }


        val rawDomain = raw.first()//experimentResult!!.experimentConfiguration["rawDomain"] as String

        /* Get action list from Application */
        val actionList: MutableList<String> = arrayListOf()
        for (i in 1..raw.size - 1){
            actionList.add(raw.get(i))
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
        //val path = Path()
        val xLoc = startX * TILE_SIZE + ((TILE_SIZE) / 2.0)
        val yLoc = startY * TILE_SIZE + ((TILE_SIZE) / 2.0)
        robot.x = xLoc
        robot.y = yLoc
        robot.translateX = xLoc
        robot.translateY = yLoc
        //path.elements.add(MoveTo(xLoc, yLoc))
        //path.stroke = Color.ORANGE
        if(isARAStar) {
            arastarXOrig = xLoc
            arastarYOrig = yLoc
            arastarX = xLoc
            arastarY = yLoc
        }

        /* Display the path */
        //if(DISPLAY_LINE)
            //root.children.add(path)

        val paths: MutableList<Path> = arrayListOf()
        //if(isARAStar){
            val p = Path()
            p.elements.add(MoveTo(xLoc, yLoc))
            paths.add(p)
        //}
        var pIndex = 0;

        for (action in actionList) {
            val p = paths.get(pIndex)

            if(action.contains(".")){
                arastarX = arastarXOrig
                arastarY = arastarYOrig
                //path.stroke = Color.RED

                val newP = Path()
                //println("" + arastarX + " " + arastarY)
                newP.elements.add(MoveTo(arastarX, arastarY))
                paths.add(newP)
                pIndex++;
                count = 0;
            }
            else if(!action.equals("UP")
                    && !action.equals("DOWN")
                    && !action.equals("LEFT")
                    && !action.equals("RIGHT")){
                println(action);
                moverobot = true;
                val newP = Path()
                newP.elements.add(MoveTo(xLoc, yLoc))
                paths.add(newP)
                pIndex ++
            }
            else {
                //println(action)
                animate(root, action, p, robot, TILE_SIZE, TILE_SIZE)
            }
        }

        //for(it in paths) {
            if (DISPLAY_LINE) {
                root.children.add(paths.get(pIndex))
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

        paths.get(pIndex).stroke = Color.ORANGE

        /* Animate the robot */
        val pathTransition = PathTransition()
        pathTransition.setDuration(Duration.millis(TIME_TO_RUN))
        pathTransition.setPath(paths.get(pIndex))
        pathTransition.setNode(robot)
        pathTransition.setInterpolator(Interpolator.LINEAR);
        pathTransition.setCycleCount(Timeline.INDEFINITE);
        pathTransition.play()


    }

    private fun animate(root: Pane, action: String, path: Path, robot: Rectangle, width: Double, height: Double) {
        //path.elements.add(MoveTo(arastarX, arastarY))
        count++;
        when (action) {
            "UP" -> {
                if(moverobot) {
                    path.elements.add(LineTo(robot.translateX, robot.translateY + height))
                    robot.translateY = robot.translateY + height
                }
                else {
                    path.elements.add(LineTo(arastarX, arastarY + height))
                    arastarY = arastarY + height
                    if(count <= 3){
                        arastarYOrig = arastarY
                    }
                }
            }
            "RIGHT" -> {
                if(moverobot) {
                    path.elements.add(LineTo(robot.translateX + width, robot.translateY))
                    robot.translateX = robot.translateX + width
                }
                else{
                    path.elements.add(LineTo(arastarX + width, arastarY))
                    arastarX = arastarX + width
                    if(count <= 3){
                        arastarXOrig = arastarX
                    }
                }
            }
            "DOWN" -> {
                if(moverobot) {
                    path.elements.add(LineTo(robot.translateX, robot.translateY - height))
                    robot.translateY = robot.translateY - height
                }
                else {
                    path.elements.add(LineTo(arastarX, arastarY - height))
                    arastarY = arastarY - height
                    if(count <= 3){
                        arastarYOrig = arastarY
                    }
                }
            }
            "LEFT" -> {
                if(moverobot) {
                    path.elements.add(LineTo(robot.translateX - width, robot.translateY))
                    robot.translateX = robot.translateX - width
                }
                else{
                    path.elements.add(LineTo(arastarX - width, arastarY))
                    arastarX = arastarX - width
                    if(count <= 3){
                        arastarXOrig = arastarX
                    }
                }
            }
        }
    }
}

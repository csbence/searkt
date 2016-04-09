package edu.unh.cs.ai.realtimesearch.visualizer.gridbased

import groovyjarjarcommonscli.CommandLine
import groovyjarjarcommonscli.Options
import javafx.animation.Interpolator
import javafx.animation.PathTransition
import javafx.animation.Timeline
import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.scene.shape.Shape
import javafx.stage.Stage
import javafx.util.Duration

/**
 * Created by Stephen on 2/11/16.
 */
class VacuumVisualizer : GridBasedVisualizer() {
    var moveRobot = true
    var arastarXOriginal = 0.0
    var arastarYOriginal = 0.0
    var arastarX = 0.0
    var arastarY = 0.0
    var count = 0

    override fun getOptions(): Options = super.getOptions()

    override fun processOptions(cmd: CommandLine) = super.processOptions(cmd)

    override fun start(primaryStage: Stage) {
        processCommandLine(parameters.raw.toTypedArray())

        visualizerSetup()

        val isARAStar = false
        if (isARAStar)
            moveRobot = false

        primaryStage.title = "RTS Visualizer"
        primaryStage.scene = Scene(grid, tileSize * mapInfo.columnCount, tileSize * mapInfo.rowCount, Color.LIGHTSLATEGRAY)
        primaryStage.show()


        //path.elements.add(MoveTo(xLoc, yLoc))
        //path.stroke = Color.ORANGE
        if (isARAStar) {
            arastarXOriginal = robotView.robot.x
            arastarYOriginal = robotView.robot.y
            arastarX = robotView.robot.x
            arastarY = robotView.robot.y
        }

        /* Display the path */
        //if(DISPLAY_LINE)
        //root.children.add(path)

        val paths: MutableList<Path> = arrayListOf()
        //if(isARAStar){
        val p = Path()
        p.elements.add(MoveTo(robotView.robot.x, robotView.robot.y))
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
                newP.elements.add(MoveTo(robotView.robot.x, robotView.robot.y))
                paths.add(newP)
                pIndex++
            } else {
                //println(action)
                animate(action, path, robotView.robot, tileSize, tileSize)
            }
        }

        //for(it in paths) {
        if (displayLine) {
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
        pathTransition.duration = Duration.millis(timeToRun)
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

package edu.unh.cs.ai.realtimesearch.visualizer.gridbased

import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.planner.Planners
import edu.unh.cs.ai.realtimesearch.visualizer.ThemeColors
import groovyjarjarcommonscli.CommandLine
import groovyjarjarcommonscli.Options
import javafx.animation.Interpolator
import javafx.animation.PathTransition
import javafx.animation.Timeline
import javafx.scene.Scene
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.stage.Stage
import javafx.util.Duration

/**
 * Created by Stephen on 2/11/16.
 */
class VacuumVisualizer : GridBasedVisualizer() {
    var isARAStar = false
    var moveRobot = true
    var araStarXOriginal = 0.0
    var araStarYOriginal = 0.0
    var araStarX = 0.0
    var araStarY = 0.0
    var anytimeCount = 0L
    var anytimeMaxCount = 3L

    override fun getOptions(): Options = super.getOptions()

    override fun processOptions(cmd: CommandLine) = super.processOptions(cmd)

    override fun start(primaryStage: Stage) {
        processCommandLine(parameters.raw.toTypedArray())

        visualizerSetup()

        isARAStar = experimentResult!!.experimentConfiguration[Configurations.ALGORITHM_NAME.toString()] == Planners.ARA_STAR.toString()
        if (isARAStar) {
            moveRobot = false
            anytimeMaxCount = experimentResult!!.experimentConfiguration[Configurations.ANYTIME_MAX_COUNT.toString()] as Long
            araStarXOriginal = agentView.agent.x
            araStarYOriginal = agentView.agent.y
            araStarX = agentView.agent.x
            araStarY = agentView.agent.y
        }

        primaryStage.title = "RTS Visualizer"
        primaryStage.scene = Scene(grid, tileSize * mapInfo.columnCount, tileSize * mapInfo.rowCount, ThemeColors.BACKGROUND.color)
        primaryStage.show()

        val path = buildAnimation()

        /* Animate the robot */
        val pathTransition = PathTransition()
        pathTransition.duration = Duration.millis(timeToRun)
        pathTransition.path = path
        pathTransition.node = agentView.agent
        pathTransition.interpolator = Interpolator.LINEAR
        pathTransition.cycleCount = Timeline.INDEFINITE
        pathTransition.play()
    }

    private fun buildAnimation(): Path {
        val paths: MutableList<Path> = arrayListOf()
        //if(isARAStar){
        val p = Path()
        p.elements.add(MoveTo(agentView.agent.x, agentView.agent.y))
        paths.add(p)
        //}
        var pIndex = 0

        for (action in actionList) {
            val path = paths[pIndex]

            if (isARAStar && action.contains(".")) {
                araStarX = araStarXOriginal
                araStarY = araStarYOriginal
                //path.stroke = Color.RED

                val newPath = Path()
                //println("" + arastarX + " " + arastarY)
                newPath.elements.add(MoveTo(araStarX, araStarY))
                paths.add(newPath)
                pIndex++
                anytimeCount = 0
            } else if (!action.equals("UP")
                    && !action.equals("DOWN")
                    && !action.equals("LEFT")
                    && !action.equals("RIGHT")) {
                //                println(action)
                moveRobot = true
                val newPath = Path()
                newPath.elements.add(MoveTo(agentView.agent.x, agentView.agent.y))
                paths.add(newPath)
                pIndex++
            } else {
                //println(action)
                animate(action, path)
            }
        }

        /* Display the path */
        if (displayLine) {
            grid.children.add(paths[pIndex])
            paths[pIndex].stroke = ThemeColors.PATH.stroke
        }

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

        return paths[pIndex]
    }

    private fun animate(action: String, path: Path) {
        val robot = agentView.agent
        val width = tileSize
        val height = tileSize

        if (isARAStar)
            anytimeCount++

        when (action) {
            "UP" -> {
                if (moveRobot) {
                    path.elements.add(LineTo(robot.translateX, robot.translateY + height))
                    robot.translateY += height
                } else if (isARAStar) {
                    path.elements.add(LineTo(araStarX, araStarY + height))
                    araStarY += height
                    if (anytimeCount <= anytimeMaxCount) {
                        araStarYOriginal = araStarY
                    }
                }
            }
            "RIGHT" -> {
                if (moveRobot) {
                    path.elements.add(LineTo(robot.translateX + width, robot.translateY))
                    robot.translateX += width
                } else if (isARAStar) {
                    path.elements.add(LineTo(araStarX + width, araStarY))
                    araStarX += width
                    if (anytimeCount <= anytimeMaxCount) {
                        araStarXOriginal = araStarX
                    }
                }
            }
            "DOWN" -> {
                if (moveRobot) {
                    path.elements.add(LineTo(robot.translateX, robot.translateY - height))
                    robot.translateY -= height
                } else if (isARAStar) {
                    path.elements.add(LineTo(araStarX, araStarY - height))
                    araStarY -= height
                    if (anytimeCount <= anytimeMaxCount) {
                        araStarYOriginal = araStarY
                    }
                }
            }
            "LEFT" -> {
                if (moveRobot) {
                    path.elements.add(LineTo(robot.translateX - width, robot.translateY))
                    robot.translateX -= width
                } else if (isARAStar) {
                    path.elements.add(LineTo(araStarX - width, araStarY))
                    araStarX -= width
                    if (anytimeCount <= anytimeMaxCount) {
                        araStarXOriginal = araStarX
                    }
                }
            }
        }
    }
}

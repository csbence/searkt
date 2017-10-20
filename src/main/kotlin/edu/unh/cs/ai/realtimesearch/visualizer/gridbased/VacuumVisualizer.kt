package edu.unh.cs.ai.realtimesearch.visualizer.gridbased

import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.planner.Planners
import edu.unh.cs.ai.realtimesearch.util.convertNanoUpDouble
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
import java.util.concurrent.TimeUnit

/**
 * Visualizer for the vacuum world and grid world domains.
 *
 * @author Stephen Chambers, Mike Bogochow
 * @since 2/11/16
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

    /**
     * The current x position of the agent in the animation that is being built.
     */
    protected var animationX = 0.0

    /**
     * The current y position of the agent in the animation that is being built.
     */
    protected var animationY = 0.0

    /**
     * The animation time for a single transition in the animation in milliseconds.
     */
    private val animationStepDuration = 200.0

    override fun getOptions(): Options = super.getOptions()

    override fun processOptions(cmd: CommandLine) = super.processOptions(cmd)

    override fun start(primaryStage: Stage) {
        processCommandLine(parameters.raw.toTypedArray())

        visualizerSetup()

        val timeToRun = actionList.size * animationStepDuration

        isARAStar = experimentResult.configuration[Configurations.ALGORITHM_NAME.toString()] == Planners.ARA_STAR.toString()
        if (isARAStar) {
            moveRobot = false
            anytimeMaxCount = experimentResult.configuration[Configurations.ANYTIME_MAX_COUNT.toString()] as Long
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

        // Delay startup of animation to simulate idle planning time
        Thread({
            val delayTime = convertNanoUpDouble(experimentResult.idlePlanningTime, TimeUnit.MILLISECONDS) * animationStepDuration / convertNanoUpDouble(experimentResult.configuration[Configurations.ACTION_DURATION.toString()] as Long, TimeUnit.MILLISECONDS)
            println("Delay:  $delayTime")
            Thread.sleep(delayTime.toLong())
            pathTransition.play()
        }).start()
    }

    /**
     * Build a path for the agent to follow from the action list.
     */
    private fun buildAnimation(): Path {
        val paths: MutableList<Path> = arrayListOf()
        //if(isARAStar){
        val p = Path()
        p.elements.add(MoveTo(initialAgentXLocation, initialAgentYLocation))
        paths.add(p)
        //}
        var pIndex = 0

        animationX = initialAgentXLocation
        animationY = initialAgentYLocation

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
                newPath.elements.add(MoveTo(animationX, animationY))
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

    /**
     * Add the proper animations to the path for the given action.
     *
     * @param path the path to add to
     * @param action the action to animate
     */
    private fun animate(action: String, path: Path) {
        val width = tileSize
        val height = tileSize

        if (isARAStar)
            anytimeCount++

        when (action) {
            "UP" -> {
                if (moveRobot) {
                    path.elements.add(LineTo(animationX, animationY + height))
                    animationY += height
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
                    path.elements.add(LineTo(animationX + width, animationY))
                    animationX += width
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
                    path.elements.add(LineTo(animationX, animationY - height))
                    animationY -= height
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
                    path.elements.add(LineTo(animationX - width, animationY))
                    animationX -= width
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

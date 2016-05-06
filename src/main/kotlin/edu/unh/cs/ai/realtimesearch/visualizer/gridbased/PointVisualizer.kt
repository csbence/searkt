package edu.unh.cs.ai.realtimesearch.visualizer.gridbased

import edu.unh.cs.ai.realtimesearch.environment.pointrobot.PointRobotHeader
import edu.unh.cs.ai.realtimesearch.environment.pointrobot.PointRobotIO
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.util.convertNanoUpDouble
import edu.unh.cs.ai.realtimesearch.util.roundToNearestDecimal
import edu.unh.cs.ai.realtimesearch.visualizer.ThemeColors
import edu.unh.cs.ai.realtimesearch.visualizer.delayPlay
import groovyjarjarcommonscli.CommandLine
import groovyjarjarcommonscli.Options
import javafx.animation.Interpolator
import javafx.animation.PathTransition
import javafx.animation.SequentialTransition
import javafx.animation.Timeline
import javafx.scene.Scene
import javafx.scene.shape.Circle
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.stage.Stage
import javafx.util.Duration
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Visualizer for the point robot domain.
 *
 * @author Stephen Chambers, Mike Bogochow
 * @since 2/29/16
 */
open class PointVisualizer : GridBasedVisualizer() {
    /**
     * Parsed header information from the raw domain.
     */
    protected lateinit var header: PointRobotHeader

    /**
     * The action duration of the experiment configuration.
     */
    protected var actionDuration: Long = 0

    /**
     * The total time the animation will run for.
     */
    protected var animationStepDuration: Double = 0.0

    /**
     * The minimum animation time to ensure that the animation does not end too quickly.
     */
    protected val minimumAnimationTime: Double = 500.0

    /**
     * The current x position of the agent in the animation that is being built.
     */
    protected var animationX = 0.0

    /**
     * The current y position of the agent in the animation that is being built.
     */
    protected var animationY = 0.0

    override var robotScale: Double = 4.0

    override fun getAnimationStepDuration(configuration: Map<String, Any?>): Double {
        val actionDuration = configuration[Configurations.ACTION_DURATION.toString()] as Long
        return convertNanoUpDouble(actionDuration, TimeUnit.MILLISECONDS)
    }

    override fun getOptions(): Options = super.getOptions()

    override fun processOptions(cmd: CommandLine) = super.processOptions(cmd)

    open protected fun setupDomain() {}

    override fun parseMapHeader(inputScanner: Scanner): GridDimensions {
        header = PointRobotIO.parseHeader(inputScanner)
        return GridDimensions(header.rowCount, header.columnCount)
    }

    override fun parseActions(): MutableList<String> {
        val actionList: MutableList<String> = arrayListOf()
        for (action in experimentResult.actions) {
            var xStart = action.indexOf('(') + 1
            var xEnd = action.indexOf(',')
            var yStart = xEnd + 2
            var yEnd = action.indexOf(')')

            val x = action.substring(xStart, xEnd)
            val y = action.substring(yStart, yEnd)
            actionList.add(x)
            actionList.add(y)
        }
        return actionList
    }

    override fun start(primaryStage: Stage) {
        processCommandLine(parameters.raw.toTypedArray())

        visualizerSetup()
        actionDuration = experimentResult.experimentConfiguration[Configurations.ACTION_DURATION.toString()] as Long
        animationStepDuration = convertNanoUpDouble(actionDuration, TimeUnit.MILLISECONDS)
        if (animationStepDuration < minimumAnimationTime)
            animationStepDuration = minimumAnimationTime

        setupDomain()
        val goalX = mapInfo.goalCells.first().x + header.goalLocationOffset.x
        val goalY = mapInfo.goalCells.first().y + header.goalLocationOffset.y

        /* the goal radius */
        val goalCircle = Circle(goalX * tileSize, goalY * tileSize, header.goalRadius * tileSize)
        goalCircle.stroke = ThemeColors.GOAL_CIRCLE.stroke
        goalCircle.fill = ThemeColors.GOAL_CIRCLE.color
        goalCircle.opacity = ThemeColors.GOAL_CIRCLE.opacity
        grid.children.add(goalCircle)

        primaryStage.title = "RTS Visualizer"
        primaryStage.scene = Scene(grid, tileSize * mapInfo.columnCount, tileSize * mapInfo.rowCount,
                ThemeColors.BACKGROUND.color)
        //primaryStage.scene = Scene(root, WIDTH, HEIGHT)
        primaryStage.show()

        playAnimation(buildAnimation())
    }

    /**
     * Forms a sequential transition from the given transitions and plays it.
     */
    open protected fun playAnimation(transitions: List<PathTransition>) {
        val sequentialTransition = SequentialTransition()
        for (pathTransition in transitions) {
            sequentialTransition.children.add(pathTransition)
        }
        sequentialTransition.cycleCount = Timeline.INDEFINITE

        // Delay startup of animation to simulate idle planning time
        delayPlay(sequentialTransition, roundToNearestDecimal(animationIdlePlanningTime, 1.0).toLong())
    }

    /**
     * Builds a list of transitions from the action list.
     */
    open protected fun buildAnimation(): List<PathTransition> {
        val pathTransitions = mutableListOf<PathTransition>()

        animationX = initialAgentAnimationLocation.x
        animationY = initialAgentAnimationLocation.y

        val actionIterator = actionList.iterator()
        while (actionIterator.hasNext()) {
            val x = actionIterator.next()
            assert(actionIterator.hasNext(), { "Action has no matching y value" })
            val y = actionIterator.next()
            val pathTransition = animate(x, y)
            pathTransitions.addAll(pathTransition)
        }

        return pathTransitions
    }

    /**
     * Forms necessary transitions for animation given x and y accelerations.
     *
     * @param xAcceleration acceleration on x-axis
     * @param yAcceleration acceleration on y-axis
     * @return list of transitions for animating the acceleration
     */
    open protected fun animate(xAcceleration: String, yAcceleration: String): MutableList<PathTransition> {
        val path = Path()
        val robot = agentView.agent
        val width = tileSize

        val xDot = xAcceleration.toDouble() * width
        val yDot = yAcceleration.toDouble() * width

        path.elements.add(MoveTo(animationX, animationY))
        path.elements.add(LineTo(animationX + xDot, animationY + yDot))
        animationX += xDot
        animationY += yDot

        if (displayLine) {
            path.stroke = ThemeColors.PATH.stroke
            grid.children.add(path)
            val action = Circle(animationX, animationY, width / 10.0)
            grid.children.add(action)
        }

        /* Animate the robot */
        val pathTransition = PathTransition()
        pathTransition.duration = Duration.millis(animationStepDuration)
        pathTransition.path = path
        pathTransition.node = robot
        pathTransition.interpolator = Interpolator.LINEAR
        return mutableListOf(pathTransition)
    }
}

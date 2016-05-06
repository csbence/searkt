package edu.unh.cs.ai.realtimesearch.visualizer.gridbased

import edu.unh.cs.ai.realtimesearch.environment.DiscretizedDomain
import edu.unh.cs.ai.realtimesearch.environment.DiscretizedEnvironment
import edu.unh.cs.ai.realtimesearch.environment.DiscretizedState
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia.PointRobotWithInertia
import edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia.PointRobotWithInertiaAction
import edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia.PointRobotWithInertiaState
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.util.convertNanoUpDouble
import edu.unh.cs.ai.realtimesearch.visualizer.ThemeColors
import javafx.animation.Interpolator
import javafx.animation.PathTransition
import javafx.animation.SequentialTransition
import javafx.animation.Timeline
import javafx.scene.shape.Circle
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.util.Duration
import java.util.concurrent.TimeUnit

/**
 * Visualizer for the point robot with inertia domain.
 *
 * @author Stephen Chambers, Mike Bogochow
 * @since 2/29/16
 */
class PointInertiaVisualizer : PointVisualizer() {
    private lateinit var domain: DiscretizedDomain<PointRobotWithInertiaState, PointRobotWithInertia>
    private lateinit var environment: DiscretizedEnvironment<PointRobotWithInertiaState,
                                                             Domain<DiscretizedState<PointRobotWithInertiaState>>>

    override fun setupDomain() {
        domain = DiscretizedDomain(PointRobotWithInertia(
                mapInfo.columnCount,
                mapInfo.rowCount,
                mapInfo.blockedCells.toHashSet(),
                mapInfo.goalCells.first().toDoubleLocation(),
                header.goalRadius,
                actionDuration
        ))
        environment = DiscretizedEnvironment(domain,
                DiscretizedState(
                        PointRobotWithInertiaState(initialAgentMapCoordinate.x, initialAgentMapCoordinate.y, 0.0, 0.0)))
    }

    override fun playAnimation(transitions: List<PathTransition>) {
        val sequentialTransition = SequentialTransition()
        for (pathTransition in transitions) {
            sequentialTransition.children.add(pathTransition)
        }
        sequentialTransition.cycleCount = Timeline.INDEFINITE

        // Delay startup of animation to simulate idle planning time
        Thread({
            val delayTime =
                    convertNanoUpDouble(experimentResult.idlePlanningTime, TimeUnit.MILLISECONDS) *
                            animationTime /
                            convertNanoUpDouble(
                                    experimentResult.experimentConfiguration[Configurations.ACTION_DURATION.toString()]
                                            as Long, TimeUnit.MILLISECONDS)
            println("Delay:  $delayTime")
            Thread.sleep(delayTime.toLong())
            sequentialTransition.play()
        }).start()
    }

    override fun buildAnimation(): List<PathTransition> {
        animationX = initialAgentAnimationLocation.x
        animationY = initialAgentAnimationLocation.y

        val pathTransitions = mutableListOf<PathTransition>()
        val actionIterator = actionList.iterator()
        while (actionIterator.hasNext()) {
            val x = actionIterator.next()
            assert(actionIterator.hasNext(), { "Action has no matching y value" })
            val y = actionIterator.next()
            var pathTransition = animate(x, y)
            pathTransitions.addAll(pathTransition)
        }

        return pathTransitions
    }

    override fun animate(xAcceleration: String, yAcceleration: String): MutableList<PathTransition> {
        val agent = agentView.agent
        val width = tileSize
        val animation: MutableList<PathTransition> = arrayListOf()
        val action = PointRobotWithInertiaAction(xAcceleration.toDouble(), yAcceleration.toDouble())

        val previousState = environment.getState()
        environment.step(action)
        val newState = environment.getState()
//println("Animating from $previousState to $newState ($action)")
        val xChange = (newState.state.x - previousState.state.x) * tileSize
        val yChange = (newState.state.y - previousState.state.y) * tileSize
//println("x: ${animationX} | y: ${animationY}")
//println("xChange: $xChange | yChange: $yChange")
        val path = Path()
        path.elements.add(MoveTo(animationX, animationY))
        animationX += xChange
        animationY += yChange
        path.elements.add(LineTo(animationX, animationY))

        if(displayLine){
            path.stroke = ThemeColors.PATH.stroke
            grid.children.add(path)
            val actionCircle = Circle(animationX, animationY, width / 10.0)
            grid.children.add(actionCircle)
        }

        /* Animate the robot */
        val pathTransition = PathTransition()
        pathTransition.duration = Duration.millis(convertNanoUpDouble(actionDuration, TimeUnit.MILLISECONDS))
        pathTransition.path = path
        pathTransition.node = agent
        pathTransition.interpolator = Interpolator.LINEAR
        animation.add(pathTransition)

        return animation
    }
}

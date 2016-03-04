package edu.unh.cs.ai.realtimesearch.visualizer

import edu.unh.cs.ai.realtimesearch.environment.acrobot.*
import edu.unh.cs.ai.realtimesearch.logging.debug
import edu.unh.cs.ai.realtimesearch.logging.error
import javafx.animation.*
import javafx.application.Application
import javafx.beans.value.WritableValue
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.stage.Stage
import javafx.util.Duration
import org.slf4j.LoggerFactory

open class AcrobotVisualizer : Application() {
    private val logger = LoggerFactory.getLogger(AcrobotVisualizer::class.java)

    override fun start(primaryStage: Stage) {
        primaryStage.title = "Acrobot Visualizer"

        /* Get domain from Application */
        val parameters = parameters
        val raw = parameters.raw
//        if (raw.isEmpty()) {
//            logger.error { "Cannot visualize without a domain!" }
//            exitProcess(1)
//        }
//        val rawDomain = raw.first()

        /* Get action list from Application */
        val actionList: MutableList<AcrobotAction> = mutableListOf()
        for (i in 1..raw.size - 1) {
            val action: String = raw[i]
            try {
                actionList.add(AcrobotAction.valueOf(action))
            } catch(e: IllegalArgumentException) {
                logger.error { "Invalid action: $action" }
            }
        }

        // TODO temp testing
        actionList.clear()
        actionList.add(AcrobotAction.NEGATIVE)
        actionList.add(AcrobotAction.NEGATIVE)
        actionList.add(AcrobotAction.NEGATIVE)
        actionList.add(AcrobotAction.NEGATIVE)
        actionList.add(AcrobotAction.NEGATIVE)
        actionList.add(AcrobotAction.NONE)
        actionList.add(AcrobotAction.POSITIVE)
        actionList.add(AcrobotAction.POSITIVE)
        actionList.add(AcrobotAction.POSITIVE)
        actionList.add(AcrobotAction.POSITIVE)
        actionList.add(AcrobotAction.POSITIVE)
        actionList.add(AcrobotAction.POSITIVE)
        actionList.add(AcrobotAction.NEGATIVE)
        actionList.add(AcrobotAction.NEGATIVE)
        actionList.add(AcrobotAction.NEGATIVE)
        actionList.add(AcrobotAction.NEGATIVE)
        actionList.add(AcrobotAction.NEGATIVE)
        actionList.add(AcrobotAction.NEGATIVE)
        actionList.add(AcrobotAction.POSITIVE)
        actionList.add(AcrobotAction.POSITIVE)
        actionList.add(AcrobotAction.POSITIVE)
        actionList.add(AcrobotAction.POSITIVE)
        actionList.add(AcrobotAction.POSITIVE)
        actionList.add(AcrobotAction.POSITIVE)
        actionList.add(AcrobotAction.POSITIVE)
        actionList.add(AcrobotAction.POSITIVE)
        actionList.add(AcrobotAction.NONE)
        actionList.add(AcrobotAction.NEGATIVE)
        actionList.add(AcrobotAction.NEGATIVE)
        actionList.add(AcrobotAction.NEGATIVE)
        actionList.add(AcrobotAction.NEGATIVE)
        actionList.add(AcrobotAction.NONE)
        actionList.add(AcrobotAction.POSITIVE)
        actionList.add(AcrobotAction.NEGATIVE)
        actionList.add(AcrobotAction.NONE)

        /* Graphical parameters */
        val stageBorder = 100.0
        val linkScale = 175.0 // pixel size per meter
        val linkScaledLength1 = linkLength1 * linkScale
        val linkScaledLength2 = linkLength2 * linkScale
        val linkWidth = linkScaledLength1 / 7.5
        val WIDTH = (linkScaledLength1 + linkScaledLength2) * 2 + stageBorder * 2
        val HEIGHT = WIDTH

        val linkStartX1 = WIDTH / 2.0
        val linkStartY1 = stageBorder + linkScaledLength1 + linkScaledLength2

        val visualAcrobot = VisualAcrobot(linkStartX1, linkStartY1, linkScaledLength1, linkWidth)

        // Add everything to the stage
        val rootPane = Pane()
        rootPane.children.addAll(visualAcrobot.getNodes())
//        rootPane.children.add(link1)
//        rootPane.children.add(link2)
//        rootPane.children.add(joint1)
//        rootPane.children.add(joint2)

        primaryStage.scene = Scene(rootPane, WIDTH, HEIGHT)
        primaryStage.show()

//        animateAcrobot(visualAcrobot, actionList, {state, action -> CustomEaseInInterpolator(state.calculateLinkAcceleration1(action))})
//        animateAcrobot(visualAcrobot, actionList, {state, action -> Interpolator.SPLINE(0.5, 0.5, 1.0, 1.0)})
//        animateAcrobot(visualAcrobot, actionList)
        animateAcrobot(visualAcrobot, actionList, {state, action -> Interpolator.DISCRETE})
    }

    protected open fun animateAcrobot(visualAcrobot: VisualAcrobot, actionList: List<AcrobotAction>,
                                      getInterpolator1: (state: AcrobotState, action: AcrobotAction) -> Interpolator = { state, action -> Interpolator.LINEAR},
                                      getInterpolator2: (state: AcrobotState, action: AcrobotAction) -> Interpolator = { state, action -> getInterpolator1(state, action)}): Animation {
        /* Animate the links */
        // TODO setup links according to initial state values
        val acrobot = DiscretizedAcrobot()
        val environment = DiscretizedAcrobotEnvironment(acrobot) // TODO read optional initial state from input
        @Suppress("UNCHECKED_CAST")
        val timeline = Timeline(60.0, KeyFrame(Duration.ZERO,
                KeyValue(visualAcrobot.linkRotate1.angleProperty() as WritableValue<Any>, visualAcrobot.linkRotate1.angle),
                KeyValue(visualAcrobot.linkRotate2.angleProperty() as WritableValue<Any>, visualAcrobot.linkRotate2.angle)))

        var previousState = environment.getState().state
        var time = timeStep
        for (action in actionList) {
            environment.step(action)
            val newState = environment.getState().state

            val diff1 = Math.toDegrees(angleDifference(previousState.linkPosition1, newState.linkPosition1))
            val diff2 = Math.toDegrees(angleDifference(previousState.linkPosition2, newState.linkPosition2)) + diff1

            val newRotate1 = visualAcrobot.addRotate1()
            val newRotate2 = visualAcrobot.addRotate2()

            logger.debug { "Adding (${String.format("%.1f", time)}: $diff1, $diff2) to timeline" }
            @Suppress("UNCHECKED_CAST")
            timeline.keyFrames.add(KeyFrame(Duration.seconds(time),
                    KeyValue(newRotate1.angleProperty() as WritableValue<Any>, -diff1, getInterpolator1(previousState, action)),
                    KeyValue(newRotate2.angleProperty() as WritableValue<Any>, -diff2, getInterpolator2(previousState, action))))

            time += timeStep
            previousState = newState
        }
        timeline.play()
        return timeline
    }
}
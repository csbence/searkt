package edu.unh.cs.ai.realtimesearch.visualizer

import edu.unh.cs.ai.realtimesearch.environment.acrobot.*
import edu.unh.cs.ai.realtimesearch.logging.debug
import edu.unh.cs.ai.realtimesearch.logging.error
import edu.unh.cs.ai.realtimesearch.logging.info
import javafx.animation.Interpolator
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.application.Application
import javafx.beans.value.WritableValue
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import javafx.scene.shape.StrokeLineCap
import javafx.scene.transform.Rotate
import javafx.scene.transform.TransformChangedEvent
import javafx.stage.Stage
import javafx.util.Duration
import org.slf4j.LoggerFactory

class AcrobotVisualizer : Application() {
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
        val linkWidth1 = linkScaledLength1 / 7.5
        val linkWidth2 = linkWidth1
        val WIDTH = (linkScaledLength1 + linkScaledLength2) * 2 + stageBorder * 2
        val HEIGHT = WIDTH

        val linkStartX1 = WIDTH / 2.0
        val linkStartY1 = stageBorder + linkScaledLength1 + linkScaledLength2
        val linkStartX2 = linkStartX1
        val linkStartY2 = linkStartY1 + linkScaledLength1

        // Link setup
        val link1 = Line(linkStartX1, linkStartY1, linkStartX1, linkStartY1 + linkScaledLength1)
        val link2 = Line(linkStartX2, linkStartY2, linkStartX2, linkStartY2 + linkScaledLength2)
        link1.strokeWidth = linkWidth1
        link2.strokeWidth = linkWidth2
        link1.strokeLineCap = StrokeLineCap.BUTT
        link2.strokeLineCap = StrokeLineCap.BUTT

        // Joint setup
        val joint1 = Circle(linkStartX1, linkStartY1, linkWidth1 * 0.6, Color.RED)
        val joint2 = Circle(linkStartX2, linkStartY2, joint1.radius, joint1.fill)

        // Rotation setup
        val linkRotate1 = Rotate(0.0, linkStartX1, linkStartY1, 0.0, Rotate.Z_AXIS)
        val linkRotate2 = Rotate(0.0, linkStartX2, linkStartY2, 0.0, Rotate.Z_AXIS)
        link1.transforms.add(linkRotate1)
        link2.transforms.add(linkRotate2)

        /*
         * Keep the moving parts attached to link1 updated as it rotates
         */
        linkRotate1.onTransformChanged = EventHandler<TransformChangedEvent> {
            var angle = Math.atan2(-link1.localToSceneTransform.mxy, link1.localToSceneTransform.mxx) + Math.PI / 2
            angle = if (angle < 0) angle + 2 * Math.PI else if (angle > 2 * Math.PI) angle - 2 * Math.PI else angle

            val newX = link1.startX + linkScaledLength1 * Math.cos(angle)
            val newY = link1.startY + linkScaledLength1 * Math.sin(angle)
            val translateX = newX - joint2.centerX
            val translateY = newY - joint2.centerY

            joint2.translateX = translateX
            joint2.translateY = translateY
            link2.translateX = translateX
            link2.translateY = translateY
        }

        // Add everything to the stage
        val rootPane = Pane()
        rootPane.children.add(link1)
        rootPane.children.add(link2)
        rootPane.children.add(joint1)
        rootPane.children.add(joint2)

        primaryStage.scene = Scene(rootPane, WIDTH, HEIGHT)
        primaryStage.show()

        /* Animate the links */
        // TODO setup links according to initial state values
        val acrobot = DiscretizedAcrobot()
        val environment = DiscretizedAcrobotEnvironment(acrobot) // TODO read optional initial state from input
        val timeline = Timeline(60.0)
        val keyFrames = timeline.keyFrames
        @Suppress("UNCHECKED_CAST")
        keyFrames.add(KeyFrame(Duration.ZERO,
                KeyValue(linkRotate1.angleProperty() as WritableValue<Any>, linkRotate1.angle),
                KeyValue(linkRotate2.angleProperty() as WritableValue<Any>, linkRotate2.angle)))

        var previousState = environment.getState().state
        var time = timeStep
        for (action in actionList) {
            environment.step(action)
            val newState = environment.getState().state

            val diff1 = Math.toDegrees(angleDifference(previousState.linkPosition1, newState.linkPosition1))
            val diff2 = Math.toDegrees(angleDifference(previousState.linkPosition2, newState.linkPosition2)) + diff1

            val newRotate1 = linkRotate1.clone()
            val newRotate2 = linkRotate2.clone()
            newRotate1.onTransformChanged = linkRotate1.onTransformChanged

            link1.transforms.add(newRotate1)
            link2.transforms.add(newRotate2)

            logger.debug { "Adding (${String.format("%.1f", time)}: $diff1, $diff2) to timeline" }
            @Suppress("UNCHECKED_CAST")
            keyFrames.add(KeyFrame(Duration.seconds(time),
                    KeyValue(newRotate1.angleProperty() as WritableValue<Any>, diff1, CustomEaseInInterpolator(previousState.calculateLinkAcceleration1(action))),
                    KeyValue(newRotate2.angleProperty() as WritableValue<Any>, diff2, CustomEaseInInterpolator(previousState.calculateLinkAcceleration2(action)))))

            time += timeStep
            previousState = newState
        }
        timeline.play()
    }
}
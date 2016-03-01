package edu.unh.cs.ai.realtimesearch.visualizer

import edu.unh.cs.ai.realtimesearch.environment.DiscretizedState
import edu.unh.cs.ai.realtimesearch.environment.acrobot.*
import edu.unh.cs.ai.realtimesearch.logging.error
import edu.unh.cs.ai.realtimesearch.logging.info
import edu.unh.cs.ai.realtimesearch.logging.trace
import javafx.animation.*
import javafx.application.Application
import javafx.beans.value.WritableValue
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.input.RotateEvent
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import javafx.scene.shape.Rectangle
import javafx.scene.shape.StrokeLineCap
import javafx.scene.transform.Rotate
import javafx.scene.transform.TransformChangedEvent
import javafx.stage.Stage
import javafx.util.Duration
import org.slf4j.LoggerFactory
import java.util.*

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
//logger.trace { "Line1: $link1" }
//logger.trace { "translate1: (${link1.translateX}, ${link1.translateY})" }
//logger.trace { "Rotate1: $linkRotate1" }

        // Joint setup
        val joint1 = Circle(linkStartX1, linkStartY1, linkWidth1 * 0.6, Color.RED)
        val joint2 = Circle(linkStartX2, linkStartY2, joint1.radius, joint1.fill)

        // Rotation setup
        val linkRotate1 = Rotate()
        val linkRotate2 = Rotate()
        linkRotate1.axis = Rotate.Z_AXIS
        linkRotate2.axis = Rotate.Z_AXIS
        linkRotate1.pivotX = linkStartX1
        linkRotate1.pivotY = linkStartY1
        linkRotate2.pivotX = linkStartX2
        linkRotate2.pivotY = linkStartY2
        link1.transforms.add(linkRotate1)
        link2.transforms.add(linkRotate2)

        /*
         * Keep the moving parts attached to link1 updated to
         */
        linkRotate1.onTransformChanged = EventHandler<TransformChangedEvent> {
        //    logger.trace { "linkRotate1 transform changed: mxx: ${linkRotate1.mxx}, mxy: ${linkRotate1.mxy}, myx: ${linkRotate1.myx}, myy: ${linkRotate1.myy}" }
            var angle = Math.atan2(-linkRotate1.mxy, linkRotate1.mxx) + Math.PI / 2
            angle = if (angle < 0) angle + 2 * Math.PI else if (angle > 2 * Math.PI) angle - 2 * Math.PI else angle
        //    logger.trace { "angle: $angle" }
            val newX = link1.startX + linkScaledLength1 * Math.cos(angle)
            val newY = link1.startY + linkScaledLength1 * Math.sin(angle)
//            joint2.centerX = newX
//            joint2.centerY = newY
            // Translate works best but the CPU and Memory usage grows over time without node caching
            val translateX = newX - joint2.centerX
            val translateY = newY - joint2.centerY
            joint2.translateX = translateX
            joint2.translateY = translateY
            //    logger.trace { "Join2: $joint2" }
            link2.translateX = translateX
            link2.translateY = translateY
//            link2.startX = newX
//            link2.startY = newY
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
        val angleProperty1 = linkRotate1.angleProperty() as WritableValue<Any>
        @Suppress("UNCHECKED_CAST")
        val angleProperty2 = linkRotate2.angleProperty() as WritableValue<Any>
        keyFrames.add(KeyFrame(Duration.ZERO,
                KeyValue(angleProperty1, linkRotate1.angle),
                KeyValue(angleProperty2, linkRotate2.angle)))

        var prevState = environment.getState().state
        var time = timeStep
        for (action in actionList) {
            environment.step(action)
            val newState = environment.getState().state
            val diff1 = Math.toDegrees(angleDifference(prevState.linkPosition1, newState.linkPosition1))
            val diff2 = Math.toDegrees(angleDifference(prevState.linkPosition2, newState.linkPosition2))
//logger.info { "$newState" }
logger.info { "Adding (${String.format("%.1f", time)}: $diff1, $diff2) to timeline" }
            keyFrames.add(KeyFrame(Duration.seconds(time),
                    KeyValue(angleProperty1, diff1, Interpolator.LINEAR),
                    KeyValue(angleProperty2, diff2, Interpolator.LINEAR)))
            time += timeStep
            prevState = newState
        }
        timeline.play()

        timeline.onFinished = EventHandler {
            logger.info { "End link 1: ${linkRotate1.angle}" }
            logger.info { "End link 2: ${linkRotate2.angle}" }
        }

//        val timeline: Timeline = Timeline(
//                KeyFrame(Duration.ZERO, KeyValue(link.angleProperty() as WritableValue<Any>, link.angle)),
//                KeyFrame(Duration.seconds(duration), KeyValue(link.angleProperty() as WritableValue<Any>, diff, Interpolator.EASE_IN)));

//        rotateActions(actionList.iterator(), linkRotate1, linkRotate2, environment.getState().state)
//        rotate(linkRotate1, 2 * Math.PI, 1.0, 5.0)
//        rotate(linkRotate2, Math.toRadians(357.987), -1.0, 5.0)

//        for (action in actionList) {
//logger.info { "Processing action $action" }
//            environment.step(action)
//            val newState = environment.getState().state
//            rotate(linkRotate1, newState.linkPosition1, newState.linkVelocity1, timeStep)
//            rotate(linkRotate2, newState.linkPosition2, newState.linkVelocity2, timeStep)
//        }
//        rotate(linkRotate1, 360.0, -1.0, timeStep)
//        rotate(linkRotate2, 360.0, 1.0, timeStep)

//logger.trace { "Line1: $link1" }
//logger.trace { "translate1: (${link1.translateX}, ${link1.translateY})" }
//logger.trace { "Rotate1: $linkRotate1" }
    }
}
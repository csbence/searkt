package edu.unh.cs.ai.realtimesearch.visualizer

import edu.unh.cs.ai.realtimesearch.environment.acrobot.AcrobotAction
import edu.unh.cs.ai.realtimesearch.environment.acrobot.linkLength1
import edu.unh.cs.ai.realtimesearch.environment.acrobot.linkLength2
import edu.unh.cs.ai.realtimesearch.logging.error
import edu.unh.cs.ai.realtimesearch.logging.info
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.RotateTransition
import javafx.animation.Timeline
import javafx.application.Application
import javafx.beans.value.WritableValue
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.input.RotateEvent
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import javafx.scene.shape.Rectangle
import javafx.scene.transform.Rotate
import javafx.scene.transform.TransformChangedEvent
import javafx.stage.Stage
import javafx.util.Duration
import org.slf4j.LoggerFactory

class AcrobotVisualizer : Application() {
    private val logger = LoggerFactory.getLogger(AcrobotVisualizer::class.java)

    override fun start(primaryStage: Stage) {
        primaryStage.title = "RTS Visualizer"

        /* Get domain from Application */
        val parameters = getParameters()!!
        val raw = parameters.getRaw()!!
//        if (raw.isEmpty()) {
//            logger.error { "Cannot visualize without a domain!" }
//            exitProcess(1)
//        }
//        val rawDomain = raw.first()

        /* Get action list from Application */
        val actionList: MutableList<AcrobotAction> = arrayListOf()
        for (i in 1..raw.size - 1) {
            val action: String = raw.get(i)
            try {
                actionList.add(AcrobotAction.valueOf(action))
            } catch(e: IllegalArgumentException) {
                logger.error { "Invalid action: $action" }
            }
        }
        /* Assuming the domain is correct because the experiment was already run */
//        val inputScanner = Scanner(rawDomain.byteInputStream())
//        val rowCount: Int
//        val columnCount: Int
//        columnCount = inputScanner.nextLine().toInt()
//        rowCount = inputScanner.nextLine().toInt()

        val root = Pane()

        /* Graphical parameters */
        val stageBorder = 100.0
        val linkScale = 100.0 // pixel size per meter
        val linkWidth1 = 10.0
        val linkWidth2 = 10.0
        val linkScaledLength1 = linkLength1 * linkScale
        val linkScaledLength2 = linkLength2 * linkScale
        val WIDTH = (linkScaledLength1 + linkScaledLength2) * 2 + stageBorder * 2
        val HEIGHT = WIDTH

        // Link setup
        val link1 = Rectangle(linkWidth1, linkScaledLength1)
        val link2 = Rectangle(linkWidth2, linkScaledLength2)
        link1.fill = Color.BLUE
        link2.fill = Color.BLUE
        link1.x = WIDTH / 2.0
        link1.y = stageBorder + linkScaledLength1 + linkScaledLength2
        link2.x = link1.x
        link2.y = link1.y + linkScaledLength1

        val linkRotate1 = Rotate()
        val linkRotate2 = Rotate()
        linkRotate1.axis = Rotate.Z_AXIS
        linkRotate2.axis = Rotate.Z_AXIS
//        link1.transforms.add(linkRotate1)
//        link2.transforms.add(linkRotate2)
        linkRotate1.pivotX = link1.x + (linkWidth1 / 2.0)
        linkRotate1.pivotY = link1.y
        linkRotate2.pivotX = link2.x + (linkWidth2 / 2.0)
        linkRotate2.pivotY = link2.y

        val linkLine1 = Line(linkRotate1.pivotX, linkRotate1.pivotY, linkRotate1.pivotX, linkRotate1.pivotY + linkScaledLength1)
        val linkLine2 = Line(linkRotate2.pivotX, linkRotate2.pivotY, linkRotate2.pivotX, linkRotate2.pivotY + linkScaledLength2)
        linkLine1.strokeWidth = linkWidth1
        linkLine2.strokeWidth = linkWidth2
        linkLine1.transforms.add(linkRotate1)
        linkLine2.transforms.add(linkRotate2)
linkLine1.onRotationStarted = EventHandler<RotateEvent> { logger.info { "linkLine1 rotation started" } }
linkLine1.onRotate = EventHandler<RotateEvent> { logger.info { "linkLine1 rotation" } }
linkLine1.onRotationFinished = EventHandler<RotateEvent> { logger.info { "linkLine1 rotation finished" } }
logger.info { "Line1: $linkLine1" }
logger.info { "translate1: (${linkLine1.translateX}, ${linkLine1.translateY})" }
logger.info { "Rotate1: $linkRotate1" }
//        linkRotate1.angle = 90.0
//        linkRotate2.angle = 90.0

        // Joint setup
        val joint1 = Circle(linkLine1.startX, linkLine1.startY, linkWidth1 / 2 + 1, Color.RED)
        val joint2 = Circle(linkLine1.endX, linkLine1.endY, linkWidth2 / 2 + 1, joint1.fill)

linkRotate1.onTransformChanged = EventHandler<TransformChangedEvent> {
    logger.info { "linkRotate1 transform changed: mxx: ${linkRotate1.mxx}, mxy: ${linkRotate1.mxy}, myx: ${linkRotate1.myx}, myy: ${linkRotate1.myy}" }
//    logger.info { "angle: ${Math.atan2(-linkRotate1.myx, linkRotate1.mxx)}" }
//    logger.info { "angle: ${Math.atan2(-linkRotate1.mxy, linkRotate1.mxx)}" }
//    logger.info { "angle: ${Math.atan2(linkRotate1.myx, linkRotate1.myy)}" }
    // start (400,305)
//    val newX = joint2.centerX * linkRotate1.mxx + joint2.centerY * linkRotate1.mxy
//    val newY = joint2.centerX * linkRotate1.myx + joint2.centerY * linkRotate1.myy
    var angle = Math.atan2(-linkRotate1.mxy, linkRotate1.mxx) + Math.PI / 2
    angle = if (angle < 0) angle + 2 * Math.PI else if (angle > 2 * Math.PI) angle - 2 * Math.PI else angle
    logger.info { "angle: $angle" }
    val newX = linkScaledLength1 * Math.cos(angle)
    val newY = linkScaledLength1 * Math.sin(angle)
    joint2.centerX = newX + linkLine1.startX
    joint2.centerY = newY + linkLine1.startY
    logger.info { "Join2: $joint2" }
//    val newX2 = linkScaledLength1 * linkRotate1.mxx
//    val newY2 = linkScaledLength1 * linkRotate1.myx
//    joint2.centerX = newX2
//    joint2.centerY = newY2
//    logger.info { "Join2: $joint2" }
}

        root.children.add(link1)
        root.children.add(link2)
        root.children.add(linkLine1)
//        root.children.add(linkLine2)
//        root.children.add(link1_2)
//        root.children.add(link2_2)
        root.children.add(joint1)
        root.children.add(joint2)

        primaryStage.scene = Scene(root, WIDTH, HEIGHT)
        primaryStage.show()

        /* Animate the links */
        /*
        RotateTransition does not allow rotating around a pivot
         */
//        val rotateTransition = RotateTransition()
//        rotateTransition.duration = Duration.millis(1000.0)
//        rotateTransition.node = link1
//        rotateTransition.byAngle = 90.0
//        rotateTransition.play()
logger.info { "Join2: $joint2" }
        val timeline: Timeline = Timeline(
                KeyFrame(Duration.ZERO, KeyValue(linkRotate1.angleProperty() as WritableValue<Any>, linkRotate1.angle)),
                KeyFrame(Duration.seconds(10.0), KeyValue(linkRotate1.angleProperty() as WritableValue<Any>, linkRotate1.angle - 360.0)));
        timeline.play()

logger.info { "Line1: $linkLine1" }
logger.info { "translate1: (${linkLine1.translateX}, ${linkLine1.translateY})" }
logger.info { "Rotate1: $linkRotate1" }
    }
}
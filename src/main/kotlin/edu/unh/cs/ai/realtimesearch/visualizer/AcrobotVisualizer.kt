package edu.unh.cs.ai.realtimesearch.visualizer

import edu.unh.cs.ai.realtimesearch.environment.acrobot.AcrobotAction
import edu.unh.cs.ai.realtimesearch.environment.acrobot.linkLength1
import edu.unh.cs.ai.realtimesearch.environment.acrobot.linkLength2
import edu.unh.cs.ai.realtimesearch.logging.error
import edu.unh.cs.ai.realtimesearch.logging.trace
import javafx.animation.*
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
import javafx.scene.shape.StrokeLineCap
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
        val link1 = Rectangle(linkWidth1, linkScaledLength1)
        val link2 = Rectangle(linkWidth2, linkScaledLength2)
        link1.fill = Color.BLUE
        link2.fill = Color.BLUE
        link1.x = linkStartX1
        link1.y = linkStartY1
        link2.x = linkStartX2
        link2.y = linkStartY2

        val linkRotate1 = Rotate()
        val linkRotate2 = Rotate()
        linkRotate1.axis = Rotate.Z_AXIS
        linkRotate2.axis = Rotate.Z_AXIS
//        link1.transforms.add(linkRotate1)
//        link2.transforms.add(linkRotate2)
        linkRotate1.pivotX = linkStartX1
        linkRotate1.pivotY = linkStartY1
        linkRotate2.pivotX = linkStartX2
        linkRotate2.pivotY = linkStartY2

        val linkLine1 = Line(linkStartX1, linkStartY1, linkStartX1, linkStartY1 + linkScaledLength1)
        val linkLine2 = Line(linkStartX2, linkStartY2, linkStartX2, linkStartY2 + linkScaledLength2)
        linkLine1.strokeWidth = linkWidth1
        linkLine2.strokeWidth = linkWidth2
        linkLine1.strokeLineCap = StrokeLineCap.BUTT
        linkLine2.strokeLineCap = StrokeLineCap.BUTT
        linkLine1.transforms.add(linkRotate1)
        linkLine2.transforms.add(linkRotate2)
logger.trace { "Line1: $linkLine1" }
logger.trace { "translate1: (${linkLine1.translateX}, ${linkLine1.translateY})" }
logger.trace { "Rotate1: $linkRotate1" }
//        linkRotate1.angle = 90.0
//        linkRotate2.angle = 90.0

        // Joint setup
        val joint1 = Circle(linkLine1.startX, linkLine1.startY, linkWidth1 * 0.6, Color.RED)
        val joint2 = Circle(linkLine1.endX, linkLine1.endY, joint1.radius, joint1.fill)

        linkRotate1.onTransformChanged = EventHandler<TransformChangedEvent> {
        //    logger.trace { "linkRotate1 transform changed: mxx: ${linkRotate1.mxx}, mxy: ${linkRotate1.mxy}, myx: ${linkRotate1.myx}, myy: ${linkRotate1.myy}" }
            var angle = Math.atan2(-linkRotate1.mxy, linkRotate1.mxx) + Math.PI / 2
            angle = if (angle < 0) angle + 2 * Math.PI else if (angle > 2 * Math.PI) angle - 2 * Math.PI else angle
        //    logger.trace { "angle: $angle" }
            val newX = linkLine1.startX + linkScaledLength1 * Math.cos(angle)
            val newY = linkLine1.startY + linkScaledLength1 * Math.sin(angle)
//            joint2.centerX = newX
//            joint2.centerY = newY
            // Translate works best but the CPU and Memory performance grows over time without node caching
            val translateX = newX - joint2.centerX
            val translateY = newY - joint2.centerY
            joint2.translateX = translateX
            joint2.translateY = translateY
            //    logger.trace { "Join2: $joint2" }
            linkLine2.translateX = translateX
            linkLine2.translateY = translateY
//            linkLine2.startX = newX
//            linkLine2.startY = newY
        }

        root.children.add(link1)
        root.children.add(link2)
        root.children.add(linkLine1)
        root.children.add(linkLine2)
        root.children.add(joint1)
        root.children.add(joint2)

        primaryStage.scene = Scene(root, WIDTH, HEIGHT)
        primaryStage.show()

        /* Animate the links */
        val timeline: Timeline = Timeline(
                KeyFrame(Duration.ZERO, KeyValue(linkRotate1.angleProperty() as WritableValue<Any>, linkRotate1.angle)),
                KeyFrame(Duration.seconds(1.0), KeyValue(linkRotate1.angleProperty() as WritableValue<Any>, linkRotate1.angle - 360.0)));
//        timeline.cycleCount = Animation.INDEFINITE
        timeline.play()

logger.trace { "Line1: $linkLine1" }
logger.trace { "translate1: (${linkLine1.translateX}, ${linkLine1.translateY})" }
logger.trace { "Rotate1: $linkRotate1" }
    }
}
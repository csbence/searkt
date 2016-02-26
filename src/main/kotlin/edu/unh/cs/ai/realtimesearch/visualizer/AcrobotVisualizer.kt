package edu.unh.cs.ai.realtimesearch.visualizer

import javafx.animation.PathTransition
import javafx.animation.Timeline
import javafx.application.Application
import javafx.animation.Interpolator
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.input.MouseEvent
import javafx.stage.Stage
import javafx.scene.paint.Color
import java.util.*
import javafx.scene.layout.Pane
import javafx.scene.shape.*
import javafx.util.Duration
import kotlin.system.exitProcess

import edu.unh.cs.ai.realtimesearch.environment.acrobot.*
import edu.unh.cs.ai.realtimesearch.logging.debug
import edu.unh.cs.ai.realtimesearch.logging.error
import edu.unh.cs.ai.realtimesearch.logging.info
import javafx.scene.transform.Rotate
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
        val TIME_TO_RUN = actionList.size * 200.0
        //val TIME_TO_RUN = 2000.0

        /* Assuming the domain is correct because the experiment was already run */
//        val inputScanner = Scanner(rawDomain.byteInputStream())
//
//        val rowCount: Int
//        val columnCount: Int
//
//        columnCount = inputScanner.nextLine().toInt()
//        rowCount = inputScanner.nextLine().toInt()

        val root = Pane()

        /* Graphical parameters */
        val stageBorder = 100.0
        val linkScale = 100.0 // pixel size per meter
        val linkScaledLength1 = linkLength1 * linkScale
        val linkScaledLength2 = linkLength2 * linkScale
        val WIDTH = (linkScaledLength1 + linkScaledLength2) * 2 + stageBorder * 2
        val HEIGHT = WIDTH


        // Link setup
        val link1 = Rectangle(linkScaledLength1, 10.0)
        val link2 = Rectangle(linkScaledLength2, 10.0)
        link1.fill = Color.BLUE
        link2.fill = Color.BLUE
        link1.x = WIDTH / 2.0
        link1.y = stageBorder + linkScaledLength1 + linkScaledLength2
        link2.x = link1.x
        link2.y = link1.y + linkScaledLength1
        val linkRotate1 = Rotate()
        linkRotate1.axis = Rotate.Z_AXIS
        val linkRotate2 = Rotate()
        linkRotate2.axis = Rotate.Z_AXIS
        link1.transforms.add(linkRotate1)
        link2.transforms.add(linkRotate2)
        linkRotate1.pivotX = link1.x
        linkRotate1.pivotY = link1.y + (link1.height / 2.0)
        linkRotate2.pivotX = link2.x
        linkRotate2.pivotY = link2.y + (link2.height / 2.0)
        linkRotate1.angle = 90.0
        linkRotate2.angle = 90.0

//        link2.transforms.add(Rotate(90.0, 0.0, 0.0))
        val link1_2 = Rectangle(linkScaledLength1, 10.0)
        val link2_2 = Rectangle(linkScaledLength2, 10.0)
        link1_2.fill = Color.RED
        link2_2.fill = Color.RED
        link1_2.x = link1.x
        link1_2.y = link1.y
        link2_2.x = link2.x
        link2_2.y = link2.y

        // Joint setup
        val joint1 = Circle(linkRotate1.pivotX, linkRotate1.pivotY, link1.height / 2 + 1, Color.BLACK)
        val joint2 = Circle(linkRotate2.pivotX, linkRotate2.pivotY, link2.height / 2 + 1, joint1.fill)

        root.children.add(link1)
        root.children.add(link2)
//        root.children.add(link1_2)
//        root.children.add(link2_2)
        root.children.add(joint1)
        root.children.add(joint2)

        primaryStage.scene = Scene(root, WIDTH, HEIGHT)
        primaryStage.show()

        for (action in actionList) {
            animate(action, link1, link2)
        }

        /* Animate the robot */
        val pathTransition = PathTransition()
        pathTransition.setDuration(Duration.millis(TIME_TO_RUN))
//        pathTransition.setPath(path)
        pathTransition.setNode(link1)
        pathTransition.setInterpolator(Interpolator.LINEAR);
        pathTransition.setCycleCount(Timeline.INDEFINITE);
        pathTransition.play()
    }

    private fun animate(action: AcrobotAction, link1: Rectangle, link2: Rectangle) {
        when (action) {
            AcrobotAction.NEGATIVE -> {
                link1.translateY = link1.translateY
            }
            AcrobotAction.POSITIVE -> {
                link1.translateX = link1.translateX
            }
            AcrobotAction.NONE -> {
                link1.translateY = link1.translateY
            }
        }
    }
}
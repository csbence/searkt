package edu.unh.cs.ai.realtimesearch.visualizer

import edu.unh.cs.ai.realtimesearch.environment.acrobot.*
import edu.unh.cs.ai.realtimesearch.logging.debug
import edu.unh.cs.ai.realtimesearch.logging.error
import edu.unh.cs.ai.realtimesearch.logging.info
import javafx.animation.*
import javafx.application.Application
import javafx.beans.value.WritableValue
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
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
        var ghost: Boolean = false
        var start = 0

        if (raw[0].equals("-g")) {
            ghost = true
            start += 1
        }

        /* Get action list from Application */
        val actionList: MutableList<AcrobotAction> = mutableListOf()
        for (i in start..raw.lastIndex) {
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

        val stateList = getStateList(actionList)
        assert(stateList.size > 1, {"Must have at least two states to animate"})

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

        val acrobotView = AcrobotView(linkStartX1, linkStartY1, linkScaledLength1, linkWidth)

        // Add everything to the stage
        val rootPane = Pane()
        rootPane.children.addAll(acrobotView.getNodes())

        primaryStage.scene = Scene(rootPane, WIDTH, HEIGHT)
        primaryStage.show()

        animateAcrobot(acrobotView, stateList)
        
        // Animate a ghost acrobot if desired
        if (ghost) {
            val ghostAcrobot = AcrobotView(linkStartX1, linkStartY1, linkScaledLength1, linkWidth)
            ghostAcrobot.opacity = 0.5
            ghostAcrobot.linkColor = Color.GRAY

            rootPane.children.addAll(ghostAcrobot.getNodes())
            ghostAcrobot.toBack()

            val ghostTransition = animateAcrobot(ghostAcrobot, stateList.subList(1, stateList.size), Interpolator.DISCRETE)
            ghostTransition.onFinished = EventHandler {
                ghostAcrobot.isVisible = false
            }
        }
    }

    private fun getStateList(actionList: List<AcrobotAction>): List<AcrobotState> {
        val stateList = mutableListOf<AcrobotState>()
        // TODO setup links according to initial state values
        val acrobot = DiscretizedAcrobot()
        val environment = DiscretizedAcrobotEnvironment(acrobot) // TODO read optional initial state from input
        for (action in actionList) {
            environment.step(action)
            stateList.add(environment.getState().state)
        }
        return stateList
    }

    protected open fun animateAcrobot(acrobotView: AcrobotView, stateList: List<AcrobotState>,
                                      interpolator1: Interpolator = Interpolator.EASE_IN,
                                      interpolator2: Interpolator = interpolator1): Animation {
        /* Animate the links */
        val sequentialTransition = SequentialTransition()

        if (stateList.size < 1)
            throw IllegalArgumentException("State list must have at least one state for animation")

        val iterator = stateList.iterator()
        var previousState = iterator.next()
        val time = timeStep
        while (iterator.hasNext()) {
            val newState = iterator.next()
            val diff1 = Math.toDegrees(angleDifference(previousState.linkPosition1, newState.linkPosition1))
            val diff2 = Math.toDegrees(angleDifference(previousState.linkPosition2, newState.linkPosition2)) + diff1

            val newRotate1 = acrobotView.addRotate1()
            val newRotate2 = acrobotView.addRotate2()

            logger.debug { "Adding (${String.format("%.1f", time)}: $diff1, $diff2) to timeline" }
            @Suppress("UNCHECKED_CAST")
            sequentialTransition.children.add(Timeline(60.0, KeyFrame(Duration.seconds(time),
                    KeyValue(newRotate1.angleProperty() as WritableValue<Any>, -diff1, interpolator1),
                    KeyValue(newRotate2.angleProperty() as WritableValue<Any>, -diff2, interpolator2))))

            previousState = newState
        }
        sequentialTransition.play()
        return sequentialTransition
    }
}
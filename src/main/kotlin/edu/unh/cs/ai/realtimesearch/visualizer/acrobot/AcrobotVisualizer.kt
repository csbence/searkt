package edu.unh.cs.ai.realtimesearch.visualizer.acrobot

import edu.unh.cs.ai.realtimesearch.environment.acrobot.Acrobot
import edu.unh.cs.ai.realtimesearch.environment.acrobot.AcrobotAction
import edu.unh.cs.ai.realtimesearch.environment.acrobot.AcrobotState
import edu.unh.cs.ai.realtimesearch.environment.acrobot.configuration.AcrobotConfiguration
import edu.unh.cs.ai.realtimesearch.environment.acrobot.configuration.AcrobotStateConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.logging.debug
import edu.unh.cs.ai.realtimesearch.util.angleDifference
import edu.unh.cs.ai.realtimesearch.util.convertNanoUpDouble
import edu.unh.cs.ai.realtimesearch.visualizer.BaseVisualizer
import edu.unh.cs.ai.realtimesearch.visualizer.InvalidResultException
import edu.unh.cs.ai.realtimesearch.visualizer.ThemeColors
import groovyjarjarcommonscli.CommandLine
import groovyjarjarcommonscli.Option
import groovyjarjarcommonscli.Options
import javafx.animation.*
import javafx.beans.value.WritableValue
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.stage.Stage
import javafx.util.Duration
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * Visualizer for the Acrobot domain.  Given a set of results, produces and runs an animation of the Acrobot domain
 * execution.
 *
 * @author Mike Bogochow (mgp36@unh.edu)
 */
open class AcrobotVisualizer : BaseVisualizer() {
    private val logger = LoggerFactory.getLogger(AcrobotVisualizer::class.java)

    private var acrobotConfiguration: AcrobotConfiguration = AcrobotConfiguration()
    private var ghost: Boolean = false
    private val actionList: MutableList<AcrobotAction> = mutableListOf()
    private var actionDuration: Long = AcrobotStateConfiguration.defaultActionDuration

    private val ghostOption = Option("g", "ghost", false, "Display ghost animation")

    override fun getOptions(): Options {
        val options = Options()
        options.addOption(ghostOption)
        return options
    }

    override fun processOptions(cmd: CommandLine) {
        ghost = cmd.hasOption(ghostOption.opt)
    }

    override fun start(primaryStage: Stage) {
        primaryStage.title = "Acrobot Visualizer"

        processCommandLine(parameters.raw.toTypedArray())

        // Parse results
        acrobotConfiguration = AcrobotConfiguration.fromJson(
                experimentResult.experimentConfiguration[Configurations.RAW_DOMAIN.toString()] as String)
        actionDuration = (experimentResult.experimentConfiguration[Configurations.ACTION_DURATION.toString()] as Long)

        for (action in experimentResult.actions) {
            actionList.add(AcrobotAction.valueOf(action))
        }

        val errorMessage = StringBuilder()
        if (experimentResult.errorMessage != null) {
            errorMessage.appendln(experimentResult.errorMessage)
        }

        val stateList = getStateList(actionList, acrobotConfiguration, actionDuration)

        if (stateList.size <= 0)
            throw InvalidResultException("Must have at least one state to animate")

        val endBounds = Acrobot.getBoundStates(acrobotConfiguration.goalState, acrobotConfiguration)
        if (!stateList.last().state.inBounds(endBounds.lowerBound, endBounds.upperBound)) {
            errorMessage.appendln("Last state not in goal bounds!!!!!")
            assert(!Acrobot(AcrobotConfiguration()).isGoal(stateList.last().state),
                    { "Found last state not in goal but Acrobot environment isGoal true" })
        }

        /* Graphical parameters */
        val stageBorder = 100.0
        val linkScale = 175.0 // pixel size per meter
        val linkScaledLength1 = AcrobotState.linkLength1 * linkScale
        val linkScaledLength2 = AcrobotState.linkLength2 * linkScale
        val linkWidth = linkScaledLength1 / 8.0
        val WIDTH = (linkScaledLength1 + linkScaledLength2) * 2 + stageBorder * 2
        val HEIGHT = WIDTH

        val linkStartX1 = WIDTH / 2.0
        val linkStartY1 = stageBorder + linkScaledLength1 + linkScaledLength2

        val acrobotView = AcrobotView(linkStartX1, linkStartY1, linkScaledLength1, linkWidth)

        // Add everything to the stage
        val rootBox = VBox()
        val headerBox = VBox()
        val animationPane = Pane()
        animationPane.children.addAll(acrobotView.getNodes())

        // Make the boxes transparent so we can easily set the background color of the scene
        rootBox.style = "-fx-background-color: rgba(0, 0, 0, 0);"
        headerBox.style = "-fx-background-color: rgba(0, 0, 0, 0);"

        if (errorMessage.isNotEmpty()) {
            val errorLabel = Label(errorMessage.toString())
            errorLabel.textFill = ThemeColors.ERROR_TEXT.color
            errorLabel.font = Font.font("Verdana", FontWeight.BOLD, 18.0)
            headerBox.children.add(errorLabel)
        }

        val info = StringBuilder()
        info.append("Algorithm: ").appendln(experimentResult.experimentConfiguration["algorithmName"])
        info.append("Instance: ").appendln(experimentResult.experimentConfiguration["domainInstanceName"])
        info.append("Path Length: ").appendln(experimentResult.pathLength)
        info.append("Action Duration: ").append(experimentResult.experimentConfiguration["actionDuration"])
        info.appendln(" ns")
        info.append("Action Execution Time: ").append(experimentResult.actionExecutionTime).appendln(" ns")
        val infoLabel = Label(info.toString())
        headerBox.children.add(infoLabel)

        rootBox.children.add(headerBox)
        rootBox.children.add(animationPane)
        primaryStage.scene = Scene(rootBox, WIDTH, HEIGHT, ThemeColors.BACKGROUND.color)
        primaryStage.show()

        // Create the animations
        val animations = mutableListOf<Animation>()
        animations.add(animateAcrobot(acrobotView, stateList))

        // Animate a ghost acrobot if desired
        if (ghost) {
            val ghostAcrobot = AcrobotView(linkStartX1, linkStartY1, linkScaledLength1, linkWidth)
            ghostAcrobot.opacity = ThemeColors.LINK.opacity
            ghostAcrobot.linkColor = ThemeColors.LINK.color

            animationPane.children.addAll(ghostAcrobot.getNodes())
            ghostAcrobot.toBack()

            val ghostTransition = animateAcrobot(ghostAcrobot, stateList.subList(1, stateList.size),
                    Interpolator.DISCRETE)
            ghostTransition.onFinished = EventHandler {
                ghostAcrobot.isVisible = false
            }
            animations.add(ghostTransition)
        }


        Thread({
            val delayTime = convertNanoUpDouble(experimentResult.idlePlanningTime, TimeUnit.MILLISECONDS)
            println("Idle planning time: $delayTime ms")
            Thread.sleep(delayTime.toLong())
            // Play the animations
            for (animation in animations)
                animation.play()
        }).start()


    }

    /**
     * Get the appropriate Interpolator given the velocity of the previous state and the velocity of the next state.
     * <ul>
     *   <li>If the velocity is increasing between states, the interpolator should ease-in.</li>
     *   <li>If the velocity is decreasing between states, the interpolator should ease-out.</li>
     *   <li>If the velocity is not changing between states, the interpolator should be linear.</li>
     * <ul>
     */
    private fun getLinkInterpolator(previousVelocity: Double, nextVelocity: Double): Interpolator {
        if (previousVelocity < nextVelocity)
            return Interpolator.EASE_IN
        else if (previousVelocity > nextVelocity)
            return Interpolator.EASE_OUT
        else // equal
            return Interpolator.LINEAR
    }

    /**
     * Retrieve a list of StateInfo given a list of actions to apply to the Acrobot domain.
     *
     * @param actionList the action list to translate to states
     * @param acrobotConfiguration the configuration used to generate the action list; default is default configuration
     * @return the list of states derived from the action list
     */
    private fun getStateList(actionList: List<AcrobotAction>,
                             acrobotConfiguration: AcrobotConfiguration = AcrobotConfiguration(),
                             actionDuration: Long): List<StateInfo> {
        val stateList = mutableListOf<StateInfo>()
        val acrobotDomain = Acrobot(acrobotConfiguration, actionDuration)

        var currentState = acrobotConfiguration.initialState

        for (action in actionList) {
            val newState = acrobotDomain.transition(currentState, action) ?:  throw RuntimeException("Invalid acrobot transition")

            // Assign interpolator for each link
            val linkInterpolation1: Interpolator =
                    getLinkInterpolator(currentState.link1.velocity, newState.link1.velocity)
            val linkInterpolation2: Interpolator =
                    getLinkInterpolator(currentState.link2.velocity, newState.link2.velocity)

            // Add the state info to list
            stateList.add(StateInfo(currentState, newState, action, linkInterpolation1, linkInterpolation2))
            currentState = newState
        }
        return stateList
    }

    /**
     * Form an animation for an Acrobot path.
     *
     * @param acrobotView the acrobot view to animate
     * @param stateList the states to transition to at each step of the animation
     * @param interpolator1 optional interpolator for override of link1 transitions
     * @param interpolator2 optional interpolator for override of link2 transitions
     * @return an animation of an acrobot given the list of states for transition
     */
    protected open fun animateAcrobot(acrobotView: AcrobotView, stateList: List<StateInfo>,
                                      interpolator1: Interpolator? = null,
                                      interpolator2: Interpolator? = null): Animation {
        /*
        Implementation note: We form a sequential transition of timelines with one keyframe each.  If we try to make it
        a single timeline then the interpolation will screw up the animation.  Also need to have separate Rotate
        objects, one for each keyframe, since manipulating a single Rotate object repeatedly causes unexpected values.
         */
        val sequentialTransition = SequentialTransition()

        if (stateList.size < 1)
            throw IllegalArgumentException("State list must have at least one state for animation")

        val time = convertNanoUpDouble(actionDuration, TimeUnit.SECONDS)
        for (state in stateList) {
            val diff1 = Math.toDegrees(angleDifference(state.previousState.link1.position, state.state.link1.position))
            val diff2 = Math.toDegrees(
                    angleDifference(state.previousState.link2.position, state.state.link2.position)) + diff1

            val newRotate1 = acrobotView.addRotate1()
            val newRotate2 = acrobotView.addRotate2()

            logger.debug { "Adding (${String.format("%.3f", time)}: $diff1, $diff2) to timeline" }
            @Suppress("UNCHECKED_CAST")
            sequentialTransition.children.add(Timeline(KeyFrame(Duration.seconds(time),
                    KeyValue(newRotate1.angleProperty() as WritableValue<Any>,
                            -diff1, interpolator1 ?: state.interpolator1),
                    KeyValue(newRotate2.angleProperty() as WritableValue<Any>,
                            -diff2, interpolator2 ?: state.interpolator2))))
        }

        return sequentialTransition
    }
}

/**
 * Info about an AcrobotState.  Contains the previous state, the current state, the action that produced the state, and
 * the interpolator to be used for animation.
 */
data class StateInfo(val previousState: AcrobotState,
                     val state: AcrobotState,
                     val action: AcrobotAction,
                     val interpolator1: Interpolator = Interpolator.EASE_IN,
                     val interpolator2: Interpolator = interpolator1)
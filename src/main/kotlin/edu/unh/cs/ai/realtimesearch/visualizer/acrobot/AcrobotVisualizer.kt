package edu.unh.cs.ai.realtimesearch.visualizer.acrobot

import edu.unh.cs.ai.realtimesearch.environment.DiscretizedDomain
import edu.unh.cs.ai.realtimesearch.environment.DiscretizedEnvironment
import edu.unh.cs.ai.realtimesearch.environment.DiscretizedState
import edu.unh.cs.ai.realtimesearch.environment.acrobot.Acrobot
import edu.unh.cs.ai.realtimesearch.environment.acrobot.AcrobotAction
import edu.unh.cs.ai.realtimesearch.environment.acrobot.AcrobotState
import edu.unh.cs.ai.realtimesearch.environment.acrobot.configuration.AcrobotConfiguration
import edu.unh.cs.ai.realtimesearch.logging.debug
import edu.unh.cs.ai.realtimesearch.util.angleDifference
import edu.unh.cs.ai.realtimesearch.util.convertTime
import edu.unh.cs.ai.realtimesearch.visualizer.BaseVisualizer
import groovyjarjarcommonscli.CommandLine
import groovyjarjarcommonscli.Option
import groovyjarjarcommonscli.Options
import javafx.animation.*
import javafx.beans.value.WritableValue
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.util.Duration
import org.slf4j.LoggerFactory

/**
 * Visualizer for the Acrobot domain.  Given a set of results, produces and runs an animation of the Acrobot domain
 * execution.
 */
open class AcrobotVisualizer : BaseVisualizer() {
    private val logger = LoggerFactory.getLogger(AcrobotVisualizer::class.java)

    private var acrobotConfiguration: AcrobotConfiguration = AcrobotConfiguration()
    private var ghost: Boolean = false
    private val actionList: MutableList<AcrobotAction> = mutableListOf()

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

        acrobotConfiguration = AcrobotConfiguration.fromJson(experimentResult!!.experimentConfiguration["rawDomain"] as String)

        for (action in experimentResult!!.actions) {
            actionList.add(AcrobotAction.valueOf(action))
        }

        val stateList = getStateList(actionList, acrobotConfiguration)
        assert(stateList.size > 0, {"Must have at least one state to animate"})

        /* Graphical parameters */
        val stageBorder = 100.0
        val linkScale = 175.0 // pixel size per meter
        val linkScaledLength1 = AcrobotState.linkLength1 * linkScale
        val linkScaledLength2 = AcrobotState.linkLength2 * linkScale
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

        // Create the animations
        val animations = mutableListOf<Animation>()
        animations.add(animateAcrobot(acrobotView, stateList))

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
            animations.add(ghostTransition)
        }

        // Play the animations
        for(animation in animations)
            animation.play()
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
            return  Interpolator.LINEAR
    }

    /**
     * Retrieve a list of StateInfo given a list of actions to apply to the Acrobot domain.
     *
     * @param actionList the action list to translate to states
     * @param acrobotConfiguration the configuration used to generate the action list; default is default configuration
     * @return the list of states derived from the action list
     */
    private fun getStateList(actionList: List<AcrobotAction>,
                             acrobotConfiguration: AcrobotConfiguration = AcrobotConfiguration()): List<StateInfo> {
        val stateList = mutableListOf<StateInfo>()
        val domain = DiscretizedDomain(Acrobot(acrobotConfiguration))
        val environment = DiscretizedEnvironment(domain, DiscretizedState(acrobotConfiguration.initialState))
        var currentState = environment.getState()
        for (action in actionList) {
            environment.step(action)
            val newState = environment.getState()

            // Assign interpolator for each link
            val linkInterpolation1: Interpolator = getLinkInterpolator(currentState.state.link1.velocity, newState.state.link1.velocity)
            val linkInterpolation2: Interpolator = getLinkInterpolator(currentState.state.link2.velocity, newState.state.link2.velocity)

            // Add the state info to list
            stateList.add(StateInfo(currentState.state, newState.state, action,
                    linkInterpolation1, linkInterpolation2))

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
        objects, one for each keyframe, since manipulating one a single Rotate object repeatedly causes unexpected
        values.
         */
        val sequentialTransition = SequentialTransition()

        if (stateList.size < 1)
            throw IllegalArgumentException("State list must have at least one state for animation")

        val time = acrobotConfiguration.stateConfiguration.timeStep
        for (state in stateList) {
            val diff1 = Math.toDegrees(angleDifference(state.previousState.link1.position, state.state.link1.position))
            val diff2 = Math.toDegrees(angleDifference(state.previousState.link2.position, state.state.link2.position)) + diff1

            val newRotate1 = acrobotView.addRotate1()
            val newRotate2 = acrobotView.addRotate2()

            logger.debug { "Adding (${String.format("%.1f", time)}: $diff1, $diff2) to timeline" }
            @Suppress("UNCHECKED_CAST")
            sequentialTransition.children.add(Timeline(60.0, KeyFrame(Duration.seconds(convertTime(time)),
                    KeyValue(newRotate1.angleProperty() as WritableValue<Any>, -diff1, interpolator1 ?: state.interpolator1),
                    KeyValue(newRotate2.angleProperty() as WritableValue<Any>, -diff2, interpolator2 ?: state.interpolator2))))
        }

        return sequentialTransition
    }
}

/**
 * Info about an AcrobotState.  Contains the previous state, the current state, the action that produced the state, and
 * the interpolator to be used for animation.
 */
data class StateInfo(val previousState: AcrobotState, val state: AcrobotState, val action: AcrobotAction, val interpolator1: Interpolator = Interpolator.EASE_IN, val interpolator2: Interpolator = interpolator1)
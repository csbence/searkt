package edu.unh.cs.ai.realtimesearch.visualizer

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.DiscretizedDomain
import edu.unh.cs.ai.realtimesearch.environment.DiscretizedState
import edu.unh.cs.ai.realtimesearch.environment.acrobot.*
import edu.unh.cs.ai.realtimesearch.environment.acrobot.configuration.AcrobotConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ConfigurationExecutor
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import edu.unh.cs.ai.realtimesearch.logging.debug
import groovyjarjarcommonscli.GnuParser
import groovyjarjarcommonscli.HelpFormatter
import groovyjarjarcommonscli.Option
import groovyjarjarcommonscli.Options
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
import java.io.File
import java.io.FileInputStream
import kotlin.system.exitProcess

open class AcrobotVisualizer : Application() {
    private val logger = LoggerFactory.getLogger(AcrobotVisualizer::class.java)

    private var acrobotConfiguration: AcrobotConfiguration = AcrobotConfiguration()
    private var ghost: Boolean = false
    private val actionList: MutableList<AcrobotAction> = mutableListOf()
    private var experimentResult: ExperimentResult? = null

    private fun actionListFromResult() {
        assert(experimentResult != null)
        assert(actionList.isEmpty())
        for (action in experimentResult!!.actions) {
            actionList.add(action as AcrobotAction)
        }
    }

    private fun copyActionList(list: List<AcrobotAction>) {
        assert(actionList.isEmpty())
        for (action in list) {
            actionList.add(action)
        }
    }

    private fun processCommandLine(args: Array<String>) {
        val options = Options()

        val helpOption = Option("h", "help", false, "Print help and exit")
        val ghostOption = Option("g", "ghost", false, "Display ghost animation")
        val pathOption = Option("r", "result", true, "Read result from file")
        val configurationFileOption = Option("c", "config", true, "Read Acrobot configuration from file")
        val configurationOption = Option("C", "Config", true, "Read Acrobot configuration from string; overwrites -${configurationFileOption.opt}")
        val algorithmOption = Option("a", "alg-name", true, "The algorithm name")
        val terminationTypeOption = Option("t", "term-type", true, "The termination type")
        val terminationParameterOption = Option("p", "term-param", true, "The termination parameter")

        options.addOption(helpOption)
        options.addOption(ghostOption)
        options.addOption(pathOption)
        options.addOption(configurationFileOption) // TODO OptionGroup
        options.addOption(configurationOption)
        options.addOption(algorithmOption)
        options.addOption(terminationTypeOption)
        options.addOption(terminationParameterOption)

        /* parse command line arguments */
        val parser = GnuParser()
        val cmd = parser.parse(options, args)

        /* print help if help option was specified*/
        val formatter = HelpFormatter()
        if (cmd.hasOption("h")) {
            formatter.printHelp("real-time-search", options)
            exitProcess(1)
        }

        ghost = cmd.hasOption(ghostOption.opt)
        val resultFile = cmd.getOptionValue(pathOption.opt)

        // Get configuration options
        if (cmd.hasOption(configurationOption.opt)) {
            val configurationJSON = cmd.getOptionValue(configurationOption.opt)
            acrobotConfiguration = AcrobotConfiguration.fromString(configurationJSON)
        } else {
            val configurationPath = cmd.getOptionValue(configurationFileOption.opt)
            if (configurationPath != null)
                acrobotConfiguration = AcrobotConfiguration.fromStream(FileInputStream(configurationPath))
        }

        if (resultFile != null) {
            // Need to read twice so can't use a stream
            val text = File(resultFile).readText()
            experimentResult = ExperimentResult.fromString(text)
            copyActionList(AcrobotAction.fromResultString(text))
        } else {
            // Run the specified algorithm to retrieve path
            val algorithm = cmd.getOptionValue(algorithmOption.opt)
            val terminationType = cmd.getOptionValue(terminationTypeOption.opt)
            val terminationParameters = cmd.getOptionValue(terminationParameterOption.opt)
            if (algorithm == null || terminationType == null || terminationParameters == null)
                throw IllegalArgumentException("Too few options provided to run algorithm")

            val manualConfiguration = GeneralExperimentConfiguration("acrobot", acrobotConfiguration.toString(), algorithm,
                    terminationType, terminationParameters.toInt())
            experimentResult = ConfigurationExecutor.executeConfiguration(manualConfiguration)
            actionListFromResult()
        }
    }

    override fun start(primaryStage: Stage) {
        primaryStage.title = "Acrobot Visualizer"

        processCommandLine(parameters.raw.toTypedArray())

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

    private fun getStateList(actionList: List<Action>, acrobotConfiguration: AcrobotConfiguration = AcrobotConfiguration()): List<AcrobotState> {
        val stateList = mutableListOf<AcrobotState>()
        val domain = DiscretizedDomain(Acrobot(acrobotConfiguration))
        var currentState = DiscretizedState(acrobotConfiguration.initialState)
        stateList.add(currentState.state)
        for (action in actionList) {
            var successorBundle = domain.successors(currentState)

            // get the state from the successors by filtering on action
            currentState = successorBundle.first { it.action == action }.state

            stateList.add(currentState.state)
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

        val time = acrobotConfiguration.stateConfiguration.timeStep
        while (iterator.hasNext()) {
            val newState = iterator.next()

            val diff1 = Math.toDegrees(angleDifference(previousState.link1.position, newState.link1.position))
            val diff2 = Math.toDegrees(angleDifference(previousState.link2.position, newState.link2.position)) + diff1

            val newRotate1 = acrobotView.addRotate1()
            val newRotate2 = acrobotView.addRotate2()

            logger.debug { "Adding (${String.format("%.1f", time)}: $diff1, $diff2) to timeline" }
            @Suppress("UNCHECKED_CAST")
            sequentialTransition.children.add(Timeline(60.0, KeyFrame(Duration.seconds(time),
                    KeyValue(newRotate1.angleProperty() as WritableValue<Any>, -diff1, interpolator1),
                    KeyValue(newRotate2.angleProperty() as WritableValue<Any>, -diff2, interpolator2))))

            previousState = newState
        }
//        sequentialTransition.play()
        return sequentialTransition
    }
}
package edu.unh.cs.ai.realtimesearch.visualizer

import edu.unh.cs.ai.realtimesearch.experiment.configuration.json.experimentResultFromJson
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import groovyjarjarcommonscli.*
import javafx.application.Application
import kotlin.system.exitProcess

abstract class BaseVisualizer : Application() {
    protected var experimentResult: ExperimentResult? = null

    /**
     * Process commandline arguments.
     */
    protected fun processCommandLine(args: Array<String>) {
        val options = getOptions()

        val helpOption = Option("h", "help", false, "Print help and exit")

        options.addOption(helpOption)

        /* parse command line arguments */
        val cmd = GnuParser().parse(options, args)

        /* print help if help option was specified*/
        val formatter = HelpFormatter()
        if (cmd.hasOption("h")) {
            formatter.printHelp("real-time-search", options)
            exitProcess(1)
        }

        if (cmd.args.size < 1) {
            throw IllegalArgumentException("Error: Must pass results to visualizer")
        }

        experimentResult = experimentResultFromJson(cmd.args.first())

        processOptions(cmd)
    }

    /**
     * Return the application's options.  Should not include help option which is provided by the base.
     */
    protected abstract fun getOptions(): Options

    /**
     * Process any additional options provided by the visualizer.
     */
    protected abstract fun processOptions(cmd: CommandLine)
}
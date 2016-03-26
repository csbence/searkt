package edu.unh.cs.ai.realtimesearch.experiment.network

import edu.unh.cs.ai.realtimesearch.experiment.configuration.ConfigurationExecutor
import org.apache.commons.cli.*
import java.util.*
import kotlin.concurrent.timerTask


/**
 * @author Bence Cserna (bence@cserna.net)
 */
class RealTimeSearchClientApplication(private val rtsServerUrl: String, private val checkInInterval: Long = 60000) {

    private val timer: Timer = Timer()
    private val realTimeSearchClient: RealTimeSearchClient
    private var checkInTask: TimerTask? = null
    private @Volatile var running = false

    init {
        if (checkInInterval !in 0..3600000) {
            throw RuntimeException("Invalid check-in interval [$checkInInterval]. The check-in interval has to be between 0 and 3600000 ms.")
        }

        if (rtsServerUrl.isBlank()) {
            throw RuntimeException("Invalid server url. The server url was empty.")
        }

        realTimeSearchClient = RealTimeSearchClient(rtsServerUrl)
    }

    /**
     * Start RealTimeSearchClientApp.
     *
     * It will periodically checkIn to the server.
     */
    fun start() {
        startPeriodicCheckIn()
        running = true

        while (running) {
            // Get configuration
            val experimentConfiguration = realTimeSearchClient.getExperimentConfiguration()
            if (experimentConfiguration != null) {
                stopPeriodicCheckIn() // Don't do anything else parallel to the experiment
                System.gc() // Make sure that we have not garbage in the memory

                // Execute configuration
                val experimentResult = ConfigurationExecutor.executeConfiguration(experimentConfiguration)

                // Submit results
                realTimeSearchClient.submitResult(experimentResult)
                startPeriodicCheckIn()
            } else {
                // Failed to get a a configuration wait a second to avoid busy wait
                Thread.sleep(10000)
            }
        }
    }

    /**
     * Stop the application gracefully.
     */
    fun stop() {
        stopPeriodicCheckIn()
        running = false
    }

    private fun startPeriodicCheckIn() {
        checkInTask = timerTask { realTimeSearchClient.checkIn() }
        timer.schedule(checkInTask, 0, checkInInterval)
    }

    private fun stopPeriodicCheckIn() {
        if (checkInTask != null) {
            checkInTask?.cancel()
            checkInTask = null
            timer.purge()
        }
    }
}

fun main(args: Array<String>) {
    val mainOptions = Options()

    val serverUrlOption = Option("u", "server-url", true, "RTS server address")
    val helpOption = Option("h", "help", false, "Print this help and exit")

    mainOptions.addOption(serverUrlOption)

    val parser: CommandLineParser = DefaultParser();

    val commandLine = parser.parse(mainOptions, args)

    val formatter = HelpFormatter()
    if (commandLine.hasOption(helpOption.opt) || !commandLine.hasOption(serverUrlOption.opt)) {
        formatter.printHelp("Real-time Search Client", mainOptions)
        return
    } else {
        RealTimeSearchClientApplication(commandLine.getOptionValue(serverUrlOption.opt)).start()
    }
}
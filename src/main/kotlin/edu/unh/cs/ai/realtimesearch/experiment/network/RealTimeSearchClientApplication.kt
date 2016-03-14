package edu.unh.cs.ai.realtimesearch.experiment.network

import edu.unh.cs.ai.realtimesearch.experiment.configuration.ConfigurationExecutor
import java.util.*
import kotlin.concurrent.timerTask

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class RealTimeSearchClientApplication(private val rtsServerUrl: String, private val checkInInterval: Long = 10000) {

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
                System.gc()

                // Execute configuration
                val experimentResult = ConfigurationExecutor.executeConfiguration(experimentConfiguration)

                // Submit results
                realTimeSearchClient.submitResult(experimentResult)

                // TODO handle errors gracefully
            } else {
                // Failed to get a a configuration wait a second to avoid busy wait
                Thread.sleep(1000)
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
package edu.unh.cs.ai.realtimesearch.experiment.network

import edu.unh.cs.ai.realtimesearch.experiment.configuration.ConfigurationExecutor
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import org.apache.commons.cli.*
import org.slf4j.LoggerFactory


/**
 * @author Bence Cserna (bence@cserna.net)
 */
class RealTimeSearchClientApplication(private val rtsServerUrl: String, private val domainPathRoot: String? = null) {

    val logger = LoggerFactory.getLogger(RealTimeSearchClientApplication::class.java)
    //    private val timer: Timer = Timer()
    private val realTimeSearchClient: RealTimeSearchClient
    //    private var checkInTask: TimerTask? = null
    private @Volatile var running = false

    init {
        //        if (checkInInterval !in 0..3600000) {
        //            throw RuntimeException("Invalid check-in interval [$checkInInterval]. The check-in interval has to be between 0 and 3600000 ms.")
        //        }

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
        //        startPeriodicCheckIn()
        running = true
        var lastActiveTimestamp = System.currentTimeMillis()

        while (running) {
            // Get configuration
            val experimentConfiguration = realTimeSearchClient.getExperimentConfiguration()
            if (experimentConfiguration != null) {
                logger.info("Experiment configuration has been received. [̵̵̵̵domain:${experimentConfiguration.domainName} :: algorithm:${experimentConfiguration.algorithmName} :: instance:${experimentConfiguration[Configurations.DOMAIN_INSTANCE_NAME.toString()]}]")
                //                stopPeriodicCheckIn() // Don't do anything else parallel to the experiment
                System.gc() // Make sure that we have not garbage in the memory

                // Execute configuration
                val experimentResult = ConfigurationExecutor.executeConfiguration(experimentConfiguration, domainPathRoot)
                System.gc()

                // Submit results
                realTimeSearchClient.submitResult(experimentResult)
                logger.info("Result submitted")
                //                startPeriodicCheckIn()

                lastActiveTimestamp = System.currentTimeMillis()
            } else {
                logger.info("No experiment available.")
                if (System.currentTimeMillis() - lastActiveTimestamp > 600000) {
                    logger.info("Stop application (timeout)")
                    //                    realTimeSearchClient.disconnect() TODO() implement in the server
                    stop()
                    return
                }

                // Failed to get a a configuration wait a second to avoid busy wait
                Thread.sleep(10000)
            }
        }
    }

    /**
     * Stop the application gracefully.
     */
    fun stop() {
        //        stopPeriodicCheckIn()
        running = false
        //        timer.cancel()
    }

    //    private fun startPeriodicCheckIn() {
    //        checkInTask = timerTask { realTimeSearchClient.checkIn() }
    //        timer.schedule(checkInTask, 0, checkInInterval)
    //    }
    //
    //    private fun stopPeriodicCheckIn() {
    //        if (checkInTask != null) {
    //            checkInTask?.cancel()
    //            checkInTask = null
    //            timer.purge()
    //        }
    //    }
}

fun main(args: Array<String>) {
    val mainOptions = Options()

    val serverUrlOption = Option("u", "server-url", true, "RTS server address")
    val domainPathOption = Option("d", "domainPath", true, "Domain path root")

    val helpOption = Option("h", "help", false, "Print this help and exit")

    mainOptions.addOption(serverUrlOption)
    mainOptions.addOption(domainPathOption)

    val parser: CommandLineParser = DefaultParser();

    val commandLine = parser.parse(mainOptions, args)

    val formatter = HelpFormatter()
    if (commandLine.hasOption(helpOption.opt) || !commandLine.hasOption(serverUrlOption.opt)) {
        formatter.printHelp("Real-time Search Client", mainOptions)
    } else {
        val rtsServerUrl = commandLine.getOptionValue(serverUrlOption.opt)

        val domainPathRoot = if (commandLine.hasOption(domainPathOption.opt)) {
            commandLine.getOptionValue(domainPathOption.opt)
        } else {
            null
        }

        RealTimeSearchClientApplication(rtsServerUrl, domainPathRoot).start()
    }

    println("\nReal-time search client has stopped.")
}
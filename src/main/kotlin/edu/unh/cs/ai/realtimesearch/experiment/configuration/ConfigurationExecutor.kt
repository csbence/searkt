@file:Suppress("DEPRECATION")

package edu.unh.cs.ai.realtimesearch.experiment.configuration

import edu.unh.cs.ai.realtimesearch.MetronomeException
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.Domains
import edu.unh.cs.ai.realtimesearch.environment.Domains.*
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.acrobot.AcrobotIO
import edu.unh.cs.ai.realtimesearch.environment.gridworld.GridWorldIO
import edu.unh.cs.ai.realtimesearch.environment.heavytiles.HeavyTilePuzzleIO
import edu.unh.cs.ai.realtimesearch.environment.heavyvacuumworld.HeavyVacuumWorldIO
import edu.unh.cs.ai.realtimesearch.environment.inversetiles.InverseTilePuzzleIO
import edu.unh.cs.ai.realtimesearch.environment.pointrobot.PointRobotIO
import edu.unh.cs.ai.realtimesearch.environment.pointrobotlost.PointRobotLOSTIO
import edu.unh.cs.ai.realtimesearch.environment.racetrack.RaceTrackIO
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleIO
import edu.unh.cs.ai.realtimesearch.environment.squareroottiles.SquareRootTilePuzzleIO
import edu.unh.cs.ai.realtimesearch.environment.traffic.VehicleWorldIO
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldIO
import edu.unh.cs.ai.realtimesearch.experiment.AnytimeExperiment
import edu.unh.cs.ai.realtimesearch.experiment.ClassicalExperiment
import edu.unh.cs.ai.realtimesearch.experiment.RealTimeExperiment
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.getTerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.Planners
import edu.unh.cs.ai.realtimesearch.planner.Planners.*
import edu.unh.cs.ai.realtimesearch.planner.RealTimePlanner
import edu.unh.cs.ai.realtimesearch.planner.anytime.AnytimeRepairingAStar
import edu.unh.cs.ai.realtimesearch.planner.classical.ClassicalPlanner
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.AStarPlanner
import edu.unh.cs.ai.realtimesearch.planner.realtime.*
import edu.unh.cs.ai.realtimesearch.planner.suboptimal.DynamicPotentialSearch
import edu.unh.cs.ai.realtimesearch.planner.suboptimal.DynamicPotentialSearchG
import edu.unh.cs.ai.realtimesearch.planner.suboptimal.ExplicitEstimationSearch
import edu.unh.cs.ai.realtimesearch.planner.suboptimal.WeightedAStar
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.NANOSECONDS

/**
 * Configuration executor to execute experiment configurations.
 */
object ConfigurationExecutor {

    fun executeConfigurations(configurations: Collection<ExperimentConfiguration>, dataRootPath: String? = null, parallelCores: Int): List<ExperimentResult> {
        class ProgressBar(val maxProgress: Int) {
            var currentProgress = 0
            var lock = Object()
            val startTime = System.currentTimeMillis()

            fun updateProgress() = synchronized(lock) {
                currentProgress++
                val ratio = currentProgress.toDouble() / maxProgress
                val millisecondPerExperiment = (System.currentTimeMillis() - startTime) / currentProgress
                val remainingProgress = maxProgress - currentProgress
                val expectedCompletionMillis = remainingProgress * millisecondPerExperiment

                val seconds = MILLISECONDS.toSeconds(expectedCompletionMillis) % 60
                val minutes = MILLISECONDS.toMinutes(expectedCompletionMillis) % 60
                val hours = MILLISECONDS.toHours(expectedCompletionMillis) % 60

                val builder = StringBuilder()
                builder.append("\r|")
                (1..28).forEach {
                    builder.append(if (it / 28.0 > ratio) "" else "\u2588")
                }
                builder.append("\r|                            | $currentProgress/$maxProgress | ${Math.round(ratio * 100)}% | avg: $millisecondPerExperiment ms/exp | rem: ${hours}h ${minutes}m ${seconds}s |")
//                println(builder.toString())
                System.err.println(builder.toString())
                System.err.flush()
            }
        }

        val progressBar = ProgressBar(configurations.size)

        val executor = Executors.newFixedThreadPool(parallelCores)
        return configurations
                .map { executor.submit(Callable<ExperimentResult> { executeConfiguration(it, dataRootPath) }) }
                .map {
                    val experimentResult = it.get()
                    progressBar.updateProgress()
                    experimentResult
                }.also { executor.shutdown() }
    }

    fun executeConfiguration(configuration: ExperimentConfiguration, dataRootPath: String? = null): ExperimentResult {
        val logger = LoggerFactory.getLogger("ConfigurationExecutor")

        var experimentResult: ExperimentResult? = null
        var executionException: Throwable? = null

        val thread = Thread({
            experimentResult = unsafeConfigurationExecution(configuration, dataRootPath)
        })

        thread.setUncaughtExceptionHandler { _, throwable ->
            executionException = throwable

            if (executionException is MetronomeException) {
                logger.info("Experiment stopped", throwable.message)
            } else {
                logger.info("Experiment stopped", throwable)
            }
        }

//        collectAndWait() // Only enable it when optimizing for time

        thread.start()
        thread.priority = Thread.MAX_PRIORITY
        thread.join(MILLISECONDS.convert(configuration.timeLimit, NANOSECONDS))

        if (executionException != null) {
            collectAndWait()

            logger.info("Experiment failed. ${executionException!!.message}")
            val failedExperimentResult = ExperimentResult(configuration, "${executionException!!.message}")
            failedExperimentResult.errorDetails = executionException!!.stackTrace!!.contentToString()
            return failedExperimentResult
        }

        if (experimentResult == null) {
            logger.info("Experiment timed out.")
            thread.stop() // This should be replaced with a graceful stop
            thread.join()

            collectAndWait()

            return ExperimentResult(configuration, "Timeout")
        }

//        logger.info("Experiment execution is done.")

        return experimentResult!!
    }

    private fun collectAndWait() {
        System.gc()
        Thread.sleep(500)
    }

    private fun unsafeConfigurationExecution(configuration: ExperimentConfiguration, dataRootPath: String? = null): ExperimentResult? {
        val domainName: String = configuration.domainName

        val domainStream: InputStream = when {
            configuration.rawDomain != null -> configuration.rawDomain.byteInputStream()
            dataRootPath != null -> FileInputStream(dataRootPath + configuration.domainPath)
            else -> Unit::class.java.classLoader.getResourceAsStream(configuration.domainPath)
                    ?: throw MetronomeException("Instance file not found: ${configuration.domainPath}")
        }

        val domain = Domains.valueOf(domainName)
        return when (domain) {
            SLIDING_TILE_PUZZLE_4 -> executeSlidingTilePuzzle(configuration, domainStream)
            SLIDING_TILE_PUZZLE_4_HEAVY -> executeHeavySlidingTilePuzzle(configuration, domainStream)
            SLIDING_TILE_PUZZLE_4_INVERSE -> executeInverseSlidingTilePuzzle(configuration, domainStream)
            SLIDING_TILE_PUZZLE_4_SQRT -> executeSquareRootSlidingTilePuzzle(configuration, domainStream)
            VACUUM_WORLD -> executeVacuumWorld(configuration, domainStream)
            HEAVY_VACUUM_WORLD -> executeHeavyVacuumWorld(configuration, domainStream)
            GRID_WORLD -> executeGridWorld(configuration, domainStream)
            ACROBOT -> executeAcrobot(configuration, domainStream)
            POINT_ROBOT -> executePointRobot(configuration, domainStream)
            POINT_ROBOT_LOST -> executePointRobotLOST(configuration, domainStream)
//            POINT_ROBOT_WITH_INERTIA -> executePointRobotWithInertia(configuration, domainStream)
            RACETRACK -> executeRaceTrack(configuration, domainStream)
            TRAFFIC -> executeVehicle(configuration, domainStream)
            else -> throw MetronomeException("Unknown or deactivated globalDomain: $domain")
        }
    }

    private fun executePointRobot(configuration: ExperimentConfiguration, domainStream: InputStream): ExperimentResult {
        val pointRobotInstance = PointRobotIO.parseFromStream(domainStream, configuration.actionDuration)
        return executeDomain(configuration, pointRobotInstance.domain, pointRobotInstance.initialState)
    }

    private fun executePointRobotLOST(configuration: ExperimentConfiguration, domainStream: InputStream): ExperimentResult {
        val pointRobotLOSTInstance = PointRobotLOSTIO.parseFromStream(domainStream, configuration.actionDuration)
        return executeDomain(configuration, pointRobotLOSTInstance.domain, pointRobotLOSTInstance.initialState)
    }

//    private fun executePointRobotWithInertia(configuration: ExperimentConfiguration, domainStream: InputStream): ExperimentResult {
//        val numActions = configuration.getTypedValue<Long>(Configurations.NUM_ACTIONS.toString())?.toInt() ?: PointRobotWithInertia.defaultNumActions
//        val actionFraction = configuration.getTypedValue<Double>(Configurations.ACTION_FRACTION.toString()) ?: PointRobotWithInertia.defaultActionFraction
//        val stateFraction = configuration.getTypedValue<Double>(Configurations.STATE_FRACTION.toString()) ?: PointRobotWithInertia.defaultStateFraction
//
//        val pointRobotWithInertiaInstance = PointRobotWithInertiaIO.parseFromStream(domainStream, numActions, actionFraction, stateFraction, configuration.actionDuration)
//
//        return executeDomain(configuration, pointRobotWithInertiaInstance.globalDomain, pointRobotWithInertiaInstance.initialState)
//    }

    private fun executeVacuumWorld(configuration: ExperimentConfiguration, domainStream: InputStream): ExperimentResult {
        val vacuumWorldInstance = VacuumWorldIO.parseFromStream(domainStream)
        return executeDomain(configuration, vacuumWorldInstance.domain, vacuumWorldInstance.initialState)
    }

    private fun executeHeavyVacuumWorld(configuration: ExperimentConfiguration, domainStream: InputStream): ExperimentResult {
        val vacuumWorldInstance = HeavyVacuumWorldIO.parseFromStream(domainStream)
        return executeDomain(configuration, vacuumWorldInstance.domain, vacuumWorldInstance.initialState)
    }

    private fun executeRaceTrack(configuration: ExperimentConfiguration, domainStream: InputStream): ExperimentResult {
        val domainSizeMultiplier = configuration.domainSizeMultiplier
        val raceTrackInstance = RaceTrackIO.parseFromStream(domainStream, configuration.actionDuration)
        return executeDomain(configuration, raceTrackInstance.domain, raceTrackInstance.initialState)
    }

    private fun executeGridWorld(configuration: ExperimentConfiguration, domainStream: InputStream): ExperimentResult {
        val gridWorldInstance = GridWorldIO.parseFromStream(domainStream, configuration.actionDuration)
        return executeDomain(configuration, gridWorldInstance.domain, gridWorldInstance.initialState)
    }

    private fun executeVehicle(configuration: ExperimentConfiguration, domainStream: InputStream):
            ExperimentResult {
        val vehicleWorldInstance = VehicleWorldIO.parseFromStream(domainStream, configuration.actionDuration)
        return executeDomain(configuration, vehicleWorldInstance.domain, vehicleWorldInstance.initialState)
    }

    private fun executeSlidingTilePuzzle(configuration: ExperimentConfiguration, domainStream: InputStream): ExperimentResult {
        val slidingTilePuzzleInstance = SlidingTilePuzzleIO.parseFromStream(domainStream, configuration.actionDuration)
        return executeDomain(configuration, slidingTilePuzzleInstance.domain, slidingTilePuzzleInstance.initialState)
    }

    private fun executeHeavySlidingTilePuzzle(configuration: ExperimentConfiguration, domainStream: InputStream): ExperimentResult {
        val slidingTilePuzzleInstance = HeavyTilePuzzleIO.parseFromStream(domainStream, configuration.actionDuration)
        return executeDomain(configuration, slidingTilePuzzleInstance.domain, slidingTilePuzzleInstance.initialState)
    }

    private fun executeInverseSlidingTilePuzzle(configuration: ExperimentConfiguration, domainStream: InputStream): ExperimentResult {
        val slidingTilePuzzleInstance = InverseTilePuzzleIO.parseFromStream(domainStream, configuration.actionDuration)
        return executeDomain(configuration, slidingTilePuzzleInstance.domain, slidingTilePuzzleInstance.initialState)
    }

    private fun executeSquareRootSlidingTilePuzzle(configuration: ExperimentConfiguration, domainStream: InputStream): ExperimentResult {
        val slidingTilePuzzleInstance = SquareRootTilePuzzleIO.parseFromStream(domainStream, configuration.actionDuration)
        return executeDomain(configuration, slidingTilePuzzleInstance.domain, slidingTilePuzzleInstance.initialState)
    }

    private fun executeAcrobot(configuration: ExperimentConfiguration, domainStream: InputStream): ExperimentResult {
        val acrobotInstance = AcrobotIO.parseFromStream(domainStream, configuration.actionDuration)
        return executeDomain(configuration, acrobotInstance.domain, acrobotInstance.initialState)
    }

    private fun <StateType : State<StateType>> executeDomain(configuration: ExperimentConfiguration, domain: Domain<StateType>, initialState: StateType): ExperimentResult {
        val algorithmName = configuration.algorithmName
        val seed = configuration.domainSeed
        val sourceState = seed?.run { domain.randomizedStartState(initialState, this) } ?: initialState

        return when (Planners.valueOf(algorithmName)) {
            WEIGHTED_A_STAR -> executeOfflineSearch(WeightedAStar(domain, configuration), configuration, domain, sourceState)
            A_STAR -> executeOfflineSearch(AStarPlanner(domain), configuration, domain, sourceState)
            LSS_LRTA_STAR -> executeRealTimeSearch(LssLrtaStarPlanner(domain, configuration), configuration, domain, sourceState)
            CES -> executeRealTimeSearch(ComprehensiveEnvelopeSearch(domain, configuration), configuration, domain, sourceState)
            ES -> executeRealTimeSearch(EnvelopeSearch(domain, configuration), configuration, domain, sourceState)
            DYNAMIC_F_HAT -> executeRealTimeSearch(DynamicFHatPlanner(domain), configuration, domain, sourceState)
            RTA_STAR -> executeRealTimeSearch(RealTimeAStarPlanner(domain, configuration), configuration, domain, sourceState)
            ARA_STAR -> executeAnytimeRepairingAStar(configuration, domain, sourceState)
            SAFE_RTS -> executeRealTimeSearch(SafeRealTimeSearch(domain, configuration), configuration, domain, sourceState)
            S_ZERO -> executeRealTimeSearch(SZeroPlanner(domain, configuration), configuration, domain, sourceState)
            SIMPLE_SAFE -> executeRealTimeSearch(SimpleSafePlanner(domain, configuration), configuration, domain, sourceState)
            DPS -> executeOfflineSearch(DynamicPotentialSearch(domain, configuration), configuration, domain, sourceState)
            DPSG -> executeOfflineSearch(DynamicPotentialSearchG(domain, configuration), configuration, domain, sourceState)
            EES -> executeOfflineSearch(ExplicitEstimationSearch(domain, configuration), configuration, domain, sourceState)
            EESF -> executeOfflineSearch(ExplicitEstimationSearch(domain, configuration), configuration, domain, sourceState)
            EEST -> executeOfflineSearch(ExplicitEstimationSearch(domain, configuration), configuration, domain, sourceState)
            TIME_BOUNDED_A_STAR -> executeRealTimeSearch(TimeBoundedAStar(domain, configuration), configuration, domain, sourceState)
            ALT_ENVELOPE -> executeRealTimeSearch(AlternateEnvelopeSearch(domain, configuration), configuration, domain, sourceState)
            ENVELOPE -> throw MetronomeException("Planner not specified - Remove enum?")
            else -> throw MetronomeException("Planner not specified or unrecognized: $algorithmName")
        }
    }

    private fun <StateType : State<StateType>> executeRealTimeSearch(planner: RealTimePlanner<StateType>, configuration: ExperimentConfiguration, domain: Domain<StateType>, initialState: StateType): ExperimentResult {
        val realTimeExperiment = RealTimeExperiment(configuration, planner, domain, initialState, getTerminationChecker(configuration))

        return realTimeExperiment.run()
    }

    private fun <StateType : State<StateType>> executeOfflineSearch(planner: ClassicalPlanner<StateType>, configuration: ExperimentConfiguration, domain: Domain<StateType>, initialState: StateType): ExperimentResult {
        return ClassicalExperiment(configuration, planner, domain, initialState, getTerminationChecker(configuration)).run()
    }

    private fun <StateType : State<StateType>> executeAnytimeRepairingAStar(experimentConfiguration: ExperimentConfiguration, domain: Domain<StateType>, initialState: StateType): ExperimentResult {
        val anytimeRepairingAStarPlanner = AnytimeRepairingAStar(domain)
        val atsExperiment = AnytimeExperiment(anytimeRepairingAStarPlanner, experimentConfiguration, domain, initialState)

        return atsExperiment.run()
    }

}
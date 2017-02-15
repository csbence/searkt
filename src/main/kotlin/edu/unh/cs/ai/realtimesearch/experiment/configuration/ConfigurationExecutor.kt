@file:Suppress("DEPRECATION")

package edu.unh.cs.ai.realtimesearch.experiment.configuration

import edu.unh.cs.ai.realtimesearch.MetronomeException
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.Domains
import edu.unh.cs.ai.realtimesearch.environment.Domains.*
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.acrobot.AcrobotIO
import edu.unh.cs.ai.realtimesearch.environment.gridworld.GridWorldIO
import edu.unh.cs.ai.realtimesearch.environment.pointrobot.PointRobotIO
import edu.unh.cs.ai.realtimesearch.environment.pointrobotlost.PointRobotLOSTIO
import edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia.PointRobotWithInertia
import edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia.PointRobotWithInertiaIO
import edu.unh.cs.ai.realtimesearch.environment.racetrack.RaceTrackIO
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleIO
import edu.unh.cs.ai.realtimesearch.environment.traffic.VehicleWorldIO
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldIO
import edu.unh.cs.ai.realtimesearch.experiment.AnytimeExperiment
import edu.unh.cs.ai.realtimesearch.experiment.ClassicalExperiment
import edu.unh.cs.ai.realtimesearch.experiment.RealTimeExperiment
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.LookaheadType
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.LookaheadType.DYNAMIC
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.LookaheadType.STATIC
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType.*
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.*
import edu.unh.cs.ai.realtimesearch.planner.Planners
import edu.unh.cs.ai.realtimesearch.planner.Planners.*
import edu.unh.cs.ai.realtimesearch.planner.RealTimePlanner
import edu.unh.cs.ai.realtimesearch.planner.anytime.AnytimeRepairingAStar
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.AStarPlanner
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.ClassicalAStarPlanner
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.SimpleAStar
import edu.unh.cs.ai.realtimesearch.planner.realtime.*
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.NANOSECONDS
import java.util.stream.Collectors

/**
 * Configuration executor to execute experiment configurations.
 */
object ConfigurationExecutor {

    fun executeConfigurations(configurations: Collection<GeneralExperimentConfiguration>, dataRootPath: String? = null, parallelCores: Int): List<ExperimentResult> {
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

                val builder = StringBuilder("\r|                            | $currentProgress/$maxProgress | ${Math.round(ratio * 100)}% | avg: $millisecondPerExperiment ms/exp | rem: ${hours}h ${minutes}m ${seconds}s |")
                builder.append("\r|")
                (1..28).forEach {
                    builder.append(if (it / 28.0 > ratio) "" else "\u2588")
                }
                print(builder.toString())
            }
        }

        val progressBar = ProgressBar(configurations.size)

        val executor = Executors.newFixedThreadPool(parallelCores)
        return configurations
                .map { executor.submit(Callable<ExperimentResult>{ executeConfiguration(it, dataRootPath)}) }
                .map { val experimentResult = it.get()
                    progressBar.updateProgress()
                    experimentResult
                }
    }

    fun executeConfiguration(configuration: GeneralExperimentConfiguration, dataRootPath: String? = null): ExperimentResult {
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
            val failedExperimentResult = ExperimentResult(configuration.valueStore, "${executionException!!.message}")
            failedExperimentResult["errorDetails"] = executionException!!.stackTrace
            return failedExperimentResult
        }

        if (experimentResult == null) {
            logger.info("Experiment timed out.")
            thread.stop() // This should be replaced with a graceful stop
            thread.join()

            collectAndWait()

            return ExperimentResult(configuration.valueStore, "Timeout")
        }

        logger.info("Experiment successful.")

        return experimentResult!!
    }

    private fun collectAndWait() {
        System.gc()
        Thread.sleep(500)
    }

    private fun unsafeConfigurationExecution(configuration: GeneralExperimentConfiguration, dataRootPath: String? = null): ExperimentResult? {
        val domainName: String = configuration.domainName

        val domainStream: InputStream = if (configuration.valueStore[Configurations.RAW_DOMAIN.toString()] != null) {
            configuration.rawDomain!!.byteInputStream()
        } else if (dataRootPath != null) {
            FileInputStream(dataRootPath + configuration.domainPath)
        } else {
            Unit::class.java.classLoader.getResourceAsStream(configuration.domainPath) ?: throw MetronomeException("Instance file not found: ${configuration.domainPath}")
        }

        val domain = Domains.valueOf(domainName)
        return when (domain) {
            SLIDING_TILE_PUZZLE_4 -> executeSlidingTilePuzzle(configuration, domainStream)
            VACUUM_WORLD -> executeVacuumWorld(configuration, domainStream)
            GRID_WORLD -> executeGridWorld(configuration, domainStream)
            ACROBOT -> executeAcrobot(configuration, domainStream)
            POINT_ROBOT -> executePointRobot(configuration, domainStream)
            POINT_ROBOT_LOST -> executePointRobotLOST(configuration, domainStream)
            POINT_ROBOT_WITH_INERTIA -> executePointRobotWithInertia(configuration, domainStream)
            RACETRACK -> executeRaceTrack(configuration, domainStream)
            TRAFFIC -> executeVehicle(configuration, domainStream)

            else -> ExperimentResult(configuration.valueStore, errorMessage = "Unknown domain type: $domainName")
        }
    }

    private fun executePointRobot(configuration: GeneralExperimentConfiguration, domainStream: InputStream): ExperimentResult {
        val pointRobotInstance = PointRobotIO.parseFromStream(domainStream, configuration.actionDuration)
        return executeDomain(configuration, pointRobotInstance.domain, pointRobotInstance.initialState)
    }

    private fun executePointRobotLOST(configuration: GeneralExperimentConfiguration, domainStream: InputStream): ExperimentResult {
        val pointRobotLOSTInstance = PointRobotLOSTIO.parseFromStream(domainStream, configuration.actionDuration)
        return executeDomain(configuration, pointRobotLOSTInstance.domain, pointRobotLOSTInstance.initialState)
    }

    private fun executePointRobotWithInertia(configuration: GeneralExperimentConfiguration, domainStream: InputStream): ExperimentResult {
        val numActions = configuration.getTypedValue<Long>(Configurations.NUM_ACTIONS.toString())?.toInt() ?: PointRobotWithInertia.defaultNumActions
        val actionFraction = configuration.getTypedValue<Double>(Configurations.ACTION_FRACTION.toString()) ?: PointRobotWithInertia.defaultActionFraction
        val stateFraction = configuration.getTypedValue<Double>(Configurations.STATE_FRACTION.toString()) ?: PointRobotWithInertia.defaultStateFraction

        val pointRobotWithInertiaInstance = PointRobotWithInertiaIO.parseFromStream(domainStream, numActions, actionFraction, stateFraction, configuration.actionDuration)

        return executeDomain(configuration, pointRobotWithInertiaInstance.domain, pointRobotWithInertiaInstance.initialState)
    }

    private fun executeVacuumWorld(configuration: GeneralExperimentConfiguration, domainStream: InputStream): ExperimentResult {
        val vacuumWorldInstance = VacuumWorldIO.parseFromStream(domainStream)
        return executeDomain(configuration, vacuumWorldInstance.domain, vacuumWorldInstance.initialState)
    }

    private fun executeRaceTrack(configuration: GeneralExperimentConfiguration, domainStream: InputStream): ExperimentResult {
        val raceTrackInstance = RaceTrackIO.parseFromStream(domainStream, configuration.actionDuration)
        return executeDomain(configuration, raceTrackInstance.domain, raceTrackInstance.initialState)
    }

    private fun executeGridWorld(configuration: GeneralExperimentConfiguration, domainStream: InputStream): ExperimentResult {
        val gridWorldInstance = GridWorldIO.parseFromStream(domainStream, configuration.actionDuration)
        return executeDomain(configuration, gridWorldInstance.domain, gridWorldInstance.initialState)
    }

    private fun executeVehicle(configuration: GeneralExperimentConfiguration, domainStream: InputStream):
            ExperimentResult {
        val vehicleWorldInstance = VehicleWorldIO.parseFromStream(domainStream, configuration.actionDuration)
        return executeDomain(configuration, vehicleWorldInstance.domain, vehicleWorldInstance.initialState)
    }

    private fun executeSlidingTilePuzzle(configuration: GeneralExperimentConfiguration, domainStream: InputStream): ExperimentResult {
        val slidingTilePuzzleInstance = SlidingTilePuzzleIO.parseFromStream(domainStream, configuration.actionDuration)
        return executeDomain(configuration, slidingTilePuzzleInstance.domain, slidingTilePuzzleInstance.initialState)
    }

    private fun executeAcrobot(configuration: GeneralExperimentConfiguration, domainStream: InputStream): ExperimentResult {
        val acrobotInstance = AcrobotIO.parseFromStream(domainStream, configuration.actionDuration)
        return executeDomain(configuration, acrobotInstance.domain, acrobotInstance.initialState)
    }

    private fun <StateType : State<StateType>> executeDomain(configuration: GeneralExperimentConfiguration, domain: Domain<StateType>, initialState: StateType): ExperimentResult {
        val algorithmName = configuration.algorithmName
        val seed = configuration[Configurations.DOMAIN_SEED.toString()] as? Long
        val sourceState = seed?.run { domain.randomizedStartState(initialState, this) } ?: initialState

        return when (Planners.valueOf(algorithmName)) {
            WEIGHTED_A_STAR -> executeWeightedAStar(configuration, domain, sourceState)
            A_STAR -> executeAStar(configuration, domain, sourceState)
            LSS_LRTA_STAR -> executeRealTimeSearch(LssLrtaStarPlanner(domain), configuration, domain, sourceState)
            DYNAMIC_F_HAT -> executeRealTimeSearch(DynamicFHatPlanner(domain), configuration, domain, sourceState)
            RTA_STAR -> executeRealTimeAStar(configuration, domain, sourceState)
            ARA_STAR -> executeAnytimeRepairingAStar(configuration, domain, sourceState)
            SAFE_RTS -> executeRealTimeSearch(SafeRealTimeSearch(domain, configuration), configuration, domain, sourceState)
            S_ZERO -> executeRealTimeSearch(SZeroPlanner(domain), configuration, domain, sourceState)
            S_ONE -> executeRealTimeSearch(SOnePlanner(domain), configuration, domain, sourceState)
            else -> ExperimentResult(configuration.valueStore, errorMessage = "Unknown algorithm: $algorithmName")
        }
    }

    private fun <StateType : State<StateType>> executeRealTimeSearch(planner: RealTimePlanner<StateType>, configuration: GeneralExperimentConfiguration, domain: Domain<StateType>, initialState: StateType): ExperimentResult {
        return RealTimeExperiment(configuration, planner, domain, initialState, getTerminationChecker(configuration)).run()
    }

    private fun <StateType : State<StateType>> executePureAStar(configuration: GeneralExperimentConfiguration, domain: Domain<StateType>, initialState: StateType): ExperimentResult {
        val aStarPlanner = SimpleAStar(domain)
        aStarPlanner.search(initialState)

        return ExperimentResult(configuration.valueStore, errorMessage = "Incompatible output format.")
    }

    private fun <StateType : State<StateType>> executeAStar(configuration: GeneralExperimentConfiguration, domain: Domain<StateType>, initialState: StateType): ExperimentResult {
        val aStarPlanner = AStarPlanner(domain, 1.0)
        val classicalExperiment = ClassicalExperiment(configuration, aStarPlanner, domain, initialState)

        return classicalExperiment.run()
    }

    private fun <StateType : State<StateType>> executeWeightedAStar(configuration: GeneralExperimentConfiguration, domain: Domain<StateType>, initialState: StateType): ExperimentResult {
        val weight = configuration.getTypedValue<Double>(Configurations.WEIGHT.toString()) ?: throw InvalidFieldException("\"${Configurations.WEIGHT}\" is not found. Please add it the the experiment configuration.")
        val aStarPlanner = ClassicalAStarPlanner(domain, weight)
        val classicalExperiment = ClassicalExperiment(configuration, aStarPlanner, domain, initialState)

        return classicalExperiment.run()
    }

    private fun <StateType : State<StateType>> executeClassicalAStar(configuration: GeneralExperimentConfiguration, domain: Domain<StateType>, initialState: StateType): ExperimentResult {
        val aStarPlanner = ClassicalAStarPlanner(domain, 1.0)
        val classicalExperiment = ClassicalExperiment(configuration, aStarPlanner, domain, initialState)

        return classicalExperiment.run()
    }

    private fun getTerminationChecker(configuration: GeneralExperimentConfiguration): TerminationChecker {
        val lookaheadTypeString = configuration.getTypedValue<String>(Configurations.LOOKAHEAD_TYPE.toString()) ?: throw  InvalidFieldException("\"${Configurations.LOOKAHEAD_TYPE}\" is not found. Please add it to the configuration.")
        val lookaheadType = LookaheadType.valueOf(lookaheadTypeString)
        val terminationTypeString = configuration.getTypedValue<String>(Configurations.TERMINATION_TYPE.toString()) ?: throw InvalidFieldException("\"${Configurations.TERMINATION_TYPE}\" is not found. Please add it to the configuration.")
        val terminationType = TerminationType.valueOf(terminationTypeString)

        return when {
            lookaheadType == DYNAMIC && terminationType == TIME -> MutableTimeTerminationChecker()
            lookaheadType == DYNAMIC && terminationType == EXPANSION -> DynamicExpansionTerminationChecker()
            lookaheadType == STATIC && terminationType == TIME -> StaticTimeTerminationChecker(configuration.actionDuration)
            lookaheadType == STATIC && terminationType == EXPANSION -> StaticExpansionTerminationChecker(configuration.actionDuration)
            terminationType == UNLIMITED -> FakeTerminationChecker
            else -> throw MetronomeException("Invalid termination checker configuration")
        }
    }

    private fun <StateType : State<StateType>> executeRealTimeAStar(experimentConfiguration: GeneralExperimentConfiguration, domain: Domain<StateType>, initialState: StateType): ExperimentResult {
        val depthLimit = experimentConfiguration.getTypedValue<Long>(Configurations.LOOKAHEAD_DEPTH_LIMIT.toString()) ?: throw InvalidFieldException("\"${Configurations.LOOKAHEAD_DEPTH_LIMIT}\" is not found. Please add it to the experiment configuration.")
        val realTimeAStarPlanner = RealTimeAStarPlanner(domain, depthLimit.toInt())
        val rtsExperiment = RealTimeExperiment(experimentConfiguration, realTimeAStarPlanner, domain, initialState, getTerminationChecker(experimentConfiguration))

        return rtsExperiment.run()
    }

    private fun <StateType : State<StateType>> executeAnytimeRepairingAStar(experimentConfiguration: GeneralExperimentConfiguration, domain: Domain<StateType>, initialState: StateType): ExperimentResult {
        val anytimeRepairingAStarPlanner = AnytimeRepairingAStar(domain)
        val atsExperiment = AnytimeExperiment(anytimeRepairingAStarPlanner, experimentConfiguration, domain, initialState)

        return atsExperiment.run()
    }

}
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
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.NANOSECONDS
import java.util.stream.Collectors

/**
 * Configuration executor to execute experiment configurations.
 */
object ConfigurationExecutor {

    fun executeConfigurations(experimentConfigurations: List<GeneralExperimentConfiguration>, dataRootPath: String? = null, parallel: Boolean): List<ExperimentResult> {
        val configurationStream = if (parallel) experimentConfigurations.parallelStream() else experimentConfigurations.stream()
        return configurationStream.map { executeConfiguration(it, dataRootPath) }.collect(Collectors.toList())
    }

    fun executeConfiguration(experimentConfiguration: GeneralExperimentConfiguration, dataRootPath: String? = null): ExperimentResult {
        val logger = LoggerFactory.getLogger("ConfigurationExecutor")

        var experimentResult: ExperimentResult? = null
        var executionException: Throwable? = null

        val thread = Thread({
            experimentResult = unsafeConfigurationExecution(experimentConfiguration, dataRootPath)
        })

        thread.setUncaughtExceptionHandler { _, throwable ->
            executionException = throwable

            if (executionException is MetronomeException) {
                logger.info("Experiment stopped", throwable.message)
            } else {
                logger.info("Experiment stopped", throwable)
            }
        }

        collectAndWait()

        thread.start()
        thread.priority = Thread.MAX_PRIORITY
        thread.join(MILLISECONDS.convert(experimentConfiguration.timeLimit, NANOSECONDS))

        if (executionException != null) {
            collectAndWait()

            logger.info("Experiment failed. ${executionException!!.message}")
            val failedExperimentResult = ExperimentResult(experimentConfiguration.valueStore, "${executionException!!.message}")
            failedExperimentResult["errorDetails"] = executionException!!.stackTrace
            return failedExperimentResult
        }

        if (experimentResult == null) {
            logger.info("Experiment timed out.")
            thread.stop() // This should be replaced with a graceful stop
            thread.join()

            collectAndWait()

            return ExperimentResult(experimentConfiguration.valueStore, "Timeout")
        }

        logger.info("Experiment successful.")

        return experimentResult!!
    }

    private fun collectAndWait() {
        System.gc()
        Thread.sleep(500)
    }

    private fun unsafeConfigurationExecution(experimentConfiguration: GeneralExperimentConfiguration, dataRootPath: String? = null): ExperimentResult? {
        val domainName: String = experimentConfiguration.domainName

        val domainStream: InputStream = if (experimentConfiguration.valueStore[Configurations.RAW_DOMAIN.toString()] != null) {
            experimentConfiguration.rawDomain!!.byteInputStream()
        } else {
            dataRootPath ?: throw RuntimeException("Data root path is not specified.")
            FileInputStream(dataRootPath + experimentConfiguration.domainPath)
        }

        val domain = Domains.valueOf(domainName)
        return when (domain) {
            SLIDING_TILE_PUZZLE_4 -> executeSlidingTilePuzzle(experimentConfiguration, domainStream)
            VACUUM_WORLD -> executeVacuumWorld(experimentConfiguration, domainStream)
            GRID_WORLD -> executeGridWorld(experimentConfiguration, domainStream)
            ACROBOT -> executeAcrobot(experimentConfiguration, domainStream)
            POINT_ROBOT -> executePointRobot(experimentConfiguration, domainStream)
            POINT_ROBOT_LOST -> executePointRobotLOST(experimentConfiguration, domainStream)
            POINT_ROBOT_WITH_INERTIA -> executePointRobotWithInertia(experimentConfiguration, domainStream)
            RACETRACK -> executeRaceTrack(experimentConfiguration, domainStream)
            TRAFFIC -> executeVehicle(experimentConfiguration, domainStream)

            else -> ExperimentResult(experimentConfiguration.valueStore, errorMessage = "Unknown domain type: $domainName")
        }
    }

    private fun executePointRobot(experimentConfiguration: GeneralExperimentConfiguration, domainStream: InputStream): ExperimentResult {
        val pointRobotInstance = PointRobotIO.parseFromStream(domainStream, experimentConfiguration.actionDuration)
        return executeDomain(experimentConfiguration, pointRobotInstance.domain, pointRobotInstance.initialState)
    }

    private fun executePointRobotLOST(experimentConfiguration: GeneralExperimentConfiguration, domainStream: InputStream): ExperimentResult {
        val pointRobotLOSTInstance = PointRobotLOSTIO.parseFromStream(domainStream, experimentConfiguration.actionDuration)
        return executeDomain(experimentConfiguration, pointRobotLOSTInstance.domain, pointRobotLOSTInstance.initialState)
    }

    private fun executePointRobotWithInertia(experimentConfiguration: GeneralExperimentConfiguration, domainStream: InputStream): ExperimentResult {
        val numActions = experimentConfiguration.getTypedValue<Long>(Configurations.NUM_ACTIONS.toString())?.toInt() ?: PointRobotWithInertia.defaultNumActions
        val actionFraction = experimentConfiguration.getTypedValue<Double>(Configurations.ACTION_FRACTION.toString()) ?: PointRobotWithInertia.defaultActionFraction
        val stateFraction = experimentConfiguration.getTypedValue<Double>(Configurations.STATE_FRACTION.toString()) ?: PointRobotWithInertia.defaultStateFraction

        val pointRobotWithInertiaInstance = PointRobotWithInertiaIO.parseFromStream(domainStream, numActions, actionFraction, stateFraction, experimentConfiguration.actionDuration)

        return executeDomain(experimentConfiguration, pointRobotWithInertiaInstance.domain, pointRobotWithInertiaInstance.initialState)
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

        return when (Planners.valueOf(algorithmName)) {
            WEIGHTED_A_STAR -> executeWeightedAStar(configuration, domain, initialState)
            A_STAR -> executeAStar(configuration, domain, initialState)
            LSS_LRTA_STAR -> executeRealTimeSearch(LssLrtaStarPlanner(domain), configuration, domain, initialState)
            DYNAMIC_F_HAT -> executeRealTimeSearch(DynamicFHatPlanner(domain), configuration, domain, initialState)
            RTA_STAR -> executeRealTimeAStar(configuration, domain, initialState)
            ARA_STAR -> executeAnytimeRepairingAStar(configuration, domain, initialState)
            SAFE_RTS -> executeRealTimeSearch(SafeRealTimeSearch(domain, configuration), configuration, domain, initialState)
            S_ZERO -> executeRealTimeSearch(SZeroPlanner(domain), configuration, domain, initialState)
            S_ONE -> executeRealTimeSearch(SOnePlanner(domain), configuration, domain, initialState)
            else -> ExperimentResult(configuration.valueStore, errorMessage = "Unknown algorithm: $algorithmName")
        }
    }

    private fun <StateType : State<StateType>> executeRealTimeSearch(planner: RealTimePlanner<StateType>, configuration: GeneralExperimentConfiguration, domain: Domain<StateType>, initialState: StateType): ExperimentResult {
        return RealTimeExperiment(configuration, planner, domain, initialState, getTerminationChecker(configuration)).run()
    }

    private fun <StateType : State<StateType>> executePureAStar(experimentConfiguration: GeneralExperimentConfiguration, domain: Domain<StateType>, initialState: StateType): ExperimentResult {
        val aStarPlanner = SimpleAStar(domain)
        aStarPlanner.search(initialState)

        return ExperimentResult(experimentConfiguration.valueStore, errorMessage = "Incompatible output format.")
    }

    private fun <StateType : State<StateType>> executeAStar(experimentConfiguration: GeneralExperimentConfiguration, domain: Domain<StateType>, initialState: StateType): ExperimentResult {
        val aStarPlanner = AStarPlanner(domain, 1.0)
        val classicalExperiment = ClassicalExperiment(experimentConfiguration, aStarPlanner, domain, initialState)

        return classicalExperiment.run()
    }

    private fun <StateType : State<StateType>> executeWeightedAStar(experimentConfiguration: GeneralExperimentConfiguration, domain: Domain<StateType>, initialState: StateType): ExperimentResult {
        val weight = experimentConfiguration.getTypedValue<Double>(Configurations.WEIGHT.toString()) ?: throw InvalidFieldException("\"${Configurations.WEIGHT}\" is not found. Please add it the the experiment configuration.")
        val aStarPlanner = ClassicalAStarPlanner(domain, weight)
        val classicalExperiment = ClassicalExperiment(experimentConfiguration, aStarPlanner, domain, initialState)

        return classicalExperiment.run()
    }

    private fun <StateType : State<StateType>> executeClassicalAStar(experimentConfiguration: GeneralExperimentConfiguration, domain: Domain<StateType>, initialState: StateType): ExperimentResult {
        val aStarPlanner = ClassicalAStarPlanner(domain, 1.0)
        val classicalExperiment = ClassicalExperiment(experimentConfiguration, aStarPlanner, domain, initialState)

        return classicalExperiment.run()
    }

    private fun getTerminationChecker(experimentConfiguration: GeneralExperimentConfiguration): TerminationChecker {
        val lookaheadTypeString = experimentConfiguration.getTypedValue<String>(Configurations.LOOKAHEAD_TYPE.toString()) ?: throw  InvalidFieldException("\"${Configurations.LOOKAHEAD_TYPE}\" is not found. Please add it to the configuration.")
        val lookaheadType = LookaheadType.valueOf(lookaheadTypeString)
        val terminationTypeString = experimentConfiguration.getTypedValue<String>(Configurations.TERMINATION_TYPE.toString()) ?: throw InvalidFieldException("\"${Configurations.TERMINATION_TYPE}\" is not found. Please add it to the configuration.")
        val terminationType = TerminationType.valueOf(terminationTypeString)

        return when {
            lookaheadType == DYNAMIC && terminationType == TIME -> MutableTimeTerminationChecker()
            lookaheadType == DYNAMIC && terminationType == EXPANSION -> DynamicExpansionTerminationChecker()
            lookaheadType == STATIC && terminationType == TIME -> StaticTimeTerminationChecker(experimentConfiguration.actionDuration)
            lookaheadType == STATIC && terminationType == EXPANSION -> StaticExpansionTerminationChecker(experimentConfiguration.actionDuration)
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
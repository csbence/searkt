@file:Suppress("DEPRECATION")

package edu.unh.cs.ai.realtimesearch.experiment.configuration

import edu.unh.cs.ai.realtimesearch.agent.ClassicalAgent
import edu.unh.cs.ai.realtimesearch.agent.RTSAgent
import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.environment.Domains.*
import edu.unh.cs.ai.realtimesearch.environment.acrobot.AcrobotIO
import edu.unh.cs.ai.realtimesearch.environment.gridworld.GridWorldEnvironment
import edu.unh.cs.ai.realtimesearch.environment.gridworld.GridWorldIO
import edu.unh.cs.ai.realtimesearch.environment.pointrobot.PointRobotEnvironment
import edu.unh.cs.ai.realtimesearch.environment.pointrobot.PointRobotIO
import edu.unh.cs.ai.realtimesearch.environment.pointrobotlost.PointRobotLOSTEnvironment
import edu.unh.cs.ai.realtimesearch.environment.pointrobotlost.PointRobotLOSTIO
import edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia.PointRobotWithInertiaEnvironment
import edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia.PointRobotWithInertiaIO
import edu.unh.cs.ai.realtimesearch.environment.racetrack.RaceTrackEnvironment
import edu.unh.cs.ai.realtimesearch.environment.racetrack.RaceTrackIO
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleEnvironment
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleIO
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldEnvironment
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldIO
import edu.unh.cs.ai.realtimesearch.experiment.AnytimeExperiment
import edu.unh.cs.ai.realtimesearch.experiment.ClassicalExperiment
import edu.unh.cs.ai.realtimesearch.experiment.RealTimeExperiment
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TimeBoundType
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.MutableTimeTerminationChecker
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.StaticTimeTerminationChecker
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TimeTerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.Planners
import edu.unh.cs.ai.realtimesearch.planner.Planners.*
import edu.unh.cs.ai.realtimesearch.planner.anytime.AnytimeRepairingAStar
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.AStarPlanner
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.ClassicalAStarPlanner
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.SimpleAStar
import edu.unh.cs.ai.realtimesearch.planner.realtime.DynamicFHatPlanner
import edu.unh.cs.ai.realtimesearch.planner.realtime.LssLrtaStarPlanner
import edu.unh.cs.ai.realtimesearch.planner.realtime.RealTimeAStarPlanner
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.NANOSECONDS

/**
 * Configuration executor to execute experiment configurations.
 */
object ConfigurationExecutor {
    fun executeConfiguration(experimentConfiguration: GeneralExperimentConfiguration): ExperimentResult {
        val logger = LoggerFactory.getLogger("ConfigurationExecutor")

        var experimentResult: ExperimentResult? = null
        var executionException: Throwable? = null

        val thread = Thread({
            experimentResult = unsafeConfigurationExecution(experimentConfiguration)
        })

        thread.setUncaughtExceptionHandler { thread, throwable ->
            executionException = throwable

            logger.info("Experiment stopped", throwable)
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

        //        val executor = Executors.newSingleThreadExecutor()
        //                // Execute the gc before every experiment.
        //                System.gc()

        //        val future = executor.submit<ExperimentResult>({
        //            unsafeConfigurationExecution(experimentConfiguration)
        //        })
        //
        //        try {
        //            val experimentResult = future.get(1, MINUTES)
        //            System.gc() // Clean up after the experiment
        //
        //            return experimentResult // Wait on the future to complete or timeout
        //        } catch (e: TimeoutException) {
        //            System.gc() // Clean up after the experiment
        //
        //            logger.info("Experiment timed out.")
        //            future.cancel(true)
        //
        //            if (!future.isCancelled && future.isDone) {
        //                logger.info("Experiment completed after the timeout before it was cancelled.")
        //
        //                return future.get()
        //            }
        //
        //            return ExperimentResult(experimentConfiguration.valueStore, "Timeout")
        //        } catch (e: ExecutionException) {
        //            System.gc() // Clean up after the experiment
        //
        //            logger.info("Experiment failed. ${e.message}")
        //            val experimentResult = ExperimentResult(experimentConfiguration.valueStore, "${e.message}")
        //            experimentResult["errorDetails"] = e.stackTrace
        //            return experimentResult
        //        } finally {
        //            executor.shutdownNow()
        //
        //            logger.info("Waiting for termination.")
        //            executor.awaitTermination(1, MINUTES)
        //            logger.info("Terminated.")
        //            System.gc()
        //
        //        }
    }

    private fun collectAndWait() {
        System.gc()
        Thread.sleep(500)
    }

    private fun unsafeConfigurationExecution(experimentConfiguration: GeneralExperimentConfiguration): ExperimentResult? {
        val domainName: String = experimentConfiguration.domainName
        val domain = Domains.valueOf(domainName)
        return when (domain) {
            SLIDING_TILE_PUZZLE_4 -> executeSlidingTilePuzzle(experimentConfiguration)
            VACUUM_WORLD -> executeVacuumWorld(experimentConfiguration)
            GRID_WORLD -> executeGridWorld(experimentConfiguration)
            ACROBOT -> executeAcrobot(experimentConfiguration)
            POINT_ROBOT -> executePointRobot(experimentConfiguration)
            POINT_ROBOT_LOST -> executePointRobotLOST(experimentConfiguration)
            POINT_ROBOT_WITH_INERTIA -> executePointRobotWithInertia(experimentConfiguration)
            RACETRACK -> executeRaceTrack(experimentConfiguration)

            else -> ExperimentResult(experimentConfiguration.valueStore, errorMessage = "Unknown domain type: $domainName")
        }
    }

    private fun executePointRobot(experimentConfiguration: GeneralExperimentConfiguration): ExperimentResult {
        val rawDomain: String = experimentConfiguration.rawDomain
        val pointRobotInstance = PointRobotIO.parseFromStream(rawDomain.byteInputStream(), experimentConfiguration.actionDuration)
        val pointRobotEnvironment = PointRobotEnvironment(pointRobotInstance.domain, pointRobotInstance.initialState)

        return executeDomain(experimentConfiguration, pointRobotInstance.domain, pointRobotInstance.initialState, pointRobotEnvironment)
    }

    private fun executePointRobotLOST(experimentConfiguration: GeneralExperimentConfiguration): ExperimentResult {
        val rawDomain: String = experimentConfiguration.rawDomain
        val pointRobotLOSTInstance = PointRobotLOSTIO.parseFromStream(rawDomain.byteInputStream(), experimentConfiguration.actionDuration)
        val pointRobotLOSTEnvironment = PointRobotLOSTEnvironment(pointRobotLOSTInstance.domain, pointRobotLOSTInstance.initialState)

        return executeDomain(experimentConfiguration, pointRobotLOSTInstance.domain, pointRobotLOSTInstance.initialState, pointRobotLOSTEnvironment)
    }

    private fun executePointRobotWithInertia(experimentConfiguration: GeneralExperimentConfiguration): ExperimentResult {
        val rawDomain: String = experimentConfiguration.rawDomain
        val pointRobotWithInertiaInstance = PointRobotWithInertiaIO.parseFromStream(rawDomain.byteInputStream(), experimentConfiguration.actionDuration)
        val discretizedDomain = DiscretizedDomain(pointRobotWithInertiaInstance.domain)
        val discretizedInitialState = DiscretizedState(pointRobotWithInertiaInstance.initialState)
        val pointRobotWithInertiaEnvironment = DiscretizedEnvironment(discretizedDomain, discretizedInitialState)

        return executeDomain(experimentConfiguration, discretizedDomain, discretizedInitialState, pointRobotWithInertiaEnvironment)
    }

    private fun executeVacuumWorld(experimentConfiguration: GeneralExperimentConfiguration): ExperimentResult {
        val rawDomain: String = experimentConfiguration.rawDomain
        val vacuumWorldInstance = VacuumWorldIO.parseFromStream(rawDomain.byteInputStream())
        val vacuumWorldEnvironment = VacuumWorldEnvironment(vacuumWorldInstance.domain, vacuumWorldInstance.initialState)

        return executeDomain(experimentConfiguration, vacuumWorldInstance.domain, vacuumWorldInstance.initialState, vacuumWorldEnvironment)
    }

    private fun executeRaceTrack(experimentConfiguration: GeneralExperimentConfiguration): ExperimentResult {
        val rawDomain: String = experimentConfiguration.rawDomain
        val raceTrackInstance = RaceTrackIO.parseFromStream(rawDomain.byteInputStream(), experimentConfiguration.actionDuration)
        val raceTrackEnvironment = RaceTrackEnvironment(raceTrackInstance.domain, raceTrackInstance.initialState)

        return executeDomain(experimentConfiguration, raceTrackInstance.domain, raceTrackInstance.initialState, raceTrackEnvironment)
    }

    private fun executeGridWorld(experimentConfiguration: GeneralExperimentConfiguration): ExperimentResult {
        val rawDomain: String = experimentConfiguration.rawDomain
        val gridWorldInstance = GridWorldIO.parseFromStream(rawDomain.byteInputStream(), experimentConfiguration.actionDuration)
        val gridWorldEnvironment = GridWorldEnvironment(gridWorldInstance.domain, gridWorldInstance.initialState)

        return executeDomain(experimentConfiguration, gridWorldInstance.domain, gridWorldInstance.initialState, gridWorldEnvironment)
    }

    private fun executeSlidingTilePuzzle(experimentConfiguration: GeneralExperimentConfiguration): ExperimentResult {
        val rawDomain: String = experimentConfiguration.rawDomain
        val slidingTilePuzzleInstance = SlidingTilePuzzleIO.parseFromStream(rawDomain.byteInputStream(), experimentConfiguration.actionDuration)
        val slidingTilePuzzleEnvironment = SlidingTilePuzzleEnvironment(slidingTilePuzzleInstance.domain, slidingTilePuzzleInstance.initialState)

        return executeDomain(experimentConfiguration, slidingTilePuzzleInstance.domain, slidingTilePuzzleInstance.initialState, slidingTilePuzzleEnvironment)
    }

    private fun executeAcrobot(experimentConfiguration: GeneralExperimentConfiguration): ExperimentResult {
        val rawDomain: String = experimentConfiguration.rawDomain
        val acrobotInstance = AcrobotIO.parseFromStream(rawDomain.byteInputStream(), experimentConfiguration.actionDuration)
        val acrobotEnvironment = DiscretizedEnvironment(acrobotInstance.domain, acrobotInstance.initialState)

        return executeDomain(experimentConfiguration, acrobotInstance.domain, acrobotInstance.initialState, acrobotEnvironment)
    }

    private fun <StateType : State<StateType>> executeDomain(experimentConfiguration: GeneralExperimentConfiguration, domain: Domain<StateType>, initialState: StateType, environment: Environment<StateType>): ExperimentResult {
        val algorithmName = experimentConfiguration.algorithmName

        return when (Planners.valueOf(algorithmName)) {
            WEIGHTED_A_STAR -> executeWeightedAStar(experimentConfiguration, domain, initialState)
            A_STAR -> executeAStar(experimentConfiguration, domain, initialState)
            LSS_LRTA_STAR -> executeLssLrtaStar(experimentConfiguration, domain, environment)
            DYNAMIC_F_HAT -> executeDynamicFHat(experimentConfiguration, domain, environment)
            RTA_STAR -> executeRealTimeAStar(experimentConfiguration, domain, environment)
            ARA_STAR -> executeAnytimeRepairingAStar(experimentConfiguration, domain, environment)
            else -> ExperimentResult(experimentConfiguration.valueStore, errorMessage = "Unknown algorithm: $algorithmName")
        }
    }

    private fun <StateType : State<StateType>> executePureAStar(experimentConfiguration: GeneralExperimentConfiguration, domain: Domain<StateType>, initialState: StateType): ExperimentResult {
        val aStarPlanner = SimpleAStar(domain)
        aStarPlanner.search(initialState)

        return ExperimentResult(experimentConfiguration.valueStore, errorMessage = "Incompatible output format.")
    }

    private fun <StateType : State<StateType>> executeAStar(experimentConfiguration: GeneralExperimentConfiguration, domain: Domain<StateType>, initialState: State<StateType>): ExperimentResult {
        val aStarPlanner = AStarPlanner(domain, 1.0)
        val classicalAgent = ClassicalAgent(aStarPlanner)
        val classicalExperiment = ClassicalExperiment(experimentConfiguration, classicalAgent, domain, initialState)

        return classicalExperiment.run()
    }

    private fun <StateType : State<StateType>> executeWeightedAStar(experimentConfiguration: GeneralExperimentConfiguration, domain: Domain<StateType>, initialState: State<StateType>): ExperimentResult {
        val weight = experimentConfiguration.getTypedValue<Double>(Configurations.WEIGHT.toString()) ?: throw InvalidFieldException("\"${Configurations.WEIGHT}\" is not found. Please add it the the experiment configuration.")
        val aStarPlanner = ClassicalAStarPlanner(domain, weight)
        val classicalAgent = ClassicalAgent(aStarPlanner)
        val classicalExperiment = ClassicalExperiment(experimentConfiguration, classicalAgent, domain, initialState)

        return classicalExperiment.run()
    }

    private fun <StateType : State<StateType>> executeClassicalAStar(experimentConfiguration: GeneralExperimentConfiguration, domain: Domain<StateType>, initialState: State<StateType>): ExperimentResult {
        val aStarPlanner = ClassicalAStarPlanner(domain, 1.0)
        val classicalAgent = ClassicalAgent(aStarPlanner)
        val classicalExperiment = ClassicalExperiment(experimentConfiguration, classicalAgent, domain, initialState)

        return classicalExperiment.run()
    }

    private fun <StateType : State<StateType>> executeLssLrtaStar(experimentConfiguration: GeneralExperimentConfiguration, domain: Domain<StateType>, environment: Environment<StateType>): ExperimentResult {
        val lssLrtaPlanner = LssLrtaStarPlanner(domain)
        val rtsAgent = RTSAgent(lssLrtaPlanner)
        val rtsExperiment = RealTimeExperiment(experimentConfiguration, rtsAgent, environment, getTerminationChecker(experimentConfiguration))

        return rtsExperiment.run()
    }

    private fun <StateType : State<StateType>> executeDynamicFHat(experimentConfiguration: GeneralExperimentConfiguration, domain: Domain<StateType>, environment: Environment<StateType>): ExperimentResult {
        val dynamicFHatPlanner = DynamicFHatPlanner(domain)
        val rtsAgent = RTSAgent(dynamicFHatPlanner)
        val rtsExperiment = RealTimeExperiment(experimentConfiguration, rtsAgent, environment, getTerminationChecker(experimentConfiguration))

        return rtsExperiment.run()
    }

    private fun getTerminationChecker(experimentConfiguration: GeneralExperimentConfiguration): TimeTerminationChecker {
        val timeBoundTypeString = experimentConfiguration.getTypedValue<String>(Configurations.TIME_BOUND_TYPE.toString()) ?: throw  InvalidFieldException("\"${Configurations.TIME_BOUND_TYPE}\" is not found. Please add it to the configuration.")
        val timeBoundType: TimeBoundType = TimeBoundType.valueOf(timeBoundTypeString)

        return when (timeBoundType) {
            TimeBoundType.DYNAMIC -> MutableTimeTerminationChecker()
            TimeBoundType.STATIC -> StaticTimeTerminationChecker(experimentConfiguration.getTypedValue<Long>(Configurations.ACTION_DURATION.toString()) ?: throw  InvalidFieldException("\"${Configurations.ACTION_DURATION}\" is not found. Please add it the the experiment configuration."))
        }
    }

    private fun <StateType : State<StateType>> executeRealTimeAStar(experimentConfiguration: GeneralExperimentConfiguration, domain: Domain<StateType>, environment: Environment<StateType>): ExperimentResult {
        val depthLimit = experimentConfiguration.getTypedValue<Long>(Configurations.LOOKAHEAD_DEPTH_LIMIT.toString()) ?: throw InvalidFieldException("\"${Configurations.LOOKAHEAD_DEPTH_LIMIT}\" is not found. Please add it to the experiment configuration.")
        val realTimeAStarPlanner = RealTimeAStarPlanner(domain, depthLimit.toInt())
        val rtsAgent = RTSAgent(realTimeAStarPlanner)
        val rtsExperiment = RealTimeExperiment(experimentConfiguration, rtsAgent, environment, getTerminationChecker(experimentConfiguration))

        return rtsExperiment.run()
    }

    private fun <StateType : State<StateType>> executeAnytimeRepairingAStar(experimentConfiguration: GeneralExperimentConfiguration, domain: Domain<StateType>, environment: Environment<StateType>): ExperimentResult {
        //val depthLimit = experimentConfiguration.getTypedValue<Int>("lookahead depth limit") ?: throw InvalidFieldException("\"lookahead depth limit\" is not found. Please add it to the experiment configuration.")
        val anytimeRepairingAStarPlanner = AnytimeRepairingAStar(domain)
        /*val atsAgent = ATSAgent(anytimeRepairingAStarPlanner)*/
        val atsExperiment = AnytimeExperiment(anytimeRepairingAStarPlanner, experimentConfiguration, environment)

        return atsExperiment.run()
    }

}
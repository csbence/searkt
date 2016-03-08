package edu.unh.cs.ai.realtimesearch.experiment.configuration

import edu.unh.cs.ai.realtimesearch.agent.ClassicalAgent
import edu.unh.cs.ai.realtimesearch.agent.RTSAgent
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.Environment
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.gridworld.GridWorldEnvironment
import edu.unh.cs.ai.realtimesearch.environment.gridworld.GridWorldIO
import edu.unh.cs.ai.realtimesearch.environment.pointrobot.PointRobotEnvironment
import edu.unh.cs.ai.realtimesearch.environment.pointrobot.PointRobotIO
import edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia.PointRobotWithInertiaEnvironment
import edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia.PointRobotWithInertiaIO
import edu.unh.cs.ai.realtimesearch.environment.racetrack.RaceTrackEnvironment
import edu.unh.cs.ai.realtimesearch.environment.racetrack.RaceTrackIO
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleEnvironment
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleIO
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldEnvironment
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldIO
import edu.unh.cs.ai.realtimesearch.experiment.ClassicalExperiment
import edu.unh.cs.ai.realtimesearch.experiment.RTSExperiment
import edu.unh.cs.ai.realtimesearch.experiment.TerminationChecker
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.CallsTerminationChecker
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TimeTerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.ClassicalAStarPlanner
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.SimpleAStar
import edu.unh.cs.ai.realtimesearch.planner.realtime.LssLrtaStarPlanner
import edu.unh.cs.ai.realtimesearch.planner.realtime.RealTimeAStarPlanner

/**
 * Configuration executor to execute experiment configurations.
 */
object ConfigurationExecutor {
    fun executeConfiguration(experimentConfiguration: ExperimentConfiguration): ExperimentResult {
        val domainName: String = experimentConfiguration.domainName

        return when (domainName) {
            "sliding tile puzzle" -> executeSlidingTilePuzzle(experimentConfiguration)
            "vacuum world" -> executeVacuumWorld(experimentConfiguration)
            "grid world" -> executeGridWorld(experimentConfiguration)
            "race track" -> executeRaceTrack(experimentConfiguration)
            "point robot" -> executePointRobot(experimentConfiguration)
            "point robot with inertia" -> executePointRobotWithInertia(experimentConfiguration)
            else -> ExperimentResult(experimentConfiguration, errorMessage = "Unknown domain type: $domainName")
        }
    }

    private fun executeRaceTrack(experimentConfiguration: ExperimentConfiguration): ExperimentResult {
        val rawDomain: String = experimentConfiguration.rawDomain
        val raceTrackInstance = RaceTrackIO.parseFromStream(rawDomain.byteInputStream())
        val raceTrackEnvironment = RaceTrackEnvironment(raceTrackInstance.domain, raceTrackInstance.initialState)

        return executeDomain(experimentConfiguration, raceTrackInstance.domain, raceTrackInstance.initialState, raceTrackEnvironment)
    }

    private fun executePointRobot(experimentConfiguration: ExperimentConfiguration): ExperimentResult {
        val rawDomain: String = experimentConfiguration.rawDomain
        val pointRobotInstance = PointRobotIO.parseFromStream(rawDomain.byteInputStream())
        val pointRobotEnvironment = PointRobotEnvironment(pointRobotInstance.domain, pointRobotInstance.initialState)

        return executeDomain(experimentConfiguration, pointRobotInstance.domain, pointRobotInstance.initialState, pointRobotEnvironment)
    }

    private fun executePointRobotWithInertia(experimentConfiguration: ExperimentConfiguration): ExperimentResult {
        val rawDomain: String = experimentConfiguration.rawDomain
        val pointRobotWithInertiaInstance = PointRobotWithInertiaIO.parseFromStream(rawDomain.byteInputStream())
        val pointRobotWithInertiaEnvironment = PointRobotWithInertiaEnvironment(pointRobotWithInertiaInstance.domain, pointRobotWithInertiaInstance.initialState)

        return executeDomain(experimentConfiguration, pointRobotWithInertiaInstance.domain, pointRobotWithInertiaInstance.initialState, pointRobotWithInertiaEnvironment)
    }

    private fun executeVacuumWorld(experimentConfiguration: ExperimentConfiguration): ExperimentResult {
        val rawDomain: String = experimentConfiguration.rawDomain
        val vacuumWorldInstance = VacuumWorldIO.parseFromStream(rawDomain.byteInputStream())
        val vacuumWorldEnvironment = VacuumWorldEnvironment(vacuumWorldInstance.domain, vacuumWorldInstance.initialState)

        return executeDomain(experimentConfiguration, vacuumWorldInstance.domain, vacuumWorldInstance.initialState, vacuumWorldEnvironment)
    }

    private fun executeGridWorld(experimentConfiguration: ExperimentConfiguration): ExperimentResult {
        val rawDomain: String = experimentConfiguration.rawDomain
        val actionDuration = experimentConfiguration.getTypedValue<Long>("action duration") ?: throw InvalidConfigurationException("\"action duration\" is not found. Please add it the the experiment configuration.")
        val gridWorldInstance = GridWorldIO.parseFromStream(rawDomain.byteInputStream(), actionDuration)
        val gridWorldEnvironment = GridWorldEnvironment(gridWorldInstance.domain, gridWorldInstance.initialState)

        return executeDomain(experimentConfiguration, gridWorldInstance.domain, gridWorldInstance.initialState, gridWorldEnvironment)
    }

    private fun executeSlidingTilePuzzle(experimentConfiguration: ExperimentConfiguration): ExperimentResult {
        val rawDomain: String = experimentConfiguration.rawDomain
        val slidingTilePuzzleInstance = SlidingTilePuzzleIO.parseFromStream(rawDomain.byteInputStream())
        val slidingTilePuzzleEnvironment = SlidingTilePuzzleEnvironment(slidingTilePuzzleInstance.domain, slidingTilePuzzleInstance.initialState)

        return executeDomain(experimentConfiguration, slidingTilePuzzleInstance.domain, slidingTilePuzzleInstance.initialState, slidingTilePuzzleEnvironment)
    }

    private fun <StateType : State<StateType>> executeDomain(experimentConfiguration: ExperimentConfiguration, domain: Domain<StateType>, initialState: State<StateType>, environment: Environment<StateType>): ExperimentResult {
        val algorithmName = experimentConfiguration.algorithmName

        return when (algorithmName) {
            "A*" -> executeAStar(experimentConfiguration, domain, initialState, environment)
            "LSS-LRTA*" -> executeLssLrtaStar(experimentConfiguration, domain, initialState, environment)
            "RTA*" -> executeRealTimeAStar(experimentConfiguration, domain, initialState, environment)
            "Simple-A*" -> executePureAStar(experimentConfiguration, domain, initialState, environment)
            "Classical-A*" -> executeClassicalAStar(experimentConfiguration, domain, initialState, environment)

            else -> ExperimentResult(experimentConfiguration, errorMessage = "Unknown algorithm: $algorithmName")
        }
    }

    private fun <StateType : State<StateType>> executePureAStar(experimentConfiguration: ExperimentConfiguration, domain: Domain<StateType>, initialState: State<StateType>, environment: Environment<StateType>): ExperimentResult {
        val aStarPlanner = SimpleAStar(domain)

        val state: StateType = environment.getState()
        aStarPlanner.search(state)

        return ExperimentResult(experimentConfiguration, errorMessage = "Incompatible output format.")
    }

    private fun <StateType : State<StateType>> executeAStar(experimentConfiguration: ExperimentConfiguration, domain: Domain<StateType>, initialState: State<StateType>, environment: Environment<StateType>): ExperimentResult {
        val aStarPlanner = ClassicalAStarPlanner(domain)
        val classicalAgent = ClassicalAgent(aStarPlanner)
        val classicalExperiment = ClassicalExperiment(experimentConfiguration, classicalAgent, domain, initialState)

        return classicalExperiment.run()
    }

    private fun <StateType : State<StateType>> executeClassicalAStar(experimentConfiguration: ExperimentConfiguration, domain: Domain<StateType>, initialState: State<StateType>, environment: Environment<StateType>): ExperimentResult {
        val aStarPlanner = ClassicalAStarPlanner(domain)
        val classicalAgent = ClassicalAgent(aStarPlanner)
        val classicalExperiment = ClassicalExperiment(experimentConfiguration, classicalAgent, domain, initialState)

        return classicalExperiment.run()
    }

    private fun <StateType : State<StateType>> executeLssLrtaStar(experimentConfiguration: ExperimentConfiguration, domain: Domain<StateType>, initialState: State<StateType>, environment: Environment<StateType>): ExperimentResult {
        val lssLrtaPlanner = LssLrtaStarPlanner(domain)
        val rtsAgent = RTSAgent(lssLrtaPlanner)
        val rtsExperiment = RTSExperiment(experimentConfiguration, rtsAgent, environment, getTerminationChecker(experimentConfiguration))

        return rtsExperiment.run()
    }

    private fun getTerminationChecker(experimentConfiguration: ExperimentConfiguration): TerminationChecker {
        val terminationCheckerType = experimentConfiguration.terminationCheckerType
        val terminationCheckerParameter = experimentConfiguration.terminationCheckerParameter

        return when (terminationCheckerType) {
            "time" -> TimeTerminationChecker(terminationCheckerParameter.toDouble())
            "calls" -> CallsTerminationChecker(terminationCheckerParameter)

            else -> throw RuntimeException("Invalid termination checker: $terminationCheckerType")
        }
    }

    private fun <StateType : State<StateType>> executeRealTimeAStar(experimentConfiguration: ExperimentConfiguration, domain: Domain<StateType>, initialState: State<StateType>, environment: Environment<StateType>): ExperimentResult {
        val depthLimit = experimentConfiguration.getTypedValue<Int>("lookahead depth limit") ?: throw InvalidConfigurationException("\"lookahead depth limit\" is not found. Please add it to the experiment configuration.")
        val realTimeAStarPlanner = RealTimeAStarPlanner(domain, depthLimit)
        val rtsAgent = RTSAgent(realTimeAStarPlanner)
        val rtsExperiment = RTSExperiment(experimentConfiguration, rtsAgent, environment, getTerminationChecker(experimentConfiguration))

        return rtsExperiment.run()
    }

}
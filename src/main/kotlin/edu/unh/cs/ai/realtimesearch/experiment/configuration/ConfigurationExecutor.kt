package edu.unh.cs.ai.realtimesearch.experiment.configuration

import edu.unh.cs.ai.realtimesearch.agent.ClassicalAgent
import edu.unh.cs.ai.realtimesearch.agent.RTSAgent
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.Environment
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.gridworld.GridWorldEnvironment
import edu.unh.cs.ai.realtimesearch.environment.gridworld.GridWorldIO
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
    fun executeConfiguration(experimentConfiguration: GeneralExperimentConfiguration): ExperimentResult {
        val domainName: String = experimentConfiguration.domainName

        return when (domainName) {
            "sliding tile puzzle" -> executeSlidingTilePuzzle(experimentConfiguration)
            "vacuum world" -> executeVacuumWorld(experimentConfiguration)
            "grid world" -> executeGridWorld(experimentConfiguration)
            else -> ExperimentResult(experimentConfiguration.valueStore, errorMessage = "Unknown domain type: $domainName")
        }
    }

    private fun executeVacuumWorld(experimentConfiguration: GeneralExperimentConfiguration): ExperimentResult {
        val rawDomain: String = experimentConfiguration.rawDomain
        val vacuumWorldInstance = VacuumWorldIO.parseFromStream(rawDomain.byteInputStream())
        val vacuumWorldEnvironment = VacuumWorldEnvironment(vacuumWorldInstance.domain, vacuumWorldInstance.initialState)

        return executeDomain(experimentConfiguration, vacuumWorldInstance.domain, vacuumWorldInstance.initialState, vacuumWorldEnvironment)
    }

    private fun executeGridWorld(experimentConfiguration: GeneralExperimentConfiguration): ExperimentResult {
        val rawDomain: String = experimentConfiguration.rawDomain
        val actionDuration = experimentConfiguration.getTypedValue<Long>("action duration") ?: throw InvalidConfigurationException("\"action duration\" is not found. Please add it the the experiment configuration.")
        val gridWorldInstance = GridWorldIO.parseFromStream(rawDomain.byteInputStream(), actionDuration)
        val gridWorldEnvironment = GridWorldEnvironment(gridWorldInstance.domain, gridWorldInstance.initialState)

        return executeDomain(experimentConfiguration, gridWorldInstance.domain, gridWorldInstance.initialState, gridWorldEnvironment)
    }

    private fun executeSlidingTilePuzzle(experimentConfiguration: GeneralExperimentConfiguration): ExperimentResult {
        val rawDomain: String = experimentConfiguration.rawDomain
        val slidingTilePuzzleInstance = SlidingTilePuzzleIO.parseFromStream(rawDomain.byteInputStream())
        val slidingTilePuzzleEnvironment = SlidingTilePuzzleEnvironment(slidingTilePuzzleInstance.domain, slidingTilePuzzleInstance.initialState)

        return executeDomain(experimentConfiguration, slidingTilePuzzleInstance.domain, slidingTilePuzzleInstance.initialState, slidingTilePuzzleEnvironment)
    }

    private fun <StateType : State<StateType>> executeDomain(experimentConfiguration: GeneralExperimentConfiguration, domain: Domain<StateType>, initialState: State<StateType>, environment: Environment<StateType>): ExperimentResult {
        val algorithmName = experimentConfiguration.algorithmName

        return when (algorithmName) {
            "A*" -> executeAStar(experimentConfiguration, domain, initialState, environment)
            "LSS-LRTA*" -> executeLssLrtaStar(experimentConfiguration, domain, initialState, environment)
            "RTA*" -> executeRealTimeAStar(experimentConfiguration, domain, initialState, environment)
            "Simple-A*" -> executePureAStar(experimentConfiguration, domain, initialState, environment)
            "Classical-A*" -> executeClassicalAStar(experimentConfiguration, domain, initialState, environment)

            else -> ExperimentResult(experimentConfiguration.valueStore, errorMessage = "Unknown algorithm: $algorithmName")
        }
    }

    private fun <StateType : State<StateType>> executePureAStar(experimentConfiguration: GeneralExperimentConfiguration, domain: Domain<StateType>, initialState: State<StateType>, environment: Environment<StateType>): ExperimentResult {
        val aStarPlanner = SimpleAStar(domain)

        val state: StateType = environment.getState()
        aStarPlanner.search(state)

        return ExperimentResult(experimentConfiguration.valueStore, errorMessage = "Incompatible output format.")
    }

    private fun <StateType : State<StateType>> executeAStar(experimentConfiguration: GeneralExperimentConfiguration, domain: Domain<StateType>, initialState: State<StateType>, environment: Environment<StateType>): ExperimentResult {
        val aStarPlanner = ClassicalAStarPlanner(domain)
        val classicalAgent = ClassicalAgent(aStarPlanner)
        val classicalExperiment = ClassicalExperiment(experimentConfiguration, classicalAgent, domain, initialState)

        return classicalExperiment.run()
    }

    private fun <StateType : State<StateType>> executeClassicalAStar(experimentConfiguration: GeneralExperimentConfiguration, domain: Domain<StateType>, initialState: State<StateType>, environment: Environment<StateType>): ExperimentResult {
        val aStarPlanner = ClassicalAStarPlanner(domain)
        val classicalAgent = ClassicalAgent(aStarPlanner)
        val classicalExperiment = ClassicalExperiment(experimentConfiguration, classicalAgent, domain, initialState)

        return classicalExperiment.run()
    }

    private fun <StateType : State<StateType>> executeLssLrtaStar(experimentConfiguration: GeneralExperimentConfiguration, domain: Domain<StateType>, initialState: State<StateType>, environment: Environment<StateType>): ExperimentResult {
        val lssLrtaPlanner = LssLrtaStarPlanner(domain)
        val rtsAgent = RTSAgent(lssLrtaPlanner)
        val rtsExperiment = RTSExperiment(experimentConfiguration, rtsAgent, environment, getTerminationChecker(experimentConfiguration))

        return rtsExperiment.run()
    }

    private fun getTerminationChecker(experimentConfiguration: GeneralExperimentConfiguration): TerminationChecker {
        val terminationCheckerType = experimentConfiguration.terminationCheckerType
        val terminationCheckerParameter = experimentConfiguration.terminationCheckerParameter

        return when (terminationCheckerType) {
            "time" -> TimeTerminationChecker(terminationCheckerParameter.toDouble())
            "calls" -> CallsTerminationChecker(terminationCheckerParameter)

            else -> throw RuntimeException("Invalid termination checker: $terminationCheckerType")
        }
    }

    private fun <StateType : State<StateType>> executeRealTimeAStar(experimentConfiguration: GeneralExperimentConfiguration, domain: Domain<StateType>, initialState: State<StateType>, environment: Environment<StateType>): ExperimentResult {
        val depthLimit = experimentConfiguration.getTypedValue<Int>("lookahead depth limit") ?: throw InvalidConfigurationException("\"lookahead depth limit\" is not found. Please add it to the experiment configuration.")
        val realTimeAStarPlanner = RealTimeAStarPlanner(domain, depthLimit)
        val rtsAgent = RTSAgent(realTimeAStarPlanner)
        val rtsExperiment = RTSExperiment(experimentConfiguration, rtsAgent, environment, getTerminationChecker(experimentConfiguration))

        return rtsExperiment.run()
    }

}
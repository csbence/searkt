package edu.unh.cs.ai.realtimesearch.experiment.configuration

import edu.unh.cs.ai.realtimesearch.agent.ClassicalAgent
import edu.unh.cs.ai.realtimesearch.agent.RTSAgent
import edu.unh.cs.ai.realtimesearch.environment.DiscretizedEnvironment
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.Environment
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.acrobot.AcrobotIO
import edu.unh.cs.ai.realtimesearch.environment.gridworld.GridWorldEnvironment
import edu.unh.cs.ai.realtimesearch.environment.gridworld.GridWorldIO
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleEnvironment
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleIO
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldEnvironment
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldIO
import edu.unh.cs.ai.realtimesearch.experiment.ClassicalExperiment
import edu.unh.cs.ai.realtimesearch.experiment.RTSExperiment
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TimeBoundType
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.MutableTimeTerminationChecker
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.StaticTimeTerminationChecker
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TimeTerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.AStarPlanner
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.ClassicalAStarPlanner
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.SimpleAStar
import edu.unh.cs.ai.realtimesearch.planner.realtime.DynamicFHatPlanner
import edu.unh.cs.ai.realtimesearch.planner.realtime.LssLrtaStarPlanner
import edu.unh.cs.ai.realtimesearch.planner.realtime.RealTimeAStarPlanner

/**
 * Configuration executor to execute experiment configurations.
 */
object ConfigurationExecutor {
    fun executeConfiguration(experimentConfiguration: GeneralExperimentConfiguration): ExperimentResult {
        val domainName: String = experimentConfiguration.domainName

        // Execute the gc before every experiment.
        System.gc()

        try {
            return when (domainName) {
                "sliding tile puzzle" -> executeSlidingTilePuzzle(experimentConfiguration)
                "vacuum world" -> executeVacuumWorld(experimentConfiguration)
                "grid world" -> executeGridWorld(experimentConfiguration)
                "acrobot" -> executeAcrobot(experimentConfiguration)
                else -> ExperimentResult(experimentConfiguration.valueStore, errorMessage = "Unknown domain type: $domainName")
            }
        } catch (e: OutOfMemoryError) {
            System.gc()
            return ExperimentResult(experimentConfiguration.valueStore, "OutOfMemory")
        } catch (e: Exception) {
            return ExperimentResult(experimentConfiguration.valueStore, e.message)
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
        val actionDuration = experimentConfiguration.getTypedValue<Long>("action duration") ?: throw InvalidFieldException("\"action duration\" is not found. Please add it to the experiment configuration.")
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

    private fun executeAcrobot(experimentConfiguration: GeneralExperimentConfiguration): ExperimentResult {
        val rawDomain: String = experimentConfiguration.rawDomain
        val acrobotInstance = AcrobotIO.parseFromStream(rawDomain.byteInputStream())
        val acrobotEnvironment = DiscretizedEnvironment(acrobotInstance.domain, acrobotInstance.initialState)

        return executeDomain(experimentConfiguration, acrobotInstance.domain, acrobotInstance.initialState, acrobotEnvironment)
    }

    private fun <StateType : State<StateType>> executeDomain(experimentConfiguration: GeneralExperimentConfiguration, domain: Domain<StateType>, initialState: StateType, environment: Environment<StateType>): ExperimentResult {
        val algorithmName = experimentConfiguration.algorithmName

        return when (algorithmName) {
            "Weighted-A*" -> executeWeightedAStar(experimentConfiguration, domain, initialState)
            "A*" -> executeAStar(experimentConfiguration, domain, initialState)
            "LSS-LRTA*" -> executeLssLrtaStar(experimentConfiguration, domain, environment)
            "Dynamic-fhat" -> executeDynamicFHat(experimentConfiguration, domain, environment)
            "RTA*" -> executeRealTimeAStar(experimentConfiguration, domain, environment)
            "Simple-A*" -> executePureAStar(experimentConfiguration, domain, initialState)
            "Classical-A*" -> executeClassicalAStar(experimentConfiguration, domain, initialState)

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
        val weight = experimentConfiguration.getTypedValue<Double>("weight") ?: throw InvalidFieldException("\"weight\" is not found. Please add it the the experiment configuration.")
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
        val rtsExperiment = RTSExperiment(experimentConfiguration, rtsAgent, environment, getTerminationChecker(experimentConfiguration))

        return rtsExperiment.run()
    }

    private fun <StateType : State<StateType>> executeDynamicFHat(experimentConfiguration: GeneralExperimentConfiguration, domain: Domain<StateType>, environment: Environment<StateType>): ExperimentResult {
        val dynamicFHatPlanner = DynamicFHatPlanner(domain)
        val rtsAgent = RTSAgent(dynamicFHatPlanner)
        val rtsExperiment = RTSExperiment(experimentConfiguration, rtsAgent, environment, getTerminationChecker(experimentConfiguration))

        return rtsExperiment.run()
    }

    private fun getTerminationChecker(experimentConfiguration: GeneralExperimentConfiguration): TimeTerminationChecker {
        val timeBoundTypeString = experimentConfiguration.getTypedValue<String>("timeBoundType") ?: throw  InvalidFieldException("timeBoundType is not found. Please add it to the configuration.")
        val timeBoundType: TimeBoundType = TimeBoundType.valueOf(timeBoundTypeString)

        return when (timeBoundType) {
            TimeBoundType.DYNAMIC -> MutableTimeTerminationChecker()
            TimeBoundType.STATIC -> StaticTimeTerminationChecker(experimentConfiguration.getTypedValue<Long>("staticStepDuration") ?: throw  InvalidFieldException("\"staticStepDuration\" is not found. Please add it the the experiment configuration."))
        }
    }

    private fun <StateType : State<StateType>> executeRealTimeAStar(experimentConfiguration: GeneralExperimentConfiguration, domain: Domain<StateType>, environment: Environment<StateType>): ExperimentResult {
        val depthLimit = experimentConfiguration.getTypedValue<Long>("lookahead depth limit") ?: throw InvalidFieldException("\"lookahead depth limit\" is not found. Please add it to the experiment configuration.")
        val realTimeAStarPlanner = RealTimeAStarPlanner(domain, depthLimit.toInt())
        val rtsAgent = RTSAgent(realTimeAStarPlanner)
        val rtsExperiment = RTSExperiment(experimentConfiguration, rtsAgent, environment, getTerminationChecker(experimentConfiguration))

        return rtsExperiment.run()
    }

}
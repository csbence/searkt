package edu.unh.cs.ai.realtimesearch.experiment

import edu.unh.cs.ai.realtimesearch.environment.Environment
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.InvalidFieldException
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import edu.unh.cs.ai.realtimesearch.logging.debug
import edu.unh.cs.ai.realtimesearch.logging.info
import edu.unh.cs.ai.realtimesearch.planner.anytime.AnytimeRepairingAStar
import edu.unh.cs.ai.realtimesearch.util.convertNanoUpDouble
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * An RTS experiment repeatedly queries the agent
 * for an action by some constraint (allowed time for example).
 * After each selected action, the experiment then applies this action
 * to its environment.
 *
 * The states are given by the environment, the world. When creating the world
 * it might be possible to determine what the initial state is.
 *
 * NOTE: assumes the same domain is used to create both the agent as the world
 *
 * @param world is the environment
 */
class ATSExperiment<StateType : State<StateType>>(val planner: AnytimeRepairingAStar<StateType>,
                                                  val experimentConfiguration: GeneralExperimentConfiguration,
        /*val agent: RTSAgent<StateType>,*/
                                                  val world: Environment<StateType>
        /*val terminationChecker: TimeTerminationChecker*/) : Experiment() {

    private val logger = LoggerFactory.getLogger(ATSExperiment::class.java)

    /**
     * Runs the experiment
     */
    override fun run(): ExperimentResult {
        val actions: MutableList<String> = arrayListOf()
        val actionsLists: MutableList<String> = arrayListOf()
        var actionList: MutableList<Action?> = arrayListOf()
//        val maxCount = 6
        val maxCount: Long = experimentConfiguration.getTypedValue<Long>(Configurations.ANYTIME_MAX_COUNT.toString()) ?: throw InvalidFieldException("\"${Configurations.ANYTIME_MAX_COUNT}\" is not found. Please add it to the experiment configuration.")

        logger.info { "Starting experiment from state ${world.getState()}" }
        var totalPlanningNanoTime = 1L
        //var timeBound = staticStepDuration

        while (!world.isGoal()) {
            //print("" + world.getState() + " " + world.getGoal() + " ")
            logger.debug { "start ATS" }
            val startTime = System.nanoTime()

            val tempActions = planner.solve(world.getState(), world.getGoal());
            val endTime = System.nanoTime()
            logger.debug { "time: " + (endTime - startTime) }
            if(actions.size == 0) {
                totalPlanningNanoTime = endTime - startTime
                actionList = tempActions
            }
            else if(experimentConfiguration.actionDuration * maxCount < endTime - startTime){
                println("Planning took too long! Use old plan.")
                for(i in 1..maxCount){
                    actionList.removeAt(0)
                }
            }
            else{
                actionList = tempActions
            }

            logger.debug { "Agent return actions: |${actionList.size}| to state ${world.getState()}" }

            val update = planner.update()
            if (update < 1.0) {
                actionList.forEach {
                    if (it/*.action*/ != null) {
                        world.step(it/*.action*/) // Move the agent
                        actions.add(it/*.action*/.toString()) // Save the action
                    }
                }
            } else {

                var count = 0
                for (it in actionList) {
                    println(it/*.action*/)
                    if (it/*.action*/ != null) {


                        if (count < maxCount) {
//                            println(it)
                            world.step(it/*.action*/) // Move the agent
                            actions.add(it/*.action*/.toString())
                        }// Save the action
                        actionsLists.add(it/*.action*/.toString())
                        count++;
                    }
                    //print(world.getState())
                    //   break;
                }
            }
            if (!world.isGoal()) {
                actionsLists.add("" + update + " ")
                //actionsLists.add("" + world.getState())
            }

            System.gc()
        }
        actionsLists.add("" + maxCount)
        for (it in actions) {
            actionsLists.add(it.toString())
        }
        //actionsLists.add(" " + maxCount + " ")

        logger.info { actionsLists.toString() }

        val pathLength = actions.size.toLong()
        val totalExecutionNanoTime = pathLength * experimentConfiguration.actionDuration
        val goalAchievementTime = totalPlanningNanoTime + totalExecutionNanoTime // TODO fix for overlap
        logger.info { "Path length: [$pathLength] \nAfter ${planner.expandedNodeCount} expanded " +
                "and ${planner.generatedNodeCount} generated nodes in ${totalPlanningNanoTime} ns. " +
                "(${planner.expandedNodeCount / convertNanoUpDouble(totalPlanningNanoTime, TimeUnit.SECONDS)} expanded nodes per sec)" }
        return ExperimentResult(
                experimentConfiguration.valueStore,
                planner.expandedNodeCount,
                planner.generatedNodeCount,
                totalPlanningNanoTime,
                totalExecutionNanoTime,
                goalAchievementTime,
                pathLength,
                actions.map { it.toString() }
        )
    }
}


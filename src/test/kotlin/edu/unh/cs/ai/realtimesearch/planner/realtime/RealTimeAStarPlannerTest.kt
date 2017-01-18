package edu.unh.cs.ai.realtimesearch.planner.realtime

//import edu.unh.cs.ai.realtimesearch.agent.RTSAgent
//import edu.unh.cs.ai.realtimesearch.environment.gridworld.GridWorldEnvironment
import edu.unh.cs.ai.realtimesearch.environment.gridworld.GridWorldIO
import edu.unh.cs.ai.realtimesearch.experiment.RealTimeExperiment
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.FakeTerminationChecker
import org.junit.Ignore
import org.junit.Test

@Ignore
class RealTimeAStarPlannerTest {

//    @Test
//    fun testRealTimeAStarPlanner() {
//        val stream = RealTimeAStarPlannerTest::class.java.classLoader.getResourceAsStream("input/vacuum/empty.vw")
//        val gridWorldInstance = GridWorldIO.parseFromStream(stream, 10)
//        val realTimeAStarPlanner = RealTimeAStarPlanner(gridWorldInstance.domain, 4)
//        val realTimeAStarAgent = RTSAgent(realTimeAStarPlanner)
//        val gridWorldEnvironment = GridWorldEnvironment(gridWorldInstance.domain, gridWorldInstance.initialState)
//
//        val rtsExperiment = RealTimeExperiment(GeneralExperimentConfiguration(), realTimeAStarAgent, gridWorldEnvironment, FakeTerminationChecker())
//        /*val experimentResults = */rtsExperiment.run()
//    }
}
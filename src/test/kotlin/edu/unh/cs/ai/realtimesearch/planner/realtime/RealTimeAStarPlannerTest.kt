package edu.unh.cs.ai.realtimesearch.planner.realtime

import edu.unh.cs.ai.realtimesearch.environment.gridworld.GridWorldIO
import edu.unh.cs.ai.realtimesearch.experiment.RealTimeExperiment
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.FakeTerminationChecker
import org.junit.Ignore
import org.junit.Test

@Ignore
class RealTimeAStarPlannerTest {

    @Test
    fun testRealTimeAStarPlanner() {
        val stream = RealTimeAStarPlannerTest::class.java.classLoader.getResourceAsStream("input/vacuum/empty.vw")
        val gridWorldInstance = GridWorldIO.parseFromStream(stream, 10)
        val realTimeAStarPlanner = RealTimeAStarPlanner(gridWorldInstance.domain, 4)

        val rtsExperiment = RealTimeExperiment(GeneralExperimentConfiguration(), realTimeAStarPlanner, gridWorldInstance.domain, gridWorldInstance.initialState, FakeTerminationChecker())
        /*val experimentResults = */rtsExperiment.run()
    }
}
package edu.unh.cs.ai.realtimesearch.planner.realtime_

import edu.unh.cs.ai.realtimesearch.agent.RTSAgent
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldEnvironment
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldIO
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldState
import edu.unh.cs.ai.realtimesearch.experiment.RTSExperiment
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.CallsTerminationChecker
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.io.FileInputStream

@Ignore
class RealTimeAStarPlannerTest {

    @Test
    fun testRealTimeAStarPlanner() {
        val instanceFileName = "input/vacuum/empty.vw"
        val vacuumWorldInstance = VacuumWorldIO.parseFromStream(FileInputStream(File(instanceFileName)))
        val realTimeAStarPlanner = RealTimeAStarPlanner(vacuumWorldInstance.domain)
        val realTimeAStarAgent = RTSAgent(realTimeAStarPlanner)
        val vacuumWorldEnvironment = VacuumWorldEnvironment(vacuumWorldInstance.domain, vacuumWorldInstance.initialState)

        val rtsExperiment = RTSExperiment<VacuumWorldState>(null, realTimeAStarAgent, vacuumWorldEnvironment, CallsTerminationChecker(10))
        val experimentResults = rtsExperiment.run()
    }
}
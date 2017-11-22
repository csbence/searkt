package edu.unh.cs.ai.realtimesearch.planner.realtime

import edu.unh.cs.ai.realtimesearch.environment.Domains
import edu.unh.cs.ai.realtimesearch.environment.gridworld.GridWorldIO
import edu.unh.cs.ai.realtimesearch.experiment.RealTimeExperiment
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.DomainPath
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.generateConfigurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.LookaheadType
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.FakeTerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.Planners
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.TimeUnit

@Ignore
class RealTimeAStarPlannerTest {
    private fun makeTestConfiguration(domain: Pair<Domains, DomainPath>, planner: Planners, depthLimit: Int) = generateConfigurations(
            domains = listOf(domain),
            planners = listOf(planner),
            actionDurations = listOf(1L),//50L, 100L, 150L, 200L, 250L, 400L, 800L, 1600L, 3200L, 6400L, 12800L),
            terminationType = TerminationType.EXPANSION,
            lookaheadType = LookaheadType.DYNAMIC,
            timeLimit = TimeUnit.NANOSECONDS.convert(15, TimeUnit.MINUTES),
            expansionLimit = 300000000,
            stepLimit = 300000000,
            plannerExtras = listOf(
                    Triple(Planners.RTA_STAR, Configurations.LOOKAHEAD_DEPTH_LIMIT, listOf(depthLimit))
            ),
            domainExtras = listOf()

    )
    @Test
    fun testRealTimeAStarPlanner() {
        val stream = RealTimeAStarPlannerTest::class.java.classLoader.getResourceAsStream("input/vacuum/empty.vw")
        val gridWorldInstance = GridWorldIO.parseFromStream(stream, 10)
        val domainPair = Pair(Domains.GRID_WORLD, "input/vacuum/empty.vw")
        val config = makeTestConfiguration(domainPair, Planners.RTA_STAR,4)
        val realTimeAStarPlanner = RealTimeAStarPlanner(gridWorldInstance.domain, config.first())

        val rtsExperiment = RealTimeExperiment(GeneralExperimentConfiguration(), realTimeAStarPlanner, gridWorldInstance.domain, gridWorldInstance.initialState, FakeTerminationChecker)
        /*val experimentResults = */rtsExperiment.run()
    }
}
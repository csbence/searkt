package edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic

import edu.unh.cs.ai.realtimesearch.Input
import edu.unh.cs.ai.realtimesearch.environment.Domains
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ConfigurationExecutor
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.planner.CommitmentStrategy
import edu.unh.cs.ai.realtimesearch.planner.Planners
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class AStarPlannerTest {

    @Test
    fun testOptimality() {
        val input = Input::class.java.classLoader.getResourceAsStream("input/vacuum/squiggle.vw") ?: throw RuntimeException("Resource not found")
        val rawDomain = Scanner(input).useDelimiter("\\Z").next()
        val manualConfiguration = GeneralExperimentConfiguration(
                //                Domains.SLIDING_TILE_PUZZLE.toString(),
                Domains.GRID_WORLD.toString(),
                rawDomain,
                Planners.A_STAR.toString(),
                "time")
        manualConfiguration["lookaheadDepthLimit"] = 4L
        manualConfiguration["actionDuration"] = 10L
        manualConfiguration["timeBoundType"] = "STATIC"
        manualConfiguration["commitmentStrategy"] = CommitmentStrategy.MULTIPLE.toString()
        manualConfiguration["singleStepLookahead"] = false
        manualConfiguration["timeLimit"] = TimeUnit.NANOSECONDS.convert(5, TimeUnit.MINUTES)

        val result = ConfigurationExecutor.executeConfiguration(manualConfiguration)
        assertTrue(result.pathLength == 15L)

    }
}
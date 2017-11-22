package edu.unh.cs.ai.realtimesearch.environment.heavytiles

import edu.unh.cs.ai.realtimesearch.environment.Domains
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.DomainPath
import edu.unh.cs.ai.realtimesearch.experiment.configuration.generateConfigurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.LookaheadType
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType
import edu.unh.cs.ai.realtimesearch.planner.Planners
import org.junit.Test
import java.io.File
import java.io.FileWriter
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InverseTilePuzzleTest {

    private fun makeTestConfiguration(domain: Pair<Domains, DomainPath>, planner: Planners, weight: Double) = generateConfigurations(
            domains = listOf(domain),
            planners = listOf(planner),
            actionDurations = listOf(1L),//50L, 100L, 150L, 200L, 250L, 400L, 800L, 1600L, 3200L, 6400L, 12800L),
            terminationType = TerminationType.EXPANSION,
            lookaheadType = LookaheadType.DYNAMIC,
            timeLimit = TimeUnit.NANOSECONDS.convert(15, TimeUnit.MINUTES),
            expansionLimit = 300000000,
            stepLimit = 300000000,
            plannerExtras = listOf(
                    Triple(Planners.WEIGHTED_A_STAR, Configurations.WEIGHT, listOf(weight))
            ),
            domainExtras = listOf()

    )

    private fun createInstanceFromString(puzzle: String): InputStream {
        val temp = File.createTempFile("tile", ".puzzle")
        temp.deleteOnExit()
        val tileReader = Scanner(puzzle)

        val fileWriter = FileWriter(temp)
        fileWriter.write("4 4\nstarting:\n")
        while (tileReader.hasNext()) {
            val num = tileReader.nextInt().toString()
            fileWriter.write(num + "\n")
        }
        fileWriter.close()
        return temp.inputStream()
    }

    @Test
    fun testZeroHeuristic() {
        val tiles = "0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = HeavyTilePuzzleIO.parseFromStream(instance, 1L)
        val heuristic = slidingTilePuzzle.domain.heuristic(slidingTilePuzzle.initialState)
        assertEquals(0.0, heuristic)
    }

    @Test
    fun testHeuristic1() {
        val tiles = "1 0 2 3 4 5 6 7 8 9 10 11 12 13 14 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = HeavyTilePuzzleIO.parseFromStream(instance, 1L)
        val heuristic = slidingTilePuzzle.domain.heuristic(slidingTilePuzzle.initialState)
        assertEquals(1.0, heuristic)
    }

    @Test
    fun testHeuristic2() {
        val tiles = "4 1 2 3 0 5 6 7 8 9 10 11 12 13 14 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = HeavyTilePuzzleIO.parseFromStream(instance, 1L)
        val heuristic = slidingTilePuzzle.domain.heuristic(slidingTilePuzzle.initialState)
        assertEquals(4.0, heuristic)
    }

    @Test
    fun testHeuristic3() {
        val tiles = "1 2 0 3 5 4 6 7 8 9 10 11 12 13 14 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = HeavyTilePuzzleIO.parseFromStream(instance, 1L)
        val heuristic = slidingTilePuzzle.domain.heuristic(slidingTilePuzzle.initialState)
        assertEquals(12.0, heuristic)
    }

    @Test
    fun testHeuristic4() {
        val tiles = "14 13 15 7 11 12 9 5 6 0 2 1 4 8 10 3"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = HeavyTilePuzzleIO.parseFromStream(instance, 1L)
        val heuristic = slidingTilePuzzle.domain.heuristic(slidingTilePuzzle.initialState)
        assertEquals(353.0, heuristic)
    }

    @Test
    fun testHeuristic5() {
        val tiles = "14 9 12 13 15 4 8 10 0 2 1 7 3 11 5 6"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = HeavyTilePuzzleIO.parseFromStream(instance, 1L)
        val heuristic = slidingTilePuzzle.domain.heuristic(slidingTilePuzzle.initialState)
        assertEquals(436.0, heuristic)

    }

    @Test
    fun testSuccessors1() {
        val tiles = "1 2 0 3 5 4 6 7 8 9 10 11 12 13 14 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = HeavyTilePuzzleIO.parseFromStream(instance, 1L)
        val successors = slidingTilePuzzle.domain.successors(slidingTilePuzzle.initialState)
        assertTrue(successors.size == 3)
    }

    @Test
    fun testSuccessors2() {
        val initialState = "1 2 0 3 5 4 6 7 8 9 10 11 12 13 14 15"
        val successor1 = "1 0 2 3 5 4 6 7 8 9 10 11 12 13 14 15"
        val successor2 = "1 2 3 0 5 4 6 7 8 9 10 11 12 13 14 15"
        val successor3 = "1 2 6 3 5 4 0 7 8 9 10 11 12 13 14 15"
        println(initialState)
        val initInstance = createInstanceFromString(initialState)
        val s1Instance = createInstanceFromString(successor1)
        val s2Instance = createInstanceFromString(successor2)
        val s3Instance = createInstanceFromString(successor3)
        val stp = HeavyTilePuzzleIO.parseFromStream(initInstance, 1L)
        val s1 = HeavyTilePuzzleIO.parseFromStream(s1Instance, 1L)
        val s2 = HeavyTilePuzzleIO.parseFromStream(s2Instance, 1L)
        val s3 =HeavyTilePuzzleIO.parseFromStream(s3Instance, 1L)
        val successors = stp.domain.successors(stp.initialState)
        println(successors)
        assertTrue { successors.contains(SuccessorBundle(s1.initialState, HeavyTilePuzzleAction.WEST, 2L)) }
        assertTrue { successors.contains(SuccessorBundle(s2.initialState, HeavyTilePuzzleAction.EAST, 3L)) }
        assertTrue { successors.contains(SuccessorBundle(s3.initialState, HeavyTilePuzzleAction.SOUTH, 6L)) }
    }

    @Test
    fun testSuccessors3() {
        val initialState = "10 2 8 4 15 0 1 14 11 13 3 6 9 7 5 12"
        val successor1 = "10 2 8 4 0 15 1 14 11 13 3 6 9 7 5 12"
        val successor2 = "10 0 8 4 15 2 1 14 11 13 3 6 9 7 5 12"
        val successor3 = "10 2 8 4 15 1 0 14 11 13 3 6 9 7 5 12"
        val successor4 = "10 2 8 4 15 13 1 14 11 0 3 6 9 7 5 12"
        println(initialState)
        val initInstance = createInstanceFromString(initialState)
        val s1Instance = createInstanceFromString(successor1)
        val s2Instance = createInstanceFromString(successor2)
        val s3Instance = createInstanceFromString(successor3)
        val s4Instance = createInstanceFromString(successor4)
        val stp = HeavyTilePuzzleIO.parseFromStream(initInstance, 1L)
        val s1 = HeavyTilePuzzleIO.parseFromStream(s1Instance, 1L)
        val s2 = HeavyTilePuzzleIO.parseFromStream(s2Instance, 1L)
        val s3 = HeavyTilePuzzleIO.parseFromStream(s3Instance, 1L)
        val s4 = HeavyTilePuzzleIO.parseFromStream(s4Instance, 1L)
        val successors = stp.domain.successors(stp.initialState)
        println(successors)
        assertTrue { stp.domain.heuristic(stp.initialState) == 373.0 }
        assertTrue { successors.contains(SuccessorBundle(s1.initialState, HeavyTilePuzzleAction.WEST, 15L)) }
        assertTrue { successors.contains(SuccessorBundle(s2.initialState, HeavyTilePuzzleAction.NORTH, 2L)) }
        assertTrue { successors.contains(SuccessorBundle(s3.initialState, HeavyTilePuzzleAction.EAST, 1L)) }
        assertTrue { successors.contains(SuccessorBundle(s4.initialState, HeavyTilePuzzleAction.SOUTH, 13L)) }
    }




}
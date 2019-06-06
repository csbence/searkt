package edu.unh.cs.searkt.environment.pancake

import org.junit.Test
import java.io.File

internal class PancakeProblemTest {


    @Test
    fun parseProblemInstance() {
        val pancakeProblem = PancakeIO.parseFromStream(File("/home/aifs2/doylew/IdeaProjects/searkt/src/main/resources/input/pancake/toy").inputStream(), 1L)
        val initialState = byteArrayOf(6, 8, 9, 10, 1, 2, 3, 5, 4, 7, 11)
        val goalState = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
        pancakeProblem.initialState.ordering.contentEquals(initialState)
        pancakeProblem.domain.startOrdering.contentEquals(initialState)
        pancakeProblem.domain.endOrdering.contentEquals(goalState)

    }

    @Test
    fun successors() {
    }

    @Test
    fun heuristic() {
    }

    @Test
    fun distance() {
    }

    @Test
    fun isGoal() {
    }
}
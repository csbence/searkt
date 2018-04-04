package edu.unh.cs.ai.realtimesearch.environment.heavytiles

import org.junit.Test
import java.io.File
import java.io.FileWriter
import java.io.InputStream
import java.util.*
import kotlin.test.assertTrue

class HeavyTilePuzzleTest {

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
    fun testDistance() {
        var tiles = "0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15"
        var instance = createInstanceFromString(tiles)
        var heavyTilePuzzle = HeavyTilePuzzleIO.parseFromStream(instance, 1L)
        var initialState = heavyTilePuzzle.initialState
        assertTrue { initialState.distance == 0.0 }

        tiles = "1 2 3 0 4 5 6 7 8 9 10 11 12 13 14 15"
        instance = createInstanceFromString(tiles)
        heavyTilePuzzle = HeavyTilePuzzleIO.parseFromStream(instance, 1L)
        initialState = heavyTilePuzzle.initialState
        assertTrue { initialState.distance == 3.0 }
        assertTrue { initialState.distance != initialState.heuristic }

    }

    @Test
    fun testSuccessorActionCost() {
        val tiles = "0 4 1 2 8 5 6 3 12 9 10 7 13 14 15 11"
        val instance = createInstanceFromString(tiles)
        val heavyTilePuzzle = HeavyTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = heavyTilePuzzle.initialState
        println(initialState)
        val successorsLevel1 = heavyTilePuzzle.domain.successors(initialState)
        successorsLevel1.forEach(::println)



    }
}

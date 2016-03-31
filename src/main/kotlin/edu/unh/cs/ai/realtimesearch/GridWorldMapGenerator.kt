package edu.unh.cs.ai.realtimesearch

import org.springframework.util.StringUtils
import java.util.*

/**
 * Generator for unique start/end location pairs.  Takes an initial map and generates random start and end locations
 * within the empty spaces of the map.
 * <p>
 * Instances of this class are <i>not</i> thread-safe.
 *
 * @author Mike Bogochow
 * @since 3/31/16
 */
class GridWorldMapGenerator(private var baseMap: String) {
    val rowCount: Int
    val columnCount: Int
    val emptyCount: Int
    private val rows = arrayListOf<CharArray>()
    private val rand = Random()

    private val START_CELL = '@'
    private val END_CELL = '#'
    private val BLOCKED_CELL = '#'
    private val EMPTY_CELL = '_'

    init {
        // Replace initial start and end states with empty space
        baseMap = baseMap.replace(START_CELL, EMPTY_CELL).replace(END_CELL, EMPTY_CELL)
        emptyCount = StringUtils.countOccurrencesOf(baseMap, EMPTY_CELL.toString())

        if (emptyCount < 2)
            throw IllegalArgumentException("Not enough empty cells in map")

        // Get row and column counts
        val inputScanner = Scanner(baseMap)
        try {
            columnCount = inputScanner.nextLine().toInt()
            rowCount = inputScanner.nextLine().toInt()
        } catch (e: NoSuchElementException) {
            throw IllegalArgumentException("Second or first line of map missing", e)
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("First and second line of map must be a number.", e)
        }

        if (columnCount < 1 || rowCount < 1)
            throw IllegalArgumentException("Row and column counts in map must be at least 1")

        // Setup internal representations
        rows.ensureCapacity(rowCount)
        while (inputScanner.hasNextLine()) {
            rows.add(inputScanner.nextLine().toCharArray())
        }
    }

    private fun getRandomX() = rand.nextInt(columnCount)
    private fun getRandomY() = rand.nextInt(rowCount)
    private fun isValid(x: Int, y: Int): Boolean = rows[y][x] == EMPTY_CELL

    private fun getValidRandomPoint(): Pair<Int, Int> {
        var x = getRandomX()
        var y = getRandomY()
        while (!isValid(x, y)) {
            x = getRandomX()
            y = getRandomY()
        }
        return Pair(x, y)
    }

    private fun buildMap(): String {
        val mapBuilder = StringBuilder()
        mapBuilder.appendln(columnCount)
        mapBuilder.appendln(rowCount)
        for (row in rows) {
            mapBuilder.appendln(row)
        }
        return mapBuilder.toString()
    }

    private fun buildMap(startX: Int, startY: Int, endX: Int, endY: Int): String {
        // Set the points
        rows[startY][startX] = START_CELL
        rows[endY][endX] = END_CELL

        val map = buildMap()

        // Reset the original map
        rows[startY][startX] = EMPTY_CELL
        rows[endY][endX] = EMPTY_CELL

        return map
    }

    /**
     * Generate the map with random start and end points.
     */
    fun generateMap(): String {
        val (startX, startY) = getValidRandomPoint()
        val (endX, endY) = getValidRandomPoint()

        if (startX == endX && startY == endY)
            return generateMap() // try again

        return buildMap(startX, startY, endX, endY)
    }

    /**
     * Generate a number of unique maps. WARNING: naive implementation so don't pass
     * too high of a value especially for small maps.
     *
     * @param quantity number of unique maps to generate
     * @return set of unique maps; Empty set if quantity < 1 || quantity > emptyCount * 2
     */
    fun generateUniqueMaps(quantity: Int): Set<String> {
        val maps = mutableSetOf<String>()
        if (quantity < 1 || quantity > emptyCount * 2)
            return maps

        for (i in 1..quantity) {
            while (maps.add(generateMap()) == false);
        }

        return maps
    }

    /**
     * Generate all possible unique maps. WARNING: could be very large for large maps.
     */
    fun generateAllMaps(): Set<String> {
        val maps = mutableSetOf<String>()

        for (startY in 1..rows.size) {
            val startRow = rows[startY]
            for (startX in 1..startRow.size) {
                if (startRow[startX] != EMPTY_CELL)
                    continue
                for (endY in 1..rows.size) {
                    val endRow = rows[endY]
                    for (endX in 1..endRow.size) {
                        if (endRow[endX] != EMPTY_CELL)
                            continue
                        if (startX == endX && startY == endY)
                            continue
                        maps.add(buildMap(startX, startY, endX, endY))
                    }
                }
            }
        }

        return maps
    }
}
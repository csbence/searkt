package edu.unh.cs.ai.realtimesearch.external;

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * The tiles domain class.

 * @author Matthew Hatem
 *
 * The constructor reads a tiles problem instance from the specified
 * input stream.

 * @param stream the input stream
 */
class Tiles(stream: InputStream) : Domain<Tiles.TileState> {

    init {
        try {
            val reader = BufferedReader(
                    InputStreamReader(stream))
            var line = reader.readLine()

            val dim = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            /*width =*/
            Integer.parseInt(dim[0])
            /*height =*/
            Integer.parseInt(dim[0])

            line = reader.readLine()
            for (t in 0..Ntiles - 1) {
                val p = Integer.parseInt(reader.readLine())
                init[p] = t
            }

            line = reader.readLine()
            for (t in 0..Ntiles - 1) {
                val p = Integer.parseInt(reader.readLine())
                if (p != t)
                    throw IllegalArgumentException("Non-canonical goal positions")
            }
        } catch (exception: IOException) {
            exception.printStackTrace()
        }

        initmd()
        initoptab()
    }

    /* (non-Javadoc)
     * @see edu.unh.ai.search.Domain#initial()
     */
    override fun initial(): TileState {
        var blank = -1
        val tiles = IntArray(Ntiles)
        for (i in 0..Ntiles - 1) {
            if (init[i] == 0)
                blank = i
            tiles[i] = init[i]
        }
        if (blank < 0)
            throw IllegalArgumentException("No blank tile")
        return TileState(tiles, blank, mdist(blank, tiles))
    }

    /* (non-Javadoc)
     * @see edu.unh.ai.search.Domain#h(java.lang.Object)
     */
    override fun h(s: TileState): Int {
        return s.h
    }

    /* (non-Javadoc)
     * @see edu.unh.ai.search.Domain#isGoal(java.lang.Object)
     */
    override fun isGoal(state: TileState): Boolean {
        return state.h == 0
    }

    /* (non-Javadoc)
     * @see edu.unh.ai.search.Domain#nops(java.lang.Object)
     */
    override fun numActions(state: TileState): Int {
        return optab_n[state.blank]
    }

    /* (non-Javadoc)
     * @see edu.unh.ai.search.Domain#nthop(java.lang.Object, int)
     */
    override fun nthAction(state: TileState, n: Int): Int {
        return optab_ops[state.blank][n]
    }

    /* (non-Javadoc)
     * @see edu.unh.ai.search.Domain#copy(java.lang.Object)
     */
    override fun copy(state: TileState): TileState {
        return TileState(state.tiles, state.blank, state.h)
    }

    /* (non-Javadoc)
     * @see edu.unh.ai.search.Domain#apply(java.lang.Object, int)
     */
    override fun apply(state: TileState, op: Int): Edge<TileState> {
        val edge = Edge<TileState>(1, op, state.blank)
        val tile = state.tiles[op]
        state.tiles[state.blank] = tile
        state.h += mdincr[tile][op][state.blank]
        state.blank = op
        return edge
    }

    /*
     * Computes the Manhattan distance for the specified blank and tile
     * configuration.
     */
    private fun mdist(blank: Int, tiles: IntArray): Int {
        var sum = 0
        for (i in 0..Ntiles - 1) {
            if (i == blank)
                continue
            val row = i / width
            val col = i % width
            val grow = tiles[i] / width
            val gcol = tiles[i] % width
            sum += Math.abs(gcol - col) + Math.abs(grow - row)
        }
        return sum
    }

    /*
     * Initializes the Manhattan distance heuristic table.
     */
    private fun initmd() {
        for (t in 1..Ntiles - 1) {
            val grow = t / width
            val gcol = t % width
            for (l in 0..Ntiles - 1) {
                val row = l / width
                val col = l % width
                md[t][l] = Math.abs(col - gcol) + Math.abs(row - grow)
            }
        }
        for (t in 1..Ntiles - 1) {
            for (d in 0..Ntiles - 1) {
                val newmd = md[t][d]
                for (s in 0..Ntiles - 1)
                    mdincr[t][d][s] = -100 // some invalid value.
                if (d >= width)
                    mdincr[t][d][d - width] = md[t][d - width] - newmd
                if (d % width > 0)
                    mdincr[t][d][d - 1] = md[t][d - 1] - newmd
                if (d % width < width - 1)
                    mdincr[t][d][d + 1] = md[t][d + 1] - newmd
                if (d < Ntiles - width)
                    mdincr[t][d][d + width] = md[t][d + width] - newmd
            }
        }
    }

    /*
     * Initializes the pre-computed operator table.
     */
    private fun initoptab() {
        for (i in 0..Ntiles - 1) {
            optab_n[i] = 0
            if (i >= width)
                optab_ops[i][optab_n[i]++] = i - width
            if (i % width > 0)
                optab_ops[i][optab_n[i]++] = i - 1
            if (i % width < width - 1)
                optab_ops[i][optab_n[i]++] = i + 1
            if (i < Ntiles - width)
                optab_ops[i][optab_n[i]++] = i + width
            assert(optab_n[i] <= 4)
        }
    }

    /*
     * The tile state class.
     */
    class TileState(tiles: IntArray, internal var blank: Int, internal var h: Int) {
        internal var tiles: IntArray

        init {
            this.tiles = IntArray(tiles.size)
            System.arraycopy(tiles, 0, this.tiles, 0, tiles.size)
        }

        override fun equals(obj: Any?): Boolean {
            if (blank != (obj as TileState).blank)
                return false
            for (i in 0..Ntiles - 1) {
                if (i != blank && tiles[i] != obj.tiles[i])
                    return false
            }
            return true

        }

        override fun hashCode(): Int {
            tiles[blank.toInt()] = 0
            var h = tiles[0]
            for (i in 1..Ntiles - 1)
                h = h * 3 + tiles[i]
            return h
        }


    }

    companion object {

        private val width = 4
        private val height = 4
        private val Ntiles = width * height
        private val init = IntArray(Ntiles)
        private val md = Array(Ntiles) { IntArray(Ntiles) }
        internal var mdincr = Array(Ntiles) { Array(Ntiles) { IntArray(Ntiles) } }
        private val optab_n = IntArray(Ntiles)
        private val optab_ops = Array(Ntiles) { IntArray(4) }
    }

}

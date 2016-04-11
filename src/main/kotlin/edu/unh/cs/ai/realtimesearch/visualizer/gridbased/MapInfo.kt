package edu.unh.cs.ai.realtimesearch.visualizer.gridbased

import edu.unh.cs.ai.realtimesearch.environment.location.Location

/**
 * Info for a grid world map.
 *
 * @author Mike Bogochow (mgp36@unh.edu)
 * @since April 8, 2016
 */
data class MapInfo(
        val rowCount: Int,
        val columnCount: Int,
        val blockedCells: MutableList<Location> = mutableListOf(),
        val startCells: MutableList<Location> = mutableListOf(),
        val goalCells: MutableList<Location> = mutableListOf()) {
    companion object {
        val ZERO = MapInfo(0, 0)
    }
}
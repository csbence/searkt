package edu.unh.cs.ai.realtimesearch.visualizer.gridbased

import edu.unh.cs.ai.realtimesearch.environment.location.DoubleLocation
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import edu.unh.cs.ai.realtimesearch.visualizer.BaseVisualizer
import groovyjarjarcommonscli.CommandLine
import groovyjarjarcommonscli.Option
import groovyjarjarcommonscli.Options
import javafx.stage.Screen
import java.util.*

/**
 * Base visualizer for grid-based domains.
 *
 * @author Mike Bogochow (mgp36@unh.edu)
 * @since April 8, 2016
 */
abstract class GridBasedVisualizer : BaseVisualizer() {
    // Options
    protected val gridOptions = Options()
    protected val trackerOption = Option("t", "tracker", true, "show tracker around agent")
    protected val displayPathOption = Option("p", "path", false, "display line for agent's path")

    // Option fields
    protected var showTracker: Boolean = false
    protected var trackerSize: Double = 10.0
    protected var displayLine: Boolean = false

    // State fields
    protected var actionList: MutableList<String> = arrayListOf()
    protected var mapInfo: MapInfo = MapInfo.ZERO

    // Graphical fields
    protected var grid: GridCanvasPane = GridCanvasPane.ZERO
    protected var agentView: AgentView = AgentView.ZERO
    private val primaryScreenBounds = Screen.getPrimary().visualBounds
    protected val windowWidth = primaryScreenBounds.width - 100
    protected val windowHeight = primaryScreenBounds.height - 100
    protected var tileSize = 0.0
    open protected var robotScale = 2.0

    /** Initial coordinates of agent in map. */
    protected lateinit var initialAgentMapCoordinate: DoubleLocation

    /** Initial location of agent in animation. */
    protected lateinit var initialAgentAnimationLocation: DoubleLocation

    init {
        trackerOption.setOptionalArg(true)
        gridOptions.addOption(trackerOption)
        gridOptions.addOption(displayPathOption)
    }

    override fun getOptions(): Options {
        return gridOptions
    }

    override fun processOptions(cmd: CommandLine) {
        showTracker = cmd.hasOption(trackerOption.opt)
        trackerSize = cmd.getOptionValue(trackerOption.opt, trackerSize.toString()).toDouble()
        displayLine = cmd.hasOption(displayPathOption.opt)
    }

    data class GridDimensions(val rowCount: Int, val columnCount: Int)

    /**
     * Parse the map header and return the row and column counts.  Domains which have a different header than the
     * standard grid world header should override and do what they need with the extra values and then return the
     * row and column counts here.  Implementations must not read more than the header from the scanner.
     *
     * @param inputScanner scanner pointing to header of raw domain string
     * @return the row and column counts given in the header
     */
    open protected fun parseMapHeader(inputScanner: Scanner): GridDimensions {
        val columnCount = inputScanner.nextLine().toInt()
        val rowCount = inputScanner.nextLine().toInt()
        return GridDimensions(rowCount, columnCount)
    }

    /**
     * Parse map; fill {@link MapInfo}
     */
    open protected fun parseMap(rawDomain: String): MapInfo {
        val inputScanner = Scanner(rawDomain.byteInputStream())
        val (rowCount, columnCount) = parseMapHeader(inputScanner)
        val mapInfo = MapInfo(rowCount, columnCount)
        for (y in 0..rowCount - 1) {
            val line = inputScanner.nextLine()
            for (x in 0..columnCount - 1) {
                when (line[x]) {
                    '#' -> {
                        mapInfo.blockedCells.add(Location(x, y))
                    }
                    '_' -> {
                    }
                    '*' -> {
                        mapInfo.goalCells.add(Location(x, y))
                    }
                    '@' -> {
                        mapInfo.startCells.add(Location(x, y))
                    }
                    else -> {
                        throw IllegalArgumentException("Invalid character ${line[x]} found in map")
                    }
                }
            }
        }
        return mapInfo
    }

    /**
     * Parse the experiment result for actions.  If the domain includes actions which cannot be directly translated
     * from the results as strings then the implementing visualizer should override this method.
     */
    open protected fun parseActions(): MutableList<String> {
        /* Get action list from Application */
        val actionList: MutableList<String> = arrayListOf()
        for (action in experimentResult.actions) {
            actionList.add(action)
        }
        return actionList
    }

    /**
     * Parse the map information for the initial relative coordinates of the agent within its start cell.  The
     * {@link #parseMap} and {@link #parseMapHeader} methods are guaranteed to have been called before this method is
     * called and therefore the {@link #mapInfo} field will be initialized.  The returned values must be in the range
     * [0.0, 1.0).
     * <p>
     * Default implementation is the center of the cell (0.5, 0.5).
     *
     * @return the initial agent coordinates on the map
     */
    open protected fun getInitialCellOffset(): DoubleLocation = DoubleLocation(0.5, 0.5)

    // Uses #tileSize so should not be used until tileSize is initialized
    private fun convertMapToGrid(value: Double): Double = value * tileSize
    private fun convertMapToGrid(value: DoubleLocation): DoubleLocation
            = DoubleLocation(convertMapToGrid(value.x), convertMapToGrid(value.y))

    /**
     * Performs parsing of results and graphical setup.  After this method is called, all {@link GridBasedVisualizer}
     * fields will be properly initialized.  This method should be called after calling
     * {@link BaseVisualizer#processCommandLine).
     */
    protected fun visualizerSetup() {
        actionList = parseActions()

        // Parse map
        mapInfo = parseMap(rawDomain)
        if (mapInfo.startCells.size != 1) {
            throw IllegalArgumentException("${mapInfo.startCells.size} start cells found in map; required 1")
        }

        val initialCellOffset = getInitialCellOffset()
        initialAgentMapCoordinate = DoubleLocation(
                mapInfo.startCells.first().x.toDouble() + initialCellOffset.x,
                mapInfo.startCells.first().y.toDouble() + initialCellOffset.y)

        // Calculate grid tile sizes
        val maxTileWidth = windowWidth / mapInfo.columnCount
        val maxTileHeight = windowHeight / mapInfo.rowCount
        tileSize = Math.min(maxTileWidth, maxTileHeight)
        while (((tileSize * mapInfo.columnCount) > windowWidth) || ((tileSize * mapInfo.rowCount) > windowHeight)) {
            tileSize /= 1.05
        }

        // Calculate robot parameters
        val agentSize = tileSize / robotScale
        initialAgentAnimationLocation = convertMapToGrid(initialAgentMapCoordinate)
        // Actual display location is offset to center of agent
        val actualAgentLocation = initialAgentAnimationLocation - agentSize / 2.0

        // Agent setup
        agentView = AgentView(agentSize, trackerSize)
        agentView.trackingEnabled = showTracker
        agentView.toFront()
        agentView.setLocation(actualAgentLocation.x, actualAgentLocation.y)

        // Grid setup
        grid = GridCanvasPane(mapInfo, tileSize)
        grid.children.add(agentView.agent)
        grid.children.add(agentView.tracker)
    }
}
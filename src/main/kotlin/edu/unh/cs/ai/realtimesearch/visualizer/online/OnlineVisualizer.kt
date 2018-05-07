package edu.unh.cs.ai.realtimesearch.visualizer.online

//import edu.unh.cs.ai.realtimesearch.visualizerLatch
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.gridworld.GridWorld
import edu.unh.cs.ai.realtimesearch.environment.gridworld.GridWorldState
import edu.unh.cs.ai.realtimesearch.planner.SearchNode
import edu.unh.cs.ai.realtimesearch.visualizer
import edu.unh.cs.ai.realtimesearch.visualizer.ThemeColors
import edu.unh.cs.ai.realtimesearch.visualizer.gridbased.AgentView
import edu.unh.cs.ai.realtimesearch.visualizer.gridbased.GridCanvasPane
import edu.unh.cs.ai.realtimesearch.visualizer.gridbased.MapInfo
import edu.unh.cs.ai.realtimesearch.visualizer.gridbased.SearchEnvelopeCell
import edu.unh.cs.ai.realtimesearch.visualizerLatch
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.stage.Screen
import javafx.stage.Stage

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class OnlineGridVisualizer : Application() {
    // Option fields
    private var showTracker: Boolean = false
    private var trackerSize: Double = 10.0
    private var displayLine: Boolean = false

    // State fields
    private var actionList: MutableList<String> = arrayListOf()
    private var mapInfo: MapInfo = MapInfo.ZERO

    // Graphical fields
    private var grid: GridCanvasPane = GridCanvasPane.ZERO
    private var agentView: AgentView = AgentView.ZERO
    private var tileWidth = 0.0
    private var tileHeight = 0.0
    private var tileSize = 0.0
    private var robotScale = 2.0

    private var initialAgentXLocation = 0.0
    private var initialAgentYLocation = 0.0

    private lateinit var primaryStage: Stage

    override fun start(primaryStage: Stage) {
        this.primaryStage = primaryStage
        visualizer = this

        val root = Group()
        val scene = Scene(root, 800.0, 600.0, Color.BLACK)
        primaryStage.scene = scene

        primaryStage.show()

        visualizerLatch.countDown()
    }

    /**
     * Performs parsing of results and graphical setup.  After this method is called, all {@link GridBasedVisualizer}
     * fields will be properly initialized.
     */
    private fun visualizerSetup() {
        val primaryScreenBounds = Screen.getPrimary().visualBounds
        val windowWidth = primaryScreenBounds.width - 100
        val windowHeight = primaryScreenBounds.height - 100

        if (mapInfo.startCells.size != 1) {
            throw IllegalArgumentException("${mapInfo.startCells.size} start cells found in map; required 1")
        }

        // Calculate tile sizes
        tileWidth = windowWidth / mapInfo.columnCount
        tileHeight = windowHeight / mapInfo.rowCount
        tileSize = Math.min(tileWidth, tileHeight)
        while (((tileSize * mapInfo.columnCount) > windowWidth) || ((tileSize * mapInfo.rowCount) > windowHeight)) {
            tileSize /= 1.05
        }

        // Calculate robot parameters
        val agentWidth = tileSize / robotScale
        val agentStartX = mapInfo.startCells.first().x
        val agentStartY = mapInfo.startCells.first().y
        initialAgentXLocation = agentStartX * tileSize + (tileSize / 2.0)
        initialAgentYLocation = agentStartY * tileSize + (tileSize / 2.0)
        val actualRobotXLocation = initialAgentXLocation - agentWidth / 2.0
        val actualRobotYLocation = initialAgentYLocation - agentWidth / 2.0

        // Agent setup
        agentView = AgentView(agentWidth, trackerSize)
        agentView.trackingEnabled = showTracker
        agentView.toFront()
        agentView.setLocation(actualRobotXLocation, actualRobotYLocation)

        // Grid setup
        grid = GridCanvasPane(mapInfo, tileSize)
        grid.children.add(agentView.agent)
        grid.children.add(agentView.tracker)
    }

    fun setup(domain: Domain<*>, initialState: State<*>) {
        if (domain !is GridWorld)
            throw UnsupportedOperationException("Unsupported globalDomain for visualization")

        if (initialState !is GridWorldState)
            throw UnsupportedOperationException("Unsupported StateType for visualization")

        mapInfo = MapInfo(
                domain.height,
                domain.width,
                domain.blockedCells.toMutableList(),
                mutableListOf(initialState.agentLocation)
        )

        visualizerSetup()

        primaryStage.title = "RTS Visualizer"
        primaryStage.scene = Scene(grid, tileSize * mapInfo.columnCount, tileSize * mapInfo.rowCount, ThemeColors.BACKGROUND.color)
    }

    fun <StateType : State<StateType>, NodeType : SearchNode<StateType, NodeType>> updateSearchEnvelope(searchEnvelope: Collection<SearchNode<StateType, NodeType>>) {

        Platform.runLater {
            mapInfo.searchEnvelope.clear()

            searchEnvelope
                    .map {
                        val state = it.state
                        if (state is GridWorldState) {

                            SearchEnvelopeCell(state.agentLocation, it.heuristic)
                        } else {
                            null
                        }
                    }
                    .filterNotNullTo(mapInfo.searchEnvelope)
        }

    }

    fun <StateType : State<StateType>, NodeType : SearchNode<StateType, NodeType>> updateBackpropagation(backpropagation: Collection<SearchNode<StateType, NodeType>>) {

        Platform.runLater {
            mapInfo.backPropagation.clear()

            backpropagation
                    .map {
                        val state = it.state
                        if (state is GridWorldState) {

                            SearchEnvelopeCell(state.agentLocation, it.heuristic)
                        } else {
                            null
                        }
                    }
                    .filterNotNullTo(mapInfo.backPropagation)

        }

    }

    fun <StateType : State<StateType>, NodeType : SearchNode<StateType, NodeType>>
            updateAgentLocation(agentNode: SearchNode<StateType, NodeType>) {
        Platform.runLater {
            val state = agentNode.state
            if (state is GridWorldState) {
                mapInfo.agentCell = state.agentLocation

//                grid.clear()
//                grid.draw()
            }
        }
    }

    fun <StateType : State<StateType>, NodeType : SearchNode<StateType, NodeType>>
            updateFocusedNode(focusedNode: SearchNode<StateType, NodeType>?) {
        Platform.runLater {
            val state = focusedNode?.state

            if (state != null && state is GridWorldState) {
                mapInfo.focusedCell = state.agentLocation

            } else {
                mapInfo.focusedCell = null
            }

            grid.clear()
            grid.draw()
        }
    }

    fun <StateType : State<StateType>, NodeType : SearchNode<StateType, NodeType>>
            updateRootToBest(rootToBestChain: Collection<SearchNode<StateType, NodeType>>) {
        Platform.runLater {
            mapInfo.rootToBestChain.clear()

            rootToBestChain
                    .map {
                        val state = it.state
                        if (state is GridWorldState) {
                            state.agentLocation
                        } else {
                            null
                        }
                    }
                    .filterNotNullTo(mapInfo.rootToBestChain)
        }
    }

    fun <StateType : State<StateType>, NodeType : SearchNode<StateType, NodeType>>
            updateCommonAncestorToAgentChain(commonAncestorToAgentChain: Collection<SearchNode<StateType, NodeType>>) {
        Platform.runLater {
            mapInfo.agentToCommonAncestorChain.clear()

            commonAncestorToAgentChain
                    .map {
                        val state = it.state
                        if (state is GridWorldState) {
                            state.agentLocation
                        } else {
                            null
                        }
                    }
                    .filterNotNullTo(mapInfo.agentToCommonAncestorChain)
        }
    }

    fun delay() {
        Thread.sleep(20)

        Platform.runLater {
            grid.clear()
            grid.draw()
        }
    }
}
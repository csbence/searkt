package edu.unh.cs.ai.realtimesearch.visualizer.gridbased

import edu.unh.cs.ai.realtimesearch.environment.location.Location
import edu.unh.cs.ai.realtimesearch.visualizer.ThemeColors
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlin.math.min

/**
 * A canvas which displays the grid specified in the provided {@link MapInfo}.  All cells except start cells are
 * displayed automatically.
 *
 * @author Mike Bogochow (mgp36@unh.edu)
 * @since April 8, 2016
 */
class GridCanvasPane(val mapInfo: MapInfo, val tileSize: Double) : Pane() {
    val canvas: Canvas = Canvas()
    val gridWidth = mapInfo.columnCount * tileSize
    val gridHeight = mapInfo.rowCount * tileSize

    // Appearance parameters
    var rowLineColor = ThemeColors.GRID.stroke
    var rowLineWidth = 0.1
    var blockedCellColor = ThemeColors.OBSTACLE.color
    var startCellBackgroundColor: Color? = ThemeColors.EMPTY_SPACE.color
    var goalCellBackgroundColor: Color? = ThemeColors.EMPTY_SPACE.color
    var goalCircleColor = ThemeColors.GOAL.color
    var goalCircleRadius = tileSize / 10.0

    init {
        children.add(canvas)
    }

    companion object {
        val ZERO = GridCanvasPane(MapInfo.ZERO, 0.0)
    }

    override fun layoutChildren() {
        draw()
    }

    fun draw() {
        val top = snappedTopInset()
        val right = snappedRightInset()
        val bottom = snappedBottomInset()
        val left = snappedLeftInset()
        val layoutWidth = width - left - right
        val layoutHeight = height - top - bottom
        canvas.layoutX = left
        canvas.layoutY = top

        canvas.width = layoutWidth
        canvas.height = layoutHeight
        val graphicsContext: GraphicsContext = canvas.graphicsContext2D

//        addGrid(graphicsContext)

        // Add blocked cells
        graphicsContext.fill = blockedCellColor
        for (cell in mapInfo.blockedCells) {
            addCell(graphicsContext, cell)
        }

        // Add searchEnvelope
        graphicsContext.fill = Color.rgb(214, 210, 196, 0.5)
        for (cell in mapInfo.searchEnvelope) {
            addCell(graphicsContext, cell.location)
            addHeuristic(graphicsContext, cell)
        }

        // Add searchEnvelope
        graphicsContext.fill = Color.rgb(214, 100, 100, 0.5)
        for (cell in mapInfo.backPropagation) {
            addCell(graphicsContext, cell.location)
            addHeuristic(graphicsContext, cell)
        }

        // Add goal cells
        val diameter = goalCircleRadius * 2
        for (cell in mapInfo.goalCells) {
            val dirtyLocX = cell.x * tileSize + tileSize / 2.0 - goalCircleRadius
            val dirtyLocY = cell.y * tileSize + tileSize / 2.0 - goalCircleRadius

            if (goalCellBackgroundColor != null) {
                addCell(graphicsContext, cell, goalCellBackgroundColor)
            }
            graphicsContext.fill = goalCircleColor
            graphicsContext.fillOval(dirtyLocX, dirtyLocY, diameter, diameter)
        }

        // Add start cells
        if (startCellBackgroundColor != null) {
            graphicsContext.fill = startCellBackgroundColor
            for (cell in mapInfo.startCells) {
                addCell(graphicsContext, cell)
            }
        }

        addLine(graphicsContext, mapInfo.rootToBestChain, ThemeColors.GOAL.color, 3.0)
        addLine(graphicsContext, mapInfo.agentToCommonAncestorChain, ThemeColors.AGENT.color, 3.0)

        // Add agent cells
        val agentCell = mapInfo.agentCell
//        val agentCircleRadius = tileSize / 5.0
        val agentCircleRadius = min(layoutWidth, layoutHeight) / 30.0
        if (startCellBackgroundColor != null && agentCell != null) {
            graphicsContext.fill = ThemeColors.AGENT.color
            addCircle(graphicsContext, agentCell, agentCircleRadius, true)
        }

        // Add agent cells
        val focusCell = mapInfo.focusedCell
        if (startCellBackgroundColor != null && focusCell != null) {
            graphicsContext.stroke = ThemeColors.AGENT.color
            addCircle(graphicsContext, focusCell, agentCircleRadius, true)
        }
    }

    private fun addHeuristic(graphicsContext: GraphicsContext, cell: SearchEnvelopeCell) {
        graphicsContext.fillText(
                cell.value.toString(),
                cell.location.x.toDouble() * tileSize,
                cell.location.y.toDouble() * tileSize
        )
    }

    private fun addCircle(graphicsContext: GraphicsContext, cell: Location, radius: Double, fill: Boolean) {
        val centerX = cell.x * tileSize + tileSize / 2.0 - radius
        val centerY = cell.y * tileSize + tileSize / 2.0 - radius
        val diameter = radius * 2

        if (fill) {
            graphicsContext.fillOval(centerX, centerY, diameter, diameter)
        } else {
            graphicsContext.strokeOval(centerX, centerY, diameter, diameter)
        }
    }

    fun clear() {
        val top = snappedTopInset()
        val right = snappedRightInset()
        val bottom = snappedBottomInset()
        val left = snappedLeftInset()
        val layoutWidth = width - left - right
        val layoutHeight = height - top - bottom

        canvas.graphicsContext2D.clearRect(0.0, 0.0, layoutWidth, layoutHeight)
    }

    private fun addLine(graphicsContext: GraphicsContext, cells: Collection<Location>, color: Color, lineWidth: Double) {
        val vertexCount = cells.size
        val xLocations = DoubleArray(vertexCount)
        val yLocations = DoubleArray(vertexCount)

        cells.forEachIndexed { i, location ->
            xLocations[i] = location.x * tileSize + tileSize / 2.0
            yLocations[i] = location.y * tileSize + tileSize / 2.0
        }

        graphicsContext.stroke = color
        graphicsContext.lineWidth = lineWidth
        graphicsContext.strokePolyline(xLocations, yLocations, vertexCount)
    }

    private fun addCell(graphicsContext: GraphicsContext, cell: Location, paint: Paint? = null) {
        if (paint != null) {
            graphicsContext.fill = paint
        }
        graphicsContext.fillRect(cell.x.toDouble() * tileSize, cell.y.toDouble() * tileSize, tileSize, tileSize)
    }

    private fun addGrid(graphicsContext: GraphicsContext) {
        graphicsContext.lineWidth = 1.0

        // Add row lines
        graphicsContext.stroke = rowLineColor
        graphicsContext.lineWidth = rowLineWidth
        for (row in 1..mapInfo.rowCount) {
            val yPosition = row * tileSize
            graphicsContext.strokeLine(0.0, yPosition, gridWidth, yPosition)
        }

        // Add column lines
        for (column in 1..mapInfo.columnCount) {
            val xPosition = column * tileSize
            graphicsContext.strokeLine(xPosition, 0.0, xPosition, gridHeight)
        }
    }
}
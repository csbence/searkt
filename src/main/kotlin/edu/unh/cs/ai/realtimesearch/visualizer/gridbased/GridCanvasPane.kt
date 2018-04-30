package edu.unh.cs.ai.realtimesearch.visualizer.gridbased

import edu.unh.cs.ai.realtimesearch.environment.location.Location
import edu.unh.cs.ai.realtimesearch.visualizer.ThemeColors
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.paint.Paint

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

//        if (layoutWidth != canvas.width || layoutHeight != canvas.height) {
        canvas.width = layoutWidth
        canvas.height = layoutHeight
        val graphicsContext: GraphicsContext = canvas.graphicsContext2D

        addGrid(graphicsContext)

        // Add blocked cells
        graphicsContext.fill = blockedCellColor
        for (cell in mapInfo.blockedCells) {
            addCell(graphicsContext, cell)
        }

        // Add searchEnvelope
        println("env size: ${mapInfo.searchEnvelope.size}")
        graphicsContext.fill = Color.rgb(214, 210, 196, 0.5)
        for (cell in mapInfo.searchEnvelope) {
            println("env cell: ${cell.location}")
            addCell(graphicsContext, cell.location)
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
        val agentCircleRadius = tileSize / 5.0
        val agentCircleDiameter = agentCircleRadius * 2
        if (startCellBackgroundColor != null && agentCell != null) {
            graphicsContext.fill = ThemeColors.AGENT.color
            val dirtyLocX = agentCell.x * tileSize + tileSize / 2.0 - agentCircleRadius
            val dirtyLocY = agentCell.y * tileSize + tileSize / 2.0 - agentCircleRadius
            graphicsContext.fillOval(dirtyLocX, dirtyLocY, agentCircleDiameter, agentCircleDiameter)
        }

//        }
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
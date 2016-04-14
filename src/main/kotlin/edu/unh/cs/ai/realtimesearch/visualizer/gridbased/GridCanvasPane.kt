package edu.unh.cs.ai.realtimesearch.visualizer.gridbased

import edu.unh.cs.ai.realtimesearch.visualizer.ThemeColors
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.layout.Pane
import javafx.scene.paint.Color

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
        val top = snappedTopInset()
        val right = snappedRightInset()
        val bottom = snappedBottomInset()
        val left = snappedLeftInset()
        val layoutWidth = width - left - right
        val layoutHeight = height - top - bottom
        canvas.layoutX = left
        canvas.layoutY = top

        if (layoutWidth != canvas.width || layoutHeight != canvas.height) {
            canvas.width = layoutWidth
            canvas.height = layoutHeight
            val g: GraphicsContext = canvas.graphicsContext2D

            // Add row lines
            g.stroke = rowLineColor
            g.lineWidth = rowLineWidth
            for (row in 1..mapInfo.rowCount) {
                val yPosition = row * tileSize
                g.strokeLine(0.0, yPosition, gridWidth, yPosition)
            }

            // Add column lines
            for (column in 1..mapInfo.columnCount) {
                val xPosition = column * tileSize
                g.strokeLine(xPosition, 0.0, xPosition, gridHeight)
            }

            // Add blocked cells
            g.fill = blockedCellColor
            for (cell in mapInfo.blockedCells) {
                g.fillRect(cell.x.toDouble() * tileSize, cell.y.toDouble() * tileSize, tileSize, tileSize)
            }

            // Add goal cells
            val diameter = goalCircleRadius * 2
            for (cell in mapInfo.goalCells) {
                val dirtyLocX = cell.x * tileSize + tileSize / 2.0 - goalCircleRadius
                val dirtyLocY = cell.y * tileSize + tileSize / 2.0 - goalCircleRadius

                if (goalCellBackgroundColor != null) {
                    g.fill = goalCellBackgroundColor
                    g.fillRect(cell.x.toDouble() * tileSize, cell.y.toDouble() * tileSize, tileSize, tileSize)
                }
                g.fill = goalCircleColor
                g.fillOval(dirtyLocX, dirtyLocY, diameter, diameter)
            }

            // Add start cells
            if (startCellBackgroundColor != null) {
                g.fill = startCellBackgroundColor
                for (cell in mapInfo.startCells) {
                    g.fillRect(cell.x.toDouble() * tileSize, cell.y.toDouble() * tileSize, tileSize, tileSize)
                }
            }
        }
    }
}
package edu.unh.cs.ai.realtimesearch.visualizer.gridbased

import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Rectangle

/**
 * Visual components of robot for grid-based visualizations.
 *
 * @author Mike Bogochow (mgp36@unh.edu)
 * @since April 8, 2016
 */
class RobotView(val width: Double) {
    private val TRACKER_SCALE = 4.0
    val robot = Rectangle(width, width, Color.ORANGE)
    val tracker = Circle(width * TRACKER_SCALE, Color.YELLOW)
    var trackingEnabled: Boolean = false
        set(value) {
            field = value
            relocateTracker()
            tracker.isVisible = true
        }

    init {
        if (!trackingEnabled)
            tracker.isVisible = false

        tracker.translateXProperty().bind(robot.translateXProperty())
        tracker.translateYProperty().bind(robot.translateYProperty())

        tracker.opacity = 0.25

        if (tracker.radius < 10.0)
            tracker.radius = 10.0
    }

    fun toFront() {
        tracker.toFront()
        robot.toFront()
    }

    fun toBack() {
        robot.toBack()
        tracker.toBack()
    }

    private fun relocateTracker() {
        tracker.centerX = robot.x + width / 2.0
        tracker.centerY = robot.y + width / 2.0
    }

    fun setLocation(x: Double, y: Double) {
        robot.x = x
        robot.y = y
        robot.translateX = robot.x
        robot.translateY = robot.y
        relocateTracker()
    }

    fun translateX(x: Double) {
        robot.translateX = x
    }

    fun translateY(y: Double) {
        robot.translateY = y
    }
}
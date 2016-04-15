package edu.unh.cs.ai.realtimesearch.visualizer.gridbased

import edu.unh.cs.ai.realtimesearch.visualizer.ThemeColors
import javafx.scene.shape.Circle
import javafx.scene.shape.Rectangle

/**
 * Visual components of robot for grid-based visualizations.
 *
 * @author Mike Bogochow (mgp36@unh.edu)
 * @since April 8, 2016
 */
class AgentView(val width: Double, val minimumTrackerRadius: Double = 10.0) {
    private val TRACKER_SCALE = 4.0
    val agent = Rectangle(width, width, ThemeColors.AGENT.color)
    val tracker = Circle(width * TRACKER_SCALE, ThemeColors.AGENT.stroke)
    var trackingEnabled: Boolean
        set(value) {
            tracker.isVisible = value
            relocateTracker()
        }
        get() = tracker.isVisible

    init {
        trackingEnabled = false

        tracker.translateXProperty().bind(agent.translateXProperty())
        tracker.translateYProperty().bind(agent.translateYProperty())

        tracker.opacity = 0.25

        if (tracker.radius < minimumTrackerRadius)
            tracker.radius = minimumTrackerRadius
    }

    companion object {
        val ZERO = AgentView(0.0, 0.0)
    }

    fun toFront() {
        tracker.toFront()
        agent.toFront()
    }

    fun toBack() {
        agent.toBack()
        tracker.toBack()
    }

    private fun relocateTracker() {
        tracker.centerX = agent.x + width / 2.0
        tracker.centerY = agent.y + width / 2.0
    }

    fun setLocation(x: Double, y: Double) {
        agent.x = x
        agent.y = y
        relocateTracker()
    }

    fun translateX(x: Double) {
        agent.translateX = x
    }

    fun translateY(y: Double) {
        agent.translateY = y
    }
}
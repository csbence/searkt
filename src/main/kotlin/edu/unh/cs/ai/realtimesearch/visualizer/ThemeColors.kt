package edu.unh.cs.ai.realtimesearch.visualizer

import javafx.scene.paint.Color
import javafx.scene.paint.Color.*

/**
 * @author Bence Cserna (bence@cserna.net)
 */
enum class ThemeColors(val color: Color, val stroke: Color = color, val opacity: Double = 1.0) {
    // General
    ERROR_TEXT(RED),
    BACKGROUND(WHITE),

    // Grid based
    GRID(UNH_SECODARY_GRAY),
    OBSTACLE(UNH_SECODARY_BLUE),
    PATH(INDIANRED),
    AGENT(UNH_ACCENT),
    START(UNH_PRIMARY_BLUE),
    GOAL(UNH_PRIMARY_BLUE),
    GOAL_CIRCLE(WHITE, UNH_PRIMARY_BLUE, 0.5),
    EMPTY_SPACE(UNH_WHITE, UNH_WHITE),

    // Acrobot
    LINK(BLACK, BLACK, 0.5),
    JOINT(RED);

    companion object {
        val UNH_PRIMARY_BLUE = Color.rgb(0, 53, 148)
        val UNH_WHITE = Color.WHITE
        val UNH_SECODARY_BLUE = Color.rgb(37, 55, 70)
        val UNH_SECODARY_GRAY = Color.rgb(162, 170, 173)
        val UNH_SECODARY_SAND = Color.rgb(214,210, 196)
        val UNH_ACCENT = Color.rgb(247, 122, 5)
    }

}
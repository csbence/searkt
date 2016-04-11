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
    GRID(Color.grayRgb(170, 0.5)),
    OBSTACLE(Color.grayRgb(50)),
    PATH(INDIANRED),
    AGENT(ORANGE),
    START(SALMON),
    GOAL(BLUEVIOLET),
    GOAL_CIRCLE(WHITE, BLUE, 0.5),
    EMPTY_SPACE(WHITE, WHITE),

    // Acrobot
    LINK(BLACK, BLACK, 0.5),
    JOINT(RED);

}
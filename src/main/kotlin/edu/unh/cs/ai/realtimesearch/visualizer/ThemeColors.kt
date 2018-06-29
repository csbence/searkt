//package edu.unh.cs.ai.realtimesearch.visualizer
//
//import javafx.scene.paint.Color
//import javafx.scene.paint.Color.WHITE
//
///**
// * @author Bence Cserna (bence@cserna.net)
// */
//enum class ThemeColors(val color: Color, val stroke: Color = color, val opacity: Double = 1.0) {
//
//    UNH_PRIMARY_BLUE (Color.rgb(0, 53, 148)),
//    UNH_WHITE (Color.WHITE),
//    UNH_SECONDARY_BLUE (Color.rgb(37, 55, 70)),
//    UNH_SECONDARY_GRAY (Color.rgb(162, 170, 173)),
//    UNH_SECONDARY_SAND (Color.rgb(214, 210, 196)),
//    UNH_ACCENT (Color.rgb(247, 122, 5)),
//
//    // General
//    ERROR_TEXT(Color.RED),
//    BACKGROUND(UNH_WHITE.color),
//
//    // Grid based
//    GRID(UNH_SECONDARY_GRAY.color),
//    OBSTACLE(UNH_SECONDARY_BLUE.color),
//    PATH(Color.INDIANRED),
//    AGENT(UNH_ACCENT.color),
//    START(UNH_PRIMARY_BLUE.color),
//    GOAL(UNH_PRIMARY_BLUE.color),
//    GOAL_CIRCLE(WHITE, UNH_PRIMARY_BLUE.color, 0.5),
//    EMPTY_SPACE(UNH_WHITE.color, UNH_WHITE.color),
//
//    // Acrobot
//    LINK(UNH_WHITE.color, UNH_SECONDARY_BLUE.color, 0.5),
//    JOINT(UNH_ACCENT.color);
//}
//package edu.unh.cs.ai.realtimesearch.visualizer.acrobot
//
//import edu.unh.cs.ai.realtimesearch.environment.acrobot.AcrobotState
//import edu.unh.cs.ai.realtimesearch.visualizer.ThemeColors
//import javafx.event.EventHandler
//import javafx.scene.Node
//import javafx.scene.paint.Paint
//import javafx.scene.shape.Circle
//import javafx.scene.shape.Line
//import javafx.scene.shape.StrokeLineCap
//import javafx.scene.transform.Rotate
//import javafx.scene.transform.TransformChangedEvent
//
///**
// * Visual components of an Acrobot for animation.
// *
// * @author Mike Bogochow (mgp36@unh.edu)
// */
//data class AcrobotView(val linkStartX1: Double, val linkStartY1: Double, val linkScale: Double, val linkWidth: Double) {
//    val linkScaledLength1 = AcrobotState.linkLength1 * linkScale
//    val linkScaledLength2 = AcrobotState.linkLength2 * linkScale
//    val linkStartX2 = linkStartX1
//    val linkStartY2 = linkStartY1 + linkScaledLength1
//
//    // Link setup
//    val link1 = Line(linkStartX1, linkStartY1, linkStartX1, linkStartY1 + linkScaledLength1)
//    val link2 = Line(linkStartX2, linkStartY2, linkStartX2, linkStartY2 + linkScaledLength2)
//
//    // Joint setup
//    val joint1 = Circle(linkStartX1, linkStartY1, linkWidth * 0.6)
//    val joint2 = Circle(linkStartX2, linkStartY2, joint1.radius)
//
//    // Rotation setup
//    val linkRotate1 = Rotate(0.0, linkStartX1, linkStartY1, 0.0, Rotate.Z_AXIS)
//    val linkRotate2 = Rotate(0.0, linkStartX2, linkStartY2, 0.0, Rotate.Z_AXIS)
//
//    var opacity: Double
//        get() = link1.opacity
//        set(value) {
//            link1.opacity = value
//            link2.opacity = value
//            joint1.opacity = value
//            joint2.opacity = value
//        }
//
//    var isVisible: Boolean
//        get() = link1.isVisible
//        set(value) {
//            link1.isVisible = value
//            link2.isVisible = value
//            joint1.isVisible = value
//            joint2.isVisible = value
//        }
//
//    var linkColor: Paint
//        get() = link1.stroke
//        set(value) {
//            link1.stroke = value
//            link2.stroke = value
//        }
//
//    var jointColor: Paint
//        get() = joint1.fill
//        set(value) {
//            joint1.fill = value
//            joint2.fill = value
//        }
//
//    fun toBack() {
//        link1.toBack()
//        link2.toBack()
//        joint1.toBack()
//        joint2.toBack()
//    }
//
//    init {
//        jointColor = ThemeColors.JOINT.color
//        linkColor = ThemeColors.LINK.color
//
//        link1.strokeWidth = linkWidth
//        link2.strokeWidth = linkWidth
//        link1.strokeLineCap = StrokeLineCap.BUTT
//        link2.strokeLineCap = StrokeLineCap.BUTT
//
//        link1.transforms.add(linkRotate1)
//        link2.transforms.add(linkRotate2)
//
//        /*
//         * Keep the moving parts attached to link1 updated as it rotates.  Uses rotation matrix math to find the
//         * anchor point of link2.
//         */
//        linkRotate1.onTransformChanged = EventHandler<TransformChangedEvent> {
//            var angle = Math.atan2(-link1.localToSceneTransform.mxy, link1.localToSceneTransform.mxx) + Math.PI / 2
//            angle = if (angle < 0) angle + 2 * Math.PI else if (angle > 2 * Math.PI) angle - 2 * Math.PI else angle
//
//            val newX = link1.startX + linkScaledLength1 * Math.cos(angle)
//            val newY = link1.startY + linkScaledLength1 * Math.sin(angle)
//            val translateX = newX - joint2.centerX
//            val translateY = newY - joint2.centerY
//
//            joint2.translateX = translateX
//            joint2.translateY = translateY
//            link2.translateX = translateX
//            link2.translateY = translateY
//        }
//    }
//
//    /**
//     * Get a list of all nodes which make up the view.
//     */
//    fun getNodes(): List<Node> = listOf(link1, link2, joint1, joint2)
//
//    /**
//     * Adds a copy of the given rotation to the given link.
//     *
//     * @return the new rotate
//     */
//    private fun addRotate(rotate: Rotate, link: Line): Rotate {
//        val newRotate = rotate.clone()
//        link.transforms.add(newRotate)
//        return newRotate
//    }
//
//    /**
//     * Add a rotation to link 1.
//     *
//     * @return the new rotate
//     */
//    fun addRotate1(): Rotate {
//        val newRotate = addRotate(linkRotate1, link1)
//        newRotate.onTransformChanged = linkRotate1.onTransformChanged
//        return newRotate
//    }
//
//    /**
//     * Add a rotation to link 2.
//     *
//     * @return the new rotate
//     */
//    fun addRotate2(): Rotate = addRotate(linkRotate2, link2)
//}
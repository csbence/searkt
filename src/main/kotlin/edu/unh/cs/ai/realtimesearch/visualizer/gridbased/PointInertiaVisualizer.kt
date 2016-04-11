package edu.unh.cs.ai.realtimesearch.visualizer.gridbased

import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.visualizer.BaseVisualizer
import groovyjarjarcommonscli.CommandLine
import groovyjarjarcommonscli.Options
import javafx.animation.Interpolator
import javafx.animation.PathTransition
import javafx.animation.SequentialTransition
import javafx.animation.Timeline
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.*
import javafx.stage.Stage
import javafx.util.Duration
import java.util.*

/**
 * Created by Stephen on 2/29/16.
 */
class PointInertiaVisualizer : PointVisualizer() {
    private var xDot = 0.0
    private var yDot = 0.0


//        val sq = SequentialTransition()
//        var count = 0
//        while (count != actionList.size) {
//            val x = actionList.get(count)
//            val y = actionList.get(count + 1)
//            val ptList = animate(root, x, y, DISPLAY_LINE, robot, TILE_SIZE)
//            for(pt in ptList)
//                sq.children.add(pt)
//            count+=2
//        }
//        sq.setCycleCount(Timeline.INDEFINITE);
//        sq.play()
//    }



    override protected fun playAnimation(transitions: List<PathTransition>) {
        val sequentialTransition = SequentialTransition()
        for (pathTransition in transitions) {
            sequentialTransition.children.add(pathTransition)
        }
        sequentialTransition.cycleCount = Timeline.INDEFINITE
        sequentialTransition.play()
    }

    override fun animate(x: String, y: String): MutableList<PathTransition> {
        val robot = robotView.robot
        val width = tileSize
        val retval: MutableList<PathTransition> = arrayListOf()

        val xDDot = x.toDouble() * width
        val yDDot = y.toDouble() * width

        val nSteps = 100
        val dt = 1.0 / nSteps

        for (i in 0..nSteps-1) {
            val path = Path()
            path.elements.add(MoveTo(robot.translateX, robot.translateY))

            var xdot = xDot + xDDot * (dt * i)
            var ydot = yDot + yDDot * (dt * i)

            path.elements.add(LineTo(robot.translateX + (xdot * dt), robot.translateY + (ydot * dt)))
            robot.translateX += xdot * dt;
            robot.translateY += ydot * dt;

            if(displayLine){
                path.stroke = Color.RED
                grid.children.add(path)
            }
            /* Animate the robot */
            val pathTransition = PathTransition()
            pathTransition.duration = Duration.millis(10.0)
            pathTransition.path = path
            pathTransition.node = robot
            pathTransition.interpolator = Interpolator.LINEAR
            retval.add(pathTransition)
        }

        xDot += xDDot
        yDot += yDDot

        if(displayLine){
            val action = Circle(robot.translateX, robot.translateY, width / 10.0)
            grid.children.add(action)
        }
        return retval
    }
}

package edu.unh.cs.ai.realtimesearch.visualizer

/**
 * Created by Stephen on 2/11/16.
 */

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import javafx.scene.paint.Color
import java.util.*
import javafx.scene.Group;
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import kotlin.system.exitProcess
import edu.unh.cs.ai.realtimesearch.environment.Action

class VaccumVisualizer : Application() {
    override fun start(primaryStage: Stage) {
        /* Get domain from Application */
        val parameters = getParameters()!!
        val raw = parameters.getRaw()!!
        if(raw.isEmpty()){
            println("Cannot visualize without a domain!")
            exitProcess(1)
        }
        val rawDomain = raw.first()

        /* Get action list from Application */
        val actionList: MutableList<String> = arrayListOf()
        for (i in 1..raw.size - 1){
            actionList.add(raw.get(i))
        }
        println(actionList)

        /* Assuming the domain is correct because the experiment was already run */
        primaryStage.title = "RTS Visualizer"
        val inputScanner = Scanner(rawDomain.byteInputStream())

        val rowCount: Int
        val columnCount: Int

        columnCount = inputScanner.nextLine().toInt()
        rowCount = inputScanner.nextLine().toInt()

        /* Graphical parameters */
        val WIDTH = 1200.0
        val HEIGHT = 700.0
        val TILE_WIDTH: Double = (WIDTH/columnCount) * 0.98
        val TILE_HEIGHT: Double = (HEIGHT/rowCount) * 0.98

        val root = Group()
        val canvas = Canvas(WIDTH,HEIGHT)
        val gc = canvas.graphicsContext2D

        for (y in 0..rowCount - 1) {
            val line = inputScanner.nextLine()

            for (x in 0..columnCount - 1) {
                when (line[x]) {
                    '#' -> {
                        gc.fill = Color.BLACK
                        gc.stroke = Color.BLACK
                        gc.fillRect(x*TILE_WIDTH, y*TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);
                    }
                    '_' -> {
                        gc.fill = Color.LIGHTSLATEGRAY
                        gc.stroke = Color.LIGHTSLATEGRAY
                        gc.fillRect(x*TILE_WIDTH, y*TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);
                    }
                    '*' -> {
                        /* Draw background */
                        gc.fill = Color.LIGHTSLATEGRAY
                        gc.stroke = Color.LIGHTSLATEGRAY
                        gc.fillRect(x*TILE_WIDTH, y*TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);

                        /* Draw dirty cell on top */
                        gc.fill = Color.BLUE
                        val radius = TILE_WIDTH/4
                        gc.fillOval(x*TILE_WIDTH + ((TILE_WIDTH-radius)/2), y*TILE_HEIGHT + ((TILE_HEIGHT-radius)/2),
                                radius, radius);
                    }
                    '@' -> {
                        /* Draw background */
                        gc.fill = Color.LIGHTSLATEGRAY
                        gc.stroke = Color.LIGHTSLATEGRAY
                        gc.fillRect(x*TILE_WIDTH, y*TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);

                        /* Draw robot on top */
                        gc.fill = Color.ORANGE
                        val width = TILE_WIDTH/4
                        gc.fillRect(x*TILE_WIDTH + ((TILE_WIDTH-width)/2), y*TILE_HEIGHT + ((TILE_HEIGHT-width)/2),
                                width, width);
                    }
                }
            }
        }

        root.children.add(canvas)
        primaryStage.scene = Scene(root, WIDTH, HEIGHT)
        primaryStage.show()
    }
    private fun animate(gc: GraphicsContext){

    }

}

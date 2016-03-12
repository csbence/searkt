package edu.unh.cs.ai.realtimesearch.visualizer

import edu.unh.cs.ai.realtimesearch.experiment.configuration.json.experimentResultFromJson
import edu.unh.cs.ai.realtimesearch.visualizer.acrobot.AcrobotVisualizer
import edu.unh.cs.ai.realtimesearch.visualizer.gridbased.PointInertiaVisualizer
import edu.unh.cs.ai.realtimesearch.visualizer.gridbased.PointVisualizer
import edu.unh.cs.ai.realtimesearch.visualizer.gridbased.RacetrackVisualizer
import edu.unh.cs.ai.realtimesearch.visualizer.gridbased.VacuumVisualizer
import javafx.application.Application
import java.io.File

/**
 * Created by Stephen on 3/10/16.
 */
fun main(args: Array<String>) {
    if (args.size < 1){
        throw IllegalArgumentException("Visualizer takes one argument which is the result file. Aborting.")
    }
    val fileName = args.first()
    val fileString = File(fileName).readText()
    val experimentResult = experimentResultFromJson(fileString)
    val domainName = experimentResult.experimentConfiguration["domainName"]

    val params: MutableList<String> = mutableListOf()
    params.add(fileString)
    for (arg in args)
        params.add(arg)

    when (domainName){
        "vacuum world" -> {
            Application.launch(VacuumVisualizer::class.java, *params.toTypedArray())
        }
        "grid world" -> {
            Application.launch(VacuumVisualizer::class.java, *params.toTypedArray())
        }
        "point robot" -> {
            Application.launch(PointVisualizer::class.java, *params.toTypedArray())
        }
        "point robot with inertia" -> {
            Application.launch(PointInertiaVisualizer::class.java, *params.toTypedArray())
        }
        "race track" -> {
            Application.launch(RacetrackVisualizer::class.java, *params.toTypedArray())
        }
        "acrobot" -> {
            Application.launch(AcrobotVisualizer::class.java, *params.toTypedArray())
        }
        else -> {
            throw IllegalArgumentException("Error: Domain '$domainName' not recognized! Aborting")
        }
    }
}
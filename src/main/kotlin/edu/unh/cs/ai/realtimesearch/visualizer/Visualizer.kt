package edu.unh.cs.ai.realtimesearch.visualizer

import edu.unh.cs.ai.realtimesearch.experiment.configuration.ConfigurationExecutor
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.json.experimentConfigurationFromJson
import edu.unh.cs.ai.realtimesearch.experiment.configuration.json.experimentDataFromJson
import edu.unh.cs.ai.realtimesearch.experiment.configuration.json.experimentResultFromJson
import javafx.application.Application
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

/**
 * Created by Stephen on 3/10/16.
 */
public class Visualizer {
    companion object {
        @JvmStatic public fun main(args: Array<String>) {
            if(args.size != 1){
                println("Error: Visualizer takes one argument which is the result file. Aborting.")
                exitProcess(1)
            }
            val fileName = args.first()
            val fileString = Files.readAllLines(Paths.get(fileName)).first()
            val experimentConfig = experimentConfigurationFromJson(fileString)
            val experimentResult = experimentResultFromJson(fileString)
            println(experimentResult["actions"])
            println(experimentResult["experimentConfiguration"])

            /*val params: MutableList<String> = arrayListOf()
            val actionList = experimentResult.actions

            params.add(experimentConfig.rawDomain)
            for (action in actionList) {
                params.add(action.toString())
            }

            when (experimentConfig.domainName){
                "grid world" -> {
                    Application.launch(VacuumVisualizer::class.java, *params.toTypedArray())
                }
                "point robot" -> {
                    Application.launch(PointVisualizer::class.java, *params.toTypedArray())
                }
                "point robot with inertia" -> {
                    Application.launch(PointIntertiaVisualizer::class.java, *params.toTypedArray())
                }
                "race track" -> {
                    Application.launch(RacetrackVisualizer::class.java, *params.toTypedArray())
                }
                else -> {
                    println("Error: Domain not recognized! Aborting")
                    exitProcess(1)
                }
            }*/
        }
    }
}

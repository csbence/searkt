package edu.unh.cs.ai.realtimesearch.visualizer

import edu.unh.cs.ai.realtimesearch.environment.Domains
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.json.experimentResultFromJson
import edu.unh.cs.ai.realtimesearch.experiment.configuration.json.toJson
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import edu.unh.cs.ai.realtimesearch.visualizer.acrobot.AcrobotVisualizer
import edu.unh.cs.ai.realtimesearch.visualizer.gridbased.PointInertiaVisualizer
import edu.unh.cs.ai.realtimesearch.visualizer.gridbased.PointVisualizer
import edu.unh.cs.ai.realtimesearch.visualizer.gridbased.RacetrackVisualizer
import edu.unh.cs.ai.realtimesearch.visualizer.gridbased.VacuumVisualizer
import javafx.application.Application
import java.io.File

/**
 * Main launcher for visualizers.
 *
 * @author Stephen Chambers, Mike Bogochow
 * @since 3/10/16
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) throw IllegalArgumentException("Visualizer takes one argument which is the result file. Aborting.")

    val argsIterator = args.iterator()
    val fileName = argsIterator.next()
    val fileString = File(fileName).readText()
    val experimentResult = experimentResultFromJson(fileString)

    val params: MutableList<String> = mutableListOf()
    while (argsIterator.hasNext())
        params.add(argsIterator.next())

    runVisualizer(experimentResult, params)
}

fun runVisualizer(result: ExperimentResult, params: MutableList<String> = mutableListOf()) {
    val domainName = result.configuration[Configurations.DOMAIN_NAME.toString()] as String
    params.add(0, result.toJson())
    when (Domains.valueOf(domainName)) {
        Domains.VACUUM_WORLD -> Application.launch(VacuumVisualizer::class.java, *params.toTypedArray())
        Domains.GRID_WORLD -> Application.launch(VacuumVisualizer::class.java, *params.toTypedArray())
        Domains.POINT_ROBOT -> Application.launch(PointVisualizer::class.java, *params.toTypedArray())
        Domains.POINT_ROBOT_LOST -> Application.launch(PointVisualizer::class.java, *params.toTypedArray())
        Domains.POINT_ROBOT_WITH_INERTIA -> Application.launch(PointInertiaVisualizer::class.java, *params.toTypedArray())
        Domains.RACETRACK -> Application.launch(RacetrackVisualizer::class.java, *params.toTypedArray())
        Domains.ACROBOT -> Application.launch(AcrobotVisualizer::class.java, *params.toTypedArray())
        else -> throw IllegalArgumentException("Error: Domain '$domainName' not recognized! Aborting")
    }
}
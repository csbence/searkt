package edu.unh.cs.searkt

import edu.unh.cs.searkt.environment.Domains
import edu.unh.cs.searkt.experiment.configuration.Configurations
import edu.unh.cs.searkt.experiment.configuration.SimpleSerializer
import edu.unh.cs.searkt.experiment.configuration.cartesianProduct
import edu.unh.cs.searkt.planner.Planners
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import java.util.concurrent.TimeUnit

/**
 * Generate experiment configurations.
 *
 * Modify the content of this function to create custom experiments.
 */
fun generateConfigurations(): String {
    val domains = mutableListOf<Pair<Domains, String>>(
//            Domains.RACETRACK to "input/racetrack/uniform.track"
//            ,Domains.RACETRACK to "input/racetrack/hansen-bigger.track"
//            ,Domains.RACETRACK to "input/racetrack/hansen-bigger-octa.track"
    )

//    domains += (1..5).map { Domains.SLIDING_TILE_PUZZLE_4 to "input/tiles/korf/4/real/$it" }
    domains += listOf(7,10,13).map { Domains.SLIDING_TILE_PUZZLE_4 to "input/tiles/korf/4/real/$it" }
//    domains += (0..10).map { Domains.GRID_WORLD to "input/vacuum/orz100d/orz100d.map_scen_$it" }
//    domains += (0..10).map { Domains.GRID_WORLD to "input/vacuum//orz100d.map_scen_$it" }

    // Maximum time per experiment
    val timeLimit = TimeUnit.NANOSECONDS.convert(5, TimeUnit.MINUTES)

    val domainExtras = listOf(
            Triple(Domains.RACETRACK, Configurations.DOMAIN_SEED.toString(), 0..2L)
    )

    var configurations = edu.unh.cs.searkt.experiment.configuration.generateConfigurations(
            domains = domains,
            planners = listOf(Planners.WEIGHTED_A_STAR, Planners.EES, Planners.BOUNDED_SUBOPTIMAL_EXPLORATION),
            actionDurations = listOf(1),
            timeLimit = timeLimit,
            plannerExtras = listOf(
            ),
            domainExtras = domainExtras
    )

    // Add these to all configurations
    configurations = configurations.cartesianProduct(Configurations.WEIGHT.toString(), listOf(1.01, 1.1, 1.4, 2.0))


    println("${configurations.size} configuration has been generated.")

    // Convert the configurations to raw string
    return Json.indented.stringify(SimpleSerializer.list, configurations.toList())
}
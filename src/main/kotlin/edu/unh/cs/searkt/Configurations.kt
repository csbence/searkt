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
            Domains.RACETRACK to "input/racetrack/uniform.track",
            Domains.RACETRACK to "input/racetrack/hansen-bigger.track"
//            ,Domains.RACETRACK to "input/racetrack/hansen-bigger-octa.track"
    )

    domains += (1..20).map { Domains.SLIDING_TILE_PUZZLE_4 to "input/tiles/korf/4/real/$it" }
    domains += (1..20).map { Domains.SLIDING_TILE_PUZZLE_4_INVERSE to "input/tiles/korf/4/real/$it" }
    domains += (1..20).map { Domains.SLIDING_TILE_PUZZLE_4_HEAVY to "input/tiles/korf/4/real/$it" }

//    domains += listOf(4).map { Domains.SLIDING_TILE_PUZZLE_4 to "input/tiles/korf/4/real/$it" }
//    domains += (0..10).map { Domains.GRID_WORLD to "input/vacuum/orz100d/orz100d.map_scen_$it" }

    domains += (0..10).map { Domains.GRID_WORLD to "input/vacuum/orz100d/orz100d.map_scen_$it" }
    domains += (0..10).map { Domains.LIFE_GRIDS to "input/vacuum/orz100d/orz100d.map_scen_$it" }

    domains += (0..10).map { Domains.VACUUM_WORLD to "input/vacuum/gen/vacuum$it.vw" }
    domains += (0..10).map { Domains.HEAVY_VACUUM_WORLD to "input/vacuum/gen/vacuum$it.vw" }

    domains += listOf(8,16,32,64).flatMap { roomSize -> (0..9).map { Domains.GRID_MAP to "input/gridmap/room-map/${roomSize}room_000.map:input/gridmap/room-scen/${roomSize}room_000.map.scen" }}

    // TODO add pancake

    // Maximum time per experiment
    val timeLimit = TimeUnit.NANOSECONDS.convert(5, TimeUnit.MINUTES)

    val domainExtras = listOf(
            Triple(Domains.RACETRACK, Configurations.DOMAIN_SEED.toString(), 0..2L),
            // There are 2000+ instances in each scen file
            Triple(Domains.GRID_MAP, Configurations.DOMAIN_SEED.toString(), 0..2000L step 10)
    )

    var configurations = edu.unh.cs.searkt.experiment.configuration.generateConfigurations(
            domains = domains,
//            planners = listOf(
//                    WEIGHTED_A_STAR,
//                    EES,
//                    DPS, DPSG,
//                    BOUNDED_SUBOPTIMAL_EXPLORATION,
//                    WEIGHTED_A_STAR_XDP,
//                    WEIGHTED_A_STAR_XUP
//            ),
            planners = listOf(Planners.WEIGHTED_A_STAR),
            actionDurations = listOf(1),
            timeLimit = timeLimit,
            plannerExtras = listOf(
            ),
            domainExtras = domainExtras
    )

    // Add these to all configurations
    configurations = configurations.cartesianProduct(Configurations.WEIGHT.toString(), listOf(1.6, 2.0, 3.0, 4.0, 10.0))
//    configurations = configurations.cartesianProduct(Configurations.WEIGHT.toString(), listOf(2.0))
//    configurations = configurations.cartesianProduct(Configurations.WEIGHT.toString(), listOf(1.01, 1.1, 1.4, 2.0))


    println("${configurations.size} configuration has been generated.")

    // Convert the configurations to raw string
    return Json.indented.stringify(SimpleSerializer.list, configurations.toList())
}

fun main() {
    val configurationString = generateConfigurations()

    // Save configurations
    val outputPath = "results/configurations.json"
    kotlinx.io.PrintWriter(outputPath, "UTF-8").use { it.write(configurationString) }
    println("Configurations has been saved to $outputPath")
}
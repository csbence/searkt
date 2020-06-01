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
//            Domains.RACETRACK to "input/racetrack/uniform.track",
//            Domains.RACETRACK to "input/racetrack/hansen-bigger.track"
//            ,Domains.RACETRACK to "input/racetrack/hansen-bigger-octa.track"
    )

//    domains += (0..33).map { Domains.PANCAKE to "input/pancake/$it.pqq" }
//    domains += (0..33).map { Domains.HEAVY_PANCAKE to "input/pancake/$it.pqq" }

    domains += (1..100).map { Domains.SLIDING_TILE_PUZZLE_4 to "input/tiles/korf/4/real/$it" }
//    domains += (1..33).map { Domains.SLIDING_TILE_PUZZLE_4_INVERSE to "input/tiles/korf/4/real/$it" }
    domains += (1..100).map { Domains.SLIDING_TILE_PUZZLE_4_HEAVY to "input/tiles/korf/4/real/$it" }

//    domains += (0..10).map { Domains.GRID_WORLD to "input/vacuum/orz100d/orz100d.map_scen_$it" }
//    domains += (0..10).map { Domains.LIFE_GRIDS to "input/lifegrids/lifegrids$it.lg" }

//    domains += (0..10).map { Domains.VACUUM_WORLD to "input/vacuum/gen/vacuum$it.vw" }
//    domains += (0..10).map { Domains.HEAVY_VACUUM_WORLD to "input/vacuum/gen/vacuum$it.vw" } //    val daoMapNames = listOf("arena", "arena2", "brc000d", "brc100d", "brc101d", "brc200d", "brc201d", "brc202d", "brc203d", "brc204d", "brc300d", "brc501d", "brc502d", "brc503d", "brc504d", "brc505d", "brc997d", "brc999d", "combat", "combat2", "den000d", "den001d", "den005d", "den009d", "den011d", "den012d", "den020d", "den101d", "den200d", "den200n", "den201d", "den202d", "den203d", "den204d", "den206d", "den207d", "den308d", "den312d", "den400d", "den401d", "den403d", "den404d", "den405d", "den407d", "den408d", "den500d", "den501d", "den502d", "den504d", "den505d", "den510d", "den520d", "den600d", "den601d", "den602d", "den900d", "den901d", "den998d", "hrt000d", "hrt001d", "hrt002d", "hrt201d", "hrt201n", "isound1", "lak100c", "lak100d", "lak100n", "lak101d", "lak102d", "lak103d", "lak104d", "lak105d", "lak106d", "lak107d", "lak108d", "lak109d", "lak110d", "lak200d", "lak201d", "lak202d", "lak203d", "lak250d", "lak300d", "lak302d", "lak303d", "lak304d", "lak307d", "lak308d", "lak400d", "lak401d", "lak403d", "lak404d", "lak405d", "lak503d", "lak504d", "lak505d", "lak506d", "lak507d", "lak510d", "lak511d", "lak512d", "lak513d", "lak514d", "lak515d", "lak519d", "lak526d", "lgt101d", "lgt300d", "lgt600d", "lgt601d", "lgt602d", "lgt603d", "lgt604d", "lgt605d", "orz000d", "orz100d", "orz101d", "orz102d", "orz103d", "orz105d", "orz106d", "orz107d", "orz200d", "orz201d", "orz203d", "orz300d", "orz301d", "orz302d", "orz303d", "orz304d", "orz500d", "orz601d", "orz700d", "orz701d", "orz702d", "orz703d", "orz704d", "orz800d", "orz900d", "orz901d", "orz999d", "ost000a", "ost000t", "ost001d", "ost002d", "ost003d", "ost004d", "ost100d", "ost101d", "ost102d", "oth000d", "oth001d", "oth999d", "rmtst", "rmtst01", "rmtst03")

//    domains += listOf(8, 64).flatMap { roomSize -> (0..10).map { Domains.GRID_MAP to "input/gridmap/room-map/${roomSize}room_000.map:input/gridmap/room-scen/${roomSize}room_000.map.scen" } }
//    domains += daoMapNames.flatMap { mapName -> (0..10).map { Domains.GRID_MAP to "input/gridmap/dao-map/$mapName.map:input/gridmap/dao-scen/$mapName.map.scen" } }


    // Maximum time per experiment
    val timeLimit = TimeUnit.NANOSECONDS.convert(10, TimeUnit.MINUTES)

    val domainExtras = listOf(
            Triple(Domains.RACETRACK, Configurations.DOMAIN_SEED.toString(), 0..2L),
            // There are 2000+ instances in each scen file
            Triple(Domains.GRID_MAP, Configurations.DOMAIN_SEED.toString(), 0..2000L step 500)
    )
//     val plannerExtras = listOf(
//             Triple(Planners.BOUNDED_SUBOPTIMAL_EXPLORATION, "embeddedAlgorithm",
//                     listOf(
//                             Planners.OPTIMISTIC,
//                             Planners.GREEDY,
//                             Planners.SPEEDY,
//                             Planners.WEIGHTED_A_STAR_XDP
//                           )
//             )
//     )

    var configurations = edu.unh.cs.searkt.experiment.configuration.generateConfigurations(
            domains = domains,
            planners = listOf(
//                    Planners.WEIGHTED_A_STAR,
//                    Planners.EES,
//                    Planners.DPS,
//                    Planners.BOUNDED_SUBOPTIMAL_EXPLORATION
//                    Planners.SUBPOTENTIAL,
//                    Planners.WEIGHTED_A_STAR_XDP,
//                    Planners.EES_DD,
                    Planners.EES,
                    Planners.EESB
//                    Planners.WEIGHTED_A_STAR_XUP
            ),
//            planners = listOf(Planners.WEIGHTED_A_STAR),
            actionDurations = listOf(1),
            timeLimit = timeLimit,
//            plannerExtras = plannerExtras,
            domainExtras = domainExtras
    )

    // Add these to all configurations
        configurations = configurations.cartesianProduct(Configurations.WEIGHT.toString(), listOf(1.01, 1.1, 1.4, 1.6, 2.4, 5.0, 10.0))
//    configurations = configurations.cartesianProduct(Configurations.WEIGHT.toString(), listOf(2.0))
//    configurations = configurations.cartesianProduct(Configurations.WEIGHT.toString(), listOf(1.01, 1.1, 1.4, 2.0))


    println(configurations.first())
    println("${configurations.size} configuration has been generated.")

    // Convert the configurations to raw string
    return Json.indented.stringify(SimpleSerializer.list, configurations.toList())
}

fun main() {
    val configurationString = generateConfigurations()

    // Save configurations
    val outputPath = "configs/configurations.json"
    kotlinx.io.PrintWriter(outputPath, "UTF-8").use { it.write(configurationString) }
    println("Configurations has been saved to $outputPath")
}
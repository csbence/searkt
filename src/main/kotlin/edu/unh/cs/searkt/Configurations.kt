package edu.unh.cs.searkt

import edu.unh.cs.searkt.environment.Domains
import edu.unh.cs.searkt.experiment.configuration.Configurations
import edu.unh.cs.searkt.experiment.configuration.SimpleSerializer
import edu.unh.cs.searkt.experiment.configuration.cartesianProduct
import edu.unh.cs.searkt.experiment.configuration.realtime.LookaheadType
import edu.unh.cs.searkt.experiment.configuration.realtime.TerminationType
import edu.unh.cs.searkt.planner.CommitmentStrategy
import edu.unh.cs.searkt.planner.LookaheadStrategy
import edu.unh.cs.searkt.planner.realtime.BidirectionalEnvelopeSearch.BidirectionalSearchStrategy.*
import edu.unh.cs.searkt.planner.Planners
import edu.unh.cs.searkt.planner.realtime.BidirectionalEnvelopeSearch
import edu.unh.cs.searkt.planner.realtime.TBAOptimization
import edu.unh.cs.searkt.planner.realtime.TBAStarConfiguration
import edu.unh.cs.searkt.planner.realtime.BidirectionalEnvelopeSearch.BiESConfiguration
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    val outputPath = if (args.size > 0) args[0]
        else "configs/configurations.json"

    val config = generateConfigurations()

    kotlinx.io.PrintWriter(outputPath, "UTF-8").use { it.write(config) }
    System.err.println("\nConfigurations have been saved to $outputPath")
}

/**
 * Generate experiment configurations.
 *
 * Modify the content of this function to create custom experiments.
 */
fun generateConfigurations(): String {
    val domains = mutableListOf<Pair<Domains, String>>(
//            Domains.GRID_WORLD to "input/vacuum/cups.vw"
//            Domains.GRID_WORLD to "input/vacuum/minima100_100-0.vw"
            Domains.GRID_MAP to "input/gridmap/room-map/8room_009.map:input/gridmap/room-scen/8room_009.map.scen",
            Domains.GRID_MAP to "input/gridmap/room-map/16room_005.map:input/gridmap/room-scen/16room_005.map.scen",
            Domains.GRID_MAP to "input/gridmap/dao-map/orz100d.map:input/gridmap/dao-scen/orz100d.map.scen",
            Domains.GRID_MAP to "input/gridmap/dao-map/ost000a.map:input/gridmap/dao-scen/ost000a.map.scen",
            Domains.GRID_MAP to "input/gridmap/sc1-map/TheFrozenSea.map:input/gridmap/sc1-scen/TheFrozenSea.map.scen",
            Domains.GRID_MAP to "input/gridmap/sc1-map/Cauldron.map:input/gridmap/sc1-scen/Cauldron.map.scen",
//            Domains.ACROBOT to "input/acrobot/default_0.1-0.1.ab"
            Domains.RACETRACK to "input/racetrack/hansen-bigger-d-wide3.track"
    )

//    domains += (1..100).map { Domains.SLIDING_TILE_PUZZLE_4 to "input/tiles/korf/4/real/$it" }
    domains += (1..10).map { Domains.SLIDING_TILE_PUZZLE_4 to "input/tiles/korf/4/real/$it" }
//    domains += (0..9).map { Domains.GRID_WORLD to "input/vacuum/orz100d/orz100d.map_scen_$it" }
    domains += (0..9).map { Domains.GRID_WORLD to "input/vacuum/minima1500/minima1500_1500-$it.vw" }
    domains += (0..9).map { Domains.GRID_WORLD to "input/vacuum/uniform1500/uniform1500_1500-$it.vw" }

    // Maximum time per experiment
    val timeLimit = TimeUnit.NANOSECONDS.convert(5, TimeUnit.MINUTES)

    val domainExtras = listOf(
            Triple(Domains.RACETRACK, Configurations.DOMAIN_SEED.toString(), (0L..9L)),
            Triple(Domains.RACETRACK, Configurations.IS_SAFE.toString(), listOf(true)),
            Triple(Domains.GRID_MAP, Configurations.DOMAIN_SEED.toString(), (0L..9L))
    )

    val weights = listOf(1.0, 3.0, 10.0, 32.0, 64.0)
//    val weights = listOf(64.0)

    var configurations = edu.unh.cs.searkt.experiment.configuration.generateConfigurations(
            domains = domains,
            planners = listOf(
                    Planners.BI_ES,
                    Planners.LSS_LRTA_STAR,
                    Planners.TIME_BOUNDED_A_STAR
            ),
            actionDurations = listOf(10
                    , 30, 100, 300, 1000, 3000
            ),
//            planners = listOf(Planners.WEIGHTED_A_STAR),
//            actionDurations = listOf(1),
            timeLimit = timeLimit,
            stepLimit = 1000000,
            terminationType = TerminationType.EXPANSION,
            expansionLimit = 10_000_000,
            lookaheadType = LookaheadType.DYNAMIC,
            plannerExtras = listOf(
                    Triple(Planners.BI_ES, Configurations.COMMITMENT_STRATEGY, listOf(CommitmentStrategy.SINGLE)),
                    Triple(Planners.BI_ES, Configurations.WEIGHT, weights),
                    Triple(Planners.BI_ES, Configurations.LOOKAHEAD_STRATEGY, listOf(LookaheadStrategy.A_STAR, LookaheadStrategy.GBFS, LookaheadStrategy.PSEUDO_F)),
                    Triple(Planners.BI_ES, BiESConfiguration.ENVELOPE_SEARCH_STRATEGY, listOf(LookaheadStrategy.A_STAR, LookaheadStrategy.GBFS)),
                    Triple(Planners.BI_ES, Configurations.LOOKAHEAD_STRATEGY, listOf(LookaheadStrategy.GBFS)),
                    Triple(Planners.BI_ES, BiESConfiguration.ENVELOPE_SEARCH_STRATEGY, listOf(LookaheadStrategy.A_STAR)),
                    Triple(Planners.BI_ES, BiESConfiguration.GENERATE_PREDECESSORS, listOf(true, false)),
                    Triple(Planners.BI_ES, BiESConfiguration.BIDIRECTIONAL_SEARCH_STRATEGY, listOf(ROUND_ROBIN, MM)),
                    Triple(Planners.BI_ES, BiESConfiguration.FRONTIER_ADJUSTMENT_RATIO, listOf(1.0)),
                    Triple(Planners.LSS_LRTA_STAR, Configurations.COMMITMENT_STRATEGY, listOf(CommitmentStrategy.MULTIPLE)),
                    Triple(Planners.TIME_BOUNDED_A_STAR, Configurations.COMMITMENT_STRATEGY, listOf(CommitmentStrategy.SINGLE)),
                    Triple(Planners.TIME_BOUNDED_A_STAR, Configurations.WEIGHT, weights),
                    Triple(Planners.TIME_BOUNDED_A_STAR, TBAStarConfiguration.TBA_OPTIMIZATION, listOf(TBAOptimization.THRESHOLD)),
                    Triple(Planners.TIME_BOUNDED_A_STAR, TBAStarConfiguration.BACKUP_RATIO, listOf(Double.MAX_VALUE)),
                    Triple(Planners.TIME_BOUNDED_A_STAR, Configurations.LOOKAHEAD_STRATEGY, listOf(LookaheadStrategy.A_STAR, LookaheadStrategy.GBFS)),
//                    Triple(Planners.TIME_BOUNDED_A_STAR, Configurations.LOOKAHEAD_STRATEGY, listOf(LookaheadStrategy.A_STAR)),
                    Triple(Planners.WEIGHTED_A_STAR, Configurations.WEIGHT, listOf(1.0))
            ),
            domainExtras = domainExtras
    )

    // filter out greedy configs with weight greater than 1
    configurations = configurations.filter {
        when (it[Configurations.ALGORITHM_NAME.toString()]) {
            Planners.BI_ES.toString() -> {
                if (it[Configurations.LOOKAHEAD_STRATEGY.toString()] == LookaheadStrategy.GBFS &&
                        it[BiESConfiguration.ENVELOPE_SEARCH_STRATEGY.toString()] == LookaheadStrategy.GBFS) {
                    it[Configurations.WEIGHT.toString()] == null || it[Configurations.WEIGHT.toString()] == 1.0
                } else true
            }
            Planners.TIME_BOUNDED_A_STAR.toString(), Planners.BACK_ES.toString() -> {
                if (it[Configurations.LOOKAHEAD_STRATEGY.toString()] == LookaheadStrategy.GBFS) {
                    it[Configurations.WEIGHT.toString()] == null || it[Configurations.WEIGHT.toString()] == 1.0
                } else true
            }
            else -> true
        }
    }

    // Re-Seed grid maps since they may all need different seeds

    // defines the start seeds for each grid map domain
    val gridMapSeedSequence = mapOf(
            "orz100d" to 2400L,
            "ost000a" to 2500L,
            "8room_009" to 2050L,
            "16room_005" to 1750L,
            "Cauldron" to 3800L,
            "TheFrozenSea" to 4000L
    )

    configurations.forEach {
        val domain = it["domainPath"] as String
        for ((token, seed) in gridMapSeedSequence) {
            if (domain.contains(token)) {
                it["domainSeed"] = seed + (it["domainSeed"] as Long)
            }
        }
    }

    println("${configurations.size} configuration has been generated.")

    // Convert the configurations to raw string
    return Json.indented.stringify(SimpleSerializer.list, configurations.toList())
}
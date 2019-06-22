package edu.unh.cs.searkt

import edu.unh.cs.searkt.environment.Domains
import edu.unh.cs.searkt.experiment.configuration.Configurations
import edu.unh.cs.searkt.experiment.configuration.SimpleSerializer
import edu.unh.cs.searkt.experiment.configuration.cartesianProduct
import edu.unh.cs.searkt.experiment.configuration.realtime.LookaheadType
import edu.unh.cs.searkt.experiment.configuration.realtime.TerminationType
import edu.unh.cs.searkt.planner.CommitmentStrategy
import edu.unh.cs.searkt.planner.LookaheadStrategy
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
        else "configs/configuration.json"

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
//            Domains.GRID_WORLD to "input/vacuum/minima/minima0.vw"
//            Domains.GRID_WORLD to "input/vacuum/orz100d/orz100d.map_scen_0"
//            Domains.GRID_WORLD to "input/vacuum/cups.vw"
//            Domains.RACETRACK to "input/racetrack/hansen-bigger-octa.track"
    )

    domains += (1..100).map { Domains.SLIDING_TILE_PUZZLE_4 to "input/tiles/korf/4/real/$it" }
//    domains += listOf(4).map { Domains.SLIDING_TILE_PUZZLE_4 to "input/tiles/korf/4/real/$it" }
    domains += (0..50).map { Domains.GRID_WORLD to "input/vacuum/orz100d/orz100d.map_scen_$it" }
    domains += (0..50).map { Domains.GRID_WORLD to "input/vacuum/minima1500/minima1500_1500-$it.vw" }
//    domains += (0..10).map { Domains.GRID_WORLD to "input/vacuum/orz100d/orz100d.map_scen_$it" }

    // Maximum time per experiment
    val timeLimit = TimeUnit.NANOSECONDS.convert(999, TimeUnit.MINUTES)


    val domainExtras = listOf(
            Triple(Domains.RACETRACK, Configurations.DOMAIN_SEED.toString(), listOf(0L)),
            Triple(Domains.RACETRACK, Configurations.IS_SAFE.toString(), listOf(true))
    )

    val weights = listOf(1.0, 1.3, 2.6, 3.0, 5.0, 10.0, 13.0)

    var configurations = edu.unh.cs.searkt.experiment.configuration.generateConfigurations(
            domains = domains,
            planners = listOf(Planners.BI_ES, Planners.LSS_LRTA_STAR, Planners.TIME_BOUNDED_A_STAR/*, Planners.BACK_ES*/),
            actionDurations = listOf(10, 30, 100, 300, 1000, 3000),
//            planners = listOf(Planners.WEIGHTED_A_STAR),
//            actionDurations = listOf(1),
            timeLimit = timeLimit,
            stepLimit = 1000000,
            terminationType = TerminationType.EXPANSION,
            expansionLimit = 50_000_000,
            lookaheadType = LookaheadType.DYNAMIC,
            plannerExtras = listOf(
                    Triple(Planners.BI_ES, Configurations.COMMITMENT_STRATEGY, listOf(CommitmentStrategy.SINGLE)),
                    Triple(Planners.BI_ES, Configurations.WEIGHT, weights),
                    Triple(Planners.BI_ES, Configurations.LOOKAHEAD_STRATEGY, listOf(LookaheadStrategy.GBFS)),
                    Triple(Planners.BI_ES, BiESConfiguration.ENVELOPE_SEARCH_STRATEGY, listOf(LookaheadStrategy.A_STAR, LookaheadStrategy.GBFS)),
                    Triple(Planners.BACK_ES, Configurations.COMMITMENT_STRATEGY, listOf(CommitmentStrategy.SINGLE)),
                    Triple(Planners.BACK_ES, Configurations.WEIGHT, weights),
                    Triple(Planners.BACK_ES, Configurations.LOOKAHEAD_STRATEGY, listOf(LookaheadStrategy.A_STAR)),
                    Triple(Planners.LSS_LRTA_STAR, Configurations.COMMITMENT_STRATEGY, listOf(CommitmentStrategy.MULTIPLE)),
                    Triple(Planners.TIME_BOUNDED_A_STAR, Configurations.COMMITMENT_STRATEGY, listOf(CommitmentStrategy.SINGLE)),
                    Triple(Planners.TIME_BOUNDED_A_STAR, Configurations.WEIGHT, weights),
                    Triple(Planners.TIME_BOUNDED_A_STAR, TBAStarConfiguration.TBA_OPTIMIZATION, listOf(TBAOptimization.THRESHOLD)),
                    Triple(Planners.TIME_BOUNDED_A_STAR, TBAStarConfiguration.BACKUP_RATIO, listOf(Double.MAX_VALUE)),
                    Triple(Planners.TIME_BOUNDED_A_STAR, Configurations.LOOKAHEAD_STRATEGY, listOf(LookaheadStrategy.A_STAR, LookaheadStrategy.GBFS)),
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


    println("${configurations.size} configuration has been generated.")

    // Convert the configurations to raw string
    return Json.indented.stringify(SimpleSerializer.list, configurations.toList())
}
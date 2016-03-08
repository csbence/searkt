package edu.unh.cs.ai.realtimesearch.experiment.local

import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult

fun ExperimentResult.toJson(experimentResult: ExperimentResult): String {
    val mapper = com.fasterxml.jackson.databind.ObjectMapper().registerKotlinModule()
    return mapper.writeValueAsString(experimentResult)
}

fun ExperimentResult.fromJson(jsonExperimentConfiguration: String): ExperimentResult {
    val mapper = com.fasterxml.jackson.databind.ObjectMapper().registerKotlinModule()
    return mapper.readValue<ExperimentResult>(jsonExperimentConfiguration)
}
package edu.unh.cs.ai.realtimesearch.experiment.local

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration

fun ExperimentConfiguration.toJson(experimentResult: ExperimentConfiguration): String {
    val mapper = ObjectMapper().registerKotlinModule()
    return mapper.writeValueAsString(experimentResult)
}

fun ExperimentConfiguration.fromJson(jsonExperimentConfiguration: String): ExperimentConfiguration {
    val mapper = ObjectMapper().registerKotlinModule()
    return mapper.readValue<ExperimentConfiguration>(jsonExperimentConfiguration)
}
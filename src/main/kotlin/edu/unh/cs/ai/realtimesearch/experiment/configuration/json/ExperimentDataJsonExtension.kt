package edu.unh.cs.ai.realtimesearch.experiment.configuration.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentData
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult

fun <T : ExperimentData> T.toJson(): String {
    val mapper = ObjectMapper()
    return mapper.writeValueAsString(this)
}

fun <T : ExperimentData> T.toIndentedJson(): String {
    val mapper = ObjectMapper()
    mapper.enable(SerializationFeature.INDENT_OUTPUT)
    return mapper.writeValueAsString(this)
}

fun experimentDataFromJson(jsonExperimentConfiguration: String): ExperimentData {
    val mapper = ObjectMapper().registerKotlinModule()
    return mapper.readValue<ExperimentData>(jsonExperimentConfiguration)
}

fun experimentConfigurationFromJson(jsonExperimentConfiguration: String): GeneralExperimentConfiguration {
    val experimentData = experimentDataFromJson(jsonExperimentConfiguration)
    return GeneralExperimentConfiguration(experimentData.valueStore)
}

fun experimentResultFromJson(jsonExperimentResult: String): ExperimentResult {
    val experimentData = experimentDataFromJson(jsonExperimentResult)
    return ExperimentResult(experimentData.valueStore)
}
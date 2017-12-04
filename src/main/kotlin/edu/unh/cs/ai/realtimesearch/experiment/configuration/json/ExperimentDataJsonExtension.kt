package edu.unh.cs.ai.realtimesearch.experiment.configuration.json

import edu.unh.cs.ai.realtimesearch.experiment.configuration.DataSerializer
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentData
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.JSON
import kotlinx.serialization.map

fun <T : ExperimentData> T.toJson(): String {
    val mapSerializer = (StringSerializer to DataSerializer).map
    return JSON.stringify(mapSerializer, valueStore)
}

fun <T : ExperimentData> T.toIndentedJson(): String {
    val mapSerializer = (StringSerializer to DataSerializer).map
    return JSON.indented.stringify(mapSerializer, valueStore)
}

fun experimentDataFromJson(jsonExperimentConfiguration: String): ExperimentData {
    val mapSerializer = (StringSerializer to DataSerializer).map
    val valueMap = JSON.parse(mapSerializer, jsonExperimentConfiguration)
    return ExperimentData(valueMap.toMutableMap())
}

fun experimentConfigurationFromJson(jsonExperimentConfiguration: String): GeneralExperimentConfiguration {
    val experimentData = experimentDataFromJson(jsonExperimentConfiguration)
    return GeneralExperimentConfiguration(experimentData.valueStore)
}

fun experimentResultFromJson(jsonExperimentResult: String): ExperimentResult {
    val experimentData = experimentDataFromJson(jsonExperimentResult)
    return ExperimentResult(experimentData.valueStore)
}
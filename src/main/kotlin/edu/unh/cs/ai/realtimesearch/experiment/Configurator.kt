package edu.unh.cs.ai.realtimesearch.experiment

import edu.unh.cs.ai.realtimesearch.Input
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ConfigurationExecutor
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import java.util.*

object Configurator {
    private var seed: () -> ArrayList<Map<String, Any?>> = {
        ArrayList<Map<String, Any?>>().apply { add(HashMap<String, Any?>()) }
    }

    fun <T> addOption(optionName: String, values: List<T>) {
        val curOptions = seed()
        val newOptions = ArrayList<Map<String, Any?>>()
        seed = {
            values.forEach {
                curOptions.mapTo(newOptions, { oldVal -> HashMap<String, Any?>(oldVal) + (optionName to it) })
            }
            newOptions
        }
    }

    fun generateConfigurations(domainPath: String,
                               domainName: String,
                               algorithmName: String,
                               terminationCheckerType: String): List<GeneralExperimentConfiguration> {

        val input = Input::class.java.classLoader.getResourceAsStream(domainPath) ?: throw RuntimeException("Resource not found")
        val rawDomain = Scanner(input).useDelimiter("\\Z").next()
        return seed().map {
            GeneralExperimentConfiguration(
                    domainName, rawDomain, algorithmName, terminationCheckerType, domainPath) // should add it here
        }
    }

}




package edu.unh.cs.ai.realtimesearch.experiment.result

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import java.util.*

data class ExperimentResult(val experimentConfiguration: GeneralExperimentConfiguration?,
                            val expandedNodes: Int = 0,
                            val generatedNodes: Int = 0,
                            val timeInMillis: Long = 0,
                            val actions: List<Action> = emptyList(),
                            val pathLength: Double? = null,
                            val errorMessage: String? = null,
                            val values: Map<String, Any> = HashMap(),
                            val timestamp: Long = System.currentTimeMillis(),
                            val systemProperties: HashMap<String, String> = HashMap()) {

    init {
        // Only initialize if empty
        if (systemProperties.isEmpty()) {
            System.getProperties().forEach {
                systemProperties.put(it.key.toString(), it.value.toString())
            }
        }
    }
}

/*
    algName/domain/paramter-set-name/instance.output
 */
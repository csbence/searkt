package edu.unh.cs.ai.realtimesearch.experiment.configuration

import edu.unh.cs.ai.realtimesearch.experiment.configuration.json.experimentDataFromJson
import edu.unh.cs.ai.realtimesearch.experiment.configuration.json.toJson
import org.junit.Test
import kotlin.test.assertTrue

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class ExperimentDataTest {

    @Test
    fun testJsonSerialization() {

        val experimentData = ExperimentData()
        experimentData["terminationCheckerParameter"] = 4
        experimentData["action duration"] = 10
        experimentData["list"] = listOf("A", 1, 1)

        val json = experimentData.toJson()
        val experimentDataFromJson = experimentDataFromJson(json)

        val generalExperimentConfiguration = GeneralExperimentConfiguration(experimentDataFromJson.valueStore)

        assertTrue(generalExperimentConfiguration.terminationCheckerParameter == 4)
        assertTrue(experimentDataFromJson["action duration"] == 10L)
        assertTrue(experimentDataFromJson["X"] == null)
    }

}
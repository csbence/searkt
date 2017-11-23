package edu.unh.cs.ai.realtimesearch.experiment.configuration

/**
 * @author Bence Cserna (bence@cserna.net)
 */
//class ExperimentDataTest {
//
//    @Test
//    fun testJsonSerialization1() {
//        val experimentData = ExperimentData()
//        experimentData["timeLimit"] = 4
//        experimentData["actionDuration"] = 10
//        experimentData["list"] = listOf("A", 1, 1)
//
//        val json = experimentData.toJson()
//        val experimentDataFromJson = experimentDataFromJson(json)
//
//        assertTrue(experimentDataFromJson["timeLimit"] == 4L)
//        assertTrue(experimentDataFromJson["actionDuration"] == 10L)
//        assertTrue(experimentDataFromJson["X"] == null)
//    }
//
//    @Test
//    fun testJsonSerialization2() {
//        val experimentData = ExperimentData()
//        experimentData["actions"] = listOf("POSITIVE", "POSITIVE", "NEGATIVE", "NONE")
//
//        val json = experimentData.toJson()
//        val experimentDataFromJson = experimentDataFromJson(json)
//
//        val experimentResult = ExperimentResult(experimentDataFromJson.valueStore)
//
//        assertTrue { experimentResult.actions.size == 4 }
//        assertTrue { experimentResult.actions.first().equals("POSITIVE") }
//        assertTrue { experimentResult.actions.last().equals("NONE") }
//    }
//}
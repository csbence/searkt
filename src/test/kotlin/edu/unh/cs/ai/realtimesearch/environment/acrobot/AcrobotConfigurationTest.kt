package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.environment.acrobot.configuration.AcrobotConfiguration
import edu.unh.cs.ai.realtimesearch.environment.acrobot.configuration.AcrobotStateConfiguration
import groovy.json.JsonOutput
import org.junit.Test
import org.slf4j.LoggerFactory
import kotlin.test.assertTrue

class AcrobotConfigurationTest {
    private val logger = LoggerFactory.getLogger(AcrobotConfigurationTest::class.java)

    @Test
    fun testJSON1() {
        val json = JsonOutput.toJson(AcrobotStateConfiguration())
        val parsed = AcrobotStateConfiguration.fromString(json)

        assertTrue { AcrobotStateConfiguration().equals(parsed) }
    }

    @Test
    fun testJSON2() {
        val json = JsonOutput.toJson(AcrobotConfiguration())
        val parsed = AcrobotConfiguration.fromString(json)

        assertTrue { AcrobotConfiguration().equals(parsed) }
    }
}
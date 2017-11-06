package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.environment.acrobot.configuration.AcrobotConfiguration
import edu.unh.cs.ai.realtimesearch.environment.acrobot.configuration.AcrobotStateConfiguration
import org.junit.Test
import edu.unh.cs.ai.realtimesearch.logging.LoggerFactory
import kotlin.test.assertTrue

/**
 * @author Mike Bogochow (mgp36@unh.edu)
 */
class AcrobotConfigurationTest {
    private val logger = LoggerFactory.getLogger(AcrobotConfigurationTest::class.java)

    @Test
    fun testJSON1() {
        val json = AcrobotStateConfiguration().toJson()
        val parsed = AcrobotStateConfiguration.fromJson(json)

        assertTrue { AcrobotStateConfiguration().equals(parsed) }
    }

    @Test
    fun testJSON2() {
        val json = AcrobotConfiguration().toJson()
        val parsed = AcrobotConfiguration.fromJson(json)

        assertTrue { AcrobotConfiguration().equals(parsed) }
    }
}
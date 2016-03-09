package edu.unh.cs.ai.realtimesearch.environment.acrobot

import groovy.json.JsonOutput
import org.junit.Test
import org.slf4j.LoggerFactory
import kotlin.test.assertTrue

class AcrobotConfigurationTest {
    private val logger = LoggerFactory.getLogger(AcrobotConfigurationTest::class.java)

    @Test
    fun testJSON1() {
        assertTrue { AcrobotStateConfiguration().equals(AcrobotStateConfiguration.fromString(JsonOutput.toJson(AcrobotStateConfiguration()))) }
    }

    @Test
    fun testJSON2() {
        assertTrue { AcrobotConfiguration().equals(AcrobotConfiguration.fromString(JsonOutput.toJson(AcrobotConfiguration()))) }
    }
}
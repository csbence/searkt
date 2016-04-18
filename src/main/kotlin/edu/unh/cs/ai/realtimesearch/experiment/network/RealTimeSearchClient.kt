package edu.unh.cs.ai.realtimesearch.experiment.network

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentData
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.util.*

/**
 * Real time search client is a connection interface to the Real Time Search Server.
 */
class RealTimeSearchClient(val url: String) {
    data class ClientInformation(@JsonUnwrapped val systemProperties: Map<String, String>)

    private val logger = LoggerFactory.getLogger(RealTimeSearchClient::class.java)
    private val restTemplate = RestTemplate()
    private var connectionId: String? = null

    private val systemProperties = HashMap<String, String>()

    init {
        // Initialize the rest template
        val mapper = ObjectMapper().registerKotlinModule()
        val messageConverter = MappingJackson2HttpMessageConverter()
        messageConverter.objectMapper = mapper
        restTemplate.messageConverters.add(messageConverter)

        // Initialize the system properties
        System.getProperties().forEach {
            systemProperties.put(it.key.toString(), it.value.toString())
        }
    }

    /**
     * Indicate client presence to the server.
     *
     * @return true if the notification was successful, else false.
     */
    fun checkIn(): Boolean {
        val checkInUrl = if (connectionId == null) {
            "$url/checkIn"
        } else {
            "$url/checkIn/$connectionId"
        }

        val responseEntity = restTemplate.exchange(checkInUrl, HttpMethod.POST, HttpEntity(ClientInformation(systemProperties)), String::class.java)
        return if (responseEntity.statusCode == HttpStatus.OK) {
            logger.info("Connection successful")
            connectionId = responseEntity.body
            true
        } else {
            logger.info("Connection failed")
            false
        }
    }

    /**
     * Notify the server that the client is not available anymore.
     */
    fun disconnect() {
        try {
            val responseEntity = restTemplate.exchange("$url/disconnect/$connectionId", HttpMethod.POST, HttpEntity(ClientInformation(systemProperties)), Nothing::class.java)
            if (responseEntity.statusCode == HttpStatus.OK) {
                logger.info("Disconnection successful")
            } else {
                logger.info("Disconnection failed")
            }
        } catch(e: RestClientException) {
            logger.error("Disconnection: failed", e)
        }
    }

    /**
     * Download experiment configuration from the server.
     *
     * @return ExperimentConfiguration if available, else null.
     */
    fun getExperimentConfiguration(): GeneralExperimentConfiguration? {
        try {
            val responseEntity = restTemplate.exchange("$url/configuration/$connectionId", HttpMethod.GET, HttpEntity(ClientInformation(systemProperties)), ExperimentData::class.java)
            return if (responseEntity.statusCode == HttpStatus.OK) {
                logger.info("Get configuration: successful")
                val valueStore = responseEntity?.body?.valueStore ?: return null
                return GeneralExperimentConfiguration(valueStore)

            } else {
                logger.warn("Get configuration: failed")
                null
            }

        } catch(e: RestClientException) {
            logger.error("Get configuration: failed", e.message)
        }

        return null
    }

    /**
     * Submit result configuration to the server.
     *
     * @return true if the submission was successful, else false.
     */
    fun submitResult(experimentResult: ExperimentResult): Boolean {
        cleanUpResult(experimentResult)

        val responseEntity: ResponseEntity<Nothing>?
        try {
            responseEntity = restTemplate.exchange("$url/result/$connectionId", HttpMethod.POST, HttpEntity(experimentResult), Nothing::class.java)
            return if (responseEntity.statusCode == HttpStatus.OK) {
                logger.info("Submit result: successful")
                true
            } else {
                logger.warn("Submit result: failed")
                false
            }
        } catch(e: RestClientException) {
            logger.error("Submit result: failed", e)
        }

        return false
    }

    private fun cleanUpResult(experimentResult: ExperimentResult) {
        @Suppress("UNCHECKED_CAST")
        val experimentConfiguration = experimentResult.valueStore["experimentConfiguration"] as MutableMap<String, Any?>
        experimentConfiguration.remove("rawDomain")
    }
}
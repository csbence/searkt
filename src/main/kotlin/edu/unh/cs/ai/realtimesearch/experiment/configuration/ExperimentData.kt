package edu.unh.cs.ai.realtimesearch.experiment.configuration

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import edu.unh.cs.ai.realtimesearch.experiment.configuration.json.ExperimentDeserializer
import edu.unh.cs.ai.realtimesearch.experiment.configuration.json.toIndentedJson

@JsonDeserialize(using = ExperimentDeserializer::class)
open class ExperimentData(@JsonIgnore val valueStore: MutableMap<String, Any> = hashMapOf()) {
    operator fun get(key: String): Any? {
        return valueStore[key]
    }

    @JsonAnySetter
    fun set(key: String, value: String) {
        valueStore[key] = value
    }

    @JsonAnyGetter
    fun getProperties() = valueStore


    operator fun set(key: String, value: Any) {
        valueStore[key] = value
    }

    fun <T> getTypedValue(key: String): T? = this[key] as? T
    open fun contains(key: String): Boolean = valueStore.contains(key)

    override fun toString(): String {
        return toIndentedJson()
    }
}


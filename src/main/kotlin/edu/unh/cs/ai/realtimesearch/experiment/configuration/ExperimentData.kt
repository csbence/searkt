package edu.unh.cs.ai.realtimesearch.experiment.configuration

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import edu.unh.cs.ai.realtimesearch.experiment.configuration.json.ExperimentDeserializer
import edu.unh.cs.ai.realtimesearch.experiment.configuration.json.toIndentedJson

/**
 * Base class for JSON serialization.
 *
 * The class supports the following structure:
 *
 * MAP -> list of <KEY, VALUE>
 * KEY -> String
 * VALUE -> String | Long | Double | Boolean | null | MAP | ARRAY
 *
 * MAP is the top level item.
 */
@JsonDeserialize(using = ExperimentDeserializer::class)
open class ExperimentData(@JsonIgnore val valueStore: MutableMap<String, Any?> = hashMapOf()) {
    init {
        valueStore.remove("valueStore")
        valueStore.remove("properties")
    }

    operator fun get(key: String): Any? {
        return valueStore[key]
    }

    operator fun get(key: Any): Any? {
        return valueStore[key.toString()]
    }

    @JsonAnySetter
    fun set(key: String, value: String) {
        valueStore[key] = value
    }

    @Suppress("unused")
    @JsonAnyGetter
    fun getProperties() = valueStore

    operator fun <T> set(key: String, value: T) {
        valueStore[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getTypedValue(key: String): T? = this[key] as? T

    open fun contains(key: String): Boolean = valueStore.contains(key)

    override fun toString(): String {
        return toIndentedJson()
    }
}


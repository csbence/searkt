package edu.unh.cs.searkt.experiment.configuration

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.internal.StringSerializer

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
open class ExperimentData(val valueStore: MutableMap<String, Any?> = hashMapOf()) {
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

    fun set(key: String, value: String) {
        valueStore[key] = value
    }

    operator fun set(key: String, value: Any) {
        valueStore[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getTypedValue(key: String): T? = this[key] as? T

    open fun contains(key: String): Boolean = valueStore.contains(key)

    override fun toString(): String {
        TODO()
    }
}

@Serializer(forClass = ExperimentData::class)
object SimpleSerializer : KSerializer<Any> {
    override val descriptor: SerialDescriptor = StringDescriptor.withName("Simple")

    @Suppress("UNCHECKED_CAST")
    @SuppressWarnings
    override fun serialize(encoder: Encoder, obj: Any) {
        when (obj) {
            is Long -> encoder.encodeLong(obj)
            is String -> encoder.encodeString(obj)
            is Boolean -> encoder.encodeBoolean(obj)
            is Char -> encoder.encodeChar(obj)
            is Short -> encoder.encodeShort(obj)
            is Int -> encoder.encodeInt(obj)
            is Float -> encoder.encodeFloat(obj)
            is Double -> encoder.encodeDouble(obj)
            is List<*> -> this.list.serialize(encoder, obj as List<Any>)
            is Map<*, *> -> (StringSerializer to this).map.serialize(encoder, obj as Map<String, Any>)
            is Set<*> -> this.set.serialize(encoder, obj as Set<Any>)
            else -> encoder.encodeString(obj.toString())
        }
    }

    override fun deserialize(decoder: Decoder): Any {
        throw UnsupportedOperationException("not implemented")
    }
}



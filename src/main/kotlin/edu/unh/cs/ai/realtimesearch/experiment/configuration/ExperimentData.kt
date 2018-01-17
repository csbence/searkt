package edu.unh.cs.ai.realtimesearch.experiment.configuration

import kotlinx.serialization.*
import kotlinx.serialization.internal.SerialClassDescImpl
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

object PolymorphicClassDesc : SerialClassDescImpl("kotlin.Any") {
    override val kind: KSerialClassKind = KSerialClassKind.POLYMORPHIC
}


@Serializer(forClass = ExperimentData::class)
object SimpleSerializer: KSerializer<Any> {
    override val serialClassDesc: KSerialClassDesc = SerialClassDescImpl("Simple")

    override fun save(output: KOutput, obj: Any) {
        when (obj) {
            is Long -> output.writeLongValue(obj)
            is String -> output.writeStringValue(obj)
            is Boolean -> output.writeBooleanValue(obj)
            is Char -> output.writeCharValue(obj)
            is Short -> output.writeShortValue(obj)
            is Int -> output.writeIntValue(obj)
            is Float -> output.writeFloatValue(obj)
            is Double -> output.writeDoubleValue(obj)
            is List<*> -> this.list.save(output, obj as List<Any>)
            is Map<*, *> -> (StringSerializer to this).map.save(output, obj as Map<String, Any>)
            is Set<*> -> this.set.save(output, obj as Set<Any>)
            else -> output.writeStringValue(obj.toString())
        }
    }

    override fun load(input: KInput): ExperimentData {
        val rawString = input.readStringValue()
        TODO()
//        input.readSerializableValue()
//        return df.parse(input.readStringValue())
    }
}

// Workaround until they fix the library
//object DataSerializer : KSerializer<Any?> {
//
//    override val serialClassDesc: KSerialClassDesc
//        get() = PolymorphicClassDesc
//
//    override fun save(output: KOutput, obj: Any?) {
//        when (obj) {
//            null -> {
//                output.writeStringValue("null")
//            }
//            is Map<*, *> -> (StringSerializer to DataSerializer).map.save(output, obj as Map<String, Any?>)
//            is List<*> -> DataSerializer.list.save(output, obj)
//            is Any -> output.writeStringValue(obj.toString())
//            else -> {
//                val saver = serializerByValue(obj)
//                output.writeSerializableElementValue(serialClassDesc, 1, saver, obj)
//            }
//        }
//    }
//
//    override fun load(input: KInput): Any {
//        @Suppress("NAME_SHADOWING")
//        val input = input.readBegin(serialClassDesc)
//        var klassName: String
//        var value: Any? = null
//        mainLoop@ while (true) {
//            when (input.readElement(serialClassDesc)) {
//                KInput.READ_ALL -> {
//                    klassName = input.readStringElementValue(serialClassDesc, 0)
//                    val loader = serializerByClass<Any>(klassName)
//                    value = input.readSerializableElementValue(serialClassDesc, 1, loader)
//                    break@mainLoop
//                }
//                KInput.READ_DONE -> {
//                    break@mainLoop
//                }
//                0 -> {
//                    klassName = input.readStringElementValue(serialClassDesc, 0)
//                }
//                1 -> {
//                    klassName = requireNotNull(klassName) { "Cannot read polymorphic value before its type token" }
//                    val loader = serializerByClass<Any>(klassName)
//                    value = input.readSerializableElementValue(serialClassDesc, 1, loader)
//                }
//                else -> throw SerializationException("Invalid index")
//            }
//        }
//
//        input.readEnd(serialClassDesc)
//        return requireNotNull(value) { "Polymorphic value have not been read" }
//    }
//}


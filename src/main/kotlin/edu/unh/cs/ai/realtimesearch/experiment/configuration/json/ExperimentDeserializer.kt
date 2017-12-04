//package edu.unh.cs.ai.realtimesearch.experiment.configuration.json
//
//import com.fasterxml.jackson.core.JsonParser
//import com.fasterxml.jackson.core.JsonToken
//import com.fasterxml.jackson.databind.DeserializationContext
//import com.fasterxml.jackson.databind.JsonDeserializer
//import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentData
//
///**
// * Json deserializer for ExperimentData.
// */
//class ExperimentDeserializer : JsonDeserializer<ExperimentData>() {
//
//    override fun deserialize(parser: JsonParser, context: DeserializationContext): ExperimentData? {
//        val valueStore: MutableMap<String, Any?> = readObject(parser, context) // Assume that the top level item is an object.
//        return ExperimentData(valueStore)
//    }
//
//    private fun readObject(parser: JsonParser, context: DeserializationContext): MutableMap<String, Any?> {
//        val map: MutableMap<String, Any?> = hashMapOf()
//
//        if (parser.currentToken != JsonToken.START_OBJECT) {
//            throw RuntimeException("Object should start with: {")
//        }
//
//        while (parser.nextToken() != JsonToken.END_OBJECT) {
//            val key = parser.currentName
//            if (parser.currentToken != JsonToken.FIELD_NAME) {
//                throw RuntimeException("Field name expected")
//            }
//
//            parser.nextToken() // Corresponding value
//            val value: Any? = readValue(context, parser)
//            map.put(key, value)
//        }
//
//        return map
//    }
//
//
//    private fun readArray(parser: JsonParser, context: DeserializationContext): MutableList<Any?> {
//        val list = mutableListOf<Any?>()
//
//        if (parser.currentToken != JsonToken.START_ARRAY) {
//            throw RuntimeException("Array should start with: [")
//        }
//
//        while (parser.nextToken() != JsonToken.END_ARRAY) {
//            val value: Any? = readValue(context, parser)
//            list.add(value)
//        }
//
//        return list
//    }
//
//    private fun readValue(context: DeserializationContext, parser: JsonParser): Any? {
//        val type = parser.currentToken
//
//        val value: Any? = when (type) {
//            JsonToken.VALUE_NUMBER_INT -> parser.longValue
//            JsonToken.START_OBJECT -> readObject(parser, context)
//            JsonToken.START_ARRAY -> readArray(parser, context)
//            JsonToken.VALUE_STRING -> parser.text
//            JsonToken.VALUE_NUMBER_FLOAT -> parser.doubleValue
//            JsonToken.VALUE_TRUE -> true
//            JsonToken.VALUE_FALSE -> false
//            JsonToken.VALUE_NULL -> null
//            else -> throw RuntimeException("Unexpected token type: $type")
//        }
//        return value
//    }
//
//}
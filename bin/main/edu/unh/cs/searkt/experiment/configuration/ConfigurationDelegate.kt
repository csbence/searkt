package edu.unh.cs.searkt.experiment.configuration

/**
 * @author Bence Cserna (bence@cserna.net)
 */

fun <T> lazyData(experimentData: ExperimentData, fieldName: String): Lazy<T> {
    return lazy {
        experimentData.getTypedValue<T>(fieldName)
                ?: throw InvalidFieldException("Missing field. Please add $fieldName.")
    }
}

inline fun <T, R> lazyData(experimentData: ExperimentData, fieldName: String, crossinline initializer: (R) -> T): Lazy<T> {
    return lazy {
        val value: R = experimentData.getTypedValue<R>(fieldName)
                ?: throw InvalidFieldException("Missing field. Please add $fieldName.")
        initializer(value)
    }
}
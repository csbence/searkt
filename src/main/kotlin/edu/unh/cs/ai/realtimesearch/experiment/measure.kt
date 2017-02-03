package edu.unh.cs.ai.realtimesearch.experiment

inline fun <T, R> measure(property: () -> T, diff: (T, T) -> R, block: () -> Unit): R {
    val initialPropertyValue = property()
    block()
    return diff(property(), initialPropertyValue)
}

inline fun measureInt(property: () -> Int, block: () -> Unit): Int {
    val initialPropertyValue = property()
    block()
    return property() - initialPropertyValue
}

inline fun measureLong(property: () -> Long, block: () -> Unit): Long {
    val initialPropertyValue = property()
    block()
    return property() - initialPropertyValue
}

//public inline fun measure(property: () -> Double, block: () -> Unit): Double {
//    val initialPropertyValue = property()
//    block()
//    return property() - initialPropertyValue
//}


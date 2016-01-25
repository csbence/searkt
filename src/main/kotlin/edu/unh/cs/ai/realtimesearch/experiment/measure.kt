package edu.unh.cs.ai.realtimesearch.experiment

public inline fun <T, R> measure(property: () -> T, diff: (T, T) -> R, block: () -> Unit): R {
    val initialPropertyValue = property()
    block()
    return diff(property(), initialPropertyValue)
}

public inline fun measureInt(property: () -> Int, block: () -> Unit): Int {
    val initialPropertyValue = property()
    block()
    return property() - initialPropertyValue
}

//public inline fun measure(property: () -> Double, block: () -> Unit): Double {
//    val initialPropertyValue = property()
//    block()
//    return property() - initialPropertyValue
//}


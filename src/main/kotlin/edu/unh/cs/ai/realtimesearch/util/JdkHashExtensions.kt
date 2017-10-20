@file:Suppress("UNCHECKED_CAST")

package edu.unh.cs.ai.realtimesearch.util

import java.util.*

/**
 * @author Bence Cserna (bence@cserna.net)
 */
fun <K, V> HashMap<K, V>.resize(): HashMap<K, V> {
    val nodesClass = this::class.java

    val resizeMethod = nodesClass.declaredMethods.first {
        it.name == "resize"
    }

    resizeMethod.isAccessible = true
    resizeMethod.invoke(this)
    return this
}

fun <K> HashSet<K>.resize(): HashSet<K> {
    val hashSetClass = javaClass

    val internalMapField = hashSetClass.getDeclaredField("map")
    internalMapField.isAccessible = true

    val internalMapInstance = internalMapField.get(this) as HashMap<K, *>
    internalMapInstance.resize()

    return this
}
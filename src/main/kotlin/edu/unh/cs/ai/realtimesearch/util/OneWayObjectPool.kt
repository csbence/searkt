package edu.unh.cs.ai.realtimesearch.util

import java.util.*

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class OneWayObjectPool<T>(val objectCount: Int, val creator: () -> T) {

    private var activeObjectCount = -1;
    private val objects: List<T>

    init {
        objects = ArrayList(objectCount)

        for (i in 1..objectCount) {
            objects.add(creator())
        }
    }

    fun getObject(): T {
        activeObjectCount++
        return objects[activeObjectCount]
    }
}
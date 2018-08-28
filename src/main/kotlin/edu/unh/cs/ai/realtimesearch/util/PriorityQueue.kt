package edu.unh.cs.ai.realtimesearch.util

/**
 * @author Kevin C. Gall
 */
interface PriorityQueue<T> {
    var size: Int

    fun add(item: T)
    fun peek(): T?
    fun clear()
    fun pop(): T?
    fun isEmpty() = size == 0
    fun isNotEmpty() = size != 0
    fun remove(item: T): Boolean = throw UnsupportedOperationException("Arbitrary remove not supported in this Priority Queue Implementation")
}
package edu.unh.cs.ai.realtimesearch.util

import java.util.*

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class AdvancedPriorityQueue<T : Indexable>(private var queue: Array<T?>, private var comparator: Comparator<in T>) {
    private val MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8

    var resizable = false
    var size = 0
        private set

    val backingArray: Array<T?>
        get() = queue

    companion object {
        inline operator fun <reified T : Indexable> invoke(capacity: Int, comparator: Comparator<in T>): AdvancedPriorityQueue<T> {
            return AdvancedPriorityQueue(arrayOfNulls(capacity), comparator)
        }
    }

    private fun grow(minCapacity: Int) {
        if (!resizable) {
            throw RuntimeException("AdvancedPriorityQueue reached its capacity[$size]. Dynamic resizing is disabled.")
        }

        val oldCapacity = queue.size
        var newCapacity = oldCapacity + if (oldCapacity < 64) {
            oldCapacity + 2
        } else {
            oldCapacity shr 1
        }
        // overflow-conscious code
        if (newCapacity - MAX_ARRAY_SIZE > 0) {
            newCapacity = hugeCapacity(minCapacity)
        }
        queue = Arrays.copyOf<T>(queue, newCapacity)
    }

    private fun hugeCapacity(minCapacity: Int): Int {
        if (minCapacity < 0) {
            throw OutOfMemoryError()
        }

        return if (minCapacity > MAX_ARRAY_SIZE) {
            Integer.MAX_VALUE
        } else {
            MAX_ARRAY_SIZE
        }
    }

    fun add(item: T) {
        if (size >= queue.size) {
            grow(size + 1)
        }

        if (size == 0) {
            queue[0] = item
            item.index = 0
        } else {
            siftUp(size, item)
        }

        size += 1
    }

    fun peek(): T? {
        return if (size == 0) null else queue[0]
    }

    operator fun contains(item: T): Boolean {
        return item.index != -1
    }

    fun remove(item: T): Boolean {
        if (item.index == -1) {
            return false
        } else {
            removeAt(item.index)
            return true
        }
    }

    fun clear() {
        for (i in 0..size - 1) {
            queue[i] = null
        }

        size = 0
    }

    fun pop(): T? {
        if (size == 0) {
            return null
        }

        --size
        val result = queue[0]
        val x = queue[size]
        queue[size] = null

        if (size != 0) {
            siftDown(0, x!!)
        }
        return result
    }

    private fun removeAt(index: Int): T? {
        --size
        if (size == index) {
            queue[index]!!.index = -1
            queue[index] = null
        } else {
            val moved = queue[size]!!
            queue[size] = null
            siftDown(index, moved)
            if (queue[index] === moved) {
                siftUp(index, moved)
                if (queue[index] !== moved) {
                    return moved
                }
            }
        }
        return null
    }

    private fun siftUp(index: Int, item: T): Int {
        var currentIndex = index
        while (currentIndex > 0) {
            val parentIndex = (currentIndex - 1).ushr(1)
            val parentNode = queue[parentIndex]!!

            if (comparator.compare(item, parentNode) >= 0) {
                break
            }

            // Move parent down and update its index
            queue[currentIndex] = parentNode
            parentNode.index = currentIndex
            currentIndex = parentIndex
        }

        queue[currentIndex] = item
        item.index = currentIndex
        return currentIndex
    }

    private fun siftDown(index: Int, item: T): Int {
        var currentIndex = index
        val half = size.ushr(1)

        while (currentIndex < half) {
            var childIndex = (currentIndex shl 1) + 1
            var childNode = queue[childIndex]!!
            val rightIndex = childIndex + 1

            if (rightIndex < size && comparator.compare(childNode, queue[rightIndex]) > 0) {
                childIndex = rightIndex
                childNode = queue[rightIndex]!!
            }

            if (comparator.compare(item, childNode) <= 0) {
                break
            }

            queue[currentIndex] = childNode
            childNode.index = currentIndex
            currentIndex = childIndex
        }

        queue[currentIndex] = item
        item.index = currentIndex
        return currentIndex
    }

    fun update(item: T) {
        val index = item.index

        if (siftUp(index) == index) {
            siftDown(index)
        }
    }

    fun reorder(comparator: Comparator<in T>) {
        this.comparator = comparator
        heapify()
    }

    private fun heapify() {
        for (i in size.ushr(1) - 1 downTo 0) {
            siftDown(i)
        }
    }

    fun siftUp(index: Int) = siftUp(index, queue[index]!!)


    fun siftDown(index: Int) = siftDown(index, queue[index]!!)

    fun isEmpty() = size == 0

    fun isNotEmpty() = !isEmpty()

    inline fun forEach(action: (T) -> Unit) {
        for (i in 0..size - 1) {
            action(backingArray[i]!!)
        }
    }

    /**
     * Apply the given action to every item in the priority queue and clear the priority queue.
     */
    inline fun applyAndClear(action: (T) -> Unit) {
        for (i in 0..size - 1) {
            action(backingArray[i]!!)
            backingArray[i] = null
        }
        size = 0
    }
}
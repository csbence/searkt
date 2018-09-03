package edu.unh.cs.ai.realtimesearch.util

import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import java.lang.Integer.max
import java.lang.Integer.min
import java.util.*

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class AdvancedPriorityQueue<T : Indexable>(private var queue: Array<T?>, private var comparator: Comparator<in T>) : AbstractAdvancedPriorityQueue<T>(queue, comparator) {
    override fun getIndex(item: T): Int = item.index
    override fun setIndex(item: T, index: Int) {
        item.index = index
    }

    companion object {
        inline operator fun <reified T : Indexable> invoke(capacity: Int, comparator: Comparator<in T>): AdvancedPriorityQueue<T> =
                AdvancedPriorityQueue(arrayOfNulls(capacity), comparator)
    }
}

interface Indexable {
    var index: Int
    val open
        get() = index >= 0
}

abstract class AbstractAdvancedPriorityQueue<T>(
        private var queue: Array<T?>,
        private var comparator: Comparator<in T>
) : PriorityQueue<T> {
    abstract fun getIndex(item: T): Int
    abstract fun setIndex(item: T, index: Int)

    private val MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8

    private var resizable = false
    override var size = 0

    val backingArray: Array<T?>
        get() = queue

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

    override fun add(item: T) {
        if (size >= queue.size) {
            grow(size + 1)
        }

        if (size == 0) {
            queue[0] = item

            setIndex(item, 0)
        } else {
            siftUp(size, item)
        }

        size += 1
    }

    override fun peek(): T? = if (size == 0) null else queue[0]

    operator fun contains(item: T): Boolean = getIndex(item) != -1

    override fun remove(item: T): Boolean = when {
        getIndex(item) == -1 -> false
        else -> {
            removeAt(getIndex(item))
            setIndex(item, -1)
            true
        }
    }

    override fun clear() {
        for (i in 0 until size) {
            val item = queue[i]
            if (item != null) {
                setIndex(item, -1)
            }
            queue[i] = null
        }

        size = 0
    }

    fun quickClear() {
        size = 0
    }

    override fun pop(): T? {
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

        if (result != null) {
            setIndex(result, -1)
        }
        return result
    }

    private fun removeAt(index: Int): T? {
        --size
        if (size == index) {
            setIndex(queue[index]!!, -1)
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
            setIndex(parentNode, currentIndex)
            currentIndex = parentIndex
        }

        queue[currentIndex] = item
        setIndex(item, currentIndex)
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
            setIndex(childNode, currentIndex)
            currentIndex = childIndex
        }

        queue[currentIndex] = item
        setIndex(item, currentIndex)
        return currentIndex
    }

    fun update(item: T) {
        val index = getIndex(item)
        if (index == -1) throw RuntimeException("Invalid index. Can't update the location of an item that is not on the heap.")

        if (siftUp(index) == index) {
            siftDown(index)
        }
    }

    fun reorder(comparator: Comparator<in T>? = null) {
        this.comparator = comparator ?: this.comparator
        heapify()
    }

    private fun heapify() {
        for (i in size.ushr(1) - 1 downTo 0) {
            siftDown(i)
        }
    }

    fun siftUp(index: Int) = siftUp(index, queue[index]!!)

    fun siftDown(index: Int) = siftDown(index, queue[index]!!)

    override fun isEmpty() = size == 0

    override fun isNotEmpty() = !isEmpty()

    inline fun forEach(action: (T) -> Unit) {
        for (i in 0 until size) {
            action(backingArray[i]!!)
        }
    }

    /**
     * Apply the given action to every item in the priority queue and clear the priority queue.
     */
    inline fun applyAndClear(action: (T) -> Unit) {
        for (i in 0 until size) {
            action(backingArray[i]!!)
            backingArray[i] = null
        }
        size = 0
    }

    /**
     * Copy elements from other priority queue.
     *
     * Notes:
     *
     * This function does not clean up the elements currently in the queue. This could lead to invalid queue indices.
     * The queue must be heapified after initialization.
     *
     * Warning: only use this function if you properly understand what it does.
     *
     * @return - last copied element of other queue or null if the initialization is complete
     */
    fun initializeFromQueue(
            other: AbstractAdvancedPriorityQueue<T>,
            terminationChecker: TerminationChecker,
            startIndex: Int,
            func: (T) -> Boolean = { false }): Int? {
        if (startIndex > size) throw RuntimeException("Initialization must be continuous!")

        size = startIndex

        while (!terminationChecker.reachedTermination()) {
            val remainingCount = other.size - size

            // Copy the remaining elements but max 800
            for (i in 1..min(499, remainingCount)) {
                backingArray[size] = other.backingArray[size]
                setIndex(backingArray[size]!!, size)
                if (func(backingArray[size]!!)) return null

                ++size
            }

            if (size == other.size) return null
        }

        return size
    }

    fun heapify(terminationChecker: TerminationChecker, startIndex: Int = size / 2): Int? {
        var currentIndex = startIndex

        while (!terminationChecker.reachedTermination()) {
            for (i in currentIndex downTo max(currentIndex - 499, 0)) {
                siftDown(currentIndex)
                --currentIndex
            }
//            println(currentIndex)

            if (currentIndex == -1) return null
        }

        return currentIndex
    }

    fun keepTopK(percent: Double) {
        val topK = mutableListOf<T>()

        for (i in 0 until (size * percent).toLong()) {
            topK.add(pop()!!)
        }

        quickClear()

        topK.forEach { add(it) }
    }

}


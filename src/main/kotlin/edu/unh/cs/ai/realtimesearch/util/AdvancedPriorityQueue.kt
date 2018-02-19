package edu.unh.cs.ai.realtimesearch.util

import java.util.*

/**
 * @author Bence Cserna (bence@cserna.net)
 */
open class AdvancedPriorityQueue<T>(private var queue: Array<T?>, private var comparator: Comparator<in T>, private val setIndex: (T, Int) -> Unit, private val getIndex: (T) -> Int) {
    private val MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8

    private var resizable = false
    var size = 0

    val backingArray: Array<T?>
        get() = queue

    companion object {
        inline operator fun <reified T> invoke(capacity: Int, comparator: Comparator<in T>, noinline setIndex: (T, Int) -> (Unit), noinline getIndex: (T) -> (Int)): AdvancedPriorityQueue<T> =
                AdvancedPriorityQueue(arrayOfNulls(capacity), comparator, setIndex, getIndex)
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
//            item.index = 0
            setIndex(item, 0)
        } else {
            siftUp(size, item)
        }

        size += 1
    }

    fun peek(): T? = if (size == 0) null else queue[0]

    operator fun contains(item: T): Boolean = getIndex(item) != -1 //item.index != -1

    fun remove(item: T): Boolean = when {
//        item.index == -1 -> false
        getIndex(item) == -1 -> false
        else -> {
//            removeAt(item.index)
            removeAt(getIndex(item))
            true
        }
    }

    fun clear() {
        for (i in 0 until size) {
//            queue[i]?.index = -1
            setIndex(queue[i]!!, -1)
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

//        result?.index = -1
        setIndex(result!!, -1)
        return result
    }

    private fun removeAt(index: Int): T? {
        --size
        if (size == index) {
//            queue[index]!!.index = -1
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
//            parentNode.index = currentIndex
            setIndex(parentNode, currentIndex)
            currentIndex = parentIndex
        }

        queue[currentIndex] = item
//        item.index = currentIndex
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
//            childNode.index = currentIndex
            setIndex(childNode, currentIndex)
            currentIndex = childIndex
        }

        queue[currentIndex] = item
//        item.index = currentIndex
        setIndex(item, currentIndex)
        return currentIndex
    }

    fun update(item: T) {
        val index = getIndex(item) //item.index
        if (index == -1) throw RuntimeException("Invalid index. Can't update the location of an item that is not on the heap.")

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
}



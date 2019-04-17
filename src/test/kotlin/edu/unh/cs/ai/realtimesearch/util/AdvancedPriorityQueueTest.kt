package edu.unh.cs.searkt.util

import edu.unh.cs.searkt.experiment.terminationCheckers.FakeTerminationChecker
import edu.unh.cs.searkt.experiment.terminationCheckers.StaticTimeTerminationChecker
import org.junit.Test
import java.util.*
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class AdvancedPriorityQueueTest {
    private data class IndexableContainer(var value: Int, override var index: Int = -1) : Indexable

    @Test
    fun testAddRemoveSingleItem() {
        val priorityQueue = AdvancedPriorityQueue<IndexableContainer>(1, Comparator { lhs, rhs ->
            lhs.value - rhs.value
        })

        priorityQueue.add(IndexableContainer(1))
        assertTrue(priorityQueue.size == 1)
        validateIndexes(priorityQueue)

        val firstValue = priorityQueue.pop()
        assertTrue(firstValue!!.value == 1)
        assertTrue(priorityQueue.size == 0)
    }

    @Test
    fun testAddRemoveTwoItems() {
        val priorityQueue = AdvancedPriorityQueue<IndexableContainer>(2, Comparator { lhs, rhs ->
            lhs.value - rhs.value
        })

        priorityQueue.add(IndexableContainer(1))
        assertTrue(priorityQueue.size == 1)
        priorityQueue.add(IndexableContainer(1))
        assertTrue(priorityQueue.size == 2)

        assertTrue(priorityQueue.pop()!!.value == 1)
        assertTrue(priorityQueue.size == 1)
        assertTrue(priorityQueue.pop()!!.value == 1)
        assertTrue(priorityQueue.size == 0)
    }

    @Test
    fun testAddRemoveMultipleItems() {
        val priorityQueue = AdvancedPriorityQueue<IndexableContainer>(100, Comparator { lhs, rhs ->
            lhs.value - rhs.value
        })

        for (i in 1..100) {
            priorityQueue.add(IndexableContainer(1))
            assertTrue(priorityQueue.size == i)
        }

        for (i in 1..100) {
            assertTrue(priorityQueue.pop()!!.value == 1)
            assertTrue(priorityQueue.size == 100 - i)
        }
    }

    @Test
    fun testAddRemoveOrderedItems() {
        val priorityQueue = AdvancedPriorityQueue<IndexableContainer>(100, Comparator { lhs, rhs ->
            lhs.value - rhs.value
        })

        for (i in 1..100) {
            priorityQueue.add(IndexableContainer(i))
            assertTrue(priorityQueue.size == i)
        }

        for (i in 1..100) {
            assertTrue(priorityQueue.pop()!!.value == i)
            assertTrue(priorityQueue.size == 100 - i)
        }
    }

    @Test
    fun testReorderOrderedItems() {
        val comparator = Comparator<IndexableContainer> { lhs, rhs ->
            lhs.value - rhs.value
        }
        val priorityQueue = AdvancedPriorityQueue(100, comparator)

        for (i in 1..100) {
            priorityQueue.add(IndexableContainer(i))
            assertTrue(priorityQueue.size == i)
        }

        val reverseComparator = Comparator<IndexableContainer> { lhs, rhs ->
            rhs.value - lhs.value
        }

        validateIndexes(priorityQueue)

        priorityQueue.reorder(reverseComparator)

        validateIndexes(priorityQueue)

        for (i in 100 downTo 1) {
            assertTrue(priorityQueue.size == i)
            assertTrue(priorityQueue.pop()!!.value == i)
            validateIndexes(priorityQueue)
        }
    }

    @Test
    fun testOrderShuffledItems() {
        val comparator = Comparator<IndexableContainer> { lhs, rhs ->
            lhs.value - rhs.value
        }
        val priorityQueue = AdvancedPriorityQueue(100, comparator)

        val numbers = Array(100) { it }
        val numberList = numbers.asList()

        Collections.shuffle(numberList)

        for (i in 0..99) {
            priorityQueue.add(IndexableContainer(numberList[i]))
            assertTrue(priorityQueue.size == i + 1)
            validateIndexes(priorityQueue)
        }

        for (i in 0..99) {
            assertTrue(priorityQueue.pop()!!.value == i)
            assertTrue(priorityQueue.size == 99 - i)
            validateIndexes(priorityQueue)
        }
    }

    @Test
    fun testUpdateItem() {
        val comparator = Comparator<IndexableContainer> { lhs, rhs ->
            lhs.value - rhs.value
        }

        val priorityQueue = AdvancedPriorityQueue(100, comparator)

        for (i in 1..100) {
            priorityQueue.add(IndexableContainer(i))
            assertTrue(priorityQueue.size == i)
        }

        validateIndexes(priorityQueue)

        priorityQueue.backingArray[50]!!.value = 0
        priorityQueue.siftUp(50)

        validateIndexes(priorityQueue)

        assertTrue(priorityQueue.size == 100)
        assertTrue(priorityQueue.peek()!!.value == 0)
        assertTrue(priorityQueue.peek()!!.index == 0)
    }

    @Test
    fun testIndexStability() {
        val comparator = Comparator<IndexableContainer> { lhs, rhs ->
            lhs.value - rhs.value
        }

        val priorityQueue = AdvancedPriorityQueue(1000, comparator)

        val numbers = Array(1000) { it }
        val numberList = numbers.asList()

        Collections.shuffle(numberList)

        for (i in 0..299) {
            priorityQueue.add(IndexableContainer(numberList[i]))
            assertTrue(priorityQueue.size == i + 1)
        }

        for (i in 0..57) {
            when  {
                i % 7 == 0 -> {
                    priorityQueue.add(IndexableContainer(numberList[i]))
                    validateIndexes(priorityQueue)
                }

                i % 5 == 0 -> {
                    priorityQueue.add(IndexableContainer(numberList[i] shr 1))
                    validateIndexes(priorityQueue)
                }

                else -> {
                    assertTrue(priorityQueue.peek() != null)
                    validateIndexes(priorityQueue)
                }
            }
        }
    }

    @Test
    fun testCopyInitialization() {
        val comparator1 = Comparator<IndexableContainer> { lhs, rhs ->
            lhs.value - rhs.value
        }

        val comparator2 = Comparator<IndexableContainer> { lhs, rhs ->
            -(lhs.value - rhs.value)
        }

        val priorityQueue = AdvancedPriorityQueue(100000, comparator1)
        val priorityQueueCopy = AdvancedPriorityQueue(100000, comparator2)

        val numbers = Array(100000) { it }
        val numberList = numbers.asList()

        Collections.shuffle(numberList)

        // Add all elements to queue 1
        numberList.forEach { priorityQueue.add(IndexableContainer(it)) }
        validateIndexes(priorityQueue)

        assertTrue(priorityQueue.size == 100000)

        val terminationChecker = StaticTimeTerminationChecker(100000, 0)
        val fakeTerminationChecker = FakeTerminationChecker

        // First incomplete step
        val completedIndex1 = priorityQueueCopy.initializeFromQueue(priorityQueue, terminationChecker, 0)
        assertNotNull(completedIndex1)

        // First incomplete step
        val completedIndex1b = priorityQueueCopy.initializeFromQueue(priorityQueue, fakeTerminationChecker, 0)
        assertNull(completedIndex1b)

        val completedIndex2 = priorityQueueCopy.initializeFromQueue(priorityQueue, fakeTerminationChecker, 0)
        assertNull(completedIndex2)

        terminationChecker.resetTo(0)
        assertNotNull(priorityQueueCopy.heapify(terminationChecker))

        assertNull(priorityQueueCopy.heapify(fakeTerminationChecker))
        validateIndexes(priorityQueueCopy)
    }

    private fun validateIndexes(queue: AdvancedPriorityQueue<*>) {
        for (i in 0 until queue.backingArray.size) {
            val indexable = queue.backingArray[i] ?: continue
            assertTrue(indexable.index == i)
        }
    }

}
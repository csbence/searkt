//package edu.unh.cs.ai.realtimesearch.util
//
//import org.junit.Test
//import java.util.*
//import kotlin.test.assertTrue
//
///**
// * @author Bence Cserna (bence@cserna.net)
// */
//class AdvancedPriorityQueueTest {
//    private data class IndexableContainer(var value: Int, var index: Int = -1)
//    private val setIndex: (item: IndexableContainer, index: Int) -> (Unit) = { item, index -> item.index = index }
//    private val getIndex: (item: IndexableContainer) -> (Int) = { item -> item.index }
//
//    @Test
//    fun testAddRemoveSingleItem() {
//        val priorityQueue = AdvancedPriorityQueue<IndexableContainer>(1, Comparator { lhs, rhs ->
//            lhs.value - rhs.value
//        }, setIndex, getIndex)
//
//        priorityQueue.add(IndexableContainer(1))
//        assertTrue(priorityQueue.size == 1)
//        validateIndexes(priorityQueue)
//
//        val firstValue = priorityQueue.pop()
//        assertTrue(firstValue!!.value == 1)
//        assertTrue(priorityQueue.size == 0)
//    }
//
//    @Test
//    fun testAddRemoveTwoItems() {
//        val priorityQueue = AdvancedPriorityQueue<IndexableContainer>(2, Comparator { lhs, rhs ->
//            lhs.value - rhs.value
//        }, setIndex, getIndex)
//
//        priorityQueue.add(IndexableContainer(1))
//        assertTrue(priorityQueue.size == 1)
//        priorityQueue.add(IndexableContainer(1))
//        assertTrue(priorityQueue.size == 2)
//
//        assertTrue(priorityQueue.pop()!!.value == 1)
//        assertTrue(priorityQueue.size == 1)
//        assertTrue(priorityQueue.pop()!!.value == 1)
//        assertTrue(priorityQueue.size == 0)
//    }
//
//    @Test
//    fun testAddRemoveMultipleItems() {
//        val priorityQueue = AdvancedPriorityQueue<IndexableContainer>(100, Comparator { lhs, rhs ->
//            lhs.value - rhs.value
//        }, setIndex, getIndex)
//
//        for (i in 1..100) {
//            priorityQueue.add(IndexableContainer(1))
//            assertTrue(priorityQueue.size == i)
//        }
//
//        for (i in 1..100) {
//            assertTrue(priorityQueue.pop()!!.value == 1)
//            assertTrue(priorityQueue.size == 100 - i)
//        }
//    }
//
//    @Test
//    fun testAddRemoveOrderedItems() {
//        val priorityQueue = AdvancedPriorityQueue<IndexableContainer>(100, Comparator { lhs, rhs ->
//            lhs.value - rhs.value
//        }, setIndex, getIndex)
//
//        for (i in 1..100) {
//            priorityQueue.add(IndexableContainer(i))
//            assertTrue(priorityQueue.size == i)
//        }
//
//        for (i in 1..100) {
//            assertTrue(priorityQueue.pop()!!.value == i)
//            assertTrue(priorityQueue.size == 100 - i)
//        }
//    }
//
//    @Test
//    fun testReorderOrderedItems() {
//        val comparator = Comparator<IndexableContainer> { lhs, rhs ->
//            lhs.value - rhs.value
//        }
//        val priorityQueue = AdvancedPriorityQueue<IndexableContainer>(100, comparator, setIndex, getIndex)
//
//        for (i in 1..100) {
//            priorityQueue.add(IndexableContainer(i))
//            assertTrue(priorityQueue.size == i)
//        }
//
//        val reverseComparator = Comparator<IndexableContainer> { lhs, rhs ->
//            rhs.value - lhs.value
//        }
//
//        validateIndexes(priorityQueue)
//
//        priorityQueue.reorder(reverseComparator)
//
//        validateIndexes(priorityQueue)
//
//        for (i in 100 downTo 1) {
//            assertTrue(priorityQueue.size == i)
//            assertTrue(priorityQueue.pop()!!.value == i)
//            validateIndexes(priorityQueue)
//        }
//    }
//
//    @Test
//    fun testOrderShuffledItems() {
//        val comparator = Comparator<IndexableContainer> { lhs, rhs ->
//            lhs.value - rhs.value
//        }
//        val priorityQueue = AdvancedPriorityQueue<IndexableContainer>(100, comparator, setIndex, getIndex)
//
//        val numbers = Array(100, { it })
//        val numberList = numbers.asList()
//
//        Collections.shuffle(numberList)
//
//        for (i in 0..99) {
//            priorityQueue.add(IndexableContainer(numberList[i]))
//            assertTrue(priorityQueue.size == i + 1)
//            validateIndexes(priorityQueue)
//        }
//
//        for (i in 0..99) {
//            assertTrue(priorityQueue.pop()!!.value == i)
//            assertTrue(priorityQueue.size == 99 - i)
//            validateIndexes(priorityQueue)
//        }
//    }
//
//    @Test
//    fun testUpdateItem() {
//        val comparator = Comparator<IndexableContainer> { lhs, rhs ->
//            lhs.value - rhs.value
//        }
//
//        val priorityQueue = AdvancedPriorityQueue<IndexableContainer>(100, comparator, setIndex, getIndex)
//
//        for (i in 1..100) {
//            priorityQueue.add(IndexableContainer(i))
//            assertTrue(priorityQueue.size == i)
//        }
//
//        validateIndexes(priorityQueue)
//
//        priorityQueue.backingArray[50]!!.value = 0
//        priorityQueue.siftUp(50)
//
//        validateIndexes(priorityQueue)
//
//        assertTrue(priorityQueue.size == 100)
//        assertTrue(priorityQueue.peek()!!.value == 0)
//        assertTrue(priorityQueue.peek()!!.index == 0)
//    }
//
//    @Test
//    fun testIndexStability() {
//        val comparator = Comparator<IndexableContainer> { lhs, rhs ->
//            lhs.value - rhs.value
//        }
//
//        val priorityQueue = AdvancedPriorityQueue<IndexableContainer>(1000, comparator, setIndex, getIndex)
//
//        val numbers = Array(1000, { it })
//        val numberList = numbers.asList()
//
//        Collections.shuffle(numberList)
//
//        for (i in 0..299) {
//            priorityQueue.add(IndexableContainer(numberList[i]))
//            assertTrue(priorityQueue.size == i + 1)
//        }
//
//        for (i in 0..57) {
//            when  {
//                i % 7 == 0 -> {
//                    priorityQueue.add(IndexableContainer(numberList[i]))
//                    validateIndexes(priorityQueue)
//                }
//
//                i % 5 == 0 -> {
//                    priorityQueue.add(IndexableContainer(numberList[i] shr 1))
//                    validateIndexes(priorityQueue)
//                }
//
//                else -> {
//                    assertTrue(priorityQueue.peek() != null)
//                    validateIndexes(priorityQueue)
//                }
//
//            }
//
//        }
//    }
//
//
//
//    private fun validateIndexes(queue: AdvancedPriorityQueue<*>) {
//        for (i in  0..queue.backingArray.size - 1) {
//            val indexable = queue.backingArray[i] ?: continue
//            assertTrue(getIndex(indexable) == i)
//        }
//    }
//
//}
package edu.unh.cs.ai.realtimesearch.util

import org.junit.Test
import java.util.*
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BucketOpenListTest {

    private data class Node(var f: Double, var g: Double, var h: Double, var state: Int) : BucketNode {
        override fun getFValue(): Double = f
        override fun getGValue(): Double = g
        override fun getHValue(): Double = h
        override fun updateIndex(i: Int) {
            state = i
        }
    }

    @Test
    fun insertSameElement() {
        val element = Node(5.0, 3.0, 2.0, 0)
        val bop = BucketOpenList<BucketOpenListTest.Node>(1.0)
        for (i in 1 until 15) {
            bop.add(element)
        }
        println(bop)
        assertTrue(bop.getBucket(element) != null)
        assertEquals(bop.getBucket(element)!!.g, element.g, "element.g ${element.g}")
        assertEquals(bop.getBucket(element)!!.h, element.h, "element.h ${element.h}")
    }

    @Test
    fun insertDifferentElements() {
        val bop = BucketOpenList<BucketOpenListTest.Node>(1.0)
        (1 until 15).forEach { i ->
            val element = Node(i + 1.0, i.toDouble(), 1.0, 0)
            bop.add(element)
            assertTrue(bop.getBucket(element) != null)
            assertEquals(i, bop.numberOfBuckets)
            assertEquals(i, bop.size)
            assertEquals(bop.getBucket(element)!!.g, element.g, "element.g ${element.g}")
            assertEquals(bop.getBucket(element)!!.h, element.h, "element.h ${element.h}")
            assertEquals(2.0, bop.minFValue)
        }
    }

    @Test
    fun insertOneRemoveOne() {
        val bop = BucketOpenList<BucketOpenListTest.Node>(1.0)
        val element = Node(5.0, 3.0, 2.0, 0)
        assertTrue { bop.minFValue == Double.MAX_VALUE }
        bop.add(element)
        assertTrue { bop.minFValue == 5.0 }
        val removedElement = bop.chooseNode()
        assertTrue { bop.minFValue == Double.MAX_VALUE }
        assertEquals(element, removedElement, "element $element | removedElement $removedElement")
    }

    @Test
    fun insertFiveDecreasingFMin() {
        var size = 1
        val bop = BucketOpenList<BucketOpenListTest.Node>(1.0)
        intArrayOf(5, 4, 3, 2, 1).forEach { i ->
            val element = Node(i + 1.0, i.toDouble(), 1.0, 0)
            bop.add(element)
            assertEquals(size, bop.size)
            assertEquals(bop.minFValue, i.toDouble() + 1, "minFValue ${bop.minFValue} | i: ${i + 1}")
            size++
        }
    }

    @Test
    fun insertRemoveFiveDecreasing() {
        var size = 1
        val bop = BucketOpenList<BucketOpenListTest.Node>(1.0)
        intArrayOf(5, 4, 3, 2, 1).forEach { i ->
            val element = Node(i + 1.0, i.toDouble(), 1.0, 0)
            bop.add(element)
            assertEquals(size, bop.size)
            assertEquals(bop.minFValue, i.toDouble() + 1, "minFValue ${bop.minFValue} | i: ${i + 1}")
            size++
        }
        println(bop)
        intArrayOf(5, 4, 3, 2, 1).forEach { _ ->
            val removedElement = bop.chooseNode()
            println(removedElement)
            assertNotEquals(removedElement!!.getFValue(), bop.minFValue)
        }
    }

    @Test
    fun addRemoveRandom() {
        val rng = Random(1L)
        val bop = BucketOpenList<BucketOpenListTest.Node>(1.0)
        (1 until 25).forEach {
            val randomDouble = (rng.nextDouble() * 100) % 25
            val anotherRandomDouble = (rng.nextDouble() * 100) % 10
            val element = Node(randomDouble, anotherRandomDouble, randomDouble - anotherRandomDouble, rng.nextInt())
            bop.add(element)
        }
        println(bop)
        (1 until 25).forEach {
            println(bop)
            println(bop.chooseNode())
            println("##$it##")
        }
    }

    @Test
    fun addRemoveSameNodes() {
        val bop = BucketOpenList<BucketOpenListTest.Node>(1.0)
        val element = Node(5.0, 4.0, 1.0, 0)
        val element2 = Node(4.0, 2.0, 2.0, 0)
        val element3 = Node(4.0, 3.0, 1.0, 1)
        val element4 = Node(3.0, 1.0, 2.0, 3)
        val element5 = Node(5.0, 3.0, 2.0, 1)

        (1 until 10).forEach {
            bop.add(element)
            bop.add(element2)
            bop.add(element3)
            bop.add(element4)
            bop.add(element5)
        }
        assertTrue { bop.minFValue == 3.0 }
        (1 until 10).forEach {
            assertTrue { bop.minFValue == 3.0 }
            val removedElement = bop.chooseNode()
            assertTrue { removedElement!!.getFValue() == 3.0 }
            assertTrue { removedElement!!.getGValue() == 1.0 }
            assertTrue { removedElement!!.getHValue() == 2.0 }
        }

        assertTrue { bop.minFValue == 4.0 }
        (1 until 10).forEach {
            assertTrue { bop.minFValue == 4.0 }
            val removedElement = bop.chooseNode()
            assertTrue { removedElement!!.getFValue() == 4.0 }
            assertTrue { removedElement!!.getGValue() == 2.0 }
            assertTrue { removedElement!!.getHValue() == 2.0 }
        }
        assertTrue { bop.minFValue == 4.0 }
        (1 until 10).forEach {
            assertTrue { bop.minFValue == 4.0 }
            val removedElement = bop.chooseNode()
            assertTrue { removedElement!!.getFValue() == 4.0 }
            assertTrue { removedElement!!.getGValue() == 3.0 }
            assertTrue { removedElement!!.getHValue() == 1.0 }
        }
        assertTrue { bop.minFValue == 5.0 }
        (1 until 10).forEach {
            assertTrue { bop.minFValue == 5.0 }
            val removedElement = bop.chooseNode()
            assertTrue { removedElement!!.getFValue() == 5.0 }
            assertTrue { removedElement!!.getGValue() == 3.0 }
            assertTrue { removedElement!!.getHValue() == 2.0 }
        }
        assertTrue { bop.minFValue == 5.0 }
        (1 until 10).forEach {
            assertTrue { bop.minFValue == 5.0 }
            val removedElement = bop.chooseNode()
            assertTrue { removedElement!!.getFValue() == 5.0 }
            assertTrue { removedElement!!.getGValue() == 4.0 }
            assertTrue { removedElement!!.getHValue() == 1.0 }
        }
        assertTrue { bop.minFValue == Double.MAX_VALUE }
        assertTrue { !bop.isNotEmpty() }
        (1 until 10).forEach {
            bop.add(element)
            bop.add(element2)
            bop.add(element3)
            bop.add(element4)
            bop.add(element5)
        }
        assertTrue { bop.minFValue == 3.0 }
        println(bop)
    }


    @Test
    fun replacementTest() {
        val bop = BucketOpenList<BucketOpenListTest.Node>(1.0)
        val element = Node(5.0, 4.0, 1.0, 0)
        val element2 = Node(4.0, 2.0, 2.0, 0)
        val element3 = Node(4.0, 3.0, 1.0, 1)
        val element4 = Node(3.0, 1.0, 2.0, 3)
        val element5 = Node(5.0, 3.0, 2.0, 1)

        bop.add(element)
        assertTrue { bop.getBucket(element)!!.nodes.first() == element }
        bop.add(element3)
        assertTrue { bop.getBucket(element3)!!.nodes.first() == element3 }
        bop.replace(element3, element2)
        assertTrue { bop.getBucket(element3)!!.nodes.size == 0 }
        assertTrue { bop.getBucket(element2)!!.nodes.first() == element2 }
        bop.add(element4)
        assertTrue { bop.getBucket(element4)!!.nodes.first() == element4 }
        bop.add(element5)
        assertTrue { bop.getBucket(element5)!!.nodes.first() == element5 }
        bop.replace(element5, element)
        println(bop)
        assertTrue { bop.getBucket(element5)!!.nodes.size == 0 }
    }
}


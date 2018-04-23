package edu.unh.cs.ai.realtimesearch.util

import org.junit.Test
import kotlin.test.assertTrue

class GEQueueTest {
    private val basicCompare = Comparator<Node> { lhs, rhs ->
        when {
            lhs.value < rhs.value -> -1
            lhs.value > rhs.value -> 1
            else -> 0
        }
    }

    class Node: RBTreeElement<Node, Node>, SearchQueueElement<Node> {
        override val hHat: Double
            get() = value.toDouble()
        override val dHat: Double
            get() = value.toDouble()
        override var parent: Node?
            get() = null
            set(value) {}

        private val indexMap = Array(2, {-1})

        override val f: Double
            get() = value.toDouble()
        override val g: Double
            get() = value .toDouble()
        override val depth: Double
            get() = value.toDouble()
        override val h: Double
            get() = value.toDouble()
        override val d: Double
            get() = value.toDouble()

        override fun setIndex(key: Int, index: Int) {
            indexMap[key] = index
        }

        override fun getIndex(key: Int): Int {
            return indexMap[key]
        }

        var value: Int = -1
        var internalNode: RBTreeNode<Node, Node>? = null

        override var node: RBTreeNode<Node, Node>?
            get() = internalNode
            set(value) { internalNode = value }
    }

    @Test
    fun testCreation() {
        val focal = BinHeap(100, basicCompare,1 )
        val geQueue = GEQueue(basicCompare, basicCompare, 1, focal)
        val initNode = Node()
        initNode.value = 110
        (1..10).forEach {
            val newNode = Node()
            newNode.value = it
            geQueue.add(newNode, initNode)
        }
        (1..10).forEach { println(geQueue.pollFocal()) }
        assertTrue { geQueue.isEmpty }
    }
}

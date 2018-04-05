package edu.unh.cs.ai.realtimesearch.util

import org.junit.Test
import java.util.*
import kotlin.test.assertTrue

class RBTreeTest {

    private val basicCompare = Comparator<Node> { lhs, rhs ->
        when {
            lhs.value < rhs.value -> -1
            lhs.value > rhs.value -> 1
            else -> 0
        }
    }

    class Node : RBTreeElement<Node, Node> {
        override fun toString(): String {
            return "Node(value=$value)"
        }

        var value: Int = -1
        var internalNode: RBTreeNode<Node, Node>? = null

        override var node: RBTreeNode<Node, Node>?
            get() = internalNode
            set(value) {
                internalNode = value
            }
    }

    @Test
    fun testCreation() {
        val rbTree = RBTree(basicCompare, basicCompare)
        (1..10).forEach {
            val newNode: Node = Node()
            newNode.value = it
            rbTree.insert(newNode, newNode)
            assertTrue { rbTree.lookup(newNode)!!.value == newNode }
            println()
            rbTree.print()
            println()
        }

        (1..10).forEach {
            val node = rbTree.poll()
            assertTrue { node != null }
        }

        assertTrue { rbTree.poll() == null }


    }
}

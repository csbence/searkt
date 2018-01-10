package edu.unh.cs.ai.realtimesearch.util

import org.junit.Test
import java.util.*

class RedBlackTreeTest {

    data class SimpleNode(val f: Int) : RedBlackTreeElement<SimpleNode, Int> {

        override var index: Int = -1

        private var node: RedBlackTreeNode<SimpleNode, Int>? = null

        override fun getNode(): RedBlackTreeNode<SimpleNode, Int>? {
           return this.node
        }

        override fun setNode(node: RedBlackTreeNode<SimpleNode, Int>?) {
            this.node = node
        }

    }

    class SimpleNodeComparator<T>: Comparator<T> {
        override fun compare(p0: T, p1: T): Int {
            if (p0 is SimpleNode && p1 is SimpleNode) {
                return p0.f - p1.f
            }
            throw Exception("Can only compare SimpleNodes!")
        }
    }

    @Test
    fun oneToTenInsertTest() {
        val tree = RedBlackTree<SimpleNode, Int>(SimpleNodeComparator(), SimpleNodeComparator())
        (1..10).forEach { number ->
            tree.insert(SimpleNode(number), number)
        }
        tree.getValues().forEach(::println)
        tree.print()
    }

    @Test
    fun tenToOneInsertTest() {
        val tree = RedBlackTree<SimpleNode, Int>(SimpleNodeComparator(), SimpleNodeComparator())
        (10 downTo 1).forEach { number ->
            tree.insert(SimpleNode(number), number)
        }
        tree.getValues().forEach(::println)
        tree.print()
    }

    @Test
    fun randomTenInsertTest() {
        val tree = RedBlackTree<SimpleNode, Int>(SimpleNodeComparator(), SimpleNodeComparator())
        val randomGenerator = Random()
        (1..10).forEach {
            val key = randomGenerator.nextInt(10000)
            val value = randomGenerator.nextInt(10000)
            tree.print()

            val nodeToInsert = SimpleNode(key)
            println("Inserting $nodeToInsert -> $value\n")

            tree.insert(nodeToInsert, value)
            tree.print()
            assert(tree.lookUp(nodeToInsert)?.value?.equals(value) ?: false)
        }
        tree.getValues().forEach(::println)
        tree.print()
    }

    @Test
    fun insertTenRemoveTenTest() {
        val tree = RedBlackTree<SimpleNode, Int>(SimpleNodeComparator(), SimpleNodeComparator())
        val nodes = ArrayList<SimpleNode>()
        (1..10).forEach{ number ->
            val node = SimpleNode(number)
            tree.insert(node, number)
            nodes.add(node)
        }
        tree.print()
        nodes.forEach { node ->
            println("Deleting $node...")
            tree.delete(node)
            tree.print()
        }
        assert(tree.peek() == null) // empty tree
    }

    @Test
    fun insertRandomRemoveRandom() {
        val tree = RedBlackTree<SimpleNode, Int>(SimpleNodeComparator(), SimpleNodeComparator())
        val nodes = ArrayList<SimpleNode>()
        val randomGenerator = Random(1L)
        (1..5000).forEach {
            val key = randomGenerator.nextInt(10000)
            val value =  randomGenerator.nextInt(10000)

            val nodeToInsert = SimpleNode(key)
            println("Inserting $nodeToInsert -> $value\n")

            tree.insert(nodeToInsert, value)
            nodes.add(nodeToInsert)
            //tree.print()
            assert(tree.lookUp(nodeToInsert)?.value?.equals(value) ?: false)
        }
        nodes.forEach {
            try {
                val randomIndex = randomGenerator.nextInt(nodes.size)
                val toRemove = nodes[randomIndex]
                println("\nDeleting $toRemove...\n")
                tree.delete(toRemove)
            } catch (e: Exception) {
                tree.print()
            }
            //tree.print()
        }
    }
}

package edu.unh.cs.ai.realtimesearch.util

private class RedBlackTreeException(msg: String) : Exception(msg)

enum class NodeColor {
    RED, BLACK
}

class RedBlackTreeNode<K : RedBlackTreeElement<K, V>, V>(val key: K, val value: V, var color: NodeColor,
                                                         val left: RedBlackTreeNode<K, V>? = null,
                                                         val right: RedBlackTreeNode<K, V>? = null) {

    val parent: RedBlackTreeNode<K,V>? = null

    val grandParent
        get() = {
            if (parent?.parent != null) {
                parent.parent
            }
        }

    val sibling
        get() = {
            if (parent != null) {
                if (this == parent.left) {
                    parent.right
                } else {
                    parent.left
                }
            }
        }

    val uncle
        get() = {
            if (parent?.parent != null) {
                parent.sibling
            }
        }

}

interface RedBlackTreeElement<K : RedBlackTreeElement<K,V>, V> {
    fun getNode() : RedBlackTreeNode<K,V>
    fun setNode(node: RedBlackTreeNode<K, V>)
}

class RedBlackTree<K : RedBlackTreeElement<K, V>, V>(val sComparator: Comparator<K>, val vComparator: Comparator<K>) {

    private val root: RedBlackTreeNode<K, V>? = null



}


package edu.unh.cs.ai.realtimesearch.util

private class RedBlackTreeException(msg: String) : Exception(msg)

enum class NodeColor {
    RED, BLACK
}

class RedBlackTreeNode<K : RedBlackTreeElement<K, V>, V>(var key: K, var value: V, var color: NodeColor,
                                                         var left: RedBlackTreeNode<K, V>? = null,
                                                         var right: RedBlackTreeNode<K, V>? = null) {

    var parent: RedBlackTreeNode<K, V>? = null

    fun grandParent(): RedBlackTreeNode<K, V>? {
        if (parent?.parent != this) {
            return parent?.parent
        }
        throw RedBlackTreeException("Tried to get the parent of the root.")
    }

    fun sibling(): RedBlackTreeNode<K, V>? {
        if (parent != this) {
            return if (this == parent?.left) {
                parent?.right
            } else {
                parent?.left
            }
        }
        throw RedBlackTreeException("Tried to get the sibling of the root.")
    }

    fun uncle(): RedBlackTreeNode<K, V>? {
        if (parent?.parent != this) {
            return parent?.sibling()
        }
        throw RedBlackTreeException("Tried to get the uncle of the root.")
    }

}

interface RedBlackTreeElement<K : RedBlackTreeElement<K, V>, V> {
    fun getNode(): RedBlackTreeNode<K, V>?
    fun setNode(node: RedBlackTreeNode<K, V>?)
}


package edu.unh.cs.ai.realtimesearch.util

private class RedBlackTreeException(msg: String) : Exception(msg)

enum class NodeColor {
    RED, BLACK
}

class RedBlackTreeNode<K : RedBlackTreeElement<K, V>, V>(var key: K, var value: V, var color: NodeColor,
                                                         var left: RedBlackTreeNode<K, V>? = null,
                                                         var right: RedBlackTreeNode<K, V>? = null) {

    var parent: RedBlackTreeNode<K, V> = this

    fun grandParent(): RedBlackTreeNode<K, V> {
        if (parent.parent != this) {
            return parent.parent
        }
        throw RedBlackTreeException("Tried to get the parent of the root.")
    }

    fun sibling(): RedBlackTreeNode<K, V>? {
        if (parent != this) {
            return if (this == parent.left) {
                parent.right
            } else {
                parent.left
            }
        }
        throw RedBlackTreeException("Tried to get the sibling of the root.")
    }

    fun uncle(): RedBlackTreeNode<K, V>? {
        if (parent.parent != this) {
            return parent.sibling()
        }
        throw RedBlackTreeException("Tried to get the uncle of the root.")
    }

    //    val grandParent
//        get() = {
//            if (parent.parent != this) {
//                parent.parent
//            }
//        }
//
//    val sibling
//        get() = {
//            if (parent != this) {
//                if (this == parent.left) {
//                    parent.right
//                } else {
//                    parent.left
//                }
//            }
//        }
//
//    val uncle
//        get() = {
//            if (parent.parent != this) {
//                parent.sibling
//            }
//        }

}

interface RedBlackTreeElement<K : RedBlackTreeElement<K, V>, V> {
    fun getNode(): RedBlackTreeNode<K, V>
    fun setNode(node: RedBlackTreeNode<K, V>?)
}

interface RedBlackTreeVisitor<in K> {
    fun visit(k: K, op: Int)
}

class RedBlackTree<K : RedBlackTreeElement<K, V>, V>(val sComparator: Comparator<K>, val vComparator: Comparator<K>) {

    private var root: RedBlackTreeNode<K, V>? = null

    fun delete(key: K) {
        val node = lookUp(key)
        deleteNode(node)
        key.setNode(null)
    }

    fun poll(): V? {
        if (root == null) return null
        val minimumNode = minimumNode(root!!)
        deleteNode(minimumNode)
        minimumNode.key.setNode(null)
        return minimumNode.value
    }

    fun peek(): V? {
        if (root == null) return null
        val minimumNode = minimumNode(root!!)
        return minimumNode.value
    }

    fun lookUp(key: K) = key.getNode()

    fun visit(l: K, u: K, op: Int, visitor: RedBlackTreeVisitor<K>) {
        visit(l, u, root!!, op, visitor)
    }

    fun insert(key: K, value: V) {
        val insertedNode = RedBlackTreeNode(key, value, NodeColor.RED)
        key.setNode(insertedNode)
        if (root == null) {
            root = insertedNode
        } else {
            var n = root!!
            while (true) {
                val compareResult = sComparator.compare(key, n.key)
                if (compareResult <= 0) {
                    if (n.left == null) {
                        n.left = insertedNode
                        break
                    } else {
                        n = n.left!!
                    }
                } else {
                    if (n.right == null) {
                        n.right = insertedNode
                        break
                    } else {
                        n = n.right!!
                    }
                }
            }
            insertedNode.parent = n
        }
        insertCase1(insertedNode)
    }

    private fun insertCase1(node: RedBlackTreeNode<K, V>) {
        if (node.parent == node) {
            node.color = NodeColor.BLACK
        } else {
            insertCase2(node)
        }
    }

    private fun insertCase2(node: RedBlackTreeNode<K, V>) {
        if (node.parent.color != NodeColor.BLACK) {
            insertCase3(node)
        }
    }

    private fun insertCase3(node: RedBlackTreeNode<K, V>) {
        if (node.uncle()?.color == NodeColor.RED) {
            node.parent.color = NodeColor.BLACK
            node.uncle()?.color = NodeColor.BLACK
            node.grandParent().color = NodeColor.RED
            insertCase1(node.grandParent())
        } else {
            insertCase4(node)
        }
    }

    private fun insertCase4(node: RedBlackTreeNode<K, V>) {
        if (node == node.parent.right && node.parent == node.grandParent().left) {
            rotateLeft(node.parent)
            insertCase5(node.left!!)
        } else if (node == node.parent && node.parent == node.grandParent().right) {
            rotateRight(node.parent)
            insertCase5(node.right!!)
        }
    }

    private fun insertCase5(node: RedBlackTreeNode<K, V>) {
        node.parent.color = NodeColor.BLACK
        node.grandParent().color = NodeColor.RED
        if (node == node.parent.left && node.parent == node.grandParent().left) {
            rotateRight(node.grandParent())
        } else if (node == node.parent.right && node.parent == node.grandParent().right) {
            rotateLeft(node.grandParent())
        }
    }

    private fun visit(l: K, u: K, root: RedBlackTreeNode<K, V>, op: Int, visitor: RedBlackTreeVisitor<K>) {
        if (vComparator.compare(root.key, l) > 0) {
            visit(l, u, root.left!!, op, visitor)
            if (vComparator.compare(root.key, u) <= 0) {
                visitor.visit(root.key, op)
                visit(l, u, root.left!!, op, visitor)
            }
        } else {
            visit(l, u, root.right!!, op, visitor)
        }
    }

    private fun rotateLeft(node: RedBlackTreeNode<K, V>) {
        val rightNode = node.right!!
        replaceNode(node, rightNode)
        node.right = rightNode.left
        if (rightNode.left != null) {
            rightNode.left?.parent = node
        }
        rightNode.left = node
        node.parent = rightNode
    }

    private fun rotateRight(node: RedBlackTreeNode<K, V>) {
        val leftNode = node.left!!
        replaceNode(node, leftNode)
        node.left = leftNode.right
        if (leftNode.right != null) {
            leftNode.left?.parent = node
        }
        leftNode.right = node
        node.parent = leftNode
    }

    private fun replaceNode(node: RedBlackTreeNode<K, V>, replacement: RedBlackTreeNode<K, V>) {
        when (node) {
            node.parent -> root = replacement
            node.parent.left -> node.parent.left = replacement
            else -> node.parent.right = replacement
        }
        replacement.parent = node.parent
    }


    private fun deleteNode(node: RedBlackTreeNode<K, V>) {
        var predecessor: RedBlackTreeNode<K, V>? = null
        if (node != node.parent) {
            if (node.left != null && node.right != null) {
                predecessor = maximumNode(node.left!!)
                node.key = predecessor.key
                node.value = predecessor.value
                node.key.setNode(node)
            }
        }

        val child = when (predecessor!!.right) {
            null -> predecessor.left
            else -> predecessor.right
        }
        if (predecessor.color == NodeColor.BLACK) {
            predecessor.color = child?.color ?: throw RedBlackTreeException("Unable to delete node predecessor has no child")
            deleteCase1(predecessor)
        }
        replaceNode(predecessor, child!!)
        if (root!!.color == NodeColor.RED) {
            root!!.color = NodeColor.BLACK
        }
    }

    private fun deleteCase1(node: RedBlackTreeNode<K, V>) {
        if (node.parent != node) {
            deleteCase2(node)
        }
    }

    private fun deleteCase2(node: RedBlackTreeNode<K, V>) {
        if (node.sibling()?.color == NodeColor.RED) {
            node.parent.color = NodeColor.RED
            val nodeSibling = node.sibling()
            nodeSibling?.color = NodeColor.BLACK
            if (node == node.parent.left) {
                rotateLeft(node.parent)
            } else {
                rotateRight(node.parent)
            }
        }
        deleteCase3(node)
    }

    private fun deleteCase3(node: RedBlackTreeNode<K, V>) {
        if (node.parent.color == NodeColor.BLACK &&
                node.sibling()?.color == NodeColor.BLACK &&
                node.sibling()?.left?.color == NodeColor.BLACK &&
                node.sibling()?.right?.color == NodeColor.BLACK){
            val nodeSibling = node.sibling()
            nodeSibling?.color = NodeColor.RED
            deleteCase1(node.parent)
        } else {
            deleteCase4(node)
        }
    }

    private fun deleteCase4(node: RedBlackTreeNode<K, V>) {
         if (node.parent.color == NodeColor.RED &&
                node.sibling()?.color == NodeColor.BLACK &&
                node.sibling()?.left?.color == NodeColor.BLACK &&
                node.sibling()?.right?.color == NodeColor.BLACK){
             val nodeSibling = node.sibling()
             nodeSibling?.color = NodeColor.RED
             node.parent.color = NodeColor.BLACK
         } else {
             deleteCase5(node)
         }
    }

    private fun deleteCase5(node: RedBlackTreeNode<K, V>) {
        if (node == node.parent.left &&
                node.sibling()?.color == NodeColor.BLACK &&
                node.sibling()?.left?.color == NodeColor.RED &&
                node.sibling()?.right?.color == NodeColor.BLACK) {
            val nodeSibling = node.sibling()
            nodeSibling?.color = NodeColor.RED
            nodeSibling?.left?.color = NodeColor.BLACK
            rotateRight(nodeSibling!!)
        } else if (node == node.parent.right &&
                        node.sibling()?.color == NodeColor.BLACK &&
                        node.sibling()?.right?.color == NodeColor.RED &&
                        node.sibling()?.left?.color == NodeColor.BLACK) {
            val nodeSibling = node.sibling()
            nodeSibling?.color = NodeColor.RED
            nodeSibling?.right?.color = NodeColor.BLACK
            rotateLeft(nodeSibling!!)
        }
        deleteCase6(node)
    }

    private fun deleteCase6(node: RedBlackTreeNode<K, V>) {
        val nodeSibling = node.sibling()
        nodeSibling?.color = node.parent.color
        node.parent.color = NodeColor.BLACK
        if (node == node.parent.left) {
            assert(nodeSibling?.right?.color == NodeColor.RED)
            nodeSibling?.right?.color = NodeColor.BLACK
            rotateLeft(node.parent)
        } else {
            assert(nodeSibling?.left?.color == NodeColor.RED)
            nodeSibling?.left?.color = NodeColor.BLACK
            rotateRight(node.parent)
        }
    }


    private fun maximumNode(node: RedBlackTreeNode<K, V>): RedBlackTreeNode<K, V> {
        var traverseMaximum: RedBlackTreeNode<K, V>? = node
        while (traverseMaximum?.right != null) {
            traverseMaximum = traverseMaximum.right
        }
        return traverseMaximum!!
    }

    private fun minimumNode(node: RedBlackTreeNode<K, V>): RedBlackTreeNode<K, V> {
        var traverseMinimum: RedBlackTreeNode<K, V>? = node
        while (traverseMinimum?.left != null) {
            traverseMinimum = traverseMinimum.left
        }
        return traverseMinimum!!
    }
}


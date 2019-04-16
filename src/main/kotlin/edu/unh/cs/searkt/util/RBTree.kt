/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unh.cs.searkt.util

import edu.unh.cs.searkt.util.RBTreeNode.Color
import java.util.*


class RBTree<K : RBTreeElement<K, V>, V>(private val sComp: Comparator<K>, private val vComp: Comparator<K>) {

    var root: RBTreeNode<K, V>? = null
    val values: List<V>
        get() {
            val list = ArrayList<V>()
            collectValues(root!!, list)
            return list
        }

    init {
        this.root = null
        verifyProperties()
    }

    private fun verifyProperties() {
        if (VERIFY_RBTREE) {
            verifyProperty1(root)
            verifyProperty2(root)
            // Property 3 is implicit
            verifyProperty4(root)
            verifyProperty5(root)
        }
    }

    /*private Node<K,V> lookupNode(K key) {
        Node<K,V> n = root;
        while (n != null) {
            int compResult = key.compareTo(n.key);
            if (compResult == 0) {
                return n;
            } else if (compResult < 0) {
                n = n.left;
            } else {
                assert compResult > 0;
                n = n.right;
            }
        }
        return n;
    }*/
    fun poll(): V? {
        if (root == null) return null
        val min = minimumNode(root!!)
        delete(min)
        min.key.node = null
        return min.value
    }

    fun peek(): V? {
        if (root == null) return null
        val min = minimumNode(root!!)
        return min.value
    }

    fun lookup(key: K): RBTreeNode<K, V>? {
        return key.node
    }

    /*public V lookup(K key) {
        Node<K,V> n = lookupNode(key);
        return n == null ? null : n.value;
    }*/
    private fun rotateLeft(n: RBTreeNode<K, V>) {
        val r = n.right
        replaceNode(n, r)
        n.right = r!!.left
        if (r.left != null) {
            r.left!!.parent = n
        }
        r.left = n
        n.parent = r
    }

    private fun rotateRight(n: RBTreeNode<K, V>) {
        val l = n.left
        replaceNode(n, l)
        n.left = l!!.right
        if (l.right != null) {
            l.right!!.parent = n
        }
        l.right = n
        n.parent = l
    }

    private fun replaceNode(oldn: RBTreeNode<K, V>, newn: RBTreeNode<K, V>?) {
        if (oldn.parent == null) {
            root = newn
        } else {
            if (oldn == oldn.parent!!.left)
                oldn.parent!!.left = newn
            else
                oldn.parent!!.right = newn
        }
        if (newn != null) {
            newn.parent = oldn.parent
        }
    }

    fun visit(l: K?, u: K?, op: Int, visitor: RBTreeVisitor<K>) {
        visit(l, u, root, op, visitor)
    }

    private fun visit(l: K?, u: K?, n: RBTreeNode<K, V>?, op: Int, visitor: RBTreeVisitor<K>) {
        if (n == null) return
        if (l == null || vComp.compare(n.key, l) > 0) {
            visit(l, u, n.left, op, visitor)
            if (vComp.compare(n.key, u) <= 0) {
                visitor.visit(n.key, op)
                visit(l, u, n.right, op, visitor)
            }
        } else {
            visit(l, u, n.right, op, visitor)
        }
    }

    fun insert(key: K, value: V) {
        val insertedNode = RBTreeNode(key, value, Color.RED, null, null)
        key.node = insertedNode
        if (root == null) {
            root = insertedNode
        } else {
            var n = root
            while (true) {
                //int compResult = key.compareTo(n.key);
                val compResult = sComp.compare(key, n!!.key)
                /*if (compResult == 0) {
                    n.value = value;
                    return;
                } else*/
                if (compResult <= 0) {
                    if (n.left == null) {
                        n.left = insertedNode
                        break
                    } else {
                        n = n.left
                    }
                } else {
                    assert(compResult > 0)
                    if (n.right == null) {
                        n.right = insertedNode
                        break
                    } else {
                        n = n.right
                    }
                }
            }
            insertedNode.parent = n
        }
        insertCase1(insertedNode)
        verifyProperties()
    }

    private fun insertCase1(n: RBTreeNode<K, V>) {
        if (n.parent == null)
            n.color = Color.BLACK
        else
            insertCase2(n)
    }

    private fun insertCase2(n: RBTreeNode<K, V>) {
        if (nodeColor(n.parent) === Color.BLACK)
            return  // Tree is still valid
        else
            insertCase3(n)
    }

    private fun insertCase3(n: RBTreeNode<K, V>) {
        if (nodeColor(n.uncle()) === Color.RED) {
            n.parent!!.color = Color.BLACK
            n.uncle()!!.color = Color.BLACK
            n.grandparent()!!.color = Color.RED
            insertCase1(n.grandparent()!!)
        } else {
            insertCase4(n)
        }
    }

    private fun insertCase4(node: RBTreeNode<K, V>?) {
        var n = node
        if (n == n!!.parent!!.right && n.parent == n.grandparent()!!.left) {
            rotateLeft(n.parent!!)
            n = n.left
        } else if (n == n.parent!!.left && n.parent == n.grandparent()!!.right) {
            rotateRight(n.parent!!)
            n = n.right
        }
        insertCase5(n!!)
    }

    private fun insertCase5(n: RBTreeNode<K, V>) {
        n.parent!!.color = Color.BLACK
        n.grandparent()!!.color = Color.RED
        if (n == n.parent!!.left && n.parent == n.grandparent()!!.left) {
            rotateRight(n.grandparent()!!)
        } else {
            assert(n == n.parent!!.right && n.parent == n.grandparent()!!.right)
            rotateLeft(n.grandparent()!!)
        }
    }

    /*public void delete(K key) {
        Node<K,V> n = lookupNode(key);
        delete(n);
    }*/

    fun delete(key: K) {
        val n = lookup(key)
        delete(n)
        key.node = null
        verifyProperties()
    }

    private fun delete(node: RBTreeNode<K, V>?) {
        var n: RBTreeNode<K, V>? = node ?: return
        // Key not found, do nothing
        if (n!!.left != null && n.right != null) {
            // Copy key/value from predecessor and then delete it instead
            val pred = maximumNode(n.left!!)
            n.key = pred.key
            n.value = pred.value
            n.key.node = n
            n = pred
        }

        assert(n.left == null || n.right == null)
        val child = if (n.right == null) n.left else n.right
        if (nodeColor(n) === Color.BLACK) {
            n.color = nodeColor(child)
            deleteCase1(n)
        }
        replaceNode(n, child)

        if (nodeColor(root) === Color.RED) {
            root!!.color = Color.BLACK
        }

        verifyProperties()
    }

    private fun maximumNode(n: RBTreeNode<K, V>): RBTreeNode<K, V> {
        var node: RBTreeNode<K, V> = n
        while (node.right != null) {
            node = node.right!!
        }
        return node
    }

    private fun minimumNode(n: RBTreeNode<K, V>): RBTreeNode<K, V> {
        var node: RBTreeNode<K, V> = n
        while (node.left != null) {
            node = node.left!!
        }
        return node
    }

    private fun deleteCase1(n: RBTreeNode<K, V>) {
        if (n.parent == null)
            return
        else
            deleteCase2(n)
    }

    private fun deleteCase2(n: RBTreeNode<K, V>) {
        if (nodeColor(n.sibling()) === Color.RED) {
            n.parent!!.color = Color.RED
            n.sibling()!!.color = Color.BLACK
            if (n == n.parent!!.left)
                rotateLeft(n.parent!!)
            else
                rotateRight(n.parent!!)
        }
        deleteCase3(n)
    }

    private fun deleteCase3(n: RBTreeNode<K, V>) {
        if (nodeColor(n.parent) === Color.BLACK &&
                nodeColor(n.sibling()) === Color.BLACK &&
                nodeColor(n.sibling()!!.left) === Color.BLACK &&
                nodeColor(n.sibling()!!.right) === Color.BLACK) {
            n.sibling()!!.color = Color.RED
            deleteCase1(n.parent!!)
        } else
            deleteCase4(n)
    }

    private fun deleteCase4(n: RBTreeNode<K, V>) {
        if (nodeColor(n.parent) === Color.RED &&
                nodeColor(n.sibling()) === Color.BLACK &&
                nodeColor(n.sibling()!!.left) === Color.BLACK &&
                nodeColor(n.sibling()!!.right) === Color.BLACK) {
            n.sibling()!!.color = Color.RED
            n.parent!!.color = Color.BLACK
        } else
            deleteCase5(n)
    }

    private fun deleteCase5(n: RBTreeNode<K, V>) {
        if (n == n.parent!!.left &&
                nodeColor(n.sibling()) === Color.BLACK &&
                nodeColor(n.sibling()!!.left) === Color.RED &&
                nodeColor(n.sibling()!!.right) === Color.BLACK) {
            n.sibling()!!.color = Color.RED
            n.sibling()!!.left!!.color = Color.BLACK
            rotateRight(n.sibling()!!)
        } else if (n == n.parent!!.right &&
                nodeColor(n.sibling()) === Color.BLACK &&
                nodeColor(n.sibling()!!.right) === Color.RED &&
                nodeColor(n.sibling()!!.left) === Color.BLACK) {
            n.sibling()!!.color = Color.RED
            n.sibling()!!.right!!.color = Color.BLACK
            rotateLeft(n.sibling()!!)
        }
        deleteCase6(n)
    }

    private fun deleteCase6(n: RBTreeNode<K, V>) {
        n.sibling()!!.color = nodeColor(n.parent)
        n.parent!!.color = Color.BLACK
        if (n == n.parent!!.left) {
            assert(nodeColor(n.sibling()!!.right) === Color.RED)
            n.sibling()!!.right!!.color = Color.BLACK
            rotateLeft(n.parent!!)
        } else {
            assert(nodeColor(n.sibling()!!.left) === Color.RED)
            n.sibling()!!.left!!.color = Color.BLACK
            rotateRight(n.parent!!)
        }
    }

    fun print() {
        printHelper(root, 0)
    }

    private fun collectValues(n: RBTreeNode<K, V>, list: MutableList<V>) {
        if (n.left != null) collectValues(n.left!!, list)
        if (n.right != null) collectValues(n.right!!, list)
        list.add(n.value)
    }

    companion object {

        const val VERIFY_RBTREE = false
        private const val INDENT_STEP = 4
        private fun verifyProperty1(n: RBTreeNode<*, *>?) {
            assert(nodeColor(n) === Color.RED || nodeColor(n) === Color.BLACK)
            if (n == null) return
            assert(nodeNode(n) != null)
            verifyProperty1(n.left)
            verifyProperty1(n.right)
        }

        private fun verifyProperty2(root: RBTreeNode<*, *>?) {
            assert(nodeColor(root) === Color.BLACK)
        }

        private fun nodeColor(n: RBTreeNode<*, *>?): Color {
            return n?.color ?: Color.BLACK
        }

        private fun nodeNode(n: RBTreeNode<*, *>?): RBTreeNode<*, *>? {
            return n?.key?.node
        }

        private fun verifyProperty4(n: RBTreeNode<*, *>?) {
            if (nodeColor(n) === Color.RED) {
                assert(nodeColor(n!!.left) === Color.BLACK)
                assert(nodeColor(n!!.right) === Color.BLACK)
                assert(nodeColor(n!!.parent) === Color.BLACK)
            }
            if (n == null) return
            verifyProperty4(n.left)
            verifyProperty4(n.right)
        }

        private fun verifyProperty5(root: RBTreeNode<*, *>?) {
            verifyProperty5Helper(root, 0, -1)
        }

        private fun verifyProperty5Helper(n: RBTreeNode<*, *>?, numberOfBlackNodes: Int, numberBlackPaths: Int): Int {
            var blackCount = numberOfBlackNodes
            var pathBlackCount = numberBlackPaths
            if (nodeColor(n) === Color.BLACK) {
                blackCount++
            }
            if (n == null) {
                if (pathBlackCount == -1) {
                    pathBlackCount = blackCount
                } else {
                    assert(blackCount == pathBlackCount)
                }
                return pathBlackCount
            }
            pathBlackCount = verifyProperty5Helper(n.left, blackCount, pathBlackCount)
            pathBlackCount = verifyProperty5Helper(n.right, blackCount, pathBlackCount)
            return pathBlackCount
        }

        private fun printHelper(n: RBTreeNode<*, *>?, indent: Int) {
            if (n == null) {
                print("<empty tree>")
                return
            }
            if (n.right != null) {
                printHelper(n.right, indent + INDENT_STEP)
            }
            for (i in 0 until indent)
                print(" ")
            if (n.color === Color.BLACK)
                println(n.key)
            else
                println("<" + n.key + ">")
            if (n.left != null) {
                printHelper(n.left, indent + INDENT_STEP)
            }
        }
    }
}


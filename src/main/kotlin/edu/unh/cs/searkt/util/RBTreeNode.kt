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


class RBTreeNode<K : RBTreeElement<K, V>, V> internal constructor(var key: K, var value: V, var color: Color, var left: RBTreeNode<K, V>?, var right: RBTreeNode<K, V>?) {
    internal enum class Color {
        RED, BLACK
    }

    override fun toString(): String {
        return "(key=$key,value=$value)"
    }

    var parent: RBTreeNode<K, V>? = null

    init {
        if (left != null) left!!.parent = this
        if (right != null) right!!.parent = this
        this.parent = null
    }

    internal fun grandparent(): RBTreeNode<K, V>? {
        assert(parent != null) // Not the root node
        assert(parent!!.parent != null) // Not child of root
        return parent!!.parent
    }

    internal fun sibling(): RBTreeNode<K, V>? {
        assert(parent != null) // Root node has no sibling
        return if (this === parent!!.left)
            parent!!.right
        else
            parent!!.left
    }

    internal fun uncle(): RBTreeNode<K, V>? {
        assert(parent != null) // Root node has no uncle
        assert(parent!!.parent != null) // Children of root have no uncle
        return parent!!.sibling()
    }
}
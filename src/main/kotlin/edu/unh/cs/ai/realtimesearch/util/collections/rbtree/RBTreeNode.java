/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unh.cs.ai.realtimesearch.util.collections.rbtree;

public class RBTreeNode<K extends RBTreeElement<K, V>, V> {
    public K key;
    public V value;
    public RBTreeNode<K, V> left;
    public RBTreeNode<K, V> right;
    public RBTreeNode<K, V> parent;
    public Color color;

    RBTreeNode(K key, V value, Color nodeColor, RBTreeNode<K, V> left, RBTreeNode<K, V> right) {
        this.key = key;
        this.value = value;
        this.color = nodeColor;
        this.left = left;
        this.right = right;
        if (left != null) left.parent = this;
        if (right != null) right.parent = this;
        this.parent = null;
    }

    RBTreeNode<K, V> grandparent() {
        assert parent != null; // Not the root node
        assert parent.parent != null; // Not child of root
        return parent.parent;
    }

    RBTreeNode<K, V> sibling() {
        assert parent != null; // Root node has no sibling
        if (this == parent.left)
            return parent.right;
        else
            return parent.left;
    }

    RBTreeNode<K, V> uncle() {
        assert parent != null; // Root node has no uncle
        assert parent.parent != null; // Children of root have no uncle
        return parent.sibling();
    }
}
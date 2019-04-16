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
package edu.unh.cs.searkt.util.collections.rbtree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

enum Color {RED, BLACK}

public class RBTree<K extends RBTreeElement<K, V>, V> {

    public static final boolean VERIFY_RBTREE = false;
    private static final int INDENT_STEP = 4;

    public RBTreeNode<K, V> root;

    private Comparator<K> sComp;
    private Comparator<K> vComp;

    public RBTree(Comparator<K> sComp, Comparator<K> vComp) {
        this.root = null;
        this.sComp = sComp;
        this.vComp = vComp;
        //verifyProperties();
    }

    private static void verifyProperty1(RBTreeNode<?, ?> n) {
        assert nodeColor(n) == Color.RED || nodeColor(n) == Color.BLACK;
        if (n == null) return;
        assert nodeNode(n) != null;
        verifyProperty1(n.left);
        verifyProperty1(n.right);
    }

    private static void verifyProperty2(RBTreeNode<?, ?> root) {
        assert nodeColor(root) == Color.BLACK;
    }

    private static Color nodeColor(RBTreeNode<?, ?> n) {
        return n == null ? Color.BLACK : n.color;
    }

    private static RBTreeNode<?, ?> nodeNode(RBTreeNode<?, ?> n) {
        return n != null ? n.key.getNode() : null;
    }

    private static void verifyProperty4(RBTreeNode<?, ?> n) {
        if (nodeColor(n) == Color.RED) {
            assert nodeColor(n.left) == Color.BLACK;
            assert nodeColor(n.right) == Color.BLACK;
            assert nodeColor(n.parent) == Color.BLACK;
        }
        if (n == null) return;
        verifyProperty4(n.left);
        verifyProperty4(n.right);
    }

    private static void verifyProperty5(RBTreeNode<?, ?> root) {
        verifyProperty5Helper(root, 0, -1);
    }

    private static int verifyProperty5Helper(RBTreeNode<?, ?> n, int blackCount, int pathBlackCount) {
        if (nodeColor(n) == Color.BLACK) {
            blackCount++;
        }
        if (n == null) {
            if (pathBlackCount == -1) {
                pathBlackCount = blackCount;
            } else {
                assert blackCount == pathBlackCount;
            }
            return pathBlackCount;
        }
        pathBlackCount = verifyProperty5Helper(n.left, blackCount, pathBlackCount);
        pathBlackCount = verifyProperty5Helper(n.right, blackCount, pathBlackCount);
        return pathBlackCount;
    }

    private static void printHelper(RBTreeNode<?, ?> n, int indent) {
        if (n == null) {
            System.out.print("<empty tree>");
            return;
        }
        if (n.right != null) {
            printHelper(n.right, indent + INDENT_STEP);
        }
        for (int i = 0; i < indent; i++)
            System.out.print(" ");
        if (n.color == Color.BLACK)
            System.out.println(n.key);
        else
            System.out.println("<" + n.key + ">");
        if (n.left != null) {
            printHelper(n.left, indent + INDENT_STEP);
        }
    }

    public void verifyProperties() {
        if (VERIFY_RBTREE) {
            verifyProperty1(root);
            verifyProperty2(root);
            // Property 3 is implicit
            verifyProperty4(root);
            verifyProperty5(root);
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
    public V poll() {
        if (root == null) return null;
        RBTreeNode<K, V> min = minimumNode(root);
        delete(min);
        min.key.setNode(null);
        return min.value;
    }

    public V peek() {
        if (root == null) return null;
        RBTreeNode<K, V> min = minimumNode(root);
        return min.value;
    }

    public RBTreeNode<K, V> lookup(K key) {
        return key.getNode();
    }

    /*public V lookup(K key) {
        Node<K,V> n = lookupNode(key);
        return n == null ? null : n.value;
    }*/
    private void rotateLeft(RBTreeNode<K, V> n) {
        RBTreeNode<K, V> r = n.right;
        replaceNode(n, r);
        n.right = r.left;
        if (r.left != null) {
            r.left.parent = n;
        }
        r.left = n;
        n.parent = r;
    }

    private void rotateRight(RBTreeNode<K, V> n) {
        RBTreeNode<K, V> l = n.left;
        replaceNode(n, l);
        n.left = l.right;
        if (l.right != null) {
            l.right.parent = n;
        }
        l.right = n;
        n.parent = l;
    }

    private void replaceNode(RBTreeNode<K, V> oldn, RBTreeNode<K, V> newn) {
        if (oldn.parent == null) {
            root = newn;
        } else {
            if (oldn == oldn.parent.left)
                oldn.parent.left = newn;
            else
                oldn.parent.right = newn;
        }
        if (newn != null) {
            newn.parent = oldn.parent;
        }
    }

    public void visit(K l, K u, int op, RBTreeVisitor<K> visitor) {
        visit(l, u, root, op, visitor);
    }

    private void visit(K l, K u, RBTreeNode<K, V> n, int op, RBTreeVisitor<K> visitor) {
        if (n == null) return;
        if (l == null || vComp.compare(n.key, l) > 0) {
            visit(l, u, n.left, op, visitor);
            if (vComp.compare(n.key, u) <= 0) {
                visitor.visit(n.key, op);
                visit(l, u, n.right, op, visitor);
            }
        } else {
            visit(l, u, n.right, op, visitor);
        }
    }

    public void insert(K key, V value) {
        RBTreeNode<K, V> insertedNode = new RBTreeNode<K, V>(key, value, Color.RED, null, null);
        key.setNode(insertedNode);
        if (root == null) {
            root = insertedNode;
        } else {
            RBTreeNode<K, V> n = root;
            while (true) {
                //int compResult = key.compareTo(n.key);
                int compResult = sComp.compare(key, n.key);
                /*if (compResult == 0) {
                    n.value = value;
                    return;
                } else*/
                if (compResult <= 0) {
                    if (n.left == null) {
                        n.left = insertedNode;
                        break;
                    } else {
                        n = n.left;
                    }
                } else {
                    assert compResult > 0;
                    if (n.right == null) {
                        n.right = insertedNode;
                        break;
                    } else {
                        n = n.right;
                    }
                }
            }
            insertedNode.parent = n;
        }
        insertCase1(insertedNode);
        //verifyProperties();
    }

    private void insertCase1(RBTreeNode<K, V> n) {
        if (n.parent == null)
            n.color = Color.BLACK;
        else
            insertCase2(n);
    }

    private void insertCase2(RBTreeNode<K, V> n) {
        if (nodeColor(n.parent) == Color.BLACK)
            return; // Tree is still valid
        else
            insertCase3(n);
    }

    void insertCase3(RBTreeNode<K, V> n) {
        if (nodeColor(n.uncle()) == Color.RED) {
            n.parent.color = Color.BLACK;
            n.uncle().color = Color.BLACK;
            n.grandparent().color = Color.RED;
            insertCase1(n.grandparent());
        } else {
            insertCase4(n);
        }
    }

    void insertCase4(RBTreeNode<K, V> n) {
        if (n == n.parent.right && n.parent == n.grandparent().left) {
            rotateLeft(n.parent);
            n = n.left;
        } else if (n == n.parent.left && n.parent == n.grandparent().right) {
            rotateRight(n.parent);
            n = n.right;
        }
        insertCase5(n);
    }

    void insertCase5(RBTreeNode<K, V> n) {
        n.parent.color = Color.BLACK;
        n.grandparent().color = Color.RED;
        if (n == n.parent.left && n.parent == n.grandparent().left) {
            rotateRight(n.grandparent());
        } else {
            assert n == n.parent.right && n.parent == n.grandparent().right;
            rotateLeft(n.grandparent());
        }
    }

    /*public void delete(K key) {
        Node<K,V> n = lookupNode(key);
        delete(n);
    }*/
    public void delete(K key) {
        RBTreeNode<K, V> n = lookup(key);
        delete(n);
        key.setNode(null);
        //verifyProperties();
    }

    private void delete(RBTreeNode<K, V> n) {
        if (n == null)
            return;  // Key not found, do nothing
        if (n.left != null && n.right != null) {
            // Copy key/value from predecessor and then delete it instead
            RBTreeNode<K, V> pred = maximumNode(n.left);
            n.key = pred.key;
            n.value = pred.value;
            n.key.setNode(n);
            n = pred;
        }

        assert n.left == null || n.right == null;
        RBTreeNode<K, V> child = (n.right == null) ? n.left : n.right;
        if (nodeColor(n) == Color.BLACK) {
            n.color = nodeColor(child);
            deleteCase1(n);
        }
        replaceNode(n, child);

        if (nodeColor(root) == Color.RED) {
            root.color = Color.BLACK;
        }

        //verifyProperties();
    }

    private RBTreeNode<K, V> maximumNode(RBTreeNode<K, V> n) {
        assert n != null;
        while (n.right != null) {
            n = n.right;
        }
        return n;
    }

    private RBTreeNode<K, V> minimumNode(RBTreeNode<K, V> n) {
        assert n != null;
        while (n.left != null) {
            n = n.left;
        }
        return n;
    }

    private void deleteCase1(RBTreeNode<K, V> n) {
        if (n.parent == null)
            return;
        else
            deleteCase2(n);
    }

    private void deleteCase2(RBTreeNode<K, V> n) {
        if (nodeColor(n.sibling()) == Color.RED) {
            n.parent.color = Color.RED;
            n.sibling().color = Color.BLACK;
            if (n == n.parent.left)
                rotateLeft(n.parent);
            else
                rotateRight(n.parent);
        }
        deleteCase3(n);
    }

    private void deleteCase3(RBTreeNode<K, V> n) {
        if (nodeColor(n.parent) == Color.BLACK &&
                nodeColor(n.sibling()) == Color.BLACK &&
                nodeColor(n.sibling().left) == Color.BLACK &&
                nodeColor(n.sibling().right) == Color.BLACK) {
            n.sibling().color = Color.RED;
            deleteCase1(n.parent);
        } else
            deleteCase4(n);
    }

    private void deleteCase4(RBTreeNode<K, V> n) {
        if (nodeColor(n.parent) == Color.RED &&
                nodeColor(n.sibling()) == Color.BLACK &&
                nodeColor(n.sibling().left) == Color.BLACK &&
                nodeColor(n.sibling().right) == Color.BLACK) {
            n.sibling().color = Color.RED;
            n.parent.color = Color.BLACK;
        } else
            deleteCase5(n);
    }

    private void deleteCase5(RBTreeNode<K, V> n) {
        if (n == n.parent.left &&
                nodeColor(n.sibling()) == Color.BLACK &&
                nodeColor(n.sibling().left) == Color.RED &&
                nodeColor(n.sibling().right) == Color.BLACK) {
            n.sibling().color = Color.RED;
            n.sibling().left.color = Color.BLACK;
            rotateRight(n.sibling());
        } else if (n == n.parent.right &&
                nodeColor(n.sibling()) == Color.BLACK &&
                nodeColor(n.sibling().right) == Color.RED &&
                nodeColor(n.sibling().left) == Color.BLACK) {
            n.sibling().color = Color.RED;
            n.sibling().right.color = Color.BLACK;
            rotateLeft(n.sibling());
        }
        deleteCase6(n);
    }

    private void deleteCase6(RBTreeNode<K, V> n) {
        n.sibling().color = nodeColor(n.parent);
        n.parent.color = Color.BLACK;
        if (n == n.parent.left) {
            assert nodeColor(n.sibling().right) == Color.RED;
            n.sibling().right.color = Color.BLACK;
            rotateLeft(n.parent);
        } else {
            assert nodeColor(n.sibling().left) == Color.RED;
            n.sibling().left.color = Color.BLACK;
            rotateRight(n.parent);
        }
    }

    public void print() {
        printHelper(root, 0);
    }

    public List<V> getValues() {
        List<V> list = new ArrayList<V>();
        collectValues(root, list);
        return list;
    }

    private void collectValues(RBTreeNode<K, V> n, List<V> list) {
        if (n.left != null) collectValues(n.left, list);
        if (n.right != null) collectValues(n.right, list);
        list.add(n.value);
    }    
    /*public static void main(String[] args) {
        RBTree<Integer,Integer> t = new RBTree<Integer,Integer>();
        t.print();

        java.util.Random gen = new java.util.Random();

        for (int i = 0; i < 5000; i++) {
            int x = gen.nextInt(10000);
            int y = gen.nextInt(10000);

            t.print();
            System.out.println("Inserting " + x + " -> " + y);
            System.out.println();

            t.insert(x, y);
            assert t.lookup(x).equals(y);
        }
        for (int i = 0; i < 60000; i++) {
            int x = gen.nextInt(10000);

            t.print();
            System.out.println("Deleting key " + x);
            System.out.println();

            t.delete(x);
        }
    }*/
}


package edu.unh.cs.searkt.util.collections.gequeue;

import edu.unh.cs.searkt.util.SearchQueueElement;
import edu.unh.cs.searkt.util.collections.binheap.BinHeap;
import edu.unh.cs.searkt.util.collections.rbtree.RBTree;
import edu.unh.cs.searkt.util.collections.rbtree.RBTreeElement;
import edu.unh.cs.searkt.util.collections.rbtree.RBTreeVisitor;

import java.util.Comparator;

public class GEQueue<E extends SearchQueueElement & RBTreeElement<E, E>> {
//public class GEQueue<E extends RBTreeElement<E, E> & MinHeapable> {

    private static final int ADD = 0;
    private static final int REMOVE = 1;
    private RBTree<E, E> open;
    private BinHeap<E> focal;
    private int id;
    private Comparator<E> geComparator;
    private RBTreeVisitor<E> focalVisitor = new RBTreeVisitor<E>() {
        public void visit(E e, int op) {
            switch (op) {
                case ADD:
                    if (e.getIndex(id) == -1) {
                        focal.add(e);
                    }
                    break;
                case REMOVE:
                    assert e.getIndex(id) != -1;
                    focal.remove(e);
                    break;
            }
        }
    };

    public GEQueue(Comparator<E> openComparator, Comparator<E> geComparator,
                   Comparator<E> focalComparator, int id) {
        this.id = id;
        this.geComparator = geComparator;
        focal = new BinHeap<E>(focalComparator, id);
        open = new RBTree<E, E>(openComparator, geComparator);
    }

    public boolean isEmpty() {
        return open.peek() == null;
    }

    public void add(E e, E oldBest) {
        assert e.getNode() == null;
        open.insert(e, e);
        // assumes oldBest is still valid
        if (geComparator.compare(e, oldBest) <= 0) {
            focal.add(e);
        }
        assert e.getNode() != null;
    }

    public void updateFocal(E oldBest, E newBest, int fHatChange) {
        assert newBest != null;
        assert newBest.getNode() != null;

        // did best f^ change?
        if (oldBest == null || fHatChange != 0) {
            // did best f^ go down?
            if (oldBest != null && fHatChange < 0) {
                open.visit(newBest, oldBest, REMOVE, focalVisitor);
            }
            // then best f^ when up
            else if (oldBest == null || oldBest.getNode() == null) {
                open.visit(oldBest, newBest, ADD, focalVisitor);
            }
        }
        //verifyFocal();
    }
  
  /*public void verifyFocal() {
    E best = open.peek();
    // make sure all nodes in focal are close to best
    List<E> heap = ((BinHeap)focal).heap;
    for (E o : heap) {
      assert geComparator.compare(o, best) <= 0;
    }
    // make sure all nodes in open that are close to best are in focal
    List<E> rbNodes = open.getValues();
    for (E o : rbNodes) {
      assert geComparator.compare(o, best) > 0 || o.getIndex(id) != -1;
    }
  }*/

    public void remove(E e) {
        assert e.getNode() != null;
        open.delete(e);
        if (e.getIndex(id) != -1) {
            focal.remove(e);
        }
    }

    public E pollOpen() {
        E e = open.poll();
        if (e != null && e.getIndex(id) != -1) {
            focal.remove(e);
        }
        return e;
    }

    public E pollFocal() {
        E e = focal.poll();
        if (e != null) {
            assert e.getNode() != null;
            open.delete(e);
        }
        return e;
    }

    public E peekOpen() {
        return open.peek();
    }

    public E peekFocal() {
        return focal.peek();
    }

}

package edu.unh.cs.ai.realtimesearch.util

import java.util.Comparator

/**
 * A data structure for open and focal lists
 *
 * @param <E> Type of data to save in the list
</E> */
//public class GEQueue<E extends RBTreeElement<E, E> & MinHeapable> {
class GEQueue<E>(openComparator: Comparator<E>,
                 private val geComparator: Comparator<E>,
                 focalComparator: Comparator<E>,
                 private val id: Int) where E : SearchQueueElement<E>, E : RBTreeElement<E, E> {
    private val open: RBTree<E, E> = RBTree(openComparator, geComparator)
    private val focal: BinHeap<E> = BinHeap(focalComparator, id)

    private val focalVisitor = object : RBTreeVisitor<E> {
        override fun visit(e: E, op: Int) {
            when (op) {
                ADD -> {
                    // Add to focal if the element isn't already contained there
                    if (e.getIndex(this@GEQueue.id) == -1) {
                        this@GEQueue.focal.add(e)
                    }
                }
                REMOVE -> {
                    // Remove from focal if the element is contained there
                    assert(e.getIndex(this@GEQueue.id) != -1)
                    this@GEQueue.focal.remove(e)
                }
            }
        }
    }

    val isEmpty: Boolean
        get() = this.open.peek() == null

    /**
     * Add to queue
     *
     * @param e The element to add
     * @param oldBest The old element which was the 'best' one in the queue
     */
    fun add(e: E, oldBest: E) {
        assert(e.node == null)
        this.open.insert(e, e)
        // assumes oldBest is still valid
        if (this.geComparator.compare(e, oldBest) <= 0) {
            this.focal.add(e)
        }
        assert(e.node != null)
    }

    fun updateFocal(oldBest: E?, newBest: E?, fHatChange: Int) {
        assert(newBest != null)
        assert(newBest!!.node != null)

        // did best f^ change?
        if (oldBest == null || fHatChange != 0) {
            // did best f^ go down?
            if (oldBest != null && fHatChange < 0) {
                // try {
                this.open.visit(newBest, oldBest, GEQueue.REMOVE, this.focalVisitor)
                // }
                // catch (ArrayIndexOutOfBoundsException e){
                //    System.out.print(oldBest);
                //    System.out.print("Error");
                //   }
                // then best f^ when up
            } else if (oldBest?.node == null) {
                this.open.visit(oldBest, newBest, GEQueue.ADD, this.focalVisitor)
            }
        }
        // verifyFocal();
    }

    //public void verifyFocal() {
    //    // Take the best f^
    //    E best = this.open.peek();
    //    // make sure all nodes in focal are close to best
    //    List<E> heap = ((BinHeap)this.focal).heap;
    //    for (E o : heap) {
    //      assert this.geComparator.compare(o, best) <= 0;
    //    }
    //    // make sure all nodes in open that are close to best are in focal
    //    List<E> rbNodes = this.open.getValues();
    //    // Go over all the nodes in open and for every node check that:
    //    // The node is not suitable to be in focal or The node is in focal
    //    for (E o : rbNodes) {
    //        assert this.geComparator.compare(o, best) > 0 || o.getIndex(this.id) != -1;
    //    }
    //}

    /**
     * Removes a node from OPEN and also from FOCAL
     *
     * @param e The node to remove
     */
    fun remove(e: E) {
        assert(e.node != null)
        this.open.delete(e)
        if (e.getIndex(id) != -1) {
            this.focal.remove(e)
        }
    }

    /**
     * Polling a node from OPEN
     *
     * NOTE: Removes the node from FOCAL!
     *
     * @return The extracted node
     */
    fun pollOpen(): E? {
        val e = this.open.poll()
        if (e != null && e.getIndex(id) != -1) {
            this.focal.remove(e)
        }
        return e
    }

    /**
     * Polling a node from FOCAL
     *
     * NOTE: Removes the node from OPEN!
     *
     * @return The extracted node
     */
    fun pollFocal(): E? {
        val e = this.focal.poll()
        if (e != null) {
            assert(e.node != null)
            this.open.delete(e)
        }
        return e
    }

    /**
     * Peeks a node from OPEN (without removing it)
     *
     * @return The extracted node
     */
    fun peekOpen(): E? {
        return this.open.peek()
    }

    /**
     * Peeks a node from FOCAL (without removing it)
     *
     * @return The extracted node
     */
    fun peekFocal(): E? {
        return this.focal.peek()
    }

    companion object {

        private val ADD = 0
        private val REMOVE = 1
    }
}

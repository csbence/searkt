package edu.unh.cs.searkt.util

class SynchronizedQueue<E>(val open: RBTree<E, E>, val focal: AbstractAdvancedPriorityQueue<E>,
                           private val comparator: Comparator<E>, private val key: Int)
        where E : RBTreeElement<E, E>, E : Indexable, E : SearchQueueElement<E> {

    private val add = 0
    private val remove = 1

    inner class FocalVisitor : RBTreeVisitor<E> {
        override fun visit(e: E, op: Int) {
            when (op) {
                add -> {
                    if (e.getIndex(key) == -1) {
                        focal.add(e)
                    }
                }
                remove -> {
                    if (e.getIndex(key) != -1) {
                        focal.remove(e)
                    }
                }
            }
        }

    }

    private val visitor = FocalVisitor()

    fun update(old: E?, new: E?, change: Int) {
        if (old == null || change != 0) {
            when {
                old != null && change < 0 -> {
                    this.open.visit(new, old, remove, visitor)
                }
                old == null || old.node == null -> {
                    this.open.visit(old, new, add, visitor)
                }
            }
        }
    }

    fun isEmpty(): Boolean = focal.isEmpty()

    fun isNotEmpty(): Boolean = !isEmpty()

    // returns true when the element qualifies for left prefix
    fun add(e: E, oldBest: E): Boolean {
        open.insert(e, e)
        if (comparator.compare(e, oldBest) <= 0) {
            focal.add(e)
            return true
        }
        return false
    }

    fun remove(e: E) {
        open.delete(e)
        if (e.getIndex(key) != -1) {
            focal.remove(e)
        }
    }

    fun pollOpen(): E? {
        val e = open.poll()
        if (e != null && e.getIndex(key) != -1) {
            focal.remove(e)
        }
        return e
    }

    fun pollFocal(): E? {
        val e = focal.pop()
        if (e != null) {
            open.delete(e)
        }
        return e
    }

    fun peekOpen(): E? = open.peek()
    fun peekFocal(): E? = focal.peek()

}
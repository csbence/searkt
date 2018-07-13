package edu.unh.cs.ai.realtimesearch.util

/**
 * @author Kevin C. Gall
 * Priority queue implemented as a Binomial Heap. Guarantees O(log n) pop
 */
class BinomialHeapPriorityQueue<T>(private var comparator: Comparator<in T>) : PriorityQueue<T> {
    private data class BinomialHeapNode<T>(val data: T, val rank: Int = 0, val next: BinomialHeapNode<T>? = null,
                                           val childrenStart: BinomialHeapNode<T>? = null)
    private var root: BinomialHeapNode<T>? = null
    private var cachedTop: BinomialHeapNode<T>? = null
    private var cachedPrevious: BinomialHeapNode<T>? = null

    override var size = 0

    override fun add(item: T) {
        //eliminate "cache" of min element and previous node in list
        cachedTop = null
        cachedPrevious = null
        root = add(BinomialHeapNode(item), root)

        size++
    }

    private fun add(item: BinomialHeapNode<T>, root: BinomialHeapNode<T>?): BinomialHeapNode<T> {
        return when {
            root == null -> item
            root.rank > item.rank -> BinomialHeapNode(item.data, item.rank, root, item.childrenStart)
            else -> {
                add(mergeNodes(item, root), root.next)
            }
        }
    }

    private fun mergeNodes(lhs: BinomialHeapNode<T>, rhs: BinomialHeapNode<T>): BinomialHeapNode<T> {
        val (lesser, greater) =
        when (comparator.compare(lhs.data, rhs.data)) {
            -1 -> Pair(lhs, rhs)
            else -> Pair(rhs, lhs)
        }

        //put "greater" at beginning of child list, which will be ordered on higher rank -> lower rank
        return BinomialHeapNode(lesser.data, lesser.rank + 1, null,
                BinomialHeapNode(greater.data, greater.rank, lesser.childrenStart, greater.childrenStart))
    }

    override fun peek(): T? {
        return findTop()?.data
    }
    override fun pop(): T? { //todo
        size--

        val minNode = findTop() ?: return null
        //do some stuff to remove from heap

        cachedPrevious = null
        cachedTop = null
        return minNode.data
    }

    private fun findTop(): BinomialHeapNode<T>? {
        when {
            cachedTop != null -> return cachedTop
            root?.next == null -> return root
        }

        var previousToMin: BinomialHeapNode<T>? = null
        var minSoFar = root!!
        var next = root!!.next
        var previous = root!!
        while (next != null) {
            if (comparator.compare(minSoFar.data, next.data) > 0) {
                previousToMin = previous
                minSoFar = next
            }
            previous = next
            next = next.next
        }

        cachedTop = minSoFar
        cachedPrevious = previousToMin

        return minSoFar
    }

    private fun merge(lhsRoot: BinomialHeapNode<T>, rhsRoot: BinomialHeapNode<T>): BinomialHeapNode<T> = TODO()

    override fun clear() {
        root = null
        cachedTop = null
    }
}
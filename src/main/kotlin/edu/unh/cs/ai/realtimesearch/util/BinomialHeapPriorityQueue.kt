package edu.unh.cs.ai.realtimesearch.util

/**
 * @author Kevin C. Gall
 * Priority queue implemented as a Binomial Heap. Guarantees O(log n) pop and insert
 * Immutable data structure, meaning snapshots of the queue can be passed around easily, i.e. Constant time clone
 */
class BinomialHeapPriorityQueue<T>(private var comparator: Comparator<in T>) : PriorityQueue<T> {
    private constructor(fromQueue: BinomialHeapPriorityQueue<T>) : this(fromQueue.comparator) {
        this.root = fromQueue.root
    }

    private data class BinomialHeapNode<T>(val data: T, val rank: Int = 0, val next: BinomialHeapNode<T>? = null,
                                           val childrenStart: BinomialHeapNode<T>? = null)
    private var root: BinomialHeapNode<T>? = null
    private var cachedTop: BinomialHeapNode<T>? = null

    override var size = 0

    override fun add(item: T) {
        //eliminate "cache" of min element and previous node in list
        cachedTop = null
        root = addOneNode(BinomialHeapNode(item), root)

        size++
    }

    /**
     * Optimization for the common case where we add one node (rank 0) to the heap. Avoids possible costly
     * behavior of mergeLists which may traverse the full top-level list (log n) of the existing tree
     */
    private fun addOneNode(lhs: BinomialHeapNode<T>, rhs: BinomialHeapNode<T>?): BinomialHeapNode<T> {
        return when {
            rhs == null -> lhs
            lhs.rank < rhs.rank -> BinomialHeapNode(lhs.data, lhs.rank, rhs, lhs.childrenStart)
            else -> addOneNode(mergeNodes(lhs, rhs), rhs.next)
        }
    }

    /**
     * Merges 2 lists. Worst case time is O(log n) since lists are guaranteed to be no larger than log n.
     * Method could be utilized in a public merge method if necessary
     * Adding 2 lists is like binary addition since we can think of a list's rank as its position in a binary number.
     * Therefore, we approach it similarly with an overflow parameter
     * @param overflow
     * @param ignoreNode Allows user to specify a node that should not be merged into the new list.
     * Useful for the delete operation
     */
    private fun mergeLists(lhs: BinomialHeapNode<T>?, rhs: BinomialHeapNode<T>?, overflow: BinomialHeapNode<T>?, ignoreNode: BinomialHeapNode<T>?): BinomialHeapNode<T>? {
        var newLhs: BinomialHeapNode<T>? = reverseIfHighRankedFirst(lhs)
        var newRhs: BinomialHeapNode<T>? = reverseIfHighRankedFirst(rhs)

        if (lhs == null && rhs == null) return overflow //could be null

        val bestNode = when {
            rhs == null -> {
                newLhs = skipNode(lhs!!.next, ignoreNode)
                lhs!!
            }
            lhs == null || lhs.rank > rhs.rank -> {
                newRhs = skipNode(rhs.next, ignoreNode)
                rhs
            }
            lhs.rank < rhs.rank -> {
                newLhs = skipNode(lhs.next, ignoreNode)
                lhs
            }
            else -> null //they are equal
        }

        return when {
            overflow != null -> {
                when {
                    //add rhs and lhs checks to allow smart casting
                    bestNode == null && rhs != null && lhs != null -> {
                        val mergedLists = mergeLists(skipNode(lhs.next, ignoreNode), skipNode(rhs.next, ignoreNode), mergeNodes(lhs, overflow), ignoreNode)
                        BinomialHeapNode(rhs.data, rhs.rank, mergedLists, rhs.childrenStart)
                    }
                    overflow.rank < bestNode!!.rank -> {
                        BinomialHeapNode(overflow.data, overflow.rank, mergeLists(lhs, rhs, null, ignoreNode), overflow.childrenStart)
                    }
                    else -> {
                        mergeLists(newLhs, newRhs, mergeNodes(bestNode, overflow), ignoreNode)
                    }
                }
            }
            bestNode == null -> { //node ranks are equal
                mergeLists(skipNode(lhs!!.next, ignoreNode), skipNode(rhs!!.next, ignoreNode), mergeNodes(lhs, rhs), ignoreNode)
            }
            else -> BinomialHeapNode(bestNode.data, bestNode.rank, mergeLists(newLhs, newRhs, null, ignoreNode), bestNode.childrenStart)
        }
    }

    private fun reverseIfHighRankedFirst(nodeList: BinomialHeapNode<T>?): BinomialHeapNode<T>? {
        return when {
            nodeList == null -> null
            nodeList.rank > nodeList.next?.rank ?: Integer.MAX_VALUE -> {
                var nextNode = nodeList.next
                var currentNode = BinomialHeapNode(nodeList.data, nodeList.rank, null, nodeList.childrenStart)

                while (nextNode != null) {
                    currentNode = BinomialHeapNode(nextNode.data, nextNode.rank, currentNode, nextNode.childrenStart)
                    nextNode = nextNode.next
                }

                currentNode
            }
            else -> nodeList
        }
    }

    private fun skipNode(node: BinomialHeapNode<T>?, nodeToSkip: BinomialHeapNode<T>?): BinomialHeapNode<T>? {
        return when {
            node == null || nodeToSkip == null -> node
            node == nodeToSkip -> node.next
            else -> node
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

        root = mergeLists(minNode.childrenStart, skipNode(root, minNode), null, minNode)

        cachedTop = null
        return minNode.data
    }

    private fun findTop(): BinomialHeapNode<T>? {
        when {
            cachedTop != null -> return cachedTop
            root?.next == null -> return root
        }

        var minSoFar = root!!
        var next = root!!.next
        while (next != null) {
            if (comparator.compare(minSoFar.data, next.data) > 0) {
                minSoFar = next
            }
            next = next.next
        }

        cachedTop = minSoFar

        return minSoFar
    }

    override fun clear() {
        root = null
        cachedTop = null
    }

    /**
     * Constant time clone: Copies root node and comparator
     */
    fun clone() = BinomialHeapPriorityQueue(this)
}
package edu.unh.cs.ai.realtimesearch.util

interface BucketNode {
    fun getFValue(): Double
    fun getGValue(): Double
    fun getHValue(): Double
    fun updateIndex(i: Int)
    override fun toString(): String
}

class BucketOpenList<T : BucketNode>(private val bound: Double, private var fMin: Double = Double.MAX_VALUE) {

    private val openList = AdvancedPriorityQueue<Bucket<T>>(1000000000, PotentialComparator(bound, fMin))
    private val lookUpTable = HashMap<GHPair, Bucket<T>>(10000000, 1.toFloat())

    private class BucketOpenListException(message: String) : Exception(message)

    private class PotentialComparator<T>(var bound: Double, var fMin: Double) : Comparator<T> {
        override fun compare(leftBucket: T, rightBucket: T): Int {
            if (leftBucket != null && rightBucket != null) {
                if (leftBucket is Bucket<*> && rightBucket is Bucket<*>) {
                    val leftBucketPotential = ((bound * fMin) - leftBucket.g) / (leftBucket.h)
                    val rightBucketPotential = ((bound * fMin) - rightBucket.g) / (rightBucket.h)
                    return Math.signum(rightBucketPotential - leftBucketPotential).toInt()
                }
            }
            return -1
        }
    }

    private data class GHPair(val g: Double, val h: Double)

    data class Bucket<T : BucketNode>(val f: Double, val g: Double, val h: Double,
                                      val nodes: ArrayList<T>) : Indexable {

        override var index: Int = -1

        override fun toString(): String {
            val stringBuilder = StringBuilder()
            stringBuilder.appendln("---\n")
            stringBuilder.appendln("f: $f | g: $g | h: $h")
            stringBuilder.appendln("BucketNodeArray ${nodes.size}")
            nodes.forEach { stringBuilder.appendln(it.toString()) }
            stringBuilder.appendln("---")
            return stringBuilder.toString()
        }
    }

    fun getBucket(element: T): Bucket<T>? {
        val checkGHPair = GHPair(element.getGValue(), element.getHValue())
        return lookUpTable[checkGHPair]
    }

    val minFValue
        get() = fMin

    val numberOfBuckets
        get() = lookUpTable.size

    val size
        get() = openList.size

    fun isNotEmpty(): Boolean = size != 0

    fun add(element: T) = insert(element)

    fun chooseNode(): T? = pop()

    override fun toString(): String {
        val stringBuilder = StringBuilder()
                .appendln("fMin: $fMin")
                .appendln("OpenList size: ${openList.size}")

        openList.forEach { stringBuilder.append(it.toString()) }

        stringBuilder.appendln("---")
                .appendln("BucketLookUp size: ${lookUpTable.size}")

        lookUpTable.forEach { stringBuilder.appendln(it.value.toString()) }

        return stringBuilder.toString()
    }

    fun update(element: T) {

    }


    fun replace(element: T, replacement: T) {
        val elementGHPair = GHPair(element.getGValue(), element.getHValue())
        val bucketLookUp = lookUpTable[elementGHPair] ?: throw BucketOpenListException("Can't replace element. Element [$element] not found! ")

        bucketLookUp.nodes.remove(element)
        element.updateIndex(-1)

        if (bucketLookUp.nodes.isEmpty()) {
            openList.remove(bucketLookUp)
        }

        if (element.getFValue() == minFValue) {
            recomputeMinFValue() // recompute the new minimum f value on open
            openList.reorder(PotentialComparator(bound, fMin)) // resort open list with new minimum f
        }

        insert(replacement)
    }

    private fun recomputeMinFValue() {
        if (openList.isNotEmpty()) {
            fMin = openList.peek()!!.f
            openList.forEach { bucket ->
                if (bucket.f < fMin) {
                    fMin = bucket.f
                }
            }
        } else {
            fMin = Double.MAX_VALUE
        }
    }

    private fun insert(element: T) {
        if (element.getFValue() < this.fMin) {
            fMin = element.getFValue()
            openList.reorder(PotentialComparator(bound, fMin))
        }

        val elementGHPair = GHPair(element.getGValue(), element.getHValue())
        val targetBucket = lookUpTable[elementGHPair]

        if (targetBucket == null) {
            // make new bucket
            val bucketNodes = arrayListOf(element)
            val newBucket = Bucket(element.getFValue(), element.getGValue(), element.getHValue(), bucketNodes)

            element.updateIndex(bucketNodes.indexOf(element))
            openList.add(newBucket)
            lookUpTable[elementGHPair] = newBucket

        } else {
            // we have a bucket go get it
            val bucketNodes = targetBucket.nodes

            bucketNodes.add(element)
            element.updateIndex(bucketNodes.indexOf(element))
            if (bucketNodes.size == 1) openList.add(targetBucket)
        }
    }

    private fun pop(): T? {
        if (size == 0) {
            throw BucketOpenListException("Nothing left to pop!")
        }

        val topBucketOnOpen = openList.peek()?.nodes ?: throw BucketOpenListException("No array in bucket!")
        val firstElementInTopBucket = topBucketOnOpen.first()

        topBucketOnOpen.remove(firstElementInTopBucket)

        if (topBucketOnOpen.isEmpty()) {
            openList.pop() // pop the empty bucket
        }

        if (firstElementInTopBucket.getFValue() == minFValue) {
            recomputeMinFValue() // recompute the new minimum f value on open
            openList.reorder(PotentialComparator(bound, fMin)) // resort open list with new minimum f
        }

        firstElementInTopBucket.updateIndex(-1)
        return firstElementInTopBucket
    }

}
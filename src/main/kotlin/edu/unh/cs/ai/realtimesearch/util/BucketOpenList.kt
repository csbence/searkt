package edu.unh.cs.ai.realtimesearch.util

interface BucketNode {
    fun getFValue(): Double
    fun getGValue(): Double
    fun getHValue(): Double
    override fun toString(): String
}

class BucketOpenList<T : BucketNode>(private val bound: Double, private var fMin: Double = Double.MAX_VALUE) {

    private val setIndex: (bucket: Bucket<T>, index: Int) -> (Unit) = { bucket, index -> bucket.index = index }
    private val getIndex: (bucket: Bucket<T>) -> (Int) = { bucket -> bucket.index }
    private val openList = AdvancedPriorityQueue(1000000000, PotentialComparator(bound, fMin), setIndex, getIndex)
    private val lookUpTable = HashMap<GHPair, Bucket<T>>(10000000, 1.toFloat())

    private class BucketOpenListException(message: String) : Exception(message)

    private class PotentialComparator<T>(var bound: Double, var fMin: Double) : Comparator<T> {
        override fun compare(leftBucket: T, rightBucket: T): Int {
            if (leftBucket != null && rightBucket != null) {
                if (leftBucket is Bucket<*> && rightBucket is Bucket<*>) {
                    var leftBucketPotential = ((bound * fMin) - leftBucket.g) / (leftBucket.h)
                    var rightBucketPotential = ((bound * fMin) - rightBucket.g) / (rightBucket.h)
                    if (leftBucket.h == 0.0) leftBucketPotential = Double.MAX_VALUE
                    if (rightBucket.h == 0.0) rightBucketPotential = Double.MAX_VALUE
                    return when {
                        leftBucketPotential > rightBucketPotential -> -1
                        leftBucketPotential < rightBucketPotential -> 1
                        leftBucket.g < rightBucket.g -> -1
                        leftBucket.g > rightBucket.g -> 1
                        leftBucket.h < rightBucket.h -> -1
                        leftBucket.h > rightBucket.h -> 1
                        leftBucket.f < rightBucket.f -> -1
                        leftBucket.f > rightBucket.f -> 1
                        else -> 0
                    }
                }
            }
            throw BucketOpenListException("Comparing $leftBucket and $rightBucket, can not be done.")
        }
    }

    private data class GHPair(val g: Double, val h: Double)


    data class Bucket<T : BucketNode>(val f: Double, val g: Double, val h: Double,
                                      val nodes: ArrayList<T>) {

        var index: Int = -1

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

    fun replace(element: T, replacement: T) {
        val elementGHPair = GHPair(element.getGValue(), element.getHValue())
        val bucketLookUp = lookUpTable[elementGHPair] ?: throw BucketOpenListException("Can't replace element. Element [$element] not found! ")

        bucketLookUp.nodes.remove(element)

        if (bucketLookUp.nodes.isEmpty()) {
            openList.remove(bucketLookUp)
        }

        if (element.getFValue() == minFValue) {
            recomputeMinFValue() // recompute the new minimum f value on open
            openList.reorder(PotentialComparator(bound, fMin)) // resort open list with new minimum f
        }

        insert(replacement)
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

            openList.add(newBucket)
            lookUpTable[elementGHPair] = newBucket

        } else {
            // we have a bucket go get it
            val bucketNodes = targetBucket.nodes

            bucketNodes.add(element)
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

        return firstElementInTopBucket
    }

}
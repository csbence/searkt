package edu.unh.cs.ai.realtimesearch.util

interface BucketNode {
    fun getFValue(): Double
    fun getGValue(): Double
    fun getHValue(): Double
    fun setOpenLocation(value: Int)
    override fun toString(): String
}

class BucketOpenList<T : BucketNode>(private val bound: Double, private var fMin: Double = Double.MAX_VALUE) {

    private val openList = AdvancedPriorityQueue(100000000, PotentialComparator<Bucket<T>>())
    private val lookUpTable = HashMap<GHPair, Bucket<T>>(100000000, 1.toFloat())

    private class BucketOpenListException(message: String) : Exception(message)

    private inner class PotentialComparator<T> : Comparator<T> {
        override fun compare(a: T, b: T): Int {
            return if (a is BucketOpenList<*>.Bucket<*> &&
                    b is BucketOpenList<*>.Bucket<*>) {
                when {
                    a.potential > b.potential -> -1
                    a.potential < b.potential -> 1

                    a.f < b.f -> -1
                    a.f > b.f -> 1

                    a.h < b.h -> -1
                    a.h > b.h -> 1

                    a.g < b.g -> -1
                    a.g > b.g -> 1

                    else -> 0
                }
            } else {
                throw BucketOpenListException("$a or $b is not a Bucket, can not compare!")
            }
        }
    }

    private data class GHPair(val g: Double, val h: Double)


    inner class Bucket<T : BucketNode>(val f: Double, val g: Double, val h: Double,
                                       val nodes: ArrayList<T>) : Indexable {

        override var index: Int = -1

        val potential: Double
            get() = calculatePotential()

        override fun toString(): String {
            val stringBuilder = StringBuilder()
            stringBuilder.appendln("---\n")
            stringBuilder.appendln("f: $f | g: $g | h: $h")
            stringBuilder.appendln("BucketNodeArray ${nodes.size}")
            nodes.forEach { stringBuilder.appendln(it.toString()) }
            stringBuilder.appendln("---")
            return stringBuilder.toString()
        }

        private fun calculatePotential(): Double {
            return (bound * fMin - g) / h
        }

        override fun hashCode(): Int {
            return g.toInt() xor h.toInt()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true

            if (other?.javaClass != javaClass) return false

            if (other is BucketOpenList<*>.Bucket<*>) {

                if (this.f == other.f && this.g == other.g && this.h == other.h) {
                    this.nodes.forEachIndexed { index, t -> if (t != other.nodes[index]) return false }
                    return true
                }
            }

            return false
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
        val bucketLookUp = lookUpTable[elementGHPair]
                ?: throw BucketOpenListException("Can't replace element. Element [$element] not found! ")

        bucketLookUp.nodes.remove(element)

        if (bucketLookUp.nodes.isEmpty()) {
            openList.remove(bucketLookUp) // remove the empty bucket
            if (element.getFValue() == minFValue) {
                recomputeMinFValue() // recompute the new minimum f value on open
            }
        }



        insert(replacement)
    }

    private fun insert(element: T) {
//        element.setOpenLocation(inOpen)

        if (element.getFValue() < this.fMin) {
            fMin = element.getFValue()
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
            if (firstElementInTopBucket.getFValue() == minFValue) {
                recomputeMinFValue() // recompute the new minimum f value on open
            }
        }



//        firstElementInTopBucket.setOpenLocation(outOfOpen)
        return firstElementInTopBucket
    }

}
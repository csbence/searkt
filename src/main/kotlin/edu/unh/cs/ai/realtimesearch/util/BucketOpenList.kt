package edu.unh.cs.ai.realtimesearch.util

interface BucketNode {
    fun getFValue(): Double
    fun getGValue(): Double
    fun getHValue(): Double
    override fun toString(): String
}

class BucketOpenList<T : BucketNode>(private val bound: Double, private var fMin: Double) {

    private val openList = AdvancedPriorityQueue<Bucket<T>>(1000000000, PotentialComparator(bound, fMin))
    private val lookUpTable = HashMap<GHPair, Bucket<T>>(1000000000, 1.toFloat()).resize()

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
    private data class Bucket<T : BucketNode>(val f: Double, val g: Double, val h: Double,
                                              val nodes: ArrayList<T>) : Indexable {
        override var index: Int = -1

        override fun toString(): String {
            var stringRepresentation = ""
            stringRepresentation += "---\n"
            stringRepresentation += "f: $f | g: $g | h: $h\n"
            stringRepresentation += "BucketNodeArray ${nodes.size}\n"
            nodes.forEach { stringRepresentation += it.toString() + "\n" }
            stringRepresentation += "---\n"
            return stringRepresentation
        }
    }

    override fun toString(): String {
        var stringRepresentation = ""
        stringRepresentation += "fMin: $fMin\n"
        stringRepresentation += "OpenList size: ${openList.size}\n"
        openList.forEach { stringRepresentation += it.toString() + "\n" }
        stringRepresentation += "---\n"
        stringRepresentation += "BucketLookUp size: ${lookUpTable.size}"
        lookUpTable.forEach { stringRepresentation += it.value.toString() + "\n" }
        return stringRepresentation
    }

    private fun insert(element: T) {
        if (element.getFValue() < this.fMin) {
            fMin = element.getFValue()
            openList.reorder(PotentialComparator(bound, fMin))
        }
        val elementGHPair = GHPair(element.getGValue(), element.getHValue())
        if (lookUpTable[elementGHPair] == null) {
            // make new bucket
        } else if (lookUpTable[elementGHPair] != null) {
            // we have a bucket go get it
            val elementBucket = lookUpTable[elementGHPair]
            val bucketNodes = elementBucket!!.nodes
            bucketNodes.add(element)
            if (bucketNodes.size == 1) openList.add(elementBucket)
        }
    }
}
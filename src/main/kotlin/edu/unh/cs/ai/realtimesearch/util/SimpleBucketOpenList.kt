package edu.unh.cs.ai.realtimesearch.util

class SimpleBucketOpenList<E>(private val weight: Double) where E : BucketNode, E: Indexable {

    private class SimpleBucketOpenListException(message: String) : Exception(message)

    private class PotentialComparator<T>(var bound: Double, var fMin: Double) : Comparator<T> {
        override fun compare(leftBucket: T, rightBucket: T): Int {
            if (leftBucket != null && rightBucket != null) {
                if (leftBucket is BucketOpenList.Bucket<*> && rightBucket is BucketOpenList.Bucket<*>) {
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
            throw SimpleBucketOpenListException("Comparing $leftBucket and $rightBucket, can not be done.")
        }
    }

    private var fMin = Double.MAX_VALUE
    private val openList = AdvancedPriorityQueue<E>(1000000000, PotentialComparator(weight, fMin)) // ArrayList<E>()

    val minFValue
        get() = fMin

    val size
        get() = openList.size

    fun isNotEmpty(): Boolean = openList.isNotEmpty()

    fun add(element: E) = insert(element)

    fun replace(element: E, replacement: E) {
        openList.remove(element)
        insert(replacement)
    }

    fun chooseNode(): E? = pop()

    private fun recomputeMinFValue() {
        if (openList.isNotEmpty()) {
            fMin = openList.peek()?.getFValue() ?: throw SimpleBucketOpenListException("OpenList is empty!")
            openList.forEach { node ->
                if (node.getFValue() < fMin) {
                    fMin = node.getFValue()
                }
            }
        } else {
            fMin = Double.MAX_VALUE
        }
    }

    private fun computePotential(element: E, currentFMin: Double): Double {
        return if (element.getHValue() == 0.0) {
            Double.MAX_VALUE
        } else {
            ((weight * currentFMin) - element.getFValue()) / element.getHValue()
        }
    }

    private fun pop(): E? {
        recomputeMinFValue()
        val currentFMin = fMin
        openList.reorder(PotentialComparator(weight, currentFMin))
        var currentHighestPotentialNode = openList.peek() ?: throw SimpleBucketOpenListException("OpenList is Empty!")
        openList.forEach { node ->
            val potential = computePotential(node, currentFMin)
            val currentHighestPotential = computePotential(currentHighestPotentialNode, currentFMin)
            if (potential > currentHighestPotential) currentHighestPotentialNode = node
        }
        openList.remove(currentHighestPotentialNode)
        return currentHighestPotentialNode
    }

    private fun insert(element: E) = openList.add(element)
}

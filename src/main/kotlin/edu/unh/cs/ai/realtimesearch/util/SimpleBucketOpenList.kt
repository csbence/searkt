package edu.unh.cs.ai.realtimesearch.util

class SimpleBucketOpenList<E: BucketNode>(private val weight: Double) {

    private val openList = ArrayList<E>()
    private var fMin = Double.MAX_VALUE

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
            fMin = openList.first().getFValue()
            openList.forEach { node ->
                if (node.getFValue() < fMin) {
                    fMin = node.getFValue()
                }
            }
        } else {
            fMin = Double.MAX_VALUE
        }
    }

    private fun pop(): E? {
        recomputeMinFValue()
        val currentFMin = fMin
        var currentHighestPotentialNode = openList.first()
        openList.forEach { node ->
            val potential = ((weight * currentFMin) - node.getGValue()) / node.getHValue()
            val currentHighestPotential = ((weight * currentFMin) - currentHighestPotentialNode.getGValue()) / currentHighestPotentialNode.getHValue()
            if (potential > currentHighestPotential) currentHighestPotentialNode = node
        }
        return currentHighestPotentialNode
    }

    private fun insert(element: E) = openList.add(element)
}

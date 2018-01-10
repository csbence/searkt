package edu.unh.cs.ai.realtimesearch.util

class SimpleBucketOpenList<T>(private var bound: Double, private var fMin: Double = Double.MAX_VALUE) where T : BucketNode, T: Indexable {

//    private val heap = AdvancedPriorityQueue(1000000000, BucketOpenList.PotentialComparator(bound, fMin))
//
//    private fun recomputeMinFValue() {
//        if (heap.isNotEmpty()) {
//            heap.forEach { node ->
//                if (node is BucketNode && node.getFValue() < fMin) {
//                        fMin = node.getFValue()
//                }
//            }
//       } else {
//            fMin = Double.MAX_VALUE
//        }
//    }
//
//    fun add(element: T) = insert(element)
//
//    fun isNotEmpty() = heap.isNotEmpty()
//
//    fun chooseNode() = pop()
//
//    fun replace(element: T, replacement: T) {
//        heap.remove(element)
//
//        if (element.getFValue() == fMin) {
//            recomputeMinFValue()
//        }
//
//        insert(replacement)
//    }
//
//    fun insert(element: T) {
//        if (element.getFValue() < fMin) {
//            fMin = element.getFValue()
//            heap.reorder(BucketOpenList.PotentialComparator(bound, fMin))
//        }
//        heap.add(element)
//    }
//
//    fun pop(): T? {
//        val toRemove = heap.pop()
//        if (toRemove is BucketNode) {
//            if (toRemove.getFValue() == fMin) {
//                recomputeMinFValue()
//            }
//        }
//        @Suppress("UNCHECKED_CAST")
//        return toRemove as T?
//    }



}
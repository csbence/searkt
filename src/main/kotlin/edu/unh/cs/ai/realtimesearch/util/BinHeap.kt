/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unh.cs.ai.realtimesearch.util

import java.util.ArrayList
import java.util.Comparator

/**
 * An implementation of a binary heap where elements are aware of their
 * location (index) in the heap.
 *
 * @author Matthew Hatem
 */
class BinHeap<E : SearchQueueElement<E>>(private var queue: ArrayList<E>, private val cmp: Comparator<E>, val key: Int) {

    val heap: ArrayList<E>
        get() = queue

    companion object {
        inline operator fun <reified E: SearchQueueElement<E>> invoke(capacity: Int, cmp: Comparator<E>, key: Int) =
                BinHeap(ArrayList(capacity), cmp, key)
    }

    val isEmpty: Boolean
        get() = heap.isEmpty()

    fun isNotEmpty() = !isEmpty

    fun size(): Int {
        return heap.size
    }

    fun poll(): E? {
        if (heap.isEmpty())
            return null
        val e = heap[0]
        setIndex(e, -1)
        if (heap.size > 1) {
            val b = heap.removeAt(heap.size - 1)
            heap[0] = b
            setIndex(b, 0)
            pushDown(0)
        } else {
            heap.removeAt(0)
        }
        return e
    }

    fun peek(): E? {
        return if (heap.isEmpty()) null else heap[0]
    }

    fun add(e: E) {
        heap.add(e)
        setIndex(e, heap.size - 1)
        pullUp(heap.size - 1)
    }

    fun clear() {
        heap.clear()
    }

    fun update(e: E) {
        var i = e.getIndex(key)
        if (i < 0 || i > heap.size)
            throw IllegalArgumentException()
        i = pullUp(i)
        pushDown(i)
    }

    fun remove(e: E): E {
        val ix = e.getIndex(key)
        return removeAt(ix)
    }

    private fun removeAt(ix: Int): E {
        val toReturn = heap[ix]
        setIndex(toReturn, -1)
        if (heap.size - 1 != ix) {
            heap[ix] = heap[heap.size - 1]
            setIndex(heap[ix], ix)
        }
        heap.removeAt(heap.size - 1)
        if (ix < heap.size) {
            pullUp(ix)
            pushDown(ix)
        }
        return toReturn
    }

    private fun pullUp(i: Int): Int {
        if (i == 0)
            return i
        val p = parent(i)
        if (compare(i, p) < 0) {
            swap(i, p)
            return pullUp(p)
        }
        return i
    }

    private fun pushDown(i: Int) {
        val l = left(i)
        val r = right(i)
        var sml = i
        if (l < heap.size && compare(l, i) < 0)
            sml = l
        if (r < heap.size && compare(r, sml) < 0)
            sml = r
        if (sml != i) {
            swap(i, sml)
            pushDown(sml)
        }
    }

    private fun compare(i: Int, j: Int): Int {
        val a = heap[i]
        val b = heap[j]
        return cmp.compare(a, b)
    }

    private fun setIndex(e: E, i: Int) {
        e.setIndex(key, i)
    }

    private fun swap(i: Int, j: Int) {
        val iE = heap[i]
        val jE = heap[j]

        heap[i] = jE
        setIndex(jE, i)
        heap[j] = iE
        setIndex(iE, j)
    }

    private fun parent(i: Int): Int {
        return (i - 1) / 2
    }

    private fun left(i: Int): Int {
        return 2 * i + 1
    }

    private fun right(i: Int): Int {
        return 2 * i + 2
    }

    fun getElementAt(i: Int): E {
        return heap[i]
    }

    inline fun forEach(action: (E) -> Unit) {
        for (item in this.heap) {
            action(item)
        }
    }
}

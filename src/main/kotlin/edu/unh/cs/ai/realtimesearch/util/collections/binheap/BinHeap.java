/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unh.cs.ai.realtimesearch.util.collections.binheap;

import edu.unh.cs.ai.realtimesearch.util.SearchQueueElement;
import edu.unh.cs.ai.realtimesearch.util.search.SearchQueue;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * An implementation of a binary heap where elements are aware of their
 * location (index) in the heap.
 *
 * @author Matthew Hatem
 */
public class BinHeap<E extends SearchQueueElement> implements SearchQueue<E> {

    final ArrayList<E> heap;
    private final Comparator<E> cmp;
    private final int key;

    public BinHeap(Comparator<E> cmp, int key) {
        this.heap = new ArrayList<E>();
        this.cmp = cmp;
        this.key = key;
    }

    @Override
    public int getKey() {
        return key;
    }

    @Override
    public boolean isEmpty() {
        return heap.isEmpty();
    }

    @Override
    public boolean isNotEmpty() {
        return !isEmpty();
    }

    @Override
    public int size() {
        return heap.size();
    }

    @Override
    public E poll() {
        if (heap.isEmpty())
            return null;
        E e = heap.get(0);
        setIndex(e, -1);
        if (heap.size() > 1) {
            E b = heap.remove(heap.size() - 1);
            heap.set(0, b);
            setIndex(b, 0);
            pushDown(0);
        } else {
            heap.remove(0);
        }
        return e;
    }

    @Override
    public E peek() {
        if (heap.isEmpty())
            return null;
        return heap.get(0);
    }

    @Override
    public void add(E e) {
        heap.add(e);
        setIndex(e, heap.size() - 1);
        pullUp(heap.size() - 1);
    }

    @Override
    public void clear() {
        heap.clear();
    }

    @Override
    public void update(E e) {
        int i = e.getIndex(key);
        if (i < 0 || i > heap.size())
            throw new IllegalArgumentException();
        i = pullUp(i);
        pushDown(i);
    }

    @Override
    public E remove(E e) {
        int ix = e.getIndex(key);
        return removeAt(ix);
    }

    protected E removeAt(int ix) {
        E toReturn = heap.get(ix);
        setIndex(toReturn, -1);
        if (heap.size() - 1 != ix) {
            heap.set(ix, heap.get(heap.size() - 1));
            setIndex(heap.get(ix), ix);
        }
        heap.remove(heap.size() - 1);
        if (ix < heap.size()) {
            pullUp(ix);
            pushDown(ix);
        }
        return toReturn;
    }

    private int pullUp(int i) {
        if (i == 0)
            return i;
        int p = parent(i);
        if (compare(i, p) < 0) {
            swap(i, p);
            return pullUp(p);
        }
        return i;
    }

    private void pushDown(int i) {
        int l = left(i);
        int r = right(i);
        int sml = i;
        if (l < heap.size() && compare(l, i) < 0)
            sml = l;
        if (r < heap.size() && compare(r, sml) < 0)
            sml = r;
        if (sml != i) {
            swap(i, sml);
            pushDown(sml);
        }
    }

    private int compare(int i, int j) {
        E a = heap.get(i);
        E b = heap.get(j);
        return cmp.compare(a, b);
    }

    private void setIndex(E e, int i) {
        e.setIndex(key, i);
    }

    private void swap(int i, int j) {
        E iE = heap.get(i);
        E jE = heap.get(j);

        heap.set(i, jE);
        setIndex(jE, i);
        heap.set(j, iE);
        setIndex(iE, j);
    }

    private int parent(int i) {
        return (i - 1) / 2;
    }

    private int left(int i) {
        return 2 * i + 1;
    }

    private int right(int i) {
        return 2 * i + 2;
    }

}

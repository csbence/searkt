package edu.unh.cs.searkt.util.search;

import edu.unh.cs.searkt.util.SearchQueueElement;

public abstract class SearchQueueElementImpl implements SearchQueueElement {

    private int[] indexMap;

    protected SearchQueueElementImpl(int keySize) {
        indexMap = new int[keySize];
        for (int i = 0; i < keySize; i++)
            indexMap[i] = -1;
    }

    @Override
    public void setIndex(int key, int index) {
        indexMap[key] = index;
    }

    @Override
    public int getIndex(int key) {
        return indexMap[key];
    }

    public abstract double gethHat();

    public abstract double getdHat();
}

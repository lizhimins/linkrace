package com.alirace.model;

/**
 * -1 表示节点不存在
 */
public class Node {
    public int bucketIndex = -1;
    public long startOffset = -1;

    public Node() {
    }

    public int getBucketIndex() {
        return bucketIndex;
    }

    public void setBucketIndex(int bucketIndex) {
        this.bucketIndex = bucketIndex;
    }

    public long getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(long startOffset) {
        this.startOffset = startOffset;
    }
}

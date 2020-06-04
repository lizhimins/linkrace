package com.alirace.model;

/**
 * -1 表示节点不存在
 */
public class Node {
    public int bucketIndex = -1;
    public long endOffset = -1;

    public Node() {
    }

    public int getBucketIndex() {
        return bucketIndex;
    }

    public void setBucketIndex(int bucketIndex) {
        this.bucketIndex = bucketIndex;
    }

    public long getEndOffset() {
        return endOffset;
    }

    public void setEndOffset(long endOffset) {
        this.endOffset = endOffset;
    }
}

package com.alirace.model;

/**
 * -1 表示节点不存在
 */
public class Node {
    public Record record;
    public int endOffset = -1;

    public Node() {
    }

    public Record getRecord() {
        return record;
    }

    public void setRecord(Record record) {
        this.record = record;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public void setEndOffset(int endOffset) {
        this.endOffset = endOffset;
    }
}

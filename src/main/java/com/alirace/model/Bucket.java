package com.alirace.model;

import java.util.Arrays;

public class Bucket {

    private static final int SIZE = 30;
    private String traceId = "TraceId";
    private int index = 0;
    private int[] list = new int[SIZE];
    private boolean isError = false;

    public Bucket() {
    }

    public void putData(String traceId, String data, int offset) {
        if (this.traceId.equals(traceId)) {
            list[index] = offset;
            index++;
        } else {
            list[index = 0] = offset;
            isError = false;
        }
        if (!isError) {
            // this.isError = Tag.isError(TraceLog.getTag(data));
        }
    }

    @Override
    public String toString() {
        return "Bucket{" +
                "traceId='" + traceId + '\'' +
                ", index=" + index +
                ", list=" + Arrays.toString(list) +
                '}';
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int[] getList() {
        return list;
    }

    public void setList(int[] list) {
        this.list = list;
    }
}

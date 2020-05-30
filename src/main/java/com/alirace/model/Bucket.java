package com.alirace.model;

public class Bucket {

    private byte[] traceId = new byte[18];

    // 保存有没有错误
    private boolean isError = false;

    private int index = 0;

    private int[] start = new int[32];
    private int[] end = new int[32];

    public Bucket() {
    }

    public byte[] getTraceId() {
        return traceId;
    }

    public void setTraceId(byte[] traceId) {
        this.traceId = traceId;
    }

    public boolean isError() {
        return isError;
    }

    public void setError(boolean error) {
        isError = error;
    }

    // 检查对应的桶中的 traceId 和当前 traceId 是否一致
    private boolean isSameTraceId(byte[] traceId) {
        for (int i = 0; i < 16 && traceId[i] != (byte) (int) '\n'; i++) {
            if (traceId[i] != this.traceId[i]) {
                return false;
            }
        }
        return true;
    }

    public void addNewSpan(byte[] traceId, int startOff, int endOff, boolean isError) {
        if (!isSameTraceId(traceId)) {
            index = 0;
            this.isError = false;
        }
        this.isError |= isError;
        this.start[index] = startOff;
        this.end[index] = endOff;
    }

    public void checkAndUpload(int preBucketOffset) {
        if () {

        }
    }
}
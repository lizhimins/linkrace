package com.alirace.model;

public class Bucket {

    private byte[] traceId = new byte[16];

    private boolean isError = false;

    private int index = 0;

    private int[] offsetList = new int[32];

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
}

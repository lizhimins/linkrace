package com.alirace.model;

import com.alirace.client.HttpClient;

public class Bucket {

    private byte[] traceId = new byte[18];

    // 保存有没有错误
    private boolean isError = false;

    private int index = 0;

    private long[] start = new long[64];
    private long[] end = new long[64];

    public Bucket() {
    }

    public byte[] getTraceId() {
        return traceId;
    }

    public void init() {
        index = 0;
        isError = false;
    }

    // 硬拷贝 traceId, 逻辑删除偏移量
    public void setTraceId(byte[] traceId) {
        for (int i = 0; i < 17; i++) {
            this.traceId[i] = traceId[i];
        }
        init();
    }

    public boolean isError() {
        return isError;
    }

    public void setError(boolean error) {
        isError = error;
    }

    // 检查对应的桶中的 traceId 和当前 traceId 是否一致
    public boolean isSameTraceId(byte[] traceId) {
        for (int i = 0; i < 16 && traceId[i] != (byte) (int) '\n'; i++) {
            if (traceId[i] != this.traceId[i]) {
                return false;
            }
        }
        return true;
    }

    public void addNewSpan(long startOff, long endOff, boolean isError) {
        this.isError |= isError;
        this.start[index] = startOff;
        this.end[index] = endOff;
        index++;
    }

    public String getQueryString() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < index; i++) {
            sb.append(start[i]);
            sb.append("-");
            sb.append(end[i]);
            sb.append(",");
        }
        return sb.substring(0, sb.lastIndexOf(","));
    }

    public void checkAndUpload(long preBucketOffset) {
        if (isError && start[index] != preBucketOffset) {
            // System.out.println(getQueryString());
            HttpClient.query(getQueryString());
            init();
        }
    }
}
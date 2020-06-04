package com.alirace.model;

import com.alirace.client.ClientService;
import com.alirace.client.HttpClient;

import java.io.IOException;

/**
 * 前缀树容器类
 */
public class Bucket {

    private byte[] traceId = new byte[18];

    // 保存有没有错误
    private boolean isError = false;
    private boolean isDone = false;

    private int index = 0;

    private long[] start = new long[64];
    private long[] end = new long[64];

    public Bucket() {
        for (int i = 0; i < 64; i++) {
            start[i] = 0L;
            end[i] = 0L;
        }
    }

    public byte[] getTraceId() {
        return traceId;
    }

    public void init() {
        index = 0;
        isError = false;
        isDone = false;
    }

    // 硬拷贝 traceId, 逻辑删除偏移量
    public void setTraceId(byte[] traceId) {
        for (int i = 0; i < 17; i++) {
            this.traceId[i] = traceId[i];
        }
        init();
    }

    public boolean isDone() {
        return isDone;
    }

    public void setDone(boolean done) {
        isDone = done;
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
        sb.append("bytes=");
        for (int i = 0; i < index; i++) {
            sb.append(start[i]);
            sb.append("-");
            sb.append(end[i]);
            sb.append(",");
        }
        return sb.substring(0, sb.lastIndexOf(","));
    }

    public void upload() throws IOException {
        if (isDone) {
            System.out.println("isDone1");
            Message message = new Message(MessageType.RESPONSE.getValue(), "1".getBytes());
            ClientService.response(message);
        }
//        byte[] bytes = ClientService.query(getQueryString());
//        Message message = new Message(MessageType.RESPONSE.getValue(), bytes);
//        ClientService.upload(message);
//        init();
    }

    public void checkAndUpload(long endOffset) throws IOException {
        // System.out.println(endOffset + " " + end[index - 1]);
        if (end[index - 1] == endOffset) {
            // System.out.print("DONE ");
            isDone = true;
            if (isError) {
                byte[] bytes = ClientService.query(getQueryString());
                Message message = new Message(MessageType.UPLOAD.getValue(), bytes);
                ClientService.upload(message);
                init();
            }
        }
    }
}
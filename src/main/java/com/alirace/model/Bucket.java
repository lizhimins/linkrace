package com.alirace.model;

import com.alirace.client.ClientMonitor;
import com.alirace.client.ClientService;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * 前缀树容器类
 */
public class Bucket {

    private byte[] traceId = new byte[18];

    // 保存有没有错误
    private boolean isError = false;
    private boolean isDone = false;

    private int index = -1;

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

    public String getTraceIdString() {
        StringBuffer sb = new StringBuffer(16);
        for (int i = 0; i < traceId.length; i++) {
            if (traceId[i] == (byte) (int) '\n') {
                break;
            }
            sb.append((char) (int) traceId[i]);
        }
        return sb.toString();
    }

    public void init() {
        index = -1;
        isError = false;
        isDone = false;
    }

    // 硬拷贝 traceId, 逻辑删除偏移量
    public void setTraceId(byte[] traceId) {
        int i;
        for (i = 0; i < traceId.length; i++) {
            if (traceId[i] == (byte) (int) '\n') {
                break;
            }
            this.traceId[i] = traceId[i];
        }
        this.traceId[i] = (byte) (int) '\n';
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
        index++;
        this.isError |= isError;
        this.start[index] = startOff;
        this.end[index] = endOff;
    }

    public String getQueryString() {
        StringBuffer sb = new StringBuffer();
        sb.append("bytes=");
        for (int i = 0; i <= index; i++) {
            sb.append(start[i]);
            sb.append("-");
            sb.append(end[i]);
            sb.append(",");
        }
        return sb.substring(0, sb.lastIndexOf(","));
    }

    public void tryResponse(String traceId) throws IOException {
        // 尝试放进去
        if (this.isError) {

            Message message = new Message(MessageType.RESPONSE.getValue(), "1".getBytes());
            ClientService.upload(message);
        } else {
            if (isDone) {
                byte[] bytes = ClientService.query(getQueryString());
                Message message = new Message(MessageType.RESPONSE.getValue(), bytes);
                ClientService.upload(message);
                init();
            } else {
                this.isError = true;
            }
        }
        // ClientService.response(message);
    }

    public void checkAndUpload(long endOffset) throws IOException {
        if (index != -1 && end[index] == endOffset) {
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
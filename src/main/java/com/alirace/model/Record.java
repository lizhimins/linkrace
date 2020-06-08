package com.alirace.model;

import com.alirace.client.ClientService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledHeapByteBuf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

public class Record {

    // 保存 traceId 的 hashCode
    private int hashCode;

    private int threadId;

    // 保存有没有错误
    private boolean isError = false;

    // 调用是否终结
    private boolean isDone = false;

    // 有没有上传过
    private AtomicBoolean isUpload = new AtomicBoolean(false);

    private int index = -1;
    private int[] start = new int[72];
    private int[] end = new int[72];

    private ByteBuf byteBuf = Unpooled.buffer();
    private int lastOffset = -1;

    public void addNewSpan(int startOff, int endOff, boolean isError) {
        index++;
        this.isError |= isError;
//        this.start[index] = startOff;
//        this.end[index] = endOff;
    }

//    public String getQueryString() {
//        StringBuffer sb = new StringBuffer();
//        sb.append("bytes=");
//        for (int i = 0; i <= index; i++) {
//            sb.append(start[i]);
//            sb.append("-");
//            sb.append(end[i]);
//            sb.append(",");
//        }
//        return sb.substring(0, sb.lastIndexOf(","));
//    }

    public void tryResponse(String traceId) throws IOException {
//        if (isDone) {
//            byte[] bytes = ClientService.services.get(from).bytes;
//            // 本地内存查, 速度快
//            int length = 0;
//            for (int i = 0; i <= index; i++) {
//                length += end[i] - start[i] + 1;
//            }
//
//            ByteBuf buffer = Unpooled.buffer(length);
//            for (int i = 0; i <= index; i++) {
//                buffer.writeBytes(bytes, start[i], end[i] - start[i] + 1);
//            }
//            // System.out.println(new String(buffer.array(), StandardCharsets.UTF_8));
//            // 上传数据
//            ClientService.response(buffer.array());
//            init();
//            buffer.release();
//        } else {
//            this.isError = true;
//        }
    }

    public void checkAndUpload(long endOffset) throws IOException {
        // System.out.println(endOffset);
        if (end[index] == endOffset) {
            isDone = true;
            byte[] bytes = ClientService.services.get(threadId).bytes;
            if (isError && isUpload.compareAndSet(false, true)) {
                // 计算长度
                int length = 0;
                for (int i = 0; i <= index; i++) {
                    length += end[i] - start[i] + 1;
                }
                ByteBuf buffer = Unpooled.buffer(length);
                for (int i = 0; i <= index; i++) {
                    buffer.writeBytes(bytes, start[i], end[i] - start[i] + 1);
                }
                // System.out.println(new String(buffer.array(), StandardCharsets.UTF_8));
                ClientService.upload(buffer.array());
                buffer.release();
            } else {
                ByteBuf buffer = Unpooled.buffer(16);
                buffer.writeBytes(bytes, start[0], 16);
                // System.out.println(new String(buffer.array(), StandardCharsets.UTF_8));
                ClientService.pass(buffer.array());
                buffer.release();
            }
        }
    }

    public int getHashCode() {
        return hashCode;
    }

    public void setHashCode(int hashCode) {
        this.hashCode = hashCode;
    }

    public boolean isError() {
        return isError;
    }

    public void setError(boolean error) {
        isError = error;
    }

    public boolean isDone() {
        return isDone;
    }

    public void setDone(boolean done) {
        isDone = done;
    }

    public AtomicBoolean getIsUpload() {
        return isUpload;
    }

    public void setIsUpload(AtomicBoolean isUpload) {
        this.isUpload = isUpload;
    }

    public int getLastOffset() {
        return lastOffset;
    }

    public void setLastOffset(int lastOffset) {
        this.lastOffset = lastOffset;
    }

    public int getThreadId() {
        return threadId;
    }

    public void setThreadId(int threadId) {
        this.threadId = threadId;
    }
}

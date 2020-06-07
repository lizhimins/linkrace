package com.alirace.model;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

public class Record {

    // 保存 traceId 的 hashCode
    private int hashCode;

    // 保存有没有错误
    private boolean isError = false;

    // 调用是否终结
    private boolean isDone = false;

    // 有没有上传过
    private AtomicBoolean isUpload = new AtomicBoolean(false);

    private int index = -1;
    private int[] start = new int[64];
    private int[] end = new int[64];

    public void addNewSpan(int startOff, int endOff, boolean isError) {
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
//        if (index != -1 && end[index] == endOffset) {
//            // System.out.print("DONE ");
//            isDone = true;
//            if (isError || isNeedUp) {
//                // RPC 查询, 速度降低10倍
////                byte[] bytes = ClientService.query(getQueryString());
////                Message message = new Message(MessageType.UPLOAD.getValue(), bytes);
////                ClientService.upload(message);
//
////                System.out.println(getQueryString());
//
//                // 本地内存查, 速度快
//                int length = 0;
//                for (int i = 0; i <= index; i++) {
//                    length += end[i] - start[i] + 1;
//                }
//
//                ByteBuf buffer = Unpooled.buffer(length);
//                for (int i = 0; i <= index; i++) {
//                    buffer.writeBytes(bytes, start[i], end[i] - start[i] + 1);
//                }
//                // System.out.println(new String(buffer.array(), StandardCharsets.UTF_8));
//                if (isError) {
//                    // 上传数据
//                    // ClientService.upload(buffer.array());
//                } else {
//                    // ClientService.response(buffer.array());
//                }
//                buffer.release();
//            }
//        }
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
}

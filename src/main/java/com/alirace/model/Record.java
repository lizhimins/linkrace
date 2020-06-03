package com.alirace.model;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

public class Record {

    // traceId, 用来标记整个链路
    public String traceId;

    public int left = 0, right = 0;
    public boolean isExist = true;

    // 懒判断策略, 是否包含错误, 默认为不包含错误, 如果一个调用已经发生错误不需要再次检查
    public boolean isError = false;

    public LinkedList<String> list;

    public Record(String traceId) {
        this.traceId = traceId;
    }

    public void addTraceLog(String traceLog) {
        if (traceLog == null || traceLog.length() < 16) {
            return;
        }

        if (list == null) {
            list = new LinkedList<>();
        }
        list.add(traceLog);
    }

    // 合并日志
    public void merge(Record other) {
        if (other == null || other.list == null || other.list.size() == 0) {
            return;
        }
        list.addAll(other.list);
        Collections.sort(list, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return (int) (TraceLog.getTime(o1) - TraceLog.getTime(o2));
            }
        });
    }

    public byte[] cmpAndMerge() {
        Collections.sort(list, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return (int) (TraceLog.getTime(o1) - TraceLog.getTime(o2));
            }
        });
        ByteBuf byteBuf = Unpooled.buffer(4096);
        Iterator<String> iterator = list.iterator();
        while (iterator.hasNext()) {
            String value = iterator.next();
            byteBuf.writeBytes(value.getBytes());
            byteBuf.writeByte((byte) (int) '\n');
            System.out.println(value);
        }
        return byteBuf.array();
    }

    @Override
    public String toString() {
        return "Record{" +
                "traceId='" + traceId + '\'' +
                ", isError=" + isError +
                ", list=" + list +
                '}';
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public int getLeft() {
        return left;
    }

    public void setLeft(int left) {
        this.left = left;
    }

    public int getRight() {
        return right;
    }

    public void setRight(int right) {
        this.right = right;
    }

    public boolean isExist() {
        return isExist;
    }

    public void setExist(boolean exist) {
        isExist = exist;
    }

    public boolean isError() {
        return isError;
    }

    public void setError(boolean error) {
        isError = error;
    }

    public LinkedList<String> getList() {
        return list;
    }

    public void setList(LinkedList<String> list) {
        this.list = list;
    }
}

package com.alirace.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

public class Record {

    // 数据分隔符
    private static final Character LOG_SEPARATOR = '|';
    private static final int TIMESTAMP_LENGTH = 16;

    // 是否上传过了, 默认没有上传过
    private volatile static AtomicBoolean isUpload = new AtomicBoolean(false);

    // traceId, 用来标记整个链路
    private String traceId;

    // 懒判断策略, 是否包含错误, 默认为不包含错误, 如果一个调用已经发生错误不需要再次检查
    private volatile boolean isError = false;
    private LinkedList<String> list = new LinkedList<>();

    public Record(String traceId) {
        this.traceId = traceId;
    }

    public static AtomicBoolean getIsUpload() {
        return isUpload;
    }

    public static void setIsUpload(AtomicBoolean isUpload) {
        Record.isUpload = isUpload;
    }

    public void addTraceLog(String traceLog) {
        if (traceLog == null || traceLog.length() == 0) {
            return;
        }
        list.add(traceLog);
    }

    // 合并日志
    public void merge(Record other) {
        if (other.getList() == null || other.getList().size() == 0) {
            return;
        }
        list.addAll(other.list);
        Collections.sort(list, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                int index1 = o1.indexOf(LOG_SEPARATOR);
                int index2 = o2.indexOf(LOG_SEPARATOR);
                long time1 = Long.parseLong(o1.substring(index1 + 1, index1 + TIMESTAMP_LENGTH + 1));
                long time2 = Long.parseLong(o2.substring(index2 + 1, index2 + TIMESTAMP_LENGTH + 1));
                return (int) (time1 - time2);
            }
        });
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

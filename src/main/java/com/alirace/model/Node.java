package com.alirace.model;

import com.alirace.client.CacheService;
import com.alirace.client.ClientMonitor;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class Node {

    private final int MAX_LENGTH = 100;
    private Record preRecord;
    private int pointer;
    private String[] list;

    // traceId, isError
    private HashMap<String, Record> map;

    public Node() {
        pointer = 0;
        preRecord = new Record("traceId");
        list = new String[MAX_LENGTH];
        map = new HashMap<>(8192);
    }

    public void putData(String traceLog) {
        String traceId = TraceLog.getTraceId(traceLog);
        if (!preRecord.getTraceId().equals(traceId)) {
            Record record = new Record(traceId);
            record.setLeft(pointer);
            map.put(traceId, record);
            preRecord = record;
        }
        preRecord.setRight(pointer);
        if (!preRecord.isError) {
            if (Tag.isError(TraceLog.getTag(traceLog))) {
                preRecord.setError(true);
                ClientMonitor.errorCount.incrementAndGet();
            }
        }
        list[pointer] = traceLog;
        pointer = (pointer++) % MAX_LENGTH;
    }

    public String[] getList() {
        return list;
    }

    public void setList(String[] list) {
        this.list = list;
    }
}

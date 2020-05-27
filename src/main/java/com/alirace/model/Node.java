package com.alirace.model;

import com.alirace.client.CacheService;
import com.alirace.client.ClientMonitor;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;


public class Node {

    private final int MAX_LENGTH = 100;

    private int head;
    private String[] list;

    // traceId, isError
    private HashMap<String, Boolean> memoryDataMap;

    public Node() {
        head = 0;
        list = new String[MAX_LENGTH];
        memoryDataMap = new HashMap<>(8192);
    }

    public int putData(String traceLog) {
        String traceId = TraceLog.getTraceId(traceLog);
        Boolean result = memoryDataMap.get(traceId) != null;
        if (!result) {
            if (Tag.isError(TraceLog.getTag(traceLog))) {
                memoryDataMap.put(traceId, true);
                ClientMonitor.errorCount.incrementAndGet();
                return -1;
            }
        }
        list[head] = traceLog;
        head = (head++) % MAX_LENGTH;
        return 1;
    }

    public String[] getList() {
        return list;
    }

    public void setList(String[] list) {
        this.list = list;
    }

    public int getHead() {
        return head;
    }

    public void setHead(int head) {
        this.head = head;
    }
}

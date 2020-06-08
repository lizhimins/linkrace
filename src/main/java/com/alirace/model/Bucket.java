package com.alirace.model;

import com.alirace.client.ClientService;

import java.util.LinkedList;
import java.util.List;


/**
 * 节点容器类
 */
public class Bucket {
    private List<Record> list;

    public Bucket() {
        list = new LinkedList<>();
    }

    public Record getRecord(int threadId, int hashCode) throws Exception {
        for (Record record : list) {
            if (record.getHashCode() == hashCode) {
                return record;
            }
        }
        Record record = ClientService.recordPool.borrowObject();
        record.setHashCode(hashCode);
        record.setThreadId(threadId);
        list.add(record);
        return record;
    }

    public void releaseRecord(int hashCode) {
        for (Record record : list) {
            if (record.getHashCode() == hashCode) {
                if (record.isDone()) {
                    list.remove(record);
                    ClientService.recordPool.returnObject(record);
                    // System.out.println(String.format("Release hashCode: %6d", hashCode));
                }
            }
        }
    }
}
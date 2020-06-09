package com.alirace.model;

import com.alirace.client.ClientService;

import java.util.LinkedList;
import java.util.List;


/**
 * 节点容器类
 */
public class Bucket {

    // private List<Record> list;
    private int index;
    private long[] traceIds;
    private int[] line;

    public Bucket() {
        // list = new LinkedList<>();
        index = 0;
        traceIds = new long[32];
        line = new int[32];
    }

    public long getRecord(int threadId, long hashCode) throws Exception {
//        for (Record record : list) {
//            if (record.getHashCode() == hashCode) {
//                return record;
//            }
//        }
//        Record record = ClientService.recordPool.borrowObject();
//        record.setHashCode(hashCode);
//        record.setThreadId(threadId);
//        list.add(record);
        for (int i = 0; i < index; i++) {
            if (traceIds[i] == hashCode) {
                return line[i];
            }
        }
        index++;
        traceIds[index] = hashCode;
//        int lineNum = ClientService.lineIndex.incrementAndGet();
//        line[index] = lineNum;
        return 0;
    }

    public void releaseRecord(int hashCode) {
//        for (Record record : list) {
//            if (record.getHashCode() == hashCode) {
//                if (record.isDone()) {
//                    list.remove(record);
//                    ClientService.recordPool.returnObject(record);
//                    // System.out.println(String.format("Release hashCode: %6d", hashCode));
//                }
//            }
//        }
    }
}
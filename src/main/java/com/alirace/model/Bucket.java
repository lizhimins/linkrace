package com.alirace.model;

import com.alirace.client.ClientService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * 节点容器类
 */
public class Bucket {
    private List<Record> list;

    public Bucket() {
        list = new LinkedList<>();
    }

    public Record getRecord(int hashCode) throws Exception {
        for (Record record : list) {
            if (record.getHashCode() == hashCode) {
                return record;
            }
        }
        Record record = ClientService.recordPool.borrowObject();
        record.setHashCode(hashCode);
        list.add(record);
        return record;
        // return new Record();
    }
}
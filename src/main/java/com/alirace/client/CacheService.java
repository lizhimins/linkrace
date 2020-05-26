package com.alirace.client;

import com.alirace.model.Record;
import com.alirace.model.Tag;
import com.alirace.model.TraceLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * 客户端核心类, 缓存模块
 */
public class CacheService extends Thread {

    // 队列最大长度
    public static final int MAX_LENGTH = 60000;
    private static final Logger log = LoggerFactory.getLogger(CacheService.class);
    // 阻塞队列
    public LinkedBlockingQueue<String> pullQueue = new LinkedBlockingQueue<>();
    // 环状文件缓存
    public Record[] fileCache = new Record[MAX_LENGTH];

    // 头指针, 尾指针
    public int head = 0, tail = 0;

    public void putRecord(Record record) {
        fileCache[head] = record;
        head = (head++) % MAX_LENGTH;
    }

    @Override
    public void run() {
        Record preRecord = new Record("traceId");
        try {
            while (true) {
                // take:若队列为空, 发生阻塞, 等待有元素.
                String data = pullQueue.take();
                if ("EOF".equals(data)) {
                    break;
                }

                String traceId = TraceLog.getTraceId(data);
                if (preRecord.getTraceId().equals(traceId)) {
                    preRecord.addTraceLog(data);
                } else {
                    Record record = new Record(traceId);
                    record.addTraceLog(data);
                    putRecord(record);
                    preRecord = record;
                }

                if (!preRecord.isError()) {
                    boolean flag = Tag.isError(TraceLog.getTag(data));
                    if (flag) {
                        preRecord.setError(true);

                    }
                }
            }
            log.info("Client clean pull cache...");


            //pullCache.invalidateAll();
//            Iterator<Map.Entry<String, Record>> iterator = pullCache.asMap().entrySet().iterator();
//            while (iterator.hasNext()) {
//                Map.Entry<String, Record> entry = iterator.next();
//                String key = entry.getKey();
//                Record value = entry.getValue();
//                AtomicBoolean flag = ClientService.waitMap.get(traceId);
//                if (flag != null) {
//                    if (flag.compareAndSet(false, true)) {
//                        ClientService.passRecord(value);
//                        ClientService.response();
//                    }
//                } else {
//                    if (value.isError()) {
//                        // 当前 traceId 有问题, 需要主动上传
//                        if (ClientService.waitMap.putIfAbsent(traceId, new AtomicBoolean(true)) == null) {
//                            ClientService.uploadRecord(value);
//                        }
//                    }
//                }
//                queryCache.put(key, value);
//            }
//            ClientService.finish();
//            log.info("All data is checked...");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
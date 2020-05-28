package com.alirace.client;

import com.alirace.model.Bucket;
import com.alirace.model.TraceLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * 客户端核心类, 缓存模块
 */
public class CacheService implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    // 阻塞队列
    public static final int PULL_QUEUE_MAX_LENGTH = 30_0000;

    private int pointer = 0;
    private static final int FILE_CACHE_LENGTH = 60_0000;
    private String[] fileCache = new String[FILE_CACHE_LENGTH];

    // 缓存分块数量
    public static final int NODE_NUM = 128 * 4096; // 大约100万

    // 环状文件缓存
    public Bucket[] buckets = new Bucket[NODE_NUM];

    public CacheService() {
        super();
        for (int i = 0; i < NODE_NUM; i++) {
            buckets[i] = new Bucket();
        }
    }

    public void putData(String span) {
        String traceId = TraceLog.getTraceId(span);
        // 放入桶中创建索引
        int index = Math.abs(traceId.hashCode()) % NODE_NUM;
        buckets[index].putData(traceId, span, pointer);
        // 放入缓冲区
        fileCache[pointer] = span;
        pointer = ++pointer % FILE_CACHE_LENGTH;
    }

//    @Override
//    public String toString() {
//        StringBuffer sb = new StringBuffer();
//        sb.append(String.format("pull: %6d", pullQueue.size()));
//        return sb.toString();
//    }


    public static void start() {
        final int THREAD_NUM = 2;
        ClientService.caches = new CacheService[THREAD_NUM];
        for (int i = 0; i < THREAD_NUM; i++) {
            ClientService.caches[i] = new CacheService();
        }
    }

    @Override
    public void run() {
//        try {
//            while (true) {
//                // take:若队列为空, 发生阻塞, 等待有元素.
//                String data = pullQueue.take();
//                if ("EOF".equals(data)) {
//                    break;
//                }
//                ClientService.logOffset++;
//                String traceId = TraceLog.getTraceId(data);
//                // 放入桶中创建索引
//                int index = Math.abs(traceId.hashCode()) % NODE_NUM;
////                buckets[index].putData(traceId, data, pointer);
//                // 放入缓冲区
//                fileCache[pointer] = data;
//                pointer = ++pointer % FILE_CACHE_LENGTH;
//            }
//        } catch (InterruptedException ex) {
//            ex.printStackTrace();
//        }
//        log.info("Client LinkedBlockingQueue is Empty...");

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
    }
}
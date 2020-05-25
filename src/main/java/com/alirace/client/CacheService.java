package com.alirace.client;

import com.alirace.model.Record;
import com.alirace.model.TraceLog;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 客户端核心类, 缓存模块
 */
public class CacheService extends Thread {

    // 阻塞队列最大长度
    public static final int MAX_LENGTH = 500 * 10000;
    // 缓存相关设置
    public static final int MAX_PULL_CACHE_SIZE = 10000;
    public static final int MAX_QUERY_CACHE_SIZE = 30000;
    private static final Logger log = LoggerFactory.getLogger(CacheService.class);
    // 阻塞队列
    public LinkedBlockingQueue<String> pullQueue = new LinkedBlockingQueue<>(MAX_LENGTH);
    // 查询缓存
    public Cache<String, Record> queryCache = Caffeine.newBuilder()
            .initialCapacity(MAX_QUERY_CACHE_SIZE)
            .maximumSize(MAX_QUERY_CACHE_SIZE)
            .build();

    // LruCache, 保持对放入数据的引用, 因为入站和上传同时使用
    // Key: traceId
    // Val: timestamp & linkedList<TraceLog>
    public Cache<String, Record> pullCache = Caffeine.newBuilder()
            .initialCapacity(MAX_PULL_CACHE_SIZE)
            .maximumSize(MAX_PULL_CACHE_SIZE)
            .removalListener(((key, value, cause) -> {
                String traceId = (String) key;
                Record record = (Record) value;
                AtomicBoolean flag = ClientService.waitMap.get(traceId);
                if (flag != null) {
                    if (flag.compareAndSet(false, true)) {
                        ClientService.passRecord(record);
                        ClientService.response(1);
                    }
                    return;
                } else {
                    if (record.isError()) {
                        // 当前 traceId 有问题, 需要主动上传
                        if (ClientService.waitMap.putIfAbsent(traceId, new AtomicBoolean(true)) == null) {
                            ClientService.uploadRecord(record);
                        }
                        return;
                    }
                }
                queryCache.put(traceId, record);
            }))
            .build();
    ;

    private Record preRecord;

    @Override
    public void run() {
        try {
            String data = pullQueue.take();
            String traceId = TraceLog.getTraceId(data);
            preRecord = new Record(traceId);
            preRecord.addTraceLog(data);
            while (true) {
                // take:若队列为空, 发生阻塞, 等待有元素.
                data = pullQueue.take();
                if (PullService.EOF.equals(data)) {
                    break;
                }
                traceId = TraceLog.getTraceId(data);
                if (preRecord.getTraceId().equals(traceId)) {
                    preRecord.addTraceLog(data);
                } else {
                    Record record = pullCache.getIfPresent(traceId);
                    if (record != null) {
                        record.addTraceLog(data);
                    } else {
                        record = new Record(traceId);
                        record.addTraceLog(data);
                        pullCache.put(traceId, record);
                    }
                    preRecord = record;
                }
            }
            log.info("Client clean pull cache...");
            pullCache.invalidateAll();
            ClientService.finish();
            log.info("All data is checked...");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("(");
        sb.append(String.format("L:%6d,", pullQueue.size()));
        sb.append(String.format("P:%6d,", pullCache.asMap().size()));
        sb.append(String.format("Q:%6d", queryCache.asMap().size()));
        sb.append(")");
        return sb.toString();
    }
}
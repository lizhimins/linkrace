package com.alirace.client;

import com.alirace.model.Record;
import com.alirace.model.TraceLog;
import com.alirace.server.ServerService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * 客户端核心类, 缓存模块
 */
public class CacheService extends Thread {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    // 阻塞队列最大长度
    public static final int MAX_LENGTH = 500 * 10000;

    // 阻塞队列
    public LinkedBlockingQueue<String> pullQueue = new LinkedBlockingQueue<>(MAX_LENGTH);

    // 缓存相关设置
    public static final int MAX_PULL_CACHE_SIZE = 20 * 1000;
    public static final int MAX_QUERY_CACHE_SIZE = 60 * 1000;

    // LruCache, 保持对放入数据的引用, 因为入站和上传同时使用
    // Key: traceId
    // Val: timestamp & linkedList<TraceLog>
    public Cache<String, Record> pullCache = Caffeine.newBuilder()
            .initialCapacity(MAX_PULL_CACHE_SIZE)
            .maximumSize(MAX_PULL_CACHE_SIZE)
            .removalListener(((key, value, cause) -> {
                //System.out.println();
            }))
            .build();;

    // 查询缓存
    public Cache<String, Record> queryCache = Caffeine.newBuilder()
            .initialCapacity(MAX_QUERY_CACHE_SIZE)
            .maximumSize(MAX_QUERY_CACHE_SIZE)
            .build();

    private Record preRecord;

    @Override
    public void run() {
        try {
            String data = pullQueue.take();
            String traceId = TraceLog.getTraceId(data);
            preRecord = new Record(traceId);
            preRecord.addTraceLog(data);
            while (!PullService.isFinish || pullQueue.size() > 0) {
                // take:若队列为空, 发生阻塞, 等待有元素.
                data = pullQueue.take();
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
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("L:%6d,", pullQueue.size()));
        sb.append(String.format("P:%6d,", pullCache.asMap().size()));
        sb.append(String.format("Q:%6d,", queryCache.asMap().size()));
        return sb.toString();
    }
}
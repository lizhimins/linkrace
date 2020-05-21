package com.alirace.client;

import com.alirace.model.Record;
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

    // LruCache, 保持对放入数据的引用, 因为入站和上传同时使用
    // Key: traceId
    // Val: timestamp & linkedList<TraceLog>
    public Cache<String, Record> pullCache;
    // 查询缓存
    public Cache<String, Record> queryCache;

    // 缓存相关设置
    public static final int MAX_PULL_CACHE_SIZE = 20 * 1000;
    public static final int MAX_QUERY_CACHE_SIZE = 60 * 1000;

    public void init() {
        Cache<String, Record> pullCache = Caffeine.newBuilder()
                .initialCapacity(MAX_PULL_CACHE_SIZE)
                .maximumSize(MAX_PULL_CACHE_SIZE)
                .removalListener(((key, value, cause) -> {
                    //System.out.println();
                }))
                .build();
        Cache<String, Record> queryCache = Caffeine.newBuilder()
                .initialCapacity(MAX_QUERY_CACHE_SIZE)
                .maximumSize(MAX_QUERY_CACHE_SIZE)
                .build();
    }

    @Override
    public void run() {

    }
}
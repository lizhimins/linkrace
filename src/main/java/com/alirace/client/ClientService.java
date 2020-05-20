package com.alirace.client;

import com.alirace.model.Record;
import com.alirace.server.ServerService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.concurrent.*;

public class ClientService {

    private static final Logger log = LoggerFactory.getLogger(ServerService.class);

    // 缓存相关设置
    public static final int MAX_PULL_CACHE_SIZE = 20 * 1000;
    public static final int MAX_QUERY_CACHE_SIZE = 60 * 1000;

    // 阻塞队列最大长度
    private static final int MAX_LENGTH = 500 * 1000;

    // LruCache, 保持对放入数据的引用, 因为入站和上传同时使用
    // Key: traceId
    // Val: timestamp & linkedList<TraceLog>
    public static Cache<String, Record> pullCache;

    // 查询缓存
    public static Cache<String, Record> queryCache;
    // 阻塞队列
    private static BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();
    // 业务线程池
    private static ExecutorService pool = new ThreadPoolExecutor(4, 4,
            0L, TimeUnit.MILLISECONDS, workQueue);

    public static void start() {
        log.info("Client initializing start...");
        pullCache = Caffeine.newBuilder()
                .initialCapacity(MAX_PULL_CACHE_SIZE)
                .maximumSize(MAX_PULL_CACHE_SIZE)
                .removalListener((key, value, cause) -> {
                    String traceId = (String) key;
                    Record record = (Record) value;
                })
                .build();

        queryCache = Caffeine.newBuilder()
                .initialCapacity(MAX_QUERY_CACHE_SIZE)
                .maximumSize(MAX_QUERY_CACHE_SIZE)
                .removalListener(((key, value, cause) -> {

                }))
                .build();
        log.info("Client initializing finish...");
    }

    public static void pullData(String path) throws IOException, InterruptedException {
        log.info("Client pull data start...");
        URL url = new URL(path);
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        InputStream input = httpConnection.getInputStream();
        BufferedReader bf = new BufferedReader(new InputStreamReader(input));
        String line;
        while ((line = bf.readLine()) != null) {
            if (line.length() > 1) {
                String data = line;
                while (workQueue.size() > MAX_LENGTH) {
                    TimeUnit.SECONDS.sleep(1L);
                }
                pool.execute(new Runnable() {
                    @Override
                    public void run() {
                        putDataToCache(data);
                    }
                });
            }
        }
        bf.close();
        input.close();
        log.info("Client pull data finish...");
    }

    public static void putDataToCache(String data) {
        //pullCache.put();
    }
}

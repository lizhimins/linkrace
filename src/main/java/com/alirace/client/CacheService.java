package com.alirace.client;

import com.alirace.model.Node;
import com.alirace.model.TraceLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 客户端核心类, 缓存模块
 */
public class CacheService implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    // 阻塞队列
    public static final int PULL_QUEUE_MAX_LENGTH = 500000;
    public static LinkedBlockingQueue<String> pullQueue = new LinkedBlockingQueue<>(PULL_QUEUE_MAX_LENGTH);

    public static ExecutorService pool = Executors.newFixedThreadPool(2);

    // 缓存分块数量
    public static final int NODE_NUM = 4096;

    // 环状文件缓存
    public Node[] nodes = new Node[NODE_NUM];

    public CacheService() {
        super();
        for (int i = 0; i < NODE_NUM; i++) {
            nodes[i] = new Node();
        }
    }

    public static void start() {
        Thread thread = new Thread(new CacheService(), "CacheService");
        thread.start();
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("pull: %6d", pullQueue.size()));
        return sb.toString();
    }

    public void print() {
        for (int i = 0; i < NODE_NUM; i++) {
            log.info(String.format("%d: %s", i, nodes[i].getList()[85]));
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                // take:若队列为空, 发生阻塞, 等待有元素.
                String traceLog = pullQueue.take();
                if ("EOF".equals(traceLog)) {
                    break;
                }
                pool.submit(new Runnable() {
                    @Override
                    public void run() {
                        ClientService.logOffset++;
                        String traceId = TraceLog.getTraceId(traceLog);
                        int index = Math.abs(traceId.hashCode()) % NODE_NUM;
                        nodes[index].putData(traceLog);
                    }
                });
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        log.info("Client LinkedBlockingQueue is Empty...");

        print();

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
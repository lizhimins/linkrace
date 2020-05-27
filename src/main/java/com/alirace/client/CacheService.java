package com.alirace.client;

import com.alirace.model.Node;
import com.alirace.model.TraceLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 客户端核心类, 缓存模块
 */
public class CacheService extends Thread {

    // 阻塞队列
    public static final int PULL_QUEUE_MAX_LENGTH = 500000;
    // 缓存分块数量
    public static final int NODE_NUM = 4096;
    private static final Logger log = LoggerFactory.getLogger(CacheService.class);
    public LinkedBlockingQueue<String> pullQueue = new LinkedBlockingQueue<>(PULL_QUEUE_MAX_LENGTH);
    public LinkedBlockingQueue<String> queryQueue = new LinkedBlockingQueue<>(PULL_QUEUE_MAX_LENGTH);
    // 环状文件缓存
    public Node[] nodes = new Node[NODE_NUM];

    public CacheService() {
        super();
        for (int i = 0; i < NODE_NUM; i++) {
            nodes[i] = new Node();
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("pull: %6d", pullQueue.size()));
        return sb.toString();
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
                String traceId = TraceLog.getTraceId(traceLog);
                int index = Math.abs(traceId.hashCode()) % NODE_NUM;
                if (nodes[index].putData(traceLog) == -1) {
                    queryQueue.put(traceId);
                }
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        log.info("Client LinkedBlockingQueue is Empty...");


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
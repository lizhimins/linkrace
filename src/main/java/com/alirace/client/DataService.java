package com.alirace.client;

import com.alirace.model.Record;
import com.alirace.model.TraceLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.alirace.client.ClientService.pullCache;
import static com.alirace.client.ClientService.pullQueue;

/**
 * 消费者
 */
public class DataService implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(DataService.class);

    private Lock readLock = new ReentrantLock();

    // 业务线程池
    public static ExecutorService pool = new ThreadPoolExecutor(2, 2,
            0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

    public static void start() {
        Thread thread = new Thread(new DataService(), "DataService");
        thread.start();
        log.info("DataService start...");
    }

    public static void putData() {
        Thread thread = new Thread();

        thread.start();
    }

    public static void putDataToCache(String data) throws InterruptedException {
        String traceId = TraceLog.getTraceId(data);
        // Lookup an entry, or null if not found
        Record record = pullCache.getIfPresent(traceId);
        // Insert or update an entry
        if (record == null) {
            record = new Record(traceId);
            record.addTraceLog(data);
            pullCache.put(traceId, record);
        } else {
            record.addTraceLog(data);
        }
        TimeUnit.NANOSECONDS.sleep(100);
    }

    @Override
    public void run() {
        while (!PullService.isFinish) {
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        readLock.lock();
                        if (pullQueue.size() > 0) {
                            String traceLog = pullQueue.poll();
//                            System.out.println(traceLog);
                            String traceId = TraceLog.getTraceId(traceLog);
//                            Record record = new Record(traceId);
//                            record.addTraceLog(traceLog);
//                            while (true) {
//                                String next = pullQueue.peekFirst();
//                                if (next != null && traceId.equals(TraceLog.getTraceId(traceLog))) {
//                                    record.addTraceLog(next);
//                                    pullQueue.poll();
//                                } else {
//                                    break;
//                                }
//                            }
                        }
                    } finally {
                        readLock.unlock();
                    }
                }
            });
        }
    }
}

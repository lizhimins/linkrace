package com.alirace.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ClientMonitor implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientMonitor.class);

    public static AtomicLong uploadCount = new AtomicLong(0L);
    public static AtomicLong passCount = new AtomicLong(0L);
    // 接收到需要查询的 traceId 数量
    public static AtomicLong queryCount = new AtomicLong(0L);
    // 被动上报 traceId 数量, 也就是查询数量
    public static AtomicLong responseCount = new AtomicLong(0L);
    // 错误总数
    public static AtomicLong errorCount = new AtomicLong(0L);

    public static void start() {
        log.info("MonitorService start...");
        Thread thread = new Thread(new ClientMonitor(), "MonitorService");
        thread.start();
    }

    private void printStatus() {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("error: %8s, ", errorCount.get()));
        sb.append(String.format("pass: %8s, ", passCount.get()));
        sb.append(String.format("upload: %5s, ", uploadCount.get()));
        sb.append(String.format("query: %5s, ", queryCount.get()));
        sb.append(String.format("response: %5s, ", responseCount.get()));
        log.info(sb.toString());
    }

    @Override
    public void run() {
        while (true) {
            try {
                printStatus();
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

package com.alirace.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ClientMonitor implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientMonitor.class);

    // 错误总数
    public static AtomicLong errorCount = new AtomicLong(0L);
    public static AtomicLong uploadCount = new AtomicLong(0L);
    public static AtomicLong queryCount = new AtomicLong(0L);
    public static AtomicLong responseCount = new AtomicLong(0L);
    public static AtomicLong findCount = new AtomicLong(0L);

    public static void start() {
        log.info("MonitorService start...");
        Thread thread = new Thread(new ClientMonitor(), "MonitorService");
        thread.start();
    }

    private void printStatus() {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("ERROR: %6s, ", errorCount.get()));
        sb.append(String.format("UPLOAD: %6s, ", uploadCount.get()));
        sb.append(String.format("QUERY: %6s, ", queryCount.get()));
        sb.append(String.format("FIND: %6s, ", findCount.get()));
        sb.append(String.format("RESP: %6s, ", responseCount.get()));
        log.info(sb.toString());
    }

    @Override
    public void run() {
        int times = 0;
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

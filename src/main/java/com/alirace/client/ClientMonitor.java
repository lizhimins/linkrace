package com.alirace.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

public class ClientMonitor implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientMonitor.class);

    // 错误总数
    public static AtomicInteger errorCount = new AtomicInteger(0);
    public static AtomicInteger uploadCount = new AtomicInteger(0);
    public static AtomicInteger queryCount = new AtomicInteger(0);
    public static AtomicInteger responseCount = new AtomicInteger(0);
    public static AtomicInteger findCount = new AtomicInteger(0);

    public static void start() {
        log.info("ClientMonitor start...");
        Thread thread = new Thread(new ClientMonitor(), "MonitorService");
        thread.start();
    }

    public static void printStatus() {
        StringBuffer sb = new StringBuffer();
//        sb.append(String.format("ERROR: %6d, %6d",
//                ClientService.services[0].errorCount,
//                // ClientService.services[1].errorCount,
//                errorCount.get()));

        sb.append(String.format("ERROR: %6d, %6d, %6d ",
                ClientService.services[0].errorCount,
                ClientService.services[1].errorCount,
                errorCount.get()));

        sb.append(String.format("UPLOAD: %6d, ", uploadCount.get()));
        sb.append(String.format("QUERY: %6d, ", queryCount.get()));
        sb.append(String.format("FIND: %6d, ", findCount.get()));
        sb.append(String.format("RESP: %6d, ", responseCount.get()));
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

package com.alirace.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.alirace.client.ClientService.*;

public class ClientMonitor implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientMonitor.class);

    public static AtomicLong uploadCount = new AtomicLong(0L);
    // 接收到需要查询的 traceId 数量
    public static AtomicLong queryCount = new AtomicLong(0L);
    // 被动上报 traceId 数量, 也就是查询数量
    public static AtomicLong responseCount = new AtomicLong(0L);
    // 延迟响应数量
    public static AtomicLong passCount = new AtomicLong(0L);

    public static void start() {
        Thread thread = new Thread(new ClientMonitor(), "MonitorService");
        thread.start();
        log.info("MonitorService start...");
    }

    private void printStatus() {
        StringBuffer sb = new StringBuffer();

        sb.append(String.format("offset: %8s, ", logOffset));
        if (ClientService.services.size() == SERVICE_NUM) {
            for (int i = 0; i < SERVICE_NUM; i++) {
                sb.append(String.format("CA%d: %s,", i, ClientService.services.get(0).toString()));
            }
        }
        sb.append(String.format("upload: %5s, ", uploadCount.get()));
        sb.append(String.format("query: %5s, ", queryCount.get()));
        sb.append(String.format("response: %5s, ", responseCount.get()));
        sb.append(String.format("delay: %5s, ", passCount.get()));
        sb.append(String.format("waitMap: %5s, ", waitMap.size()));

//        for (Map.Entry<String, Boolean> entry : waitMap.entrySet()) {
//            sb.append(entry.getValue().equals(false) ? entry.getKey() : "");
//        }
        log.info(sb.toString());
    }

    @Override
    public void run() {
        while (true) {
            try {
                printStatus();
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

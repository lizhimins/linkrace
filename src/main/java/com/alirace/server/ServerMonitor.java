package com.alirace.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static com.alirace.server.ServerService.*;

public class ServerMonitor implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ServerMonitor.class);

    public static void printStatus() {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("MergeMap: %5d, ", mergeMap.size()));
        sb.append(String.format("Result: %5d, ", resultMap.size()));
        sb.append(String.format("Request: %5d, ", queryRequestCount.get()));
        sb.append(String.format("Response: %5d, ", queryResponseCount.get()));
//        sb.append(String.format("Total Yes: %5d ", CollectService.totalYes));
//        CollectService.mergeMap.keySet().forEach(
//                key -> sb.append(key + " ")
//        );
        log.info(sb.toString());
    }

    public static void start() {
        Thread t = new Thread(new ServerMonitor(), "ServerMonitorService");
        t.start();
    }

    @Override
    public void run() {
        while (true) {
            printStatus();
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

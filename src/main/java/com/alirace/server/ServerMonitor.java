package com.alirace.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static com.alirace.server.ServerService.*;

public class ServerMonitor implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ServerMonitor.class);

    public static void printStatus() {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("MERGE: %5d, ", mergeMap.size()));
        sb.append(String.format("RESULT: %5d, ", resultMap.size()));
        sb.append(String.format("REQ: %5d, ", queryRequestCount.get()));
        sb.append(String.format("RSP: %5d, ", queryResponseCount.get()));
//        sb.append(String.format("Total Yes: %5d ", CollectService.totalYes));
//        ServerService.mergeMap.keySet().forEach(
//                key -> sb.append(key + "_")
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
                TimeUnit.MILLISECONDS.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

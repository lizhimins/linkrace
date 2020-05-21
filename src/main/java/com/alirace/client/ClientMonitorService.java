package com.alirace.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.alirace.client.ClientService.logOffset;

public class ClientMonitorService implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientMonitorService.class);

    // 当前读入日志的时间戳, 用来进行过滤服务的同步, 这个时间在并发下是非精确的
    public static long logStartTimestamp = 0L;
    // 日志错误数据数
    public static long errorCount = 0L;
    // 主动上报 traceId 数量, 第一个LRU淘汰的数据量
    public static AtomicLong uploadCount = new AtomicLong(0L);
    // 接收到需要查询的 traceId 数量
    public static AtomicLong queryCount = new AtomicLong(0L);
    // 被动上报 traceId 数量, 也就是查询数量
    public static AtomicLong responseCount = new AtomicLong(0L);
    // 主动丢弃 traceId 数量, 第二个LRU淘汰的数据量
    public static long activeDropCount = 0L;
    // 被动丢弃 traceId 数量, 根据汇总数据丢弃数据量
    public static long passiveDropCount = 0L;

    public static void start() {
        Thread thread = new Thread(new ClientMonitorService(), "MonitorService");
        thread.start();
        log.info("MonitorService start...");
    }

    private void printStatus() {
        StringBuffer sb = new StringBuffer();

        sb.append(String.format("offset: %8s, ", logOffset));
        if (ClientService.services.size() == 2) {
            sb.append(String.format("CA1: %s,", ClientService.services.get(0).toString()));
            sb.append(String.format("CA2: %s,", ClientService.services.get(1).toString()));
        }
        sb.append("upload: " + String.format("%5s, ", uploadCount.get()));
        sb.append("query: " + String.format("%5s, ", queryCount.get()));
        sb.append("response: " + String.format("%5s, ", responseCount.get()));



//        sb.append("EC: " + String.format("%5s, ", errorCount));

//        sb.append("QC: " + String.format("%5s, ", queryCount.get()));
//        sb.append("AD: " + String.format("%5s, ", activeDropCount));
//        sb.append("PD: " + String.format("%5s, ", passiveDropCount));
//        UploadService.waitMap.forEach(
//                (key, value) -> {
//                    sb.append(!value ? key + " " : "");
//                }
//        );
        log.info(sb.toString());
    }

    @Override
    public void run() {
        while (true) {
            try {
                printStatus();
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

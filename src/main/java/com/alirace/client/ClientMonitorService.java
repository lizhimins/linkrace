package com.alirace.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ClientMonitorService implements Runnable {


    private static final Logger log = LoggerFactory.getLogger(ClientMonitorService.class);

    // 进过过滤服务的日志总量, 单线程调用
    public static long logOffset = 0L;
    // 当前读入日志的时间戳, 用来进行过滤服务的同步, 这个时间在并发下是非精确的
    public static long logStartTimestamp = 0L;
    // 日志错误数据数
    public static long errorCount = 0L;
    // 主动上报 traceId 数量, 第一个LRU淘汰的数据量
    public static AtomicLong activeUploadCount = new AtomicLong(0L);
    // 接收到需要查询的 traceId 数量
    public static AtomicLong queryCount = new AtomicLong(0L);
    // 被动上报 traceId 数量, 也就是查询数量
    public static AtomicLong passiveUploadCount = new AtomicLong(0L);
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
        sb.append("LO: " + String.format("%8s, ", logOffset));
//        sb.append("QU: " + String.format("%6s, ", ClientService.pullQueue.size()));
        // sb.append("time: " + String.format("%13s, ", DateUtil.getHumanReadTime(logStartTimestamp)));
        // sb.append("pullCache: " + String.format("%5s, ", DownloadService.pullCache.asMap().size()));
        // sb.append("queryCache: " + String.format("%5s, ", UploadService.queryCache.asMap().size()));
//        sb.append("EC: " + String.format("%5s, ", errorCount));
//        sb.append("AU: " + String.format("%5s, ", activeUploadCount.get()));
//        sb.append("QC: " + String.format("%5s, ", queryCount.get()));
//        sb.append("PU: " + String.format("%5s, ", passiveUploadCount.get()));
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

package com.alirace.util;

public class TimeUtil {
    // 启动时间
    private static Long startSystemTime;

    public static Long getMockTime() {
        return startSystemTime;
    }

    public static void init() {
        startSystemTime = System.currentTimeMillis() * 1000;
        new Thread() {

        };
    }

    public void run() {
        // TODO: 让时间走的比系统时间快，模拟数据

    }
}

package com.alirace.model;

public class TraceLog {
    // 数据分隔符
    private static final String LOG_SEPARATOR = "|";

    private static final int TIMESTAMP_LENGTH = 16;

    public static String getTraceId(String data) {
        int index = data.indexOf(LOG_SEPARATOR);
        return data.substring(0, index);
    }

    public static long getTime(String data) {
        int index = data.indexOf(LOG_SEPARATOR);
        String timeStr = data.substring(index + 1, index + TIMESTAMP_LENGTH + 1);
        return Long.parseLong(timeStr);
    }

    public static String getTag(String data) {
        int index = data.lastIndexOf(LOG_SEPARATOR);
        String tagStr = data.substring(index + 1);
        return tagStr;
    }
}

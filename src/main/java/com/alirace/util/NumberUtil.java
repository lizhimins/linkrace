package com.alirace.util;

public class NumberUtil {

    public static long combineInt2Long(int high, int low) {
        return (((long) high << 32) & 0xFFFFFFFF00000000L) | ((long) low & 0xFFFFFFFFL);
    }
}

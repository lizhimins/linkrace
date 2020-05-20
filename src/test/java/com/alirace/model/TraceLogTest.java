package com.alirace.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class TraceLogTest {

    @Test
    public void getTraceId() {
        String log1 = "1d37a8b17db8568b|1589285985482007|||||||";
        System.out.println(TraceLog.getTraceId(log1));
    }
}
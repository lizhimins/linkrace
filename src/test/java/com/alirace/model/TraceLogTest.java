package com.alirace.model;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class TraceLogTest {

    @Test
    public void getTraceId() {
        String log1 = "1d37a8b17db8568b|1589285985482007|||||||";
        assertEquals("1d37a8b17db8568b", TraceLog.getTraceId(log1));
    }

    @Test
    public void getTime() {
        String log1 = "1d37a8b17db8568b|1589285985482007|||||||";
        assertEquals(1589285985482007L, TraceLog.getTime(log1));
        String log2 = "1d37a8b17db8568b|1589285985482050|||||||";
        assertEquals(1589285985482050L, TraceLog.getTime(log2));
    }

    @Test
    public void getTag() {
        String tag = "http.status_code=200&http.url=123";
        String log = "||||||||" + tag;
        assertEquals(tag, TraceLog.getTag(log));
    }

}
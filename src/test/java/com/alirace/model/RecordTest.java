package com.alirace.model;

import org.junit.Test;

import java.awt.geom.Area;

import static org.junit.Assert.*;

public class RecordTest {

    @Test
    public void addTraceLog() {
        Record record = new Record("1");
        record.addTraceLog("traceLog1");
        record.addTraceLog("traceLog2");
        assertEquals(2, record.getList().size());
    }

    @Test
    public void merge() {
        String log1 = "1d37a8b17db8568b|1589285985482007|||||||";
        String log2 = "1d37a8b17db8568b|1589285985482050|||||||";
        String log3 = "1d37a8b17db8568b|1589285985482023|||||||";
        Record result = new Record("result");
        result.addTraceLog(log1);
        result.addTraceLog(log2);
        Record other = new Record("other");
        other.addTraceLog(log3);
        result.merge(other);
        // System.out.println(result);
    }
}
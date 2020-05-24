package com.alirace.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RecordTest {

    @Test
    public void addTraceLog() {
        Record record = new Record("1");
        String log1 = "1d37a8b17db8568b|1589285985482007|||||||http.status_code=200";
        String log2 = "1d37a8b17db8568b|1589285985482050|||||||http.status_code=201";
        record.addTraceLog(log1);
        record.addTraceLog(log2);
        assertEquals(2, record.getList().size());
        assertEquals(true, record.isError());
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
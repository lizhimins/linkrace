package com.alirace.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class BucketTest {

    @Test
    public void getQueryStringTest() {
        byte[] traceId = "5dc05bde4b5c68bfqwekqwkepqkwepq".getBytes();
        Bucket bucket = new Bucket();
        bucket.setTraceId(traceId);

        bucket.addNewSpan(1, 3, true);
        System.out.println(bucket.getQueryString());

        bucket.addNewSpan(5, 88, false);

        System.out.println(bucket.getQueryString());

    }

    @Test
    public void getTraceIdString() {
        byte[] bytes = "0123456789abcde\n".getBytes();
        Bucket bucket = new Bucket();
        bucket.setTraceId(bytes);
        System.out.println(bucket.getTraceIdString());
    }
}
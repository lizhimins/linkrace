package com.alirace.model;

public class Bucket {

    private String traceId;

    private boolean isError = false;

    private int index = 0;
    private static final int SIZE = 30;
    private int[] offsetList = new int[SIZE];

    public Bucket() {

    }


}

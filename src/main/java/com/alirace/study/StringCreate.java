package com.alirace.study;

import java.util.ArrayList;

public class StringCreate {

    private static ArrayList<String> list = new ArrayList<>();

    public static void main(String[] args) {
        long k = -1L;

        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000_0000; i++) {
            String s = i + "traceId";
            list.add(s);
        }
        long end = System.currentTimeMillis();
        System.out.println(end - start);
    }
}

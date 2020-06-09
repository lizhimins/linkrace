package com.alirace.study;

import java.util.HashMap;

public class StudyHashMap {
    public static void main(String[] args) {
        HashMap<Integer, Integer> map = new HashMap<>(120_0000);
        for (int i = 0; i < 80_0000; i++) {
            map.put(i, 0);
        }
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1500_0000; i++) {
            map.put(i % 80_0000, map.get(i % 80_0000) + 1);
        }
        long end = System.currentTimeMillis();
        System.out.println(end - start);
    }
}

package com.alirace.study;

import java.util.concurrent.atomic.LongAdder;

public class LongAddStudy {

    private static LongAdder longAdder = new LongAdder();
    public static void main(String[] args) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int j = 0; j < 10000; j++) {
                    longAdder.add(1);
                }
            }
        });
        thread.start();
        for (int i = 0; i < 2; i++) {

        }
        longAdder.increment();
        System.out.println(longAdder.sum());
    }
}

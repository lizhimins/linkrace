package com.alirace.study;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

public class CaffineCache implements Runnable {

    private static final String key = "key";

    private static Cache<String, Integer> cache = Caffeine.newBuilder()
            .initialCapacity(10)
            .maximumSize(10)
            .build();

    public static void main(String[] args) throws InterruptedException {
        cache.put(key, 0);
        Thread thread1 = new Thread(new CaffineCache());
        Thread thread2 = new Thread(new CaffineCache());
        thread1.start();
        thread2.start();
        TimeUnit.SECONDS.sleep(10);
        System.out.println(cache.asMap().get(key));
    }


    @Override
    public void run() {
        for (int i = 0; i < 1000 * 1000; i++) {
            int value = cache.getIfPresent(key);
            value++;
            cache.put(key, value);
        }
    }
}

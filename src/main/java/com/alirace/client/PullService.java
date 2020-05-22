package com.alirace.client;

import com.alirace.model.TraceLog;
import com.alirace.server.ServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.alirace.client.ClientService.*;

/**
 * 生产者
 */
public class PullService implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ServerService.class);

    // 数据获取地址
    public static String path;

    public static final String EOF = "EOF";
    public static void start() {
        Thread thread = new Thread(new PullService(), "PullService");
        thread.start();
    }

    public static void pullData() throws IOException, InterruptedException {
        log.info("Client pull data start...");
        log.info("Data path: " + path);
        URL url = new URL(path);
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        InputStream input = httpConnection.getInputStream();
        BufferedReader bf = new BufferedReader(new InputStreamReader(input));
        String line;
        while ((line = bf.readLine()) != null) {
            if (line.length() > 1) {
                // 计算在哪个队列
                int index = line.charAt(1) & 0x01;
                // 获得队列引用
                LinkedBlockingQueue<String> queue = services.get(index).pullQueue;
                // 队列满的话需要等待
                while (queue.size() >= CacheService.MAX_LENGTH) {
                    TimeUnit.MILLISECONDS.sleep(1000);
                }
                queue.put(line);
            }
        }
        for (int i = 0; i < 2; i++) {
            services.get(i).pullQueue.put(EOF);
        }
        bf.close();
        input.close();
        log.info("Client pull data finish...");
    }

    @Override
    public void run() {
        try {
            pullData();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

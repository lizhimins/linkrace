package com.alirace.client;

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

import static com.alirace.client.ClientService.MAX_LENGTH;
import static com.alirace.client.ClientService.pullQueue;

/**
 * 生产者
 */
public class PullService implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ServerService.class);

    // 数据获取地址
    public static String path;

    // 默认没有完成
    public static boolean isFinish = false;

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
                // 队列满的话需要等待
                while (pullQueue.size() >= MAX_LENGTH) {
                    TimeUnit.SECONDS.sleep(1L);
                }
                pullQueue.add(line);
            }
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
        isFinish = true;
    }
}

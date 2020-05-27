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

import static com.alirace.client.CacheService.pullQueue;
import static com.alirace.client.ClientService.*;

/**
 * 生产者
 */
public class PullService implements Runnable {

    public static final String EOF = "EOF";
    private static final Logger log = LoggerFactory.getLogger(ServerService.class);
    public static boolean isFinish = false;
    // 数据获取地址
    public static String path;
    public static long sleepTime = 0L;

    public static void start() {
        ClientService.pullService.start();
    }

    public static void pullData() throws IOException, InterruptedException {
        log.info("Client pull data start...");
        log.info("Data path: " + path);
        URL url = new URL(path);
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        InputStream input = httpConnection.getInputStream();
        BufferedReader bf = new BufferedReader(new InputStreamReader(input), 512);
        String line;
        while ((line = bf.readLine()) != null) {
            pullQueue.put(line);
        }
        isFinish = true;
        bf.close();
        input.close();
        log.info("Client pull data finish..." + logOffset);
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

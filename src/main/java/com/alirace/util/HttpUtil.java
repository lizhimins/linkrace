package com.alirace.util;

import com.alirace.controller.CommonController;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

/**
 * 发送 HTTP 请求的工具类
 */
public class HttpUtil {

    private final static OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(50L, TimeUnit.SECONDS)
            .readTimeout(60L, TimeUnit.SECONDS)
            .build();

    public static Response callHttp(Request request) throws IOException {
        Call call = OK_HTTP_CLIENT.newCall(request);
        return call.execute();
    }



    private static int sendCount = 0;
    private static URL url;
    private static URLConnection conn;
    private static PrintWriter out;
    private static BufferedReader in;

    public static void init() throws IOException {
        url = new URL("http://10.66.1.107:8002/api/finish");
        conn = url.openConnection();
        conn.setRequestProperty("accept", "*/*");
        conn.setRequestProperty("connection", "Keep-Alive");
        conn.setRequestProperty("Content-Type", "application/json");

        // 发送POST请求必须设置如下两行
        conn.setDoOutput(true);
        conn.setDoInput(true);

        // 获取URLConnection对象对应的输出流
        out = new PrintWriter(conn.getOutputStream());
        out.write("{");
    }

    public static void post(String key, String value) {
        if (sendCount != 0) {
            out.write(",");
        }
        sendCount++;
        out.write("\"" + key + "\":\"" + value + "\"");
    }

    public static void postEOF() throws IOException {
        out.write("}");
        out.flush();

        in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String result = "", line;
        while ((line = in.readLine()) != null) {
            result += line;
        }

        out.close();
        in.close();
    }

    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        init();
        for (int i = 0; i < 100; i++) {
            post("1", "2");
        }
        postEOF();
        long end = System.currentTimeMillis();
        System.out.println("It cost time: " + (end - start) + " ms.");
    }
}

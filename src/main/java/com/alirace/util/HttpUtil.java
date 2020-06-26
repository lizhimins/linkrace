package com.alirace.util;

import com.alirace.controller.CommonController;
import com.alirace.server.ServerService;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.alirace.server.ServerService.resultMap;

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
//
//    private static AtomicBoolean start = new AtomicBoolean(false);
//    public static int sendCount = 0;
//    private static URL url;
//    private static HttpURLConnection conn;
//    private static PrintWriter out;
//    private static BufferedReader in;
//
//    public static void init() throws IOException {
//        if (start.compareAndSet(false, true)) {
//            postHead();
//        }
//    }
//
//    public static void postHead() throws IOException {
//        // url = new URL("http://127.0.0.1:" + CommonController.getDataSourcePort() + "/api/finish");
//        url = new URL("http://127.0.0.1:8002/api/finished");
//
//        conn = (HttpURLConnection) url.openConnection();
//        conn.setRequestMethod("POST");
//        conn.setRequestProperty("accept", "*/*");
//        conn.setRequestProperty("connection", "Keep-Alive");
//        conn.setRequestProperty("Content-Type", "application/json");
//        // conn.setRequestProperty("Content-Type", "application/json");
//        // 发送POST请求必须设置如下两行
//        conn.setDoOutput(true);
//        conn.setDoInput(true);
//
//        // 获取URLConnection对象对应的输出流
//        out = new PrintWriter(conn.getOutputStream());
//        out.write("{");
//    }
//
//    public static void post(String key, String value) throws IOException{
//        init();
//        if (sendCount != 0) {
//            out.write(",");
//        }
//        sendCount++;
//        out.write("\\\"" + key + "\":\"" + value + "\"");
////        out.write("\\\"" + key + "\\\":\\\"" + value + "\\\"");
//    }
//
//    public static void postEOF() throws IOException {
//        out.write("}");
//        out.flush();
//
//        System.out.println(conn.getResponseCode());
//        in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
//        String result = "", line;
//        while ((line = in.readLine()) != null) {
//            result += line;
//        }
//        System.out.println(result);
//        out.close();
//        in.close();
//    }

//    public static void main(String[] args) throws IOException {
//        long start = System.currentTimeMillis();
//        init();
//        // post("ABCD", "ABCD");
//        postEOF();
////
////        Map<String, String> map = new HashMap<>();
////        map.put("result", "{}");
//
////        resultMap.put("KEY", "VALUE");
////        ServerService.uploadData();
//        long end = System.currentTimeMillis();
//        System.out.println("It cost time: " + (end - start) + " ms.");
//    }

//    /**
//     * @param args
//     * @throws Exception
//     */
//    public static void main(String[] args) throws Exception{
//        URL url = new URL("http://localhost:8002/api/finished");
//        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//        conn.setDoInput(true);
//        conn.setDoOutput(true);
//        conn.setUseCaches(false);
//        conn.setRequestProperty("Content-Type", "application/json");
//        conn.setRequestProperty("Connection", "close");
//        //==============================请求参数==============================
//        DataOutputStream out = new DataOutputStream(conn.getOutputStream());
//        String content = "result={}";
//        out.writeBytes(content);
//        out.flush();
//        out.close();
//        //==============================响应内容==============================
//        DataInputStream in = new DataInputStream(conn.getInputStream());
//        String str;
//        while (null != ((str = in.readUTF()))) {
//            System.out.println(str );
//        }
//        in.close();
//
//    }
}

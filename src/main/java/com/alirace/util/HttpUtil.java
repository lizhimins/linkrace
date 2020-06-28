package com.alirace.util;

import com.alirace.controller.CommonController;
import com.alirace.server.ServerService;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
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
            .proxy(Proxy.NO_PROXY)
            .connectTimeout(50L, TimeUnit.SECONDS)
            .readTimeout(60L, TimeUnit.SECONDS)
            .build();

    public static Response callHttp(Request request) throws IOException {
        Call call = OK_HTTP_CLIENT.newCall(request);
        return call.execute();
    }

//    public static void main(String[] args) throws IOException {
//        postData("{}");
//    }
//
//    public static void postData(String data) throws IOException {
//        String urlStr = String.format("http://localhost:%s/api/finished", CommonController.getDataSourcePort());
//
//        // Post请求的url，与get不同的是不需要带参数
//        URL url = new URL(urlStr);
//        // 打开连接
//        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//        // 设置是否向connection输出，因为这个是 post 请求，参数要放在 http 正文内，因此需要设为true
//
//        connection.setDoOutput(true);
//        // Read from the connection. Default is true.
//        connection.setDoInput(true);
//        // 默认是 GET方式
//        connection.setRequestMethod("POST");
//        // Post 请求不能使用缓存
//        connection.setUseCaches(false);
//
//        //设置本次连接是否自动重定向
//        connection.setInstanceFollowRedirects(true);
//
//        // 配置本次连接的Content-type，配置为application/x-www-form-urlencoded的
//        // 意思是正文是urlencoded编码过的form参数
//        connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
//        // 连接，从postUrl.openConnection()至此的配置必须要在connect之前完成，
//        // 要注意的是connection.getOutputStream会隐含的进行connect。
//        connection.connect();
//
//        DataOutputStream out = new DataOutputStream(connection.getOutputStream());
//        // 正文，正文内容其实跟get的URL中 '? '后的参数字符串一致
//        String content = "result=" + URLEncoder.encode(data, StandardCharsets.UTF_8.toString());
//        // DataOutputStream.writeBytes将字符串中的16位的unicode字符以8位的字符形式写到流里面
//
//        out.writeBytes(content);
//        //流用完记得关
//        out.flush();
//        out.close();
//
//        //获取响应
//        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//        String line;
//        while ((line = reader.readLine()) != null){
//            System.out.println(line);
//        }
//        reader.close();
//        //该干的都干完了,记得把连接断了
//        connection.disconnect();
//    }
}
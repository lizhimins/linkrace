package com.alirace.server;

import com.alibaba.fastjson.JSON;
import com.alirace.controller.CommonController;
import com.alirace.model.TraceLog;
import com.alirace.netty.MyDecoder;
import com.alirace.netty.MyEncoder;
import com.alirace.util.HttpUtil;
import com.alirace.util.MD5Util;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerService implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ServerService.class);

    // 客户端机器数量
    public static final int MACHINE_NUM = 2;
    // 总的服务数量
    public static final int TOTAL_SERVICES_COUNT = MACHINE_NUM;

    // 用来存放待合并的数据 traceId -> record
    private static final int MAX_HASHMAP_SIZE = 1024 * 16;
    public static ConcurrentHashMap<String, byte[]> mergeMap = new ConcurrentHashMap(MAX_HASHMAP_SIZE);

    // 用来存放剩下的数据 traceId -> md5
    public static ConcurrentHashMap<String, String> resultMap = new ConcurrentHashMap(MAX_HASHMAP_SIZE);

    // 发出的查询数量 和 收到的响应数量, 需要支持并发
    public static AtomicInteger queryRequestCount = new AtomicInteger(0);
    public static AtomicInteger queryResponseCount = new AtomicInteger(0);
    public static AtomicInteger finishCount = new AtomicInteger(0);
    public static AtomicInteger doneCount = new AtomicInteger(0);

    // 监听的端口号
    private static int PORT = 8003;

    public static void start() throws InterruptedException {
        log.info("Server initializing start...");
        // 状态监控服务
        ServerMonitor.start();

        Thread thread = new Thread(new ServerService(), "ServerService");
        thread.start();

//        TimeUnit.MILLISECONDS.sleep(5500);
//        Iterator<Map.Entry<String, byte[]>> iterator = mergeMap.entrySet().iterator();
//        while (iterator.hasNext()) {
//            Map.Entry<String, byte[]> entry = iterator.next();
//            String key = entry.getKey();
//            byte[] value = entry.getValue();
//            flushResult(key, value);
//            iterator.remove();
//        }
//        uploadData();
    }

    public static void startNetty() throws Exception {
        log.info("Server start listen at " + PORT);
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup(1);
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast("decoder", new MyDecoder());
                            pipeline.addLast("encoder", new MyEncoder());
                            pipeline.addLast(new ServerHandler());
                        }
                    });
            // bind port
            ChannelFuture future = bootstrap.bind(PORT).sync();
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    // 结果转移 + 刷盘
    public static void flushResult(String traceId, byte[] body1, byte[] body2) {
        String[] span1 = new String(body1).split("\n");
        String[] span2 = new String(body2).split("\n");
        int length = span1.length + span2.length;
        String[] spans = new String[length];
        for (int i = 0; i < span1.length; i++) {
            spans[i] = span1[i];
        }
        for (int i = span1.length; i < length; i++) {
            spans[i] = span2[i-span1.length];
        }

        Arrays.sort(spans, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return (int) (TraceLog.getTime(o1) - TraceLog.getTime(o2));
            }
        });
        byte[] body = new byte[body1.length + body2.length];
        int pos = 0;
        for (int i = 0; i < spans.length; i++) {
            for (int j = 0; j < spans[i].length(); j++) {
                body[pos] = (byte) (int) spans[i].charAt(j);
                pos++;
            }
            body[pos++] = (byte) (int) '\n';
        }

//        System.out.println();
//        for (int i = 0; i < spans.length; i++) {
//            System.out.println(spans[i]);
//        }

        String md5 = MD5Util.byteToMD5(body);
        // log.info(String.format("TraceId: %16s, MD5: %32s", traceId, md5));
//
//        for (int i = 0; i < bytes.length; i++) {
//            if (bytes[i] != body[i]) {
//                System.out.print((char) (int) body[i] +  " ");
//            }
//        }
        resultMap.put(traceId, md5);
        mergeMap.remove(traceId);
    }

    // 结果转移 + 刷盘
    public static void flushResult(String traceId, byte[] bytes) {
        String[] spans = new String(bytes).split("\n");

//        for (int i = 0; i < spans.length; i++) {
//            System.out.println(spans[i]);
//        }

        Arrays.sort(spans, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return (int) (TraceLog.getTime(o1) - TraceLog.getTime(o2));
            }
        });
        byte[] body = new byte[bytes.length];
        int pos = 0;
        for (int i = 0; i < spans.length; i++) {
            for (int j = 0; j < spans[i].length(); j++) {
                body[pos] = (byte) (int) spans[i].charAt(j);
                pos++;
            }
            body[pos++] = (byte) (int) '\n';
        }

//        System.out.println();
//        for (int i = 0; i < spans.length; i++) {
//            System.out.println(spans[i]);
//        }

        String md5 = MD5Util.byteToMD5(body);
//        log.info(String.format("TraceId: %16s, MD5: %32s", traceId, md5));
//
//        for (int i = 0; i < bytes.length; i++) {
//            if (bytes[i] != body[i]) {
//                System.out.print((char) (int) body[i] +  " ");
//            }
//        }
        resultMap.put(traceId, md5);
        // mergeMap.remove(traceId);
    }

    // http 调用上传接口
    public static void uploadData() {
        log.info("Server start upload data...");
        try {
            String result = JSON.toJSONString(resultMap);
            RequestBody body = new FormBody.Builder()
                    .add("result", result).build();
            String url = String.format("http://localhost:%s/api/finished", CommonController.getDataSourcePort());
            Request request = new Request.Builder().url(url).post(body).build();
            Response response = HttpUtil.callHttp(request);
            if (response.isSuccessful()) {
                response.close();
                log.warn("Server success to sendCheckSum, result.");
                return;
            }
            log.warn("fail to sendCheckSum:" + response.message());
            response.close();
        } catch (Exception e) {
            log.warn("fail to call finish", e);
        }
        log.info("Server data upload success...");
    }

    @Override
    public void run() {
        try {
            startNetty();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package com.alirace.server;

import com.alibaba.fastjson.JSON;
import com.alirace.constant.Constant;
import com.alirace.controller.CommonController;
import com.alirace.model.TraceLog;
import com.alirace.netty.MyDecoder;
import com.alirace.netty.MyEncoder;
import com.alirace.util.HttpUtil;
import com.alirace.util.MD5Util;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.runner.RunnerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
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
    public static HashMap<String, byte[]> mergeMap = new HashMap(MAX_HASHMAP_SIZE);

    // 用来存放剩下的数据 traceId -> md5
    public static HashMap<String, String> resultMap = new HashMap(MAX_HASHMAP_SIZE);

    // 发出的查询数量 和 收到的响应数量, 需要支持并发
    public static int queryRequestCount = 0;
    public static int queryResponseCount = 0;
    public static int finishCount = 0;
    public static int doneCount = 0;

    // 监听的端口号
    private static int PORT = 8003;

    public static void start() throws InterruptedException, RunnerException {
        log.info("Server initializing start...");
        // 状态监控服务
        // ServerMonitor.start();

//        Options opt = new OptionsBuilder()
//                .include(ServerService.class.getSimpleName())
//                .warmupIterations(1)
//                .measurementIterations(1)
//                .mode(Mode.Throughput)
//                .mode(Mode.AverageTime)
//                .forks(1)
//                .build();
//        new Runner(opt).run();

        Thread thread = new Thread(new ServerService(), "ServerService");
        thread.start();


        TimeUnit.MILLISECONDS.sleep(20000);
//        Iterator<Map.Entry<String, byte[]>> iterator = mergeMap.entrySet().iterator();
//        while (iterator.hasNext()) {
//            Map.Entry<String, byte[]> entry = iterator.next();
//            String key = entry.getKey();
//            byte[] value = entry.getValue();
//            flushResult(key, value);
//            iterator.remove();
//        }
        uploadData();
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

    @Benchmark
    public static void warmUp() {
        String bodyStr1 = ""
                + "447eb726d7d5e8c9|1590216545652169|447eb726d7d5e8c9|0|1261|LogisticsCenter|DoQueryStatData|192.168.58.85|biz=fxtius&sampler.type=const&sampler.param=1\n"
                + "447eb726d7d5e8c9|1590216545652175|347dafb6970e12b8|521f164a0650351|1255|Frontend|TraceSegmentReportService/collect|192.168.58.87|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://localhost:9003/getAddress?id=4&peer.port=9003&http.method=GET\n"
                + "447eb726d7d5e8c9|1590216545652181|a2500deaee4d02f|521f164a0650351|1249|ItemCenter|/status.html|192.168.58.89|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://localhost:9001/getItem?id=3&peer.port=9001&http.method=GET\n"
                + "447eb726d7d5e8c9|1590216545652187|1fdf6f1c165e1167|521f164a0650351|1243|LogisticsCenter|processZipkin|192.168.58.91|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://localhost:9003/getAddress?id=3&peer.port=9003&http.method=GET\n"
                + "447eb726d7d5e8c9|1590216545652190|8732a74a7afec5e|521f164a0650351|1240|InventoryCenter|Register/doEndpointRegister|192.168.58.92|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://tracing.console.aliyun.com/getInventory?id=3&peer.port=9005&http.method=GET\n"
                + "447eb726d7d5e8c9|1590216545652193|32fef78ec5c82ab0|521f164a0650351|1237|Frontend|sls.getOperator|192.168.58.93|http.status_code=200&component=java-web-servlet&span.kind=server&http.url=http://tracing.console.aliyun.com/createOrder&entrance=pc&http.method=GET&userId=110\n"
                + "447eb726d7d5e8c9|1590216545652202|1c666119dee0dd90|521f164a0650351|1228|OrderCenter|postHandleData|192.168.58.96|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://tracing.console.aliyun.com/createOrder?id=3&peer.port=9002&http.method=GET\n"
                + "447eb726d7d5e8c9|1590216545652205|15b5b42245efdd6b|521f164a0650351|1225|LogisticsCenter|db.AlertDao.listByTitleAndUserId(..)|192.168.58.97|&component=java-web-servlet&span.kind=server&http.url=http://tracing.console.aliyun.com/getOrder&http.method=GET&&error=1\n"
                + "447eb726d7d5e8c9|1590216545652208|4557ced6f95e1cc|447eb726d7d5e8c9|1222|InventoryCenter|DoSearchAlertTemplates|192.168.58.98|&component=java-spring-rest-template&span.kind=client&http.url=http://tracing.console.aliyun.com/getOrder?id=4&peer.port=9002&http.method=GET\n";

        String bodyStr2 = ""
                + "447eb726d7d5e8c9|1590216545652172|521f164a0650351|447eb726d7d5e8c9|1258|InventoryCenter|DoSearchAlertByName|192.168.58.86|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://localhost:9004/getPromotion?id=4&peer.port=9004&http.method=GET\n"
                + "447eb726d7d5e8c9|1590216545652178|14caa7851514f1aa|521f164a0650351|1252|PromotionCenter|DoGetDatas|192.168.58.88|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://tracing.console.aliyun.com/getInventory?id=4&peer.port=9005&http.method=GET\n"
                + "447eb726d7d5e8c9|1590216545652184|188156abbc17dfcf|521f164a0650351|1246|OrderCenter|noToName2|192.168.58.90|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://localhost:9004/getPromotion?id=3&peer.port=9004&http.method=GET\n"
                + "447eb726d7d5e8c9|1590216545652196|7d610a59da2e8962|521f164a0650351|1234|PromotionCenter|DoGetTProfInteractionSnapshot|192.168.58.94|http.status_code=200&component=java-web-servlet&span.kind=server&http.url=http://tracing.console.aliyun.com/createOrder&entrance=pc&http.method=GET&userId=8970\n"
                + "447eb726d7d5e8c9|1590216545652199|43709914b27298c|521f164a0650351|1231|ItemCenter|db.ArmsAppDao.getAppListByUserIdAllRegion(..)|192.168.58.95|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://tracing.console.aliyun.com/getOrder?id=3&peer.port=9002&http.method=GET\n"
                + "447eb726d7d5e8c9|1590216545652211|6999f212cdb17d03|4557ced6f95e1cc|1219|Frontend|db.ArmsAppDao.selectByComplex(..)|192.168.58.99|&component=java-web-servlet&span.kind=server&http.url=http://tracing.console.aliyun.com/createOrder&entrance=pc&http.method=GET&userId=26758&http.status_code=400\n";

        String md5 = ServerService.flushResult(bodyStr1.getBytes(), bodyStr2.getBytes());
    }

    public static String flushResult(byte[] body1, byte[] body2) {
        // 计算 traceId 长度
        int traceIdLength = 10;
        while (body1[traceIdLength] != Constant.LOG_SEPARATOR) {
            traceIdLength++;
        }
        // log.info(traceIdLength + "");

        int length1 = body1.length, length2 = body2.length, total = length1 + length2;
        int index = 0;
        byte[] body = new byte[total];
        int startOffset1 = 0, startOffset2 = 0;

        // 归并排序
        while (startOffset1 < length1 && startOffset2 < length2) {
            /*
            for (int i = 0; i < 16; i++) {
                byte b1 = body1[startOffset1 + traceIdLength + 1 + i];
                System.out.print((char) (int) b1);
            }
            System.out.print("-");

            for (int i = 0; i < 16; i++) {
                byte b2 = body2[startOffset2 + traceIdLength + 1 + i];
                System.out.print((char) (int) b2);
            }
            System.out.println();
            */
            for (int i = 0; i < 16; i++) {
                byte b1 = body1[startOffset1 + traceIdLength + 1 + i];
                byte b2 = body2[startOffset2 + traceIdLength + 1 + i];

//                System.out.print((char) (int) b1 + "_" + (char) (int) b2 + " ");
                if (b1 == b2) {
                    continue;
                }
                if (b1 < b2) {
                    while (body1[startOffset1] != Constant.LINE_SEPARATOR) {
                        body[index++] = body1[startOffset1++];
                    }
                    body[index++] = body1[startOffset1++];
                    break;
                }
                if (b1 > b2) {
                    while (body2[startOffset2] != Constant.LINE_SEPARATOR) {
                        body[index++] = body2[startOffset2++];
                    }
                    body[index++] = body2[startOffset2++];
                    break;
                }
            }
            // log.info(String.format("%d-%d", startOffset1, endOffset1));
        }

        while (startOffset1 < length1) {
            body[index++] = body1[startOffset1++];
        }

        while (startOffset2 < length2) {
            body[index++] = body2[startOffset2++];
        }

        body[total - 1] = '\n';
        // System.out.println("Result: \n" + new String(body));

//        StringBuffer buffer = new StringBuffer(16);
//        for (int i = 0; i < 16; i++) {
//            if (body[i] == (byte) '|') {
//                break;
//            }
//            buffer.append((char) (int) body[i]);
//        }
//        String traceId = buffer.toString();
//
//        if (traceId.equals("1c2b9d10fde34")) {
//            System.out.println(new String(body));
//        }

        return MD5Util.byteToMD5(body);
    }

    public static void buildLink() {
        log.info("Server start build link...");
        try {
            RequestBody body = new FormBody.Builder().build();
            String url = String.format("http://localhost:%s/ready", CommonController.getDataSourcePort());
            Request request = new Request.Builder().url(url).get().build();
            Response response = HttpUtil.callHttp(request);
            if (response.isSuccessful()) {
                response.close();
                // log.warn("Server success to build link.");
                return;
            }
            // log.warn("fail to build link:" + response.message());
            response.close();
        } catch (Exception e) {
            log.warn("fail to call finish", e);
        }
    }

    // http 调用上传接口
    public static void uploadData() {
        log.info("Server start upload data...");
        String result = null;
        try {
             result = JSON.toJSONString(resultMap);
             RequestBody body = new FormBody.Builder().add("result", result).build();
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
             log.warn("Server success to sendCheckSum, result.");
        } catch (Exception e) {
            log.warn("fail to call finish", e);
        }
        log.info("Server data upload success...");
        log.info(JSON.toJSONString(resultMap));
    }

    public static void flushResult(byte[] bytes) {
    }

    @Override
    public void run() {
        try {
            startNetty();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

/*
    public static String flushResult1(byte[] body1, byte[] body2) {
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
//        resultMap.put(traceId, md5);
//        mergeMap.remove(traceId);
        return md5;
    }
*/

    public static String flushResult3(byte[] bytes) {
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
//        resultMap.put(traceId, md5);
        // mergeMap.remove(traceId);
        return md5;
    }
}

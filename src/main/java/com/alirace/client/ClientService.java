package com.alirace.client;

import com.alirace.controller.CommonController;
import com.alirace.model.*;
import com.alirace.netty.MyDecoder;
import com.alirace.netty.MyEncoder;
import com.alirace.util.SerializeUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.alirace.client.ClientMonitor.*;

public class ClientService implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientService.class);

    // 通信相关参数配置
    private static final String HOST = "localhost";
    private static final int PORT = 8003;

    // 和汇总服务器 通信 Netty 相关配置
    private static EventLoopGroup workerGroup;
    private static Bootstrap bootstrap;
    private static ChannelFuture future;

    // 实际执行任务的线程
    private static final int THREAD_NUM = 2;
    public static List<HttpClient> threads;

    // 精确一次上传
    private static HashMap<String /*traceId*/, AtomicBoolean /*isUpload*/> waitArea = new HashMap<>();

    // 查询
    public static void queryRecord(String traceId) throws InterruptedException {
        queryCount.incrementAndGet();
    }

    // 上传读入进度
    public static void uploadStatus(long logOffset) {
        Message message = new Message(MessageType.STATUS.getValue(), String.valueOf(logOffset).getBytes());
        future.channel().writeAndFlush(message);
    }

    // 上传调用链
    public static void upload(byte[] body) {
        uploadCount.incrementAndGet();
        Message message = new Message(MessageType.UPLOAD.getValue(), body);
        future.channel().writeAndFlush(message);
    }

    // 查询结果都用这个上报
    public static void passRecord(Record record) {
        passCount.incrementAndGet();
        byte[] body = SerializeUtil.serialize(record);
        Message message = new Message(MessageType.PASS.getValue(), body);
        future.channel().writeAndFlush(message);
    }

    // 查询响应
    public static void response() {
        responseCount.incrementAndGet();
        byte[] body = "response".getBytes();
        Message message = new Message(MessageType.RESPONSE.getValue(), body);
        future.channel().writeAndFlush(message);
    }

    // 读入完成
    public static void finish() {
        byte[] body = "finish".getBytes();
        Message message = new Message(MessageType.FINISH.getValue(), body);
        future.channel().writeAndFlush(message);
    }

    public static void startNetty() {
        log.info("Client Netty doConnect...");
        workerGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast("decoder", new MyDecoder());
                        ch.pipeline().addLast("encoder", new MyEncoder());
                        ch.pipeline().addLast(new ClientHandler());
                    }
                });
        doConnect();
        // workerGroup.shutdownGracefully();
    }

    // 连接到服务器
    public static void doConnect() {
        if (future != null && future.channel() != null && future.channel().isActive()) {
            return;
        }
        future = bootstrap.connect(HOST, PORT);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    Channel channel = future.channel();
                    channel.writeAndFlush(new Message(MessageType.STATUS.getValue(), "OK".getBytes())).sync();
                    log.info("Connect to server successfully!");
                } else {
                    // log.info("Failed to connect to server, try connect after 1s.");
                    future.channel().eventLoop().schedule(new Runnable() {
                        @Override
                        public void run() {
                            doConnect();
                        }
                    }, 1000, TimeUnit.MILLISECONDS);
                }
            }
        });
    }

    // 表示初始化
    public static void start() throws URISyntaxException {
        log.info("Client initializing start...");

        // 监控服务
        // ClientMonitor.start();

        // 初始化数据服务线程
        threads = HttpClient.init(THREAD_NUM);
        HttpClient.initBucket();
//        Thread thread = new Thread(new ClientService(), "Client");
//        thread.start();

        // 启动 netty 进行通信
        startNetty();
    }

    // 单例
    public static void startPullHttpData(String filePath) throws InterruptedException, URISyntaxException {
        HttpClient.setUri(filePath);
        HttpClient.doConnect(THREAD_NUM);
        HttpClient.getFileLength();
    }

    public static void startThread() {
        for (int i = 0; i < THREAD_NUM; i++) {
            HttpClient httpClient = threads.get(i);
            long blockLength = HttpClient.contentLength / THREAD_NUM;
            httpClient.setStartOffset(blockLength * i);
            httpClient.setFinishOffset(blockLength * (i + 1) - 1);
            if (i == THREAD_NUM - 1) {
                httpClient.setFinishOffset(HttpClient.contentLength - 1);
            }
            httpClient.start();
        }
    }

    @Override
    public void run() {

    }
}


//                int checkResult = AhoCorasickAutomation.find(bytes, nowOffset, logOffset);
//                // 粘包的时候回溯检查数据
//                if (checkResult == 0) {
//                    // System.out.println(String.format("Bucket: %d 粘包", bucketIndex));
//                    continue;
//                }
//                if (checkResult > 0) {
//                    // System.out.println(String.format("Bucket: %d YES", bucketIndex));
//                } else {
//                    for (int i = 0; i < pos; i++) {
//                        System.out.print((char) (int) traceId[i]);
//                    }
//                    System.out.println(String.format(" Bucket: %d No", bucketIndex));
//                }
//
//                nowOffset++;
//                StringBuffer sb = new StringBuffer();
//                for (; nowOffset < logOffset; nowOffset++) {
//                    if (LINE_SEPARATOR == bytes[nowOffset]) {
//                        break;
//                    }
//                    // System.out.print((char) (int) bytes[nowOffset]);
//                    // if (nowOffset + 7 > logOffset) {
//                    //    break;
//                    // }
//                    // FIXME: 和暴力的结果对比
//                    sb.append((char) bytes[nowOffset]);
//                }
//                // System.out.println(sb.toString());
//                boolean flag = Tag.isError(sb.toString());
//                if (flag) {
//                    for (int i = 0; i < pos; i++) {
//                        System.out.print((char) (int) traceId[i]);
//                    }
//                    // System.out.print(" " + sb.toString());
//                    System.out.println(String.format(" Bucket: %d has ERROR", bucketIndex));
//                }
//
//                preOffset = ++nowOffset;
//                // System.out.println();


/*

对拍检查
int offset = nowOffset;
int endOff = nowOffset;
StringBuffer sb = new StringBuffer();
while (bytes[endOff] != LINE_SEPARATOR) {
    char ch = (char) (int) bytes[endOff];
    sb.append(ch);
    endOff++;
}

boolean flag = Tag.isError(sb.toString());
if (nowOffset < 0 && !flag || nowOffset > 0 && flag) {
    System.out.println(sb.toString());
}
 */
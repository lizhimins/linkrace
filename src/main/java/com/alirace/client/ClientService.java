package com.alirace.client;

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
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.alirace.client.ClientMonitor.*;

public class ClientService implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientService.class);

    // 通信相关参数配置
    private static final String HOST = "localhost";
    private static final int PORT = 8003;

    // Netty 相关配置
    private static EventLoopGroup workerGroup;
    private static Bootstrap bootstrap;
    private static ChannelFuture future;

    // 数据获取地址, 由 CommonController 传入
    private static String path;

    // 精确一次上传
    private static ConcurrentHashMap<String /*traceId*/, AtomicBoolean /*isUpload*/> waitArea;

    // 真实数据
    private static int preOffset = 0; // 上一次处理的偏移
    private static int logOffset = 0; // 当前偏移
    private static final int LENGTH_PER_READ = 0x01 << 13; // 每一次读 8192 B
    private static final int BYTES_LENGTH = 0x01 << 30; // 1G = 1024M = 2^30 B
    private static byte[] bytes = new byte[BYTES_LENGTH];

    // 索引部分
    private static final int BUCKETS_NUM = 0x01 << 20; // 100万
    private static Bucket[] buckets = new Bucket[BUCKETS_NUM];
    private static final int WINDOW_SIZE = 20000; // 窗口大小配置为 2万
    private static Node[] nodes = new Node[WINDOW_SIZE];

    // 执行线程
    private static Thread clientService;


    public static void pullData() throws IOException {
        log.info("Client pull data start... Data path: " + path);
        URL url = new URL(path);
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        InputStream input = httpConnection.getInputStream();

        int readCharNum = 0;
        while (true) {
            readCharNum = input.read(bytes, logOffset, LENGTH_PER_READ);

            if (readCharNum == -1) {
                break;
            } else {
                logOffset += readCharNum;
            }

            for (int i = preOffset; i < logOffset; i++) {
                // TODO:
            }
        }

        input.close();
        log.info("Client pull data finish..." + logOffset);
    }

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
    public static void uploadRecord(Record record) {
        uploadCount.incrementAndGet();
        byte[] body = SerializeUtil.serialize(record);
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
    public static void init() {
        log.info("Client initializing start...");
        // 监控服务
        ClientMonitor.start();
        // 初始化数据服务线程
        clientService = new Thread(new ClientService(), "Client");
        // 在最后启动 netty 进行通信
        startNetty();
    }

    @Override
    public void run() {
        try {
            pullData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setPath(String path) {
        ClientService.path = path;
    }

    public static Thread getClientService() {
        return clientService;
    }

    public static int getPreOffset() {
        return preOffset;
    }

    public static int getLogOffset() {
        return logOffset;
    }
}

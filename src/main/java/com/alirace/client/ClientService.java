package com.alirace.client;

import com.alirace.model.*;
import com.alirace.netty.MyDecoder;
import com.alirace.netty.MyEncoder;
import com.alirace.util.SerializeUtil;
import com.alirace.util.StringUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
    private static final byte LOG_SEPARATOR = (byte) '|';
    private static final byte LINE_SEPARATOR = (byte) '\n'; // key:value 分隔符
    private static final int LENGTH_PER_READ = 0x1 << 13; // 每一次读 8kb
    private static int preOffset = LENGTH_PER_READ; // 上一次处理的偏移
    private static int nowOffset = LENGTH_PER_READ; // 当前偏移
    private static int logOffset = LENGTH_PER_READ; // 当前偏移
    private static final int BYTES_LENGTH = 0x01 << 30;
    private static byte[] bytes = new byte[BYTES_LENGTH + (0x01 << 13)];

    // 索引部分
    private static final int BUCKETS_NUM = 0x01 << 20; // 100万
    private static Bucket[] buckets = new Bucket[BUCKETS_NUM];
    private static final int WINDOW_SIZE = 20000; // 窗口大小配置为 2万
    private static Node[] nodes = new Node[WINDOW_SIZE];

    // 执行线程
    private static Thread clientService;

    public void pullData() throws IOException {
        log.info("Client pull data start... Data path: " + path);
        URL url = new URL(path);
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        InputStream input = httpConnection.getInputStream();

        int readCharNum = 0;
        int bucketIndex = 0;
        int logSeparatorTime = 0; // 日志分割符次数

        // 保存 traceId
        int pos = 0;
        byte[] traceId = new byte[16];

        while (true) {

            readCharNum = input.read(bytes, logOffset, LENGTH_PER_READ);

            // 文件结束不处理
            if (readCharNum == -1) {
                break;
            }

            // 日志坐标右移
            logOffset += readCharNum;

            while (nowOffset < logOffset) {

                // 剩余长度过短不处理, 最小长度
                if (logOffset - nowOffset < 16) {
                    break;
                }

                // 计算桶的索引
                bucketIndex = StringUtil.byteToHex(bytes, preOffset, preOffset + 5);
                // System.out.print(String.format("Bucket: %d ", bucketIndex));

                for (pos = 0; pos < 16; pos++, nowOffset++) {
                    if (bytes[nowOffset] == LOG_SEPARATOR) {
                        break;
                    }
                    traceId[pos] = bytes[nowOffset];
                }
                // System.out.print(StringUtil.byteToString(traceId) + " ");

                // 划过中间部分
                logSeparatorTime = 0;
                for (; nowOffset <= logOffset; nowOffset++) {
                    if (bytes[nowOffset] == LOG_SEPARATOR) {
                        logSeparatorTime++;
                        if (logSeparatorTime == 8) {
                            break;
                        }
                    }
                }
                if (logSeparatorTime != 8) {
                    continue;
                }

                nowOffset++;

                StringBuffer sb = new StringBuffer();
                for (; nowOffset <= logOffset; nowOffset++) {
                    if (LINE_SEPARATOR == bytes[nowOffset]) {
                        break;
                    }
                    // System.out.print((char) (int) bytes[nowOffset]);
                    // if (nowOffset + 7 > logOffset) {
                    //    break;
                    // }
                    // FIXME: 先写一个暴力吧
                    sb.append((char) bytes[nowOffset]);
                }
                // System.out.println(sb.toString());
                boolean flag = Tag.isError(sb.toString());
                if (flag) {
                    System.out.println(String.format("Bucket: %d ", bucketIndex) + "ERROR");
                }
                preOffset = ++nowOffset;
                // System.out.println();
            }

            // 如果太长了要从头开始写
            if (logOffset >= BYTES_LENGTH) {
                // 拷贝末尾的数据
                for (int i = 0; i < LENGTH_PER_READ; i++) {
                    bytes[i] = bytes[i + BYTES_LENGTH];
                }
                preOffset -= BYTES_LENGTH;
                logOffset -= BYTES_LENGTH;
            }
        }
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
        // ClientMonitor.start();
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

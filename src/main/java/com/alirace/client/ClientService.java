package com.alirace.client;

import com.alirace.controller.CommonController;
import com.alirace.model.*;
import com.alirace.netty.MyDecoder;
import com.alirace.netty.MyEncoder;
import com.alirace.util.AhoCorasickAutomation;
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
import java.util.HashMap;
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
    private static final int LENGTH_PER_READ = 8192; // 每一次读 8kb

    private static int preOffset = 0; // 起始偏移
    private static int nowOffset = 0; // 当前偏移
    private static int logOffset = 0; // 日志偏移

    private static final int BYTES_LENGTH = 8192 * 1024 * 32;
    private static byte[] bytes = new byte[BYTES_LENGTH + 2 * LENGTH_PER_READ];

    // 索引部分
    private static final int BUCKETS_NUM = 0x01 << 20; // 100万
    private static Bucket[] buckets = new Bucket[BUCKETS_NUM];

    // 窗口大小配置为 2万
    private static int nodeIndex;
    private static final int WINDOW_SIZE = 20000;
    private static Node[] nodes = new Node[WINDOW_SIZE];

    // 保存 traceId
    private static int pos = 0;
    private static byte[] traceId = new byte[32];
    private static int bucketIndex = 0;

    // 执行线程
    private static Thread clientService;

    public void pullData() throws IOException {
        log.info("Client pull data start... Data path: " + path);
        URL url = new URL(path);
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        InputStream input = httpConnection.getInputStream();

        // 初始化左右指针, 空出第一小段数据以备复制
        nowOffset = LENGTH_PER_READ;
        preOffset = LENGTH_PER_READ;
        logOffset = LENGTH_PER_READ;

        int readByteCount = input.read(bytes, logOffset, LENGTH_PER_READ);
        logOffset += readByteCount;

        while (true) {
            // 尝试读入一次
            readByteCount = input.read(bytes, logOffset, LENGTH_PER_READ);

            // 文件结束退出
            if (readByteCount == -1) {
                break;
            }

            // 日志坐标右移
            logOffset += readByteCount;

            while (nowOffset + LENGTH_PER_READ < logOffset) {
                preOffset = nowOffset;

                // 调试用延迟
//                try {
//                    TimeUnit.MILLISECONDS.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }

                // traceId 部分处理
                pos = 0;
                while (bytes[nowOffset] != LOG_SEPARATOR) {
                    traceId[pos] = bytes[nowOffset];
                    pos++;
                    nowOffset++;
                }
                traceId[pos] = (byte) '\n';

                // System.out.print(StringUtil.byteToString(traceId) + " ");

                // 计算桶的索引
                bucketIndex = StringUtil.byteToHex(traceId, 0, 5);
                // System.out.println(String.format("index: %d ", bucketIndex));

                // 滑过中间部分
                for (int sep = 0; sep < 8; nowOffset++) {
                    if (bytes[nowOffset] == LOG_SEPARATOR) {
                        sep++;
                    }
                }

                nowOffset = AhoCorasickAutomation.find(bytes, nowOffset - 1);

                // 返回值 < 0, 说明当前 traceId 有问题
                if (nowOffset < 0) {
                    // System.out.println("No");
                    nowOffset = -nowOffset;
                    errorCount.incrementAndGet();
                    buckets[bucketIndex].addNewSpan(traceId, preOffset, nowOffset + 1, true);
                } else {
                    // System.out.println("Yes");
                    buckets[bucketIndex].addNewSpan(traceId, preOffset, nowOffset + 1, false);
                }

                // 窗口操作
                int preBucketIndex = nodes[nodeIndex].bucketIndex;
                int preBucketOffset = nodes[nodeIndex].startOffset;
                // 如果已经有数据了
                if (preBucketIndex != -1) {
                    buckets[preBucketIndex].checkAndUpload(preBucketOffset);
                }
                nodes[nodeIndex].bucketIndex = bucketIndex;
                nodes[nodeIndex].startOffset = preOffset;
                nowOffset++;
            }

            // 如果太长了要从头开始写
            if (logOffset > BYTES_LENGTH + LENGTH_PER_READ) {
                // 拷贝末尾的数据
                for (int i = nowOffset; i < logOffset; i++) {
                    bytes[i - BYTES_LENGTH] = bytes[i];
                }
                nowOffset -= BYTES_LENGTH;
                logOffset -= BYTES_LENGTH;
                log.info("rewrite");
            }
        }
        log.info("Client pull data finish..." + logOffset);
        log.info("errorCount: " + errorCount);
        // log.info("traceCount: " + traceCount.size());
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

        log.info("Bucket initializing start...");
        for (int i = 0; i < BUCKETS_NUM; i++) {
            buckets[i] = new Bucket();
        }

        log.info("Windows initializing start...");
        for (int i = 0; i < WINDOW_SIZE; i++) {
            nodes[i] = new Node();
        }
        // 初始化数据服务线程
        clientService = new Thread(new ClientService(), "Client");
        HttpClient.init();

        // 在最后启动 netty 进行通信
        startNetty();
    }

    @Override
    public void run() {
        try {
            HttpClient.connect("10.66.1.107", CommonController.getDataSourcePort());
            pullData();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setPath(String path) {
        ClientService.path = path;
    }

    public static Thread getClientService() {
        return clientService;
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
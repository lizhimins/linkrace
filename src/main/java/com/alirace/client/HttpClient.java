package com.alirace.client;


import com.alirace.model.Bucket;
import com.alirace.model.Message;
import com.alirace.model.MessageType;
import com.alirace.model.Node;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HttpClient extends Thread {

    private static final Logger log = LoggerFactory.getLogger(HttpClient.class);

    // 通信相关参数配置
    private static final String HOST = "10.66.1.107";
    private static final int PORT = 8004;
    // 可以调整的常量
    private static final byte LOG_SEPARATOR = (byte) '|';
    private static final byte LINE_SEPARATOR = (byte) '\n'; // 行尾分隔符
    protected static final int BYTES_LENGTH = 8192 * 1024; // ByteBuf 最大长度
    protected static final int BUCKETS_NUM = 0x01 << 20; // 100万
    protected static final int WINDOW_SIZE = 20000; // 窗口的大小
    private static Bootstrap bootstrap;
    private static ChannelFuture future;
    // 数据获取地址, 由 CommonController 传入
    private static URI uri;
    private int preOffset = 0; // 起始偏移
    private int nowOffset = 0; // 当前偏移
    private int logOffset = 0; // 日志偏移
    private long roundOffset = 0; // 真实偏移

    // 保存 traceId
    private int pos = 0;
    private byte[] traceId = new byte[32];
    private int bucketIndex = 0;

    // 树索引
    private Bucket[] buckets = new Bucket[BUCKETS_NUM];

    // 窗口
    private int nodeIndex;
    private Node[] nodes = new Node[WINDOW_SIZE];

    // 构造函数, 完成初始化任务
    public HttpClient(String name) {
        super(name);

        log.info("Bucket initializing start..." + name);
        for (int i = 0; i < BUCKETS_NUM; i++) {
            buckets[i] = new Bucket();
        }

        log.info("Windows initializing start..." + name);
        for (int i = 0; i < WINDOW_SIZE; i++) {
            nodes[i] = new Node();
        }
    }
//    public void pullData() throws IOException {
//        log.info("Client pull data start... Data path: " + path);
//        URL url = new URL(path);
//        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
//        // httpConnection.setRequestProperty("range","bytes=14517359-14517659");
//        InputStream input = httpConnection.getInputStream();
//
//        // 初始化左右指针, 空出第一小段数据以备复制
//        nowOffset = LENGTH_PER_READ;
//        preOffset = LENGTH_PER_READ;
//        logOffset = LENGTH_PER_READ;
//        roundOffset = 0L;
//
//        int readByteCount = input.read(bytes, logOffset, LENGTH_PER_READ);
//        logOffset += readByteCount;
//
//        while (true) {
//            // 尝试读入一次
//            readByteCount = input.read(bytes, logOffset, LENGTH_PER_READ);
//
//            // 文件结束退出
//            if (readByteCount == -1) {
//                break;
//            }
//
//            // 日志坐标右移
//            logOffset += readByteCount;
//
//            while (nowOffset + LENGTH_PER_READ < logOffset) {
//                preOffset = nowOffset;
//
//                // 调试用延迟
////                try {
////                    TimeUnit.MILLISECONDS.sleep(1);
////                } catch (InterruptedException e) {
////                    e.printStackTrace();
////                }
//
//                // traceId 部分处理
//                pos = 0;
//                while (bytes[nowOffset] != LOG_SEPARATOR) {
//                    traceId[pos] = bytes[nowOffset];
//                    pos++;
//                    nowOffset++;
//                }
//                traceId[pos] = (byte) '\n';
//
//                // System.out.print(StringUtil.byteToString(traceId) + " ");
//
//                // 计算桶的索引
//                bucketIndex = StringUtil.byteToHex(traceId, 0, 5);
//                // System.out.println(String.format("index: %d ", bucketIndex));
//
//                // 将 traceId
//                boolean isSame = buckets[bucketIndex].isSameTraceId(traceId);
//
//                if (!isSame) {
//                    buckets[bucketIndex].setTraceId(traceId);
//                }
//
//                // 滑过中间部分
//                for (int sep = 0; sep < 8; nowOffset++) {
//                    if (bytes[nowOffset] == LOG_SEPARATOR) {
//                        sep++;
//                    }
//                }
//
//                nowOffset = AhoCorasickAutomation.find(bytes, nowOffset - 1);
//
//                // 返回值 < 0, 说明当前 traceId 有问题
//                if (nowOffset < 0) {
//                    // System.out.println("No");
//                    nowOffset = -nowOffset;
//                    errorCount.incrementAndGet();
//                    long start = roundOffset + preOffset - LENGTH_PER_READ;
//                    long end = roundOffset + nowOffset - LENGTH_PER_READ;
//                    buckets[bucketIndex].addNewSpan(start, end, true);
//                } else {
//                    // System.out.println("Yes");
//                    long start = roundOffset + preOffset - LENGTH_PER_READ;
//                    long end = roundOffset + nowOffset - LENGTH_PER_READ;
//                    buckets[bucketIndex].addNewSpan(start, end, false);
//                }
//
//                // 窗口操作, 当前写 nodeIndex
//                // 先取出数据
//                int preBucketIndex = nodes[nodeIndex].bucketIndex;
//                long preStartOffset = nodes[nodeIndex].startOffset;
//
//                // 如果已经有数据了
//                if (preBucketIndex != -1) {
//                    buckets[preBucketIndex].checkAndUpload(preStartOffset);
//                }
//                nodes[nodeIndex].bucketIndex = bucketIndex;
//                nodes[nodeIndex].startOffset = preOffset;
//                nodeIndex = (nodeIndex + 1) % WINDOW_SIZE;
//
//                nowOffset++;
//            }
//
//            // 如果太长了要从头开始写
//            if (logOffset > BYTES_LENGTH + LENGTH_PER_READ) {
//                // 拷贝末尾的数据
//                for (int i = nowOffset; i < logOffset; i++) {
//                    bytes[i - BYTES_LENGTH] = bytes[i];
//                }
//                nowOffset -= BYTES_LENGTH;
//                logOffset -= BYTES_LENGTH;
//                roundOffset += BYTES_LENGTH;
//                // log.info("rewrite");
//            }
//        }
//        log.info("Client pull data finish...");
//        log.info("errorCount: " + errorCount);
//        // log.info("traceCount: " + traceCount.size());
//    }

    public void pullData() {
        log.info(this.getName() + "start pull data...");
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.toASCIIString());
        // 构建http请求
        request.headers().set(HttpHeaderNames.HOST, "localhost");
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderNames.CONNECTION);
        request.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());
        request.headers().set(HttpHeaderNames.RANGE, "bytes=1-2000");
        // 发送http请求
        future.channel().writeAndFlush(request).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                // System.out.println(future);
            }
        });
    }

    public static void query(String requestOffset) {
        log.info("requestOffset: " + requestOffset);
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.toASCIIString());
        // 构建http请求
        request.headers().set(HttpHeaderNames.HOST, "localhost");
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderNames.CONNECTION);
        request.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());
        request.headers().set(HttpHeaderNames.RANGE, requestOffset);
        // 发送http请求
        future.channel().writeAndFlush(request).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                // System.out.println(future);
            }
        });
    }

    public static void setUri(String filePath) throws URISyntaxException {
        HttpClient.uri = new URI(filePath);
    }

    // 初始化实际线程
    public static List<HttpClient> init(int nThreads) {
        log.info("HttpClient initializing start...");
        EventLoopGroup workerGroup = new NioEventLoopGroup(nThreads);
        try {
            bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            // 客户端接收到的是 httpResponse 响应, 所以要使用 HttpResponseDecoder 进行解码
                            ch.pipeline().addLast(new HttpResponseDecoder());
                            // 客户端发送的是 httprequest, 所以要使用 HttpRequestEncoder 进行编码
                            ch.pipeline().addLast(new HttpRequestEncoder());
                            // ch.pipeline().addLast(new HttpObjectAggregator(1048576));
                            // ch.pipeline().addLast(new ChunkedWriteHandler());
                            ch.pipeline().addLast(new HttpClientHandler());
                        }
                    });
        } finally {
            // workerGroup.shutdownGracefully();
        }

        List<HttpClient> threads = new ArrayList<>();
        for (int i = 0; i < nThreads; i++) {
            HttpClient httpClient = new HttpClient("HttpClient" + i);
            threads.add(httpClient);
        }
        return threads;
    }

    // 自动重连
    public static void doConnect() throws InterruptedException {
        if (future != null && future.channel() != null && future.channel().isActive()) {
            return;
        }
        future = bootstrap.connect(HOST, PORT).sync();
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    log.info("Connect to http server successfully!");
                } else {
                    // log.info("Failed to connect to server, try connect after 1s.");
                    future.channel().eventLoop().schedule(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                doConnect();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }, 1000, TimeUnit.MILLISECONDS);
                }
            }
        });
    }

    @Override
    public void run() {
        log.info("start success..");
        pullData();
    }

    public static void main(String[] args) throws Exception {
//        HttpClient.init(1);
//        HttpClient.connect("10.66.1.107", 8004);
//        uri = new URI("http://10.66.1.107:8004/trace1.data");
//        query("bytes=0-2000000");
//        query("bytes=14517359-14517659");
//        query("bytes=14517359-14517659");
    }
}
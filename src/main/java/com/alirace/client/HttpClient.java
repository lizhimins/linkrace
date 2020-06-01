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
    protected static final int BYTES_LENGTH = 8192 * 1024; // ByteBuf 最大长度
    protected static final int BUCKETS_NUM = 0x01 << 20; // 100万
    protected static final int WINDOW_SIZE = 20000; // 窗口的大小
    protected static final int BLOCK_SIZE = 8192;
    // 文件的总长度
    protected static long contentLength = 0;

    private static Bootstrap bootstrap;
    private static List<ChannelFuture> futureList;
    // 数据获取地址, 由 CommonController 传入
    private static URI uri;

    private int threadIndex = 0; // 当前线程编号
    private long startOffset = 0;
    private long finishOffset = 0;

    // 数据索引
    private static Bucket[] buckets = new Bucket[BUCKETS_NUM];

    // 窗口
    private int nodeIndex;
    private Node[] nodes = new Node[WINDOW_SIZE];

    public static void initBucket() {
        log.info("Bucket initializing start...");
        for (int i = 0; i < BUCKETS_NUM; i++) {
            buckets[i] = new Bucket();
        }
    }

    // 构造函数, 完成初始化任务
    public HttpClient(String name) {
        super(name);

        log.info("Windows initializing start..." + name);
        for (int i = 0; i < WINDOW_SIZE; i++) {
            nodes[i] = new Node();
        }
    }

    public void pullData() {
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.toASCIIString());
        // 构建http请求
        request.headers().set(HttpHeaderNames.HOST, "localhost");
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderNames.CONNECTION);
        request.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());
        request.headers().set(HttpHeaderNames.RANGE, String.format("bytes=%d-%d", startOffset, finishOffset));
        // 发送http请求
        futureList.get(threadIndex).channel().writeAndFlush(request);
    }

    public static void query(String requestOffset) {
        log.info("requestOffset: " + requestOffset);
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.toASCIIString());
        // 构建http请求
        request.headers().set(HttpHeaderNames.HOST, "localhost");
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderNames.CONNECTION);
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        request.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());
        request.headers().set(HttpHeaderNames.RANGE, requestOffset);
        // 发送http请求
        futureList.get(futureList.size() - 1).channel().writeAndFlush(request);
    }

    public static void setUri(String filePath) throws URISyntaxException {
        HttpClient.uri = new URI(filePath);
    }

    // 初始化实际线程
    public static List<HttpClient> init(int nThreads) {
        log.info("HttpClient initializing start...");
        EventLoopGroup workerGroup = new NioEventLoopGroup(nThreads + 1);
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
            httpClient.setThreadIndex(i);
            threads.add(httpClient);
        }

        futureList = new ArrayList<>(nThreads + 1);
        return threads;
    }

    public static void getFileLength() throws InterruptedException {
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.HEAD, uri.toASCIIString());
        // 构建http请求
        request.headers().set(HttpHeaderNames.HOST, "localhost");
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderNames.CONNECTION);
        request.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());
        futureList.get(futureList.size() - 1).channel().writeAndFlush(request).sync();
    }

    // 自动重连
    public static void doConnect(int nThreads) throws InterruptedException {
        for (int i = 0; i < nThreads + 1; i++) {
//            if (future1 != null && future1.channel() != null && future1.channel().isActive()) {
//                return;
//            }
            ChannelFuture future = bootstrap.connect(HOST, PORT).sync();
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        log.info(String.format("Connect to http server successfully!"));
                    }
                }
            });
            futureList.add(future);
        }
    }

    @Override
    public void run() {
        log.info(String.format("Http Client start pull data, %d-%d", startOffset, finishOffset));
        pullData();
    }

    public static void main(String[] args) throws Exception {

        HttpClient.init(2);
        HttpClient.doConnect(2);
        uri = new URI("http://10.66.1.107:8004/trace1.data");

        HttpClient httpClient1 = new HttpClient("Client");
        httpClient1.setThreadIndex(0);
        httpClient1.setStartOffset(200);
        httpClient1.setFinishOffset(20000);
        httpClient1.pullData();

        HttpClient httpClient2 = new HttpClient("Client");
        httpClient2.setThreadIndex(1);
        httpClient2.setStartOffset(800);
        httpClient1.setFinishOffset(33000);
        httpClient2.pullData();

        query("bytes=0-10000");
//        query("bytes=80001-800000");

//        query("bytes=14517359-14517659");
//        query("bytes=14517359-14517659");
    }

    public long getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(long startOffset) {
        this.startOffset = startOffset;
    }

    public long getFinishOffset() {
        return finishOffset;
    }

    public void setFinishOffset(long finishOffset) {
        this.finishOffset = finishOffset;
    }

    public void setThreadIndex(int threadIndex) {
        this.threadIndex = threadIndex;
    }
}
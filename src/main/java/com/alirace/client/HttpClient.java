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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HttpClient extends Thread {

    private static final Logger log = LoggerFactory.getLogger(HttpClient.class);

    // 通信相关参数配置
    private static final String HOST = "10.66.1.107";
    private static final int PORT = 8004;

    private static Bootstrap bootstrap;
    private static ChannelFuture future;

    // 数据获取地址, 由 CommonController 传入
    private static URI uri;

    public static void query(String requestOffset) throws IOException {
//        log.info("requestOffset: " + requestOffset);
//        DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.toASCIIString());
//        // 构建http请求
//        request.headers().set(HttpHeaderNames.HOST, "localhost");
//        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderNames.CONNECTION);
//        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
//        request.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());
//        request.headers().set(HttpHeaderNames.RANGE, requestOffset);
//        // 发送http请求
//        future.channel().writeAndFlush(request);
        URL url = new URL("http://10.66.1.107:" + "8004" + "/trace1.data");
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        httpConnection.setRequestProperty("range", requestOffset);
        InputStream input = httpConnection.getInputStream();
        BufferedReader bf = new BufferedReader(new InputStreamReader(input));
        String line;
        while ((line = bf.readLine()) != null) {
            if (line.length() == 0 || line.startsWith("\r") || line.startsWith("-") || line.startsWith("C")) {
                continue;
            }
            // log.info(line);
        }
        bf.close();
        input.close();
    }

    public static void setUri(String filePath) throws URISyntaxException {
        HttpClient.uri = new URI(filePath);
    }

    // 初始化实际线程
    public static void init() {
        log.info("HttpClient initializing start...");
        EventLoopGroup workerGroup = new NioEventLoopGroup();
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
                            // ch.pipeline().addLast(new HttpObjectAggregator(65535));
                            // ch.pipeline().addLast(new ChunkedWriteHandler());
                            ch.pipeline().addLast(new HttpClientHandler());
                        }
                    });
        } finally {
            // workerGroup.shutdownGracefully();
        }
    }

    public static void queryFileLength() {
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.HEAD, uri.toASCIIString());
        // 构建http请求
        request.headers().set(HttpHeaderNames.HOST, HOST);
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderNames.CONNECTION);
        request.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());
        future.channel().writeAndFlush(request);
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
                    log.info(String.format("Connect to http server successfully!"));
                } else {
                    doConnect();
                }
            }
        });
    }

    public static void main(String[] args) throws Exception {

        ClientService.contentLength = 1;
        HttpClient.init();
        HttpClient.doConnect();
        uri = new URI("http://10.66.1.107:8004/trace1.data");
        // queryFileLength();
//        query("bytes=0-10000");
//        query("bytes=80001-800000");

        query("bytes=1072465-1072726,1180131-1180422,1233460-1233737,1312064-1312354");
        query("bytes=4280813-4281033,4385440-4385712,4487448-4487683,4536482-4536763,4584726-4584944,4631196-4631477,4675427-4675690,4737167-4737450,4756834-4757072,4776177-4776414,4813880-4814169,4831034-4831282,4848186-4848473,4879205-4879467,4893992-4894287,4908395-4908683,4922117-4922417,4960778-4961057,4971834-4972127,4982869-4983160,4993152-4993415,5003157-5003462,5021321-5021593,5035986-5036291,5047816-5048095");
    }
}
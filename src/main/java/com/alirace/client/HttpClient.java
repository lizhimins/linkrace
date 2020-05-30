package com.alirace.client;


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

public class HttpClient {

    private static final Logger log = LoggerFactory.getLogger(HttpClient.class);

    private static Bootstrap bootstrap;
    private static ChannelFuture future;

    private static URI uri;

    public static void init() throws URISyntaxException {

        uri = new URI("http://10.66.1.107:8004/trace1.data");

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
                            ch.pipeline().addLast(new HttpClientHandler());
                        }
                    });
        } finally {
            // workerGroup.shutdownGracefully();
        }
    }

    public static void connect(String host, int port) throws Exception {
        log.info("HttpClient connect to Nginx...");
        future = bootstrap.connect(host, port).sync();
    }

    public static void query(String requestOffset) {
        log.info("requestOffset: " + requestOffset);
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.toASCIIString());
        // 构建http请求
        request.headers().set(HttpHeaderNames.HOST, "localhost");
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderNames.CONNECTION);
        request.headers().set(HttpHeaderNames.RANGE, requestOffset);
        // 发送http请求
        future.channel().writeAndFlush(request);
    }

    public static void main(String[] args) throws Exception {
        HttpClient.init();
        HttpClient.connect("10.66.1.107", 8004);
        uri = new URI("http://10.66.1.107:8004/trace1.data");
        query("bytes=500-13000");
    }
}
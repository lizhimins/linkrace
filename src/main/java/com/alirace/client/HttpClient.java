package com.alirace.client;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;

import java.net.URI;

public class HttpClient {

    private static Bootstrap bootstrap;
    private static ChannelFuture future;

    private static URI uri;

    public static void init() {
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
                            ch.pipeline().addLast(new HttpClientHandler());
                        }
                    });
        } finally {
            // workerGroup.shutdownGracefully();
        }
    }

    public static void connect(String host, int port) throws Exception {
        future = bootstrap.connect(host, port).sync();
    }

    public static void query(int start, int end) {
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.toASCIIString());
        // 构建http请求
        request.headers().set(HttpHeaderNames.HOST, "localhost");
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderNames.CONNECTION);
        request.headers().set(HttpHeaderNames.RANGE, "bytes=200-2000");
        // 发送http请求
        future.channel().writeAndFlush(request);
    }

    public static void main(String[] args) throws Exception {
        HttpClient.init();
        HttpClient.connect("10.66.1.107", 8004);
        uri = new URI("http://10.66.1.107:8004/trace1.data");
        query(200, 2000);
    }
}
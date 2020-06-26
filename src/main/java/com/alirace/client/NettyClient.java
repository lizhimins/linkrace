package com.alirace.client;

import com.alirace.constant.Constant;
import com.alirace.model.Message;
import com.alirace.model.MessageType;
import com.alirace.netty.MyDecoder;
import com.alirace.netty.MyEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.rmi.runtime.Log;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import static com.alirace.client.ClientMonitor.responseCount;
import static com.alirace.client.ClientMonitor.uploadCount;

public class NettyClient {

    private static final Logger log = LoggerFactory.getLogger(NettyClient.class);

    // 通信相关参数配置
    private static final String HOST = "localhost";
    private static final int PORT = 8003;
    private static EventLoopGroup workerGroup;
    private static Bootstrap bootstrap;
    private static ChannelFuture future;

    // 上传调用链
    public static void upload(byte[] body) {
        uploadCount.incrementAndGet();
//        StringBuffer buffer = new StringBuffer(16);
//        for (int i = 0; i <16; i++) {
//            if (body[i] == (byte) '|') {
//                break;
//            }
//            buffer.append((char) (int) body[i]);
//        }
//        String traceId = buffer.toString();
        // log.info("SEND QUERY: " + traceId.toString());

        // log.info(new String(body));
//        String[] split = new String(body).split("\n");
//        boolean flag = true;
//        StringBuffer sb = new StringBuffer();
//        for (int i = 0; i < split.length; i++) {
//            long pre = 0L;
//            String traceId = split[i];
//            if (traceId != null) {
//                String[] seg = traceId.split("\\|");
//                // System.out.print(seg[1] + " ");
//                sb.append(seg[1]);
//                sb.append(" ");
//
//                if (Long.parseLong(seg[1]) <= pre) {
//                    flag = false;
//                }
//
//                pre = Long.parseLong(seg[1]);
//            }
//
//        }
//        if (!flag) {
//            log.info("NO " + sb.toString());
//        }

        // log.info("UPLOAD -> " + new String(body));
        Message message = new Message(MessageType.UPLOAD.getValue(), body);
        // future.channel().writeAndFlush(message);
    }

    // 查询响应
    public static void response(byte[] body) {
        responseCount.incrementAndGet();
        Message message = new Message(MessageType.RESPONSE.getValue(), body);
//        log.info("RESPONSE -> " + new String(body));
        future.channel().writeAndFlush(message);
    }

    // 发送自己的进度
    public static void sendSync(long syncValue) {
        Message message = new Message(MessageType.SYNC.getValue(), String.valueOf(syncValue).getBytes());
        // log.info(String.format("SEND: %x", syncValue));
        future.channel().writeAndFlush(message);
    }

    // 结束
    public static void finish(byte[] body) {
        Message message = new Message(MessageType.FINISH.getValue(), body);
        future.channel().writeAndFlush(message);
    }

    // 结束
    public static void done(byte[] body) {
        Message message = new Message(MessageType.DONE.getValue(), body);
        future.channel().writeAndFlush(message);
    }

    // 启动 netty 进行通信服务
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
}

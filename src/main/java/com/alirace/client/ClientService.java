package com.alirace.client;

import com.alirace.model.Message;
import com.alirace.model.MessageType;
import com.alirace.model.Record;
import com.alirace.netty.MyDecoder;
import com.alirace.netty.MyEncoder;
import com.alirace.server.ServerService;
import com.alirace.util.SerializeUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import static com.alirace.client.ClientMonitor.*;

public class ClientService implements Runnable {

    // 服务器相关信息
    public static final String host = "localhost";
    public static final int port = 8003;
    // 每隔多少条上报消费状态, 用来同步消费进度命中更多的缓存
    public static final int READ_FILE_GAP = 10000;
    // 为了尽快消费, 设置两台机器之间同步时间差的阈值, 单位是纳秒, 默认30秒, 越大越快命中率低
    public static final long TIMESTAMP_SYNC_THRESHOLD = 60 * 1000 * 1000L;
    private static final Logger log = LoggerFactory.getLogger(ServerService.class);
    public static int SERVICE_NUM = 2;
    public static List<CacheService> services = new ArrayList<>();
    // Netty 相关配置
    public static EventLoopGroup workerGroup;
    public static Bootstrap bootstrap;
    public static ChannelFuture future;
    // 进过过滤服务的日志总量, 单线程调用
    public static long logOffset = 0L;
    public static LinkedBlockingQueue<Message> uploadQueue = new LinkedBlockingQueue<>();

    public static Thread pullService;

    // 监听等待池
    public static ConcurrentHashMap<String, AtomicBoolean> waitMap = new ConcurrentHashMap<>();

    public static void queryRecord(String traceId) throws InterruptedException {
        queryCount.incrementAndGet();
        // response(1);
        // 已经主动上传过了
        AtomicBoolean flag = waitMap.get(traceId);
        if (flag != null && flag.get()) {
            response(1);
            return;
        }
        // 去等待区进行锁定
        flag = waitMap.putIfAbsent(traceId, new AtomicBoolean(false));
        if (flag == null) {
            // 锁定成功, 计算在哪个队列
            int index = traceId.charAt(1) % SERVICE_NUM;
            // 获得引用
            Record record = services.get(index).queryCache.getIfPresent(traceId);
            // 如果找到了并且改写结果
            if (record != null && flag.compareAndSet(false, true)) {
                // passRecord(record);
                response(1);
            }
        }
    }

    // 上传读入进度
    public static void uploadStatus(long logOffset) throws InterruptedException {
        Message message = new Message(MessageType.STATUS.getValue(), String.valueOf(logOffset).getBytes());
        future.channel().writeAndFlush(message);
    }

    // 上传调用链
    public static void uploadRecord(Record record) throws InterruptedException {
        uploadCount.incrementAndGet();
        byte[] body = SerializeUtil.serialize(record);
        Message message = new Message(MessageType.UPLOAD.getValue(), body);
        future.channel().writeAndFlush(message);
    }

    // 查询结果都用这个上报
    public static void passRecord(Record record) throws InterruptedException {
        passCount.incrementAndGet();
        byte[] body = SerializeUtil.serialize(record);
        Message message = new Message(MessageType.PASS.getValue(), body);
        future.channel().writeAndFlush(message);
    }

    // 查询响应
    public static void response(int num) throws InterruptedException {
        responseCount.incrementAndGet();
        byte[] body = String.valueOf(num).getBytes();
        Message message = new Message(MessageType.RESPONSE.getValue(), body);
        future.channel().writeAndFlush(message);
    }

    // 读入完成
    public static void finish() throws InterruptedException {
        byte[] body = "finish".getBytes();
        Message message = new Message(MessageType.FINISH.getValue(), body);
        future.channel().writeAndFlush(message);
    }

    public static void start() throws InterruptedException {
        log.info("Client initializing start...");
        ClientMonitor.start();
        for (int i = 0; i < SERVICE_NUM; i++) {
            CacheService cacheService = new CacheService();
            cacheService.start();
            services.add(cacheService);
        }
        Thread thread = new Thread(new ClientService(), "ClientService");
        thread.start();
        pullService = new Thread(new PullService(), "PullService");
    }

    public static void startNetty() throws InterruptedException {
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
    public static void doConnect() throws InterruptedException {
        if (future != null && future.channel() != null && future.channel().isActive()) {
            return;
        }
        future = bootstrap.connect(host, port);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    Channel channel = future.channel();
                    channel.writeAndFlush(new Message(MessageType.STATUS.getValue(), "OK".getBytes())).sync();
                    log.info("Connect to server successfully!");
                    // CommonController.isReady.set(true);
                } else {
                    // log.info("Failed to connect to server, try connect after 0ms");
                    future.channel().eventLoop().schedule(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                doConnect();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }, 0, TimeUnit.MILLISECONDS);
                }
            }
        });
    }

    @Override
    public void run() {
        try {
            startNetty();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        while (true) {
//            Message message = uploadQueue.poll();
//            if (message != null) {
//                future.channel().writeAndFlush(message);
//            } else {
//                try {
//                    TimeUnit.MILLISECONDS.sleep(1);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
    }
}

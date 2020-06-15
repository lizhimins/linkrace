package com.alirace.client;

import com.alirace.model.Message;
import com.alirace.model.MessageType;
import com.alirace.netty.MyDecoder;
import com.alirace.netty.MyEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledHeapByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alirace.client.ClientMonitor.*;

public class ClientService extends Thread {

    private static final Logger log = LoggerFactory.getLogger(ClientService.class);

    protected static final int nThreads = 1;
    public static List<ClientService> services;

    // 通信相关参数配置
    private static final String HOST = "localhost";
    private static final int PORT = 8003;
    private static URL url;

    // 和汇总服务器 通信 Netty 相关配置
    private static EventLoopGroup workerGroup;
    private static Bootstrap bootstrap;
    private static ChannelFuture future;

    // 数据获取地址, 由 CommonController 传入
    private static String path;

    // 常量
    private static final byte LOG_SEPARATOR = (byte) '|';
    private static final byte LINE_SEPARATOR = (byte) '\n';
    private static final int LENGTH_PER_READ = 1024 * 1024; // 每一次读 1M 2.8秒

    // 控制偏移量
    protected long startOffset = -1L;
    protected long finishOffset = -1L;
    public int threadId = -1;

    private int preOffset = 0; // 起始偏移
    private int nowOffset = 0; // 当前偏移
    private int logOffset = 0; // 日志偏移

    private static final int BYTES_LENGTH = 512 * 1024 * 1024;
    public byte[] bytes;

    // 找到对应的行号, 低 32 字节保存 hashcode, 高 32 字节保存行号
    private static final int BUCKETS_NUM = 0x01 << 20; // 100万
    private static final int TRACE_ID_HASH_CODE_AND_LINE_NUM = 16; // 每行32
    private static AtomicInteger lineIndex = new AtomicInteger(-1);
    private static int[] bucketElements = new int[BUCKETS_NUM];
    private static long[][] buckets = new long[BUCKETS_NUM][16];

    // 行结构
    private static long[][] offset;

    // 滑动窗口, 大小配置为 2万
    private static final int WINDOW_SIZE = 20000;
    private int windowIndex = 0;
    private long[] window = new long[WINDOW_SIZE];

    // 保存 traceId
    private int bucketIndex = 0;
    private int hash = 0;

    private StringBuffer sb = new StringBuffer();

    // 构造函数
    public ClientService(String name) {
        super(name);
        bytes = new byte[BYTES_LENGTH + 2 * LENGTH_PER_READ];
        for (int i = 0; i < WINDOW_SIZE; i++) {
            window[i] = -1L; // 0x11111111
        }
    }

    // 获得所在行的编号, 注意该方法会自动推进 nowOffset
    public int queryLineIndex() throws Exception {
//        StringBuffer sb = new StringBuffer(16);
//        for (int i = nowOffset; i < 16 + nowOffset; i++) {
//            sb.append((char) (int) bytes[i]);
//        }
//        log.info(sb.toString());


        // System.out.println((char) (int) bytes[nowOffset]);
        int i = 0;
        byte b;

        // 根据前几位计算桶的编号
        bucketIndex = 0;
        while (i < 5) {
            b = bytes[nowOffset++];
            if ('0' <= b && b <= '9') {
                bucketIndex = bucketIndex * 16 + ((int) b - '0');
            } else {
                bucketIndex = bucketIndex * 16 + ((int) b - 'a') + 10;
            }
            i++;
        }

        hash = 0;
        while (i < 5 + 8) {
            b = bytes[nowOffset++];
            if ('0' <= b && b <= '9') {
                hash = hash * 16 + ((int) b - '0');
            } else {
                hash = hash * 16 + ((int) b - 'a') + 10;
            }
            i++;
        }

        // 处理到尾巴
        while ((b = bytes[nowOffset]) != LOG_SEPARATOR) {
            nowOffset++;
        }

        // log.info(String.format("bucketIndex: %6d, hashCode: %6d", bucketIndex, hash));
        // hash = buckets[bucketIndex].getRecord(threadId, hash);

        int elements = bucketElements[bucketIndex];
        for (int j = 0; j < elements; j++) {
            long value = buckets[bucketIndex][j];
            if (hash == (int) value) {
                // log.info(String.format("bucketIndex: %6d, hashCode: %6d, ele: %6d", bucketIndex, hash, elements));
                return (int) (value >> 32);
            }
        }

        int line = lineIndex.incrementAndGet();
        // log.info(String.format("bucketIndex: %6d, hashCode: %6d, ele: %6d", bucketIndex, hash, elements));

        buckets[bucketIndex][elements] = ((long) hash & 0xFFFFFFFFL) | (((long) line << 32) & 0xFFFFFFFF00000000L);
        bucketElements[bucketIndex]++;

        return line;
    }

    public boolean checkTag() {

//        System.out.println((char) (int) bytes[nowOffset - 4]);
//        System.out.println((char) (int) bytes[nowOffset - 3]);
//        System.out.println((char) (int) bytes[nowOffset - 2]);
//        System.out.println((char) (int) bytes[nowOffset - 1]);

        // http.status_code=200\n
        if (   bytes[nowOffset - 9] == (byte) (int) '_'
                && bytes[nowOffset - 4] == (byte) (int) '=') {
            if (!(bytes[nowOffset - 3] == (byte) (int) '2'
                    && bytes[nowOffset - 2] == (byte) (int) '0'
                    && bytes[nowOffset - 1] == (byte) (int) '0')
            ) {
                return true;
            }
        }

        // error=1\n
        if (   bytes[nowOffset - 4] == (byte) (int) 'o'
                && bytes[nowOffset - 3] == (byte) (int) 'r'
                && bytes[nowOffset - 2] == (byte) (int) '='
                && bytes[nowOffset - 1] == (byte) (int) '1'
        ) {
            return true;
        }
        return false;
    }

    public void scanData() throws Exception {
        preOffset = nowOffset;

        // traceId
        int lineId = queryLineIndex();

        while (bytes[nowOffset] != LOG_SEPARATOR) {
            nowOffset++;
        }

        // 处理时间戳, spanId
        nowOffset += 70;

        // 处理到末尾
        while (bytes[nowOffset] != LINE_SEPARATOR) {
            nowOffset++;
        }
        // log.info(preOffset + "-" + nowOffset);

        // 保存到同一个 long 上
        // offset 数组的第一个格子, 高位保存状态, 低位保存数据条数
        // 状态: 0000 0000 0000 0000 0000 0000 000error 000done
        offset[lineId][0]++; // 数量 +1

        // 如果数据包含错误统计 +1
        if (checkTag()) {
            errorCount.incrementAndGet();
            offset[lineId][0] |= (0x1L << 36);
        }

        long firstElement = offset[lineId][0];
        int spanNum = (int) firstElement;
        // 保存偏移量
        offset[lineId][spanNum] = (((long) preOffset << 32) & 0xFFFFFFFF00000000L) | ((long) nowOffset & 0xFFFFFFFFL);
        // log.info(lineId + "|" + spanNum + "|" + tmp + "|" + offset[lineId][0]);

        // 窗口操作, 当前写 nodeIndex
        // 取出2w记录之前的数据
        // 高位存行号 低位存最大偏移
        long val = window[windowIndex];
        // 如果已经有数据了
        if (val != -1L) {
            int high = (int) (val >> 32);
            int maxOffset = (int) val;
            int length = (int) offset[high][0];
            if (maxOffset == length) {
                queryAndUpload(high);
                // log.info("equal: " + high + " " + maxOffset + " " + length + " " + offset[high][length]);
            }
            // log.info(high + " " + maxOffset + " " + length + " " + offset[high][length] + " " + val);
        }
        // 循环覆盖写
        val = (((long) lineId) << 32) + (long) spanNum;
        window[windowIndex] = val;
        windowIndex = (windowIndex + 1) % WINDOW_SIZE;

        // 最后一个符号是 \n
        nowOffset++;
    }

    // BIO 读取数据
    public void pullData() throws Exception {
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
//        long start = startOffset != 0 ? startOffset - 3000_0000 : startOffset;
//        long finish = startOffset == 0 ? finishOffset + 3000_0000 : finishOffset;
//        long total = finish - start;
//        long deal = 0;
//        log.info(String.format("Start receive file: %10d-%10d, Data path: %s", start, finish, path));
        log.info(String.format("Start receive file: %10d-%10d, Data path: %s", startOffset, finishOffset, path));
        String range = String.format("bytes=%d-%d", startOffset, finishOffset);
        httpConnection.setRequestProperty("range", range);
        InputStream input = httpConnection.getInputStream();

        // 初始化左右指针, 空出第一小段数据以备复制
        preOffset = LENGTH_PER_READ;
        nowOffset = LENGTH_PER_READ;
        logOffset = LENGTH_PER_READ;

        // 读入一小段数据
        int readByteCount = input.read(bytes, logOffset, LENGTH_PER_READ);
        logOffset += readByteCount;

        while (true) {
            // 读入一小段数据
            readByteCount = input.read(bytes, logOffset, LENGTH_PER_READ);

            // 文件结束退出
            if (readByteCount == -1) {
                while (nowOffset < logOffset) {
                    scanData();
                }
                break;
            }

            // 日志坐标右移
            logOffset += readByteCount;

            // 如果不是处理第一段数据的线程, 就会有半包问题, 这时候跳过最前面的半条日志
            if (startOffset != 0) {
                while (bytes[nowOffset] != LINE_SEPARATOR) {
                    nowOffset++;
                }
                nowOffset++;
            }

            // 循环处理所有数据
            while (nowOffset + LENGTH_PER_READ < logOffset) {
                scanData();
            }

            // 如果太长了要从头开始写
            if (nowOffset >= BYTES_LENGTH + LENGTH_PER_READ) {
                // 拷贝末尾的数据
                for (int i = nowOffset; i < logOffset; i++) {
                    bytes[i - BYTES_LENGTH] = bytes[i];
                }
                nowOffset -= BYTES_LENGTH;
                logOffset -= BYTES_LENGTH;
                // log.info("rewrite");
            }
        }

        for (int i = windowIndex; i < windowIndex + WINDOW_SIZE; i++) {
            int now = i % 20000;
            long val = window[now];
            int high = (int) (val >> 32);
            int maxOffset = (int) val;
            int length = (int) offset[high][0];
            if (maxOffset == length) {
                queryAndUpload(high);
                // log.info("equal: " + high + " " + maxOffset + " " + length + " " + offset[high][length]);
            }
        }
        log.info("Client pull data finish...");
    }

    // 查询
    public void queryAndUpload(int lineId) {
        // 状态: 0000 0000 0000 0000 0000 0000 000error 000done
        offset[lineId][0] |= (0x001L << 32);
        long status = offset[lineId][0];
        int high = (int) (status >> 32); // 状态
        int low = (int) status; // 数据条数
        // log.info(String.format("%h", status));
        if ((high & 0x10) == 0x10) {
            offset[lineId][0] |= (0x010L << 32); // 标记上传过了
            int length = 0;
            for (int i = 1; i <= low; i++) {
                long value = offset[lineId][i];
                int start = (int) (value >> 32);
                int end = (int) value;
                length += end - start + 1;
            }
            ByteBuf body = Unpooled.buffer(length, length);
            for (int i = 1; i <= low; i++) {
                long value = offset[lineId][i];
                int start = (int) (value >> 32);
                int end = (int) value;
                body.writeBytes(bytes, start, end - start + 1);
            }
            upload(body.array());
            // log.info(new String(body.array()));
            body.release();
        }
    }

    public void queryAndResponse(byte[] traceId) {
        queryCount.incrementAndGet();
        log.info(new String(traceId));

        // 根据前几位计算桶的编号
        int bucketIndex1 = 0;
        for (int i = 0; i < 5; i++) {
            byte b = traceId[i];
            if ('0' <= b && b <= '9') {
                bucketIndex1 = bucketIndex1 * 16 + ((int) b - '0');
            } else {
                bucketIndex1 = bucketIndex1 * 16 + ((int) b - 'a') + 10;
            }
        }

        int hashCode = 0;
        for (int i = 5; i < 13; i++) {
            byte b = traceId[i];
            if ('0' <= b && b <= '9') {
                hashCode = hashCode * 16 + ((int) b - '0');
            } else {
                hashCode = hashCode * 16 + ((int) b - 'a') + 10;
            }
        }

        // log.info(String.format("bucketIndex: %6d, hashCode: %6d", bucketIndex1, hashCode));
        // hash = buckets[bucketIndex].getRecord(threadId, hash);

        int lineId = -2;
        int elements = bucketElements[bucketIndex1];
        for (int j = 0; j < elements; j++) {
            long value = buckets[bucketIndex][j];
            if (hashCode == (int) value) {
                lineId = (int) (value >> 32);
            }
        }

        // 状态: 0000 0000 0000 0000 0000 0000 000error 000done
        // 如果找到行
        if (lineId != -2) {
            // 没有结束
            long tmp = offset[lineId][0];
            if ((tmp & (0x1L << 36)) == (0x1L << 36)) {
                offset[lineId][0] |= (0x1L << 40); // 错误标记
                responseCount.incrementAndGet();
            } else {
                // 已经结束了
                int low = (int) tmp; // 数据条数
                // log.info(String.format("%h", status));
                int length = 0;
                for (int i = 1; i <= low; i++) {
                    long value = offset[lineId][i];
                    int start = (int) (value >> 32);
                    int end = (int) value;
                    length += end - start;
                }
                ByteBuf body = Unpooled.buffer(length);
                for (int i = 1; i <= low; i++) {
                    long value = offset[lineId][i];
                    int start = (int) (value >> 32);
                    int end = (int) value;
                    body.writeBytes(bytes, start, end - start + 1);
                }
                response(body.array());
                // log.info(new String(body.array()));
                body.release();
                responseCount.incrementAndGet();
            }
        } else {
            // 没找到行就新建行
            lineId = lineIndex.incrementAndGet();
            responseCount.incrementAndGet();
            offset[lineId][0] |= (0x1L << 40);
        }
    }

    // 上传调用链
    public static void upload(byte[] bytes) {
        uploadCount.incrementAndGet();
        Message message = new Message(MessageType.UPLOAD.getValue(), bytes);
        future.channel().writeAndFlush(message);
    }

    // 正确同步
    public static void pass(byte[] bytes) {
        uploadCount.incrementAndGet();
        Message message = new Message(MessageType.PASS.getValue(), bytes);
        future.channel().writeAndFlush(message);
    }

    // 查询响应
    public static void response(byte[] bytes) {
        Message message = new Message(MessageType.RESPONSE.getValue(), bytes);
        future.channel().writeAndFlush(message);
    }

    public static void setOffsetAndRun(long length) {
        long blockSize = length / nThreads;
        log.info(HttpHeaderNames.CONTENT_LENGTH.toString() + ": " + length + ", " + blockSize);
        for (int i = 0; i < nThreads; i++) {
            ClientService service = services.get(i);
            service.threadId = i;
            service.startOffset = i * blockSize;
            service.finishOffset = (i + 1) * blockSize - 1;
            if (i == nThreads - 1) {
                service.finishOffset = length - 1;
            }
            service.start();
        }
    }

    // 只初始化一次
    public static void init() throws Exception {
        log.info("Client initializing start...");

        // 监控服务
        ClientMonitor.start();

        services = new ArrayList<>(nThreads);
        for (int i = 0; i < nThreads; i++) {
            ClientService clientService = new ClientService(String.format("Client%d", i));
            services.add(clientService);
        }

        offset = new long[BUCKETS_NUM][108];

        // 在最后启动 netty 进行通信
        startNetty();
    }

    public static void setPathAndPull(String path) throws IOException {
        ClientService.path = path;
        url = new URL(path);
        setOffsetAndRun(queryFileLength());
    }

    // 获得文件大小
    public static long queryFileLength() throws IOException {
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        httpConnection.setRequestMethod("HEAD");
        // Map<String, List<String>> headerFields = httpConnection.getHeaderFields();
        // Iterator iterator = headerFields.keySet().iterator();
        // while (iterator.hasNext()) {
        //     String key = (String) iterator.next();
        //     List values = headerFields.get(key);
        //     log.info(key + ":" + values.toString());
        // }
        long contentLength = httpConnection.getContentLengthLong();
        httpConnection.disconnect();
        return contentLength;
    }

    @Override
    public void run() {
        try {
            pullData();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
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
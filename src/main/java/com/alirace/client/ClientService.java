package com.alirace.client;

import com.alirace.model.Message;
import com.alirace.model.MessageType;
import com.alirace.netty.MyDecoder;
import com.alirace.netty.MyEncoder;
import com.alirace.util.NumberUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alirace.client.ClientMonitor.*;

public class ClientService extends Thread {

    private static final Logger log = LoggerFactory.getLogger(ClientService.class);

    // 实际工作线程
    private int threadId;
    private static final int nThreads = 1;
    private static ClientService[] services = new ClientService[nThreads];

    // 通信相关参数配置
    private static final String HOST = "localhost";
    private static final int PORT = 8003;
    private static URL url;
    private static EventLoopGroup workerGroup;
    private static Bootstrap bootstrap;
    private static ChannelFuture future;

    // 常量
    private static final byte LOG_SEPARATOR = (byte) '|';
    private static final byte LINE_SEPARATOR = (byte) '\n';
    private static final int LENGTH_PER_READ = 1024 * 1024; // 每一次读 1M 2.8秒

    // 数据获取地址, 由 CommonController 传入
    private static String path;

    // 控制偏移量
    private long startOffset = -1L;
    private long finishOffset = -1L;

    // 机器同步算法, 防止快机太快
    public int readBlockTimes = 0;
    public int otherBlockTimes = 0;

    private int preOffset = 0; // 起始偏移 左指针
    private int nowOffset = 0; // 当前偏移
    private int logOffset = 0; // 日志偏移 右指针

    private static final int BYTES_LENGTH = 512 * 1024 * 1024;
    private byte[] bytes;

    // HashMap, 低 32 字节保存 hashcode, 高 32 字节保存行号, 单例
    private static final int BUCKET_NUM = 0x1 << 20;
    private static final int BUCKET_CAP = 16;
    private static int[] bucketStatus = new int[BUCKET_NUM];
    private static long[][] buckets = new long[BUCKET_NUM][BUCKET_CAP];

    // 偏移表, 单例
    private static final int OFFSET_NUM = 120_0000;
    private static final int OFFSET_CAP = 108;
    private static long[] offsetStatus = new long[OFFSET_NUM];;
    private static long[][] offset = new long[OFFSET_NUM][OFFSET_CAP];;
    private static AtomicInteger maxLineIndex = new AtomicInteger(-1); // 行号原子增加

    // 滑动窗口, 大小配置为 2万
    private static final int WINDOW_SIZE = 20000;
    private int windowIndex = 0;
    private long[] window = new long[WINDOW_SIZE];

    // 等待表
    public static ConcurrentHashMap<Integer, byte[]> queryArea = new ConcurrentHashMap<>();

    // 构造函数
    public ClientService(String name) {
        super(name);
        log.info("Init byte buffer successfully!");
        bytes = new byte[BYTES_LENGTH + 4 * LENGTH_PER_READ];
        log.info("Init window successfully!");
        for (int i = 0; i < WINDOW_SIZE; i++) {
            window[i] = -1L;
        }
    }

    // 获得所在行的编号, 若不存在则会创建新的行注意该方法会自动推进 nowOffset
    private int queryLineIndex() {
        // 根据前几位计算桶的编号
        int bucketIndex = 0;
        for (int i = 0; i < 5; i++) {
            byte b = bytes[nowOffset++];
            if ('0' <= b && b <= '9') {
                bucketIndex = bucketIndex * 16 + ((int) b - '0');
            } else {
                bucketIndex = bucketIndex * 16 + ((int) b - 'a') + 10;
            }
        }

        int hash = 0;
        for (int i = 5; i < 13; i++) {
            byte b = bytes[nowOffset++];
            if ('0' <= b && b <= '9') {
                hash = hash * 16 + ((int) b - '0');
            } else {
                hash = hash * 16 + ((int) b - 'a') + 10;
            }
        }

        // 处理到尾巴
        while (bytes[nowOffset] != LOG_SEPARATOR) {
            nowOffset++;
        }

        // log.info(String.format("bucketIndex: %6d, hashCode: %6d", bucketIndex, hash));

        // 检测是否已经出现过
        int depth = bucketStatus[bucketIndex];
        for (int i = 0; i < depth; i++) {
            long value = buckets[bucketIndex][i];
            if (hash == (int) value) {
                // log.info(String.format("bucketIndex: %6d, hashCode: %6d, ele: %6d", bucketIndex, hash, elements));
                return (int) (value >> 32);
            }
        }

        // 没有出现过就新建行
        int line = maxLineIndex.incrementAndGet();
        buckets[bucketIndex][depth] = NumberUtil.combineInt2Long(line, hash);
        bucketStatus[bucketIndex]++;
        return line;
    }

    // 检查标签中是否包含错误
    private boolean checkTags() {
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

    // 处理1条日志需要调用一次这个函数
    private void handleSpan() throws Exception {
        // 左指针 = 当前指针
        preOffset = nowOffset;

        // 查询行号: traceId -> nowLine
        int nowLine = queryLineIndex();

        // 跳过一大段数据
        nowOffset += 110;

        // 处理到末尾
        while (bytes[nowOffset] != LINE_SEPARATOR) {
            nowOffset++;
        }
        // log.info(preOffset + "-" + nowOffset);

        // 检查错误
        if (checkTags()) {
            errorCount.incrementAndGet(); // 错误数量 + 1
            offsetStatus[nowLine] |= (0x1L << 32); // 错误打标
        }

        long status = offsetStatus[nowLine];
        offset[nowLine][(int) status] = NumberUtil.combineInt2Long(preOffset, nowOffset);
        // log.info(lineId + "|" + spanNum + "|" + tmp + "|" + offset[lineId][0]);

        // 保存到同一个 long 上
        // offset 数组的第一个格子, 高位保存状态, 低位保存数据条数
        offsetStatus[nowLine]++;

        // 滑动窗口
        long data = window[windowIndex];
        if (data != -1L) {
            int preLineId = (int) (data >> 32);
            int preLength = (int) data;
            int nowlength = (int) offsetStatus[preLineId];
            if (preLength == nowlength) {
                queryAndUpload(preLineId);
            }
        }
        // 循环覆盖写
        window[windowIndex] = NumberUtil.combineInt2Long(nowLine, (int) (offsetStatus[nowLine]));
        windowIndex = (windowIndex + 1) % WINDOW_SIZE;

        // 最后一个符号是 \n
        nowOffset++;
    }

    public void syncBlock() throws InterruptedException {
        // 发送自己的进度
        Message message = new Message(MessageType.WAIT.getValue(), String.valueOf(readBlockTimes).getBytes());
        future.channel().writeAndFlush(message);
        // log.info(String.format("SELF: %d, OTHER: %d", readBlockTimes, otherBlockTimes));
        readBlockTimes++;
        while (readBlockTimes - otherBlockTimes > 256) {
            TimeUnit.MILLISECONDS.sleep(1);
        }
    }

    public static void setWait(int other) {
        services[0].otherBlockTimes = other;
        // log.info(String.format("SELF: %d OTHER: %d", services[0].readBlockTimes, other));
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
            syncBlock();

            // 文件结束退出
            if (readByteCount == -1) {
                while (nowOffset < logOffset) {
                    handleSpan();
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
                handleSpan();
            }

            // 如果太长了要从头开始写
            if (nowOffset >= BYTES_LENGTH + LENGTH_PER_READ) {
                // 拷贝末尾的数据
                for (int i = nowOffset; i <= logOffset; i++) {
                    bytes[i - BYTES_LENGTH] = bytes[i];
                }
                nowOffset -= BYTES_LENGTH;
                logOffset -= BYTES_LENGTH;
                // log.info("rewrite");
            }
        }

        for (int i = windowIndex; i < windowIndex + WINDOW_SIZE; i++) {
            int now = i % 20000;
            int preLineId = (int) (window[now] >> 32);
            int preLength = (int) window[now];
            int nowlength = (int) offsetStatus[preLineId];
            if (preLength == nowlength) {
                queryAndUpload(preLineId);
            }
        }
        log.info("Client pull data finish...");
    }

    // 查询
    public void queryAndUpload(int lineId) {
        // 获得这一行最新的状态 0x1 错误 0x10000 表示结束
        int status = (int) (offsetStatus[lineId] >> 32);
        int total = (int) (offsetStatus[lineId]); // 数据条数
        // log.info(String.format("%h", status));

        // 有错误
        if (status != 0) {
            int length = 0;
            for (int i = 0; i < total; i++) {
                long spanOffset = offset[lineId][i];
                length += (int) spanOffset - (int) (spanOffset >> 32) + 1;
            }

            int index = 0;
            byte[] body = new byte[length];
            for (int i = 0; i < total; i++) {
                long spanOffset = offset[lineId][i];
                int start = (int) (spanOffset >> 32);
                int finish = (int) spanOffset;
                for (int j = start; j <= finish; j++) {
                    body[index] = bytes[j];
                    index++;
                }
            }

            if (status == 0x00000001) {
                upload(body);
            }

            if (status == 0x00000100) {
                response(body);
                queryArea.remove(lineId);
            }

            if (status == 0x00000101) {
                response(body);
                queryArea.remove(lineId);
                // response("\r".getBytes());
            }
            // log.info(new String(body));
        }

        offsetStatus[lineId] |= (0x1L << 48); // 标记结束
    }

    public static void queryOrSetFlag(byte[] traceId) {
        services[0].queryAndSetFlag(traceId);
    }

    public void queryAndSetFlag(byte[] traceId) {
        // 根据前几位计算桶的编号
        int bucketIndex = 0;
        for (int i = 0; i < 5; i++) {
            byte b = traceId[i];
            if ('0' <= b && b <= '9') {
                bucketIndex = bucketIndex * 16 + ((int) b - '0');
            } else {
                bucketIndex = bucketIndex * 16 + ((int) b - 'a') + 10;
            }
        }

        int hash = 0;
        for (int i = 5; i < 13; i++) {
            byte b = traceId[i];
            if ('0' <= b && b <= '9') {
                hash = hash * 16 + ((int) b - '0');
            } else {
                hash = hash * 16 + ((int) b - 'a') + 10;
            }
        }

        // 检测是否已经出现过
        int lineId = -1;
        int depth = bucketStatus[bucketIndex];
        for (int i = 0; i < depth; i++) {
            long value = buckets[bucketIndex][i];
            if (hash == (int) value) {
                lineId = (int) (value >> 32);
            }
        }

        if (lineId == -1) {
            // log.info(new String(traceId) + " non-existent");
            lineId = maxLineIndex.incrementAndGet(); // 新行
            buckets[bucketIndex][depth] = NumberUtil.combineInt2Long(lineId, hash); // 保存 traceId
            bucketStatus[bucketIndex]++; // hash 数量+1
            offsetStatus[lineId] |= ((0x1L) << 40); // 标记为需要被动上传
            queryArea.put(lineId, traceId);
        } else {
            // log.info(new String(traceId) + " existent");
            // offsetStatus[lineId] |= ((0x1L) << 40); // 标记为需要被动上传
            tryResponse(traceId, lineId);
        }

//        log.info(new String(traceId) + " "
//                + String.format("bucketIndex: %6d, hashCode: %6d", bucketIndex, hash)
//                + String.format(" %s", " " + lineId));
    }

    public static void print() {
        for (int i = 0; i <= maxLineIndex.get(); i++) {
            if ((int) (offsetStatus[i] >> 48) != 1) {
                log.info(String.format("%x %d", offsetStatus[i], i));
            }
        }
    }
    // 查询
    public void tryResponse(byte[] traceId, int lineId) {
        findCount.incrementAndGet();
        // 获得这一行最新的状态 0x1 错误 0x10000 表示结束
        int status = (int) (offsetStatus[lineId] >> 32);
        int total = (int) (offsetStatus[lineId]); // 数据条数
        // log.info(String.format("%h", status));
        // log.info(new String(traceId) + " " + String.format(" %x", status));
        // 结束但没有上传, 进入条件是
        if (status == 0x00010000) {
            int length = 0;
            for (int i = 0; i < total; i++) {
                long spanOffset = offset[lineId][i];
                length += (int) spanOffset - (int) (spanOffset >> 32) + 1;
            }

            boolean isComplete = true;
            int index = 0;
            byte[] body = new byte[length];
            for (int i = 0; i < total; i++) {
                long spanOffset = offset[lineId][i];
                int start = (int) (spanOffset >> 32);
                int finish = (int) spanOffset;
                for (int j = 0; j < 12; j++) {
                    if (traceId[j] != bytes[start + j]) {
                        isComplete = false; break;
                    }
                }
                for (int j = start; j <= finish; j++) {
                    body[index] = bytes[j];
                    index++;
                }
            }

            // TODO: 校验数据, 校验不通过直接丢弃
            if (isComplete) {
                response(body);
            } else {
                response("\n".getBytes());
                log.error("DROP DATA: " + new String(traceId));
                // log.info(new String(body));
            }
            // log.info(new String(body));
        } else {
            // 还没结束的话只需要标记一下有错误就可以了
            offsetStatus[lineId] |= ((0x1L) << 40); // 标记为第二种错误
        }
    }

    // 上传调用链
    public static void upload(byte[] body) {
        uploadCount.incrementAndGet();

        StringBuffer buffer = new StringBuffer(16);
        for (int i = 0; i <16; i++) {
            if (body[i] == (byte) '|') {
                break;
            }
            buffer.append((char) (int) body[i]);
        }
        String traceId = buffer.toString();
        // log.info("SEND QUERY: " + traceId.toString());

        Message message = new Message(MessageType.UPLOAD.getValue(), body);
        future.channel().writeAndFlush(message);
    }

    // 查询响应
    public static void response(byte[] body) {
        responseCount.incrementAndGet();
        Message message = new Message(MessageType.RESPONSE.getValue(), body);
        future.channel().writeAndFlush(message);
    }

    // 结束
    public static void done(byte[] body) {
        Message message = new Message(MessageType.DONE.getValue(), body);
        future.channel().writeAndFlush(message);
    }

    public static void setOffsetAndRun(long length) {
        long blockSize = length / nThreads;
        log.info(HttpHeaderNames.CONTENT_LENGTH.toString() + ": " + length + ", " + blockSize);
        for (int i = 0; i < nThreads; i++) {
            services[i].threadId = i;
            services[i].startOffset = i * blockSize;
            services[i].finishOffset = (i + 1) * blockSize - 1;
            if (i == nThreads - 1) {
                services[i].finishOffset = length - 1;
            }
            services[i].start();
        }
    }

    // 只初始化一次
    public static void init() throws Exception {
        log.info("Client initializing start...");

        // 监控服务
        ClientMonitor.start();

        for (int i = 0; i < nThreads; i++) {
            services[i] = new ClientService(String.format("Client %d", i));
        }

        // 在最后启动 netty 进行通信
        startNetty();
    }

    public static void setPathAndPull(String path) throws IOException {
        ClientService.path = path;
        url = new URL(path);
        setOffsetAndRun(queryFileLength());
    }

    // 获得文件大小
    private static long queryFileLength() throws IOException {
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

            Message message = new Message(MessageType.WAIT.getValue(), String.valueOf(0x7FFFFFFF).getBytes());
            future.channel().writeAndFlush(message);

            message = new Message(MessageType.FINISH.getValue(), "\n".getBytes());
            future.channel().writeAndFlush(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 启动 netty 进行通信服务
    private static void startNetty() {
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
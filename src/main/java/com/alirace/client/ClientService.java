package com.alirace.client;

import com.alirace.model.Message;
import com.alirace.model.MessageType;
import com.alirace.netty.MyDecoder;
import com.alirace.netty.MyEncoder;
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
    private static final byte CR_SEPARATOR = (byte) '\r';
    private static final byte MINUS_SEPARATOR = (byte) '-';
    private static final byte LINE_SEPARATOR = (byte) '\n';
    private static final byte C_SEPARATOR = (byte) 'C';
    private static final byte[] HTTP_STATUS_CODE = "http.status_code=200".getBytes();
    private static final byte[] ERROR_EQUAL_1 = "error=1".getBytes();
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
    private static AtomicInteger lineIndex = new AtomicInteger(0);
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
    private boolean flag = false;
    private byte b;
    private long tmp;

    // 构造函数
    public ClientService(String name) {
        super(name);
        bytes = new byte[BYTES_LENGTH + 2 * LENGTH_PER_READ];
        for (int i = 0; i < WINDOW_SIZE; i++) {
            window[i] = -1L; // 0x11111111
        }
    }

    public void byteToHex() {
        hash = 0;
        while ((b = bytes[nowOffset]) != LOG_SEPARATOR) {
            // System.out.println(bytes[i]);
            if ('0' <= b && b <= '9') {
                hash = hash * 16 + ((int) b - '0');
            } else {
                hash = hash * 16 + ((int) b - 'a') + 10;
            }
            nowOffset++;
        }
    }

    // 获得所在行的编号, 注意该方法会自动推进 nowOffset
    public int queryLineIndex() throws Exception {
        int i = 0;

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
                return (int) (value >> 32);
            }
        }

        int line = lineIndex.incrementAndGet();
        log.info(String.format("bucketIndex: %6d, hashCode: %6d, ele: %6d", bucketIndex, hash, elements));

        buckets[bucketIndex][elements] = (((long) line) << 32) + (long) hash;
        bucketElements[bucketIndex]++;

        return line;
    }

    private static ConcurrentHashMap<Long, Integer> times = new ConcurrentHashMap<>();

    // BIO 读取数据
    public void pullData() throws Exception {
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        long start = startOffset != 0 ? startOffset - 3000_0000 : startOffset;
        long finish = startOffset == 0 ? finishOffset + 3000_0000 : finishOffset;
        long total = finish - start;
        long deal = 0;
        log.info(String.format("Start receive file: %10d-%10d, Data path: %s", start, finish, path));
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
                preOffset = nowOffset;

                // traceId
                int lineId = queryLineIndex();

                while (bytes[nowOffset] != LOG_SEPARATOR) {
                    nowOffset++;
                }

                // 处理时间戳, spanId
                nowOffset += 1 + 16 + 1 + 14;
//
                // 滑过中间部分
                int sep = 0;
                while (sep < 6) {
                    if (bytes[nowOffset] == LOG_SEPARATOR) {
                        sep++;
                    }
                    nowOffset++;
                }

                // 对 tag 的检查, 循环展开
                flag = false;
                while (bytes[nowOffset] != LINE_SEPARATOR) {
                    if (bytes[nowOffset] == HTTP_STATUS_CODE[0]
                            && bytes[nowOffset + 1] == HTTP_STATUS_CODE[1]
                            && bytes[nowOffset + 2] == HTTP_STATUS_CODE[2]
                            && bytes[nowOffset + 3] == HTTP_STATUS_CODE[3]
                            && bytes[nowOffset + 4] == HTTP_STATUS_CODE[4]
                            && bytes[nowOffset + 5] == HTTP_STATUS_CODE[5]
                            && bytes[nowOffset + 6] == HTTP_STATUS_CODE[6]
                            && bytes[nowOffset + 7] == HTTP_STATUS_CODE[7]
                            && bytes[nowOffset + 8] == HTTP_STATUS_CODE[8]
                            && bytes[nowOffset + 9] == HTTP_STATUS_CODE[9]
                            && bytes[nowOffset + 10] == HTTP_STATUS_CODE[10]
                            && bytes[nowOffset + 11] == HTTP_STATUS_CODE[11]
                            && bytes[nowOffset + 12] == HTTP_STATUS_CODE[12]
                            && bytes[nowOffset + 13] == HTTP_STATUS_CODE[13]
                            && bytes[nowOffset + 14] == HTTP_STATUS_CODE[14]
                            && bytes[nowOffset + 15] == HTTP_STATUS_CODE[15]
                            && bytes[nowOffset + 16] == HTTP_STATUS_CODE[16]
                    ) {
                        if (!(bytes[nowOffset + 17] == HTTP_STATUS_CODE[17]
                                && bytes[nowOffset + 18] == HTTP_STATUS_CODE[18]
                                && bytes[nowOffset + 19] == HTTP_STATUS_CODE[19])
                        ) {
                            flag = true;
                            break;
                        }
                    }

                    if (bytes[nowOffset] == ERROR_EQUAL_1[0]
                            && bytes[nowOffset + 1] == ERROR_EQUAL_1[1]
                            && bytes[nowOffset + 2] == ERROR_EQUAL_1[2]
                            && bytes[nowOffset + 3] == ERROR_EQUAL_1[3]
                            && bytes[nowOffset + 4] == ERROR_EQUAL_1[4]
                            && bytes[nowOffset + 5] == ERROR_EQUAL_1[5]
                            && bytes[nowOffset + 6] == ERROR_EQUAL_1[6]) {
                        flag = true;
                        break;
                    }
                    nowOffset++;
                }

                // 如果数据包含错误统计 +1
                if (flag) {
                    errorCount.incrementAndGet();
                    offset[lineId][0] = offset[lineId][0] & 0x0000_0111_1111_1111L;
                }

                // 处理到末尾
                while (bytes[nowOffset] != LINE_SEPARATOR) {
                    nowOffset++;
                }
                nowOffset++;
                // log.info(preOffset + "-" + nowOffset);

                // 保存到同一个 long 上
                tmp = (((long) preOffset) << 32) + (long) nowOffset;
                // log.info(preOffset + "|" + nowOffset + "|" + tmp);

                // offset 数组的第一个格子, 高位保存状态, 低位保存数据条数
                // 状态: 0000 0000 0000 0000 0000 000error 000upload 000done
                long firstOffset = offset[lineId][0];
                int spanNum = ((int) firstOffset) + 1;
                offset[lineId][0]++;
                // log.info(lineId + "|" + spanNum + "|" + tmp + "|" + offset[lineId][0]);
                deal += nowOffset - preOffset;

                // 窗口操作, 当前写 nodeIndex
                // 取出2w记录之前的数据
                // 高位存行号 低位存最大偏移
                long val = window[windowIndex];
                // 如果已经有数据了
                if (val != -1L) {
                    if (deal < 3000_0000 && threadId == 1) {
                        break;
                    }
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
            }

            // 如果太长了要从头开始写
            if (logOffset > BYTES_LENGTH + LENGTH_PER_READ) {
                // 拷贝末尾的数据
                for (int i = nowOffset; i < logOffset; i++) {
                    bytes[i - BYTES_LENGTH] = bytes[i];
                }
                nowOffset -= BYTES_LENGTH;
                logOffset -= BYTES_LENGTH;
                // log.info("rewrite");
            }
        }

//        for (int i = nodeIndex; i < nodeIndex + WINDOW_SIZE; i++) {
//            int now = i % 20000;
//            int pre = nodes[now].bucketIndex;
//            buckets[pre].checkAndUpload(bytes, nodes[now].endOffset);
//        }
        log.info("Client pull data finish...");
    }

    // 查询
    public static void queryAndUpload(int lineId) throws InterruptedException, IOException {
        long length = 0;
        long status = offset[lineId][0];
        int high = (int) (status >> 32); // 状态
        int low = (int) status; // 数据条数

        if (low == 1) {
            responseCount.incrementAndGet();
        }
//        if ((high & 0x0000100) == 0x0000100) {
//            log.info(lineId + " " + high + " " + low);
//        }

//        log.info(lineId + " " + high + " " + low);
        // log.info(lineId + " " + high + " " + low);
        // int bucketIndex = StringUtil.byteToHex(traceId, 0, 5);
        // log.info("query: " + new String(traceId));
        // buckets[bucketIndex].tryResponse(traceId.toString());
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
        responseCount.incrementAndGet();
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

    public static long queryFileLength() throws IOException {
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        httpConnection.setRequestMethod("HEAD");
//        Map<String, List<String>> headerFields = httpConnection.getHeaderFields();
//        Iterator iterator = headerFields.keySet().iterator();
//        while (iterator.hasNext()) {
//            String key = (String) iterator.next();
//            List values = headerFields.get(key);
//            log.info(key + ":" + values.toString());
//        }
        long contentLength = httpConnection.getContentLengthLong();
        httpConnection.disconnect();
        return contentLength;
    }

//
//                /*
//                // 对拍算法
//                StringBuffer sb = new StringBuffer();
//                for (int k = nowOffset; k < logOffset; k++) {
//                    if (LINE_SEPARATOR == bytes[k]) {
//                        break;
//                    }
//                    sb.append((char) bytes[k]);
//                }
//                boolean flag = Tag.isError(sb.toString());
//                */

//    public static byte[] query(String requestOffset) throws IOException {
//         log.info(requestOffset);
//        Request request = new Request.Builder()
//                .url(url)
//                .header("range", requestOffset)
//                .build();
//
//        // 返回的结果
//        byte[] bytes = client.newCall(request).execute().body().bytes();
//
//        // 结果合并处理
//        int index = 0, length = bytes.length;
//        byte[] result = new byte[length];
//        int pos = 0;
//
//        while (index < length) {
//            if (bytes[index] == LINE_SEPARATOR || bytes[index] == CR_SEPARATOR
//                    || bytes[index] == MINUS_SEPARATOR || bytes[index] == C_SEPARATOR) {
//                for ( ; index < length; index++) {
//                    if (bytes[index] == LINE_SEPARATOR) {
//                        break;
//                    }
//                }
//                index++;
//            } else {
//                for ( ; index < length; index++) {
//                    result[pos++] = bytes[index];
//                    if (bytes[index] == LINE_SEPARATOR) {
//                        break;
//                    }
//                }
//                index++;
//            }
//        }
//        // log.info(new String(result, 0, pos));
//        return new String(result, 0, pos).getBytes();
//    }

    @Override
    public void run() {
        try {
            pullData();
            // log.info("errorCount: " + errorCount);
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
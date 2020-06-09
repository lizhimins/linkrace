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

    protected static final int nThreads = 2;
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

    // 文件总长度
    protected static volatile long contentLength = 0L;

    // 控制偏移量
    protected long startOffset = -1L;
    protected long finishOffset = -1L;
    public int threadId = -1;

    private int preOffset = 0; // 起始偏移
    private int nowOffset = 0; // 当前偏移
    private int logOffset = 0; // 日志偏移

    private static final int BYTES_LENGTH = 512 * 1024 * 1024;
    public byte[] bytes = new byte[BYTES_LENGTH + 2 * LENGTH_PER_READ];

    // 找到对应的行号, 低 32 字节保存 hashcode, 高 32 字节保存行号
    private static final int BUCKETS_NUM = 0x01 << 20; // 100万
    private static final int TRACE_ID_HASH_CODE_AND_LINE_NUM = 16; // 每行32
    private static AtomicInteger lineIndex = new AtomicInteger(0);
    private static int[] bucketElements = new int[BUCKETS_NUM];
    private static long[][] buckets = new long[BUCKETS_NUM][TRACE_ID_HASH_CODE_AND_LINE_NUM];

    // 滑动窗口, 大小配置为 2万
    private static final int WINDOW_SIZE = 20000;
    private int nodeIndex;
    private long[] nodes = new long[WINDOW_SIZE];

    // 保存 traceId
    private int bucketIndex = 0;
    private int hash = 0;
    private boolean flag = false;
    private byte b;
    private int sep;

    // 构造函数
    public ClientService(String name) {
        super(name);

        // 各自拥有一个窗口
        log.info("Windows initializing start...");
        for (int i = 0; i < WINDOW_SIZE; i++) {
            nodes[i] = 0L;
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
            int value_low = (int) value;
            int value_high = (int) (value >> 32);
            // log.info("value : " + value_high + " " + value_low);
            if (hash == (int) value) {
                return (int) (value >> 32);
            }
        }

        int high = lineIndex.incrementAndGet();
        // log.info(String.format("bucketIndex: %6d, hashCode: %6d, ele: %6d", bucketIndex, hash, elements));

        buckets[bucketIndex][elements] = (((long) high) << 32) + (long) hash;
        bucketElements[bucketIndex]++;

        return high;
    }

    // 获得 Record 的引用, 注意该方法会自动推进 nowOffset
    public static void releaseRecord(byte[] traceId) throws Exception {
        int i = 0;

        // 根据前几位计算桶的编号
        int bucketIndex = 0;
        for (i = 0; i < 5; i++) {
            int b = traceId[i];
            if ('0' <= b && b <= '9') {
                bucketIndex = bucketIndex * 16 + (b - '0');
            } else {
                bucketIndex = bucketIndex * 16 + (b - 'a') + 10;
            }
        }

        int hash = 0;
        for (i = 5; i < 14; ++i) {
            hash += traceId[i];
            hash += (hash << 10);
            hash ^= (hash >> 6);
        }
        hash += (hash << 3);
        hash ^= (hash >> 11);
        hash += (hash << 15);

        // buckets[bucketIndex].releaseRecord(hash);
    }

    private static ConcurrentHashMap<Long, Integer> times = new ConcurrentHashMap<>();

    // BIO 读取数据
    public void pullData() throws Exception {
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        long start = startOffset != 0 ? startOffset - 3000_0000 : startOffset;
        long finish = finishOffset != contentLength ? finishOffset + 3000_0000 : finishOffset;
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
                queryLineIndex();

                // long hashCode = StringUtil.byteToHex(bytes, nowOffset, nowOffset + 16);
                // byteToHex();
                // System.out.println(hash);
//                Integer k = times.get(hash);
//                if (k == null) {
//                    times.put(hash, lineIndex.incrementAndGet());
//                } else {
//                    // times.put(hash, 0);
//                }

                // times.put(hashCode, times.get(hashCode) + 1);

                while (bytes[nowOffset] != LOG_SEPARATOR) {
                    nowOffset++;
                }


                // 处理时间戳, spanId
                nowOffset += 1 + 16 + 1 + 14;
//
                // 滑过中间部分
                sep = 0;
                while (sep < 6) {
                    if (bytes[nowOffset] == LOG_SEPARATOR) {
                        sep++;
                    }
                    nowOffset++;
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
//
//                // nowOffset = AhoCorasickAutomation.find(bytes, nowOffset - 1);
//
                // 循环展开
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

                while (bytes[nowOffset] != LINE_SEPARATOR) {
                    nowOffset++;
                }
                nowOffset++;
                // log.info(preOffset + "-" + nowOffset);


//                final int start = preOffset;
//                final int end = nowOffset;
//                fixedThreadPool.execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        // System.out.println(start + " " + end);
//
//                    }
//                });
//
//                if (flag) {
//                    errorCount.incrementAndGet();
//                }
//
//                record.addNewSpan(preOffset, nowOffset, flag);
//
//                // 窗口操作, 当前写 nodeIndex
//                // 取出2w记录之前的数据
//                Record pre = nodes[nodeIndex].record;
//                // 如果已经有数据了
//                if (pre != null && nodes[nodeIndex].endOffset != -1) {
//                    pre.checkAndUpload(nodes[nodeIndex].endOffset);
//                }
//                // 覆盖写
//                // System.print(String.format("Node:%d Pre:%d  ", nodeIndex, pre));
//                nodes[nodeIndex].record = record;
//                nodes[nodeIndex].endOffset = nowOffset;
//                nodeIndex = (nodeIndex + 1) % WINDOW_SIZE;
//
//                nowOffset++;
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
    public static void queryRecord(byte[] traceId) throws InterruptedException, IOException {
        queryCount.incrementAndGet();
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
        contentLength = length;
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

        // 共享桶结构
        log.info("Bucket initializing start...");
        for (int i = 0; i < BUCKETS_NUM; i++) {
            // buckets[i] = new Bucket();
        }
//        log.info("Object pool initializing start...");
//        RecordPoolFactory factory = new RecordPoolFactory();
//        // 设置对象池的相关参数
//        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
//        poolConfig.setMaxTotal(BUCKETS_NUM);
//        // 新建一个对象池,传入对象工厂和配置
//        recordPool = new GenericObjectPool<>(factory, poolConfig);
//        for (int i = 0; i < 10_0000; i++) {
//            recordPool.addObject();
//        }

        // 在最后启动 netty 进行通信
        startNetty();
    }

    public static void setPathAndPull(String path) throws IOException {
        ClientService.path = path;
        url = new URL(path);
        queryFileLength();
        setOffsetAndRun(contentLength);
    }

    public static void queryFileLength() throws IOException {
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        httpConnection.setRequestMethod("HEAD");
//        Map<String, List<String>> headerFields = httpConnection.getHeaderFields();
//        Iterator iterator = headerFields.keySet().iterator();
//        while (iterator.hasNext()) {
//            String key = (String) iterator.next();
//            List values = headerFields.get(key);
//            log.info(key + ":" + values.toString());
//        }
        contentLength = httpConnection.getContentLengthLong();
        httpConnection.disconnect();
    }

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
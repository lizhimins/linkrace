package com.alirace.client;

import com.alirace.model.Bucket;
import com.alirace.model.Message;
import com.alirace.model.MessageType;
import com.alirace.model.Node;
import com.alirace.netty.MyDecoder;
import com.alirace.netty.MyEncoder;
import com.alirace.util.AhoCorasickAutomation;
import com.alirace.util.StringUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpHeaderNames;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.alirace.client.ClientMonitor.*;

public class ClientService extends Thread {

    private static final Logger log = LoggerFactory.getLogger(ClientService.class);

    protected static final int nThreads = 2;
    protected static List<ClientService> services;

    // 通信相关参数配置
    private static final String HOST = "localhost";
    private static final int PORT = 8003;
    private static URL url;

    // 和汇总服务器 通信 Netty 相关配置
    private static EventLoopGroup workerGroup;
    private static Bootstrap bootstrap;
    private static ChannelFuture future;

    // http 连接池
    private static ConnectionPool connectionPool;

    // 数据获取地址, 由 CommonController 传入
    private static String path;

    // 真实数据
    private static final byte LOG_SEPARATOR = (byte) '|';
    private static final byte CR_SEPARATOR = (byte) '\r';
    private static final byte MINUS_SEPARATOR = (byte) '-';
    private static final byte LINE_SEPARATOR = (byte) '\n'; // key:value 分隔符
    private static final byte C_SEPARATOR = (byte) 'C';
    private static final int LENGTH_PER_READ = 8192; // 每一次读 8kb

    // 控制偏移量
    protected long startOffset = -1L;
    protected long finishOffset = -1L;
    protected static volatile long contentLength = 0L;
    private int preOffset = 0; // 起始偏移
    private int nowOffset = 0; // 当前偏移
    private int logOffset = 0; // 日志偏移
    private long truthOffset = 0L; // 真实偏移

    private static final int BYTES_LENGTH = 256 * 1024 * 1024;
    private byte[] bytes = new byte[BYTES_LENGTH + 2 * LENGTH_PER_READ];

    // 索引部分
    private static final int BUCKETS_NUM = 0x01 << 20; // 100万
    private static Bucket[] buckets = new Bucket[BUCKETS_NUM];

    // 窗口大小配置为 2万
    private int nodeIndex;
    private static final int WINDOW_SIZE = 20000;
    private Node[] nodes = new Node[WINDOW_SIZE];

    // 保存 traceId
    private int pos = 0;
    private byte[] traceId = new byte[32];
    private int bucketIndex = 0;

    // 构造函数
    public ClientService(String name) {
        super(name);

        // 各自拥有一个窗口
        log.info("Windows initializing start...");
        for (int i = 0; i < WINDOW_SIZE; i++) {
            nodes[i] = new Node();
        }
    }

    // 获得数据
    public void pullData() throws IOException {
        log.info(String.format("Start receive file: %10d-%10d, Data path: %s", startOffset, finishOffset, path));

        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        String range = String.format("bytes=%d-%d", startOffset, finishOffset);
        httpConnection.setRequestProperty("range", range);
        InputStream input = httpConnection.getInputStream();

        // 真实偏移设为起始偏移
        truthOffset = startOffset;

        // 初始化左右指针, 空出第一小段数据以备复制
        preOffset = LENGTH_PER_READ;
        nowOffset = LENGTH_PER_READ;
        logOffset = LENGTH_PER_READ;

        int readByteCount = input.read(bytes, logOffset, LENGTH_PER_READ);
        logOffset += readByteCount;

        while (true) {
            // 尝试读入一次
            readByteCount = input.read(bytes, logOffset, LENGTH_PER_READ);

            // 文件结束退出
            if (readByteCount == -1) {
                break;
            }

            // 日志坐标右移
            logOffset += readByteCount;

            // 半包开始
            if (startOffset != 0) {
                while (bytes[nowOffset] != LINE_SEPARATOR) {
                    nowOffset++;
                }
                nowOffset++;
            }

            while (nowOffset + LENGTH_PER_READ < logOffset) {
                preOffset = nowOffset;

                // 调试用延迟
//                try {
//                    TimeUnit.MILLISECONDS.sleep(1);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }

                // traceId 部分处理
                pos = 0;
                while (bytes[nowOffset] != LOG_SEPARATOR) {
                    traceId[pos] = bytes[nowOffset];
                    pos++;
                    nowOffset++;
                }
                traceId[pos] = LINE_SEPARATOR;

                // System.out.print(StringUtil.byteToString(traceId) + " ");

                // 计算桶的索引
                bucketIndex = StringUtil.byteToHex(traceId, 0, 5);
                // System.out.println(String.format("index: %d ", bucketIndex));

                // 将 traceId
                boolean isSame = buckets[bucketIndex].isSameTraceId(traceId);

                if (!isSame) {
                    buckets[bucketIndex].setTraceId(traceId);
                }

                // 处理时间戳, spanId
                nowOffset += 1 + 16 + 1 + 14;

                // 滑过中间部分
                for (int sep = 0; sep < 6; nowOffset++) {
                    if (bytes[nowOffset] == LOG_SEPARATOR) {
                        sep++;
                    }
                }

                /*
                // 对拍算法
                StringBuffer sb = new StringBuffer();
                for (int k = nowOffset; k < logOffset; k++) {
                    if (LINE_SEPARATOR == bytes[k]) {
                        break;
                    }
                    sb.append((char) bytes[k]);
                }
                boolean flag = Tag.isError(sb.toString());
                */

                nowOffset = AhoCorasickAutomation.find(bytes, nowOffset - 1);

                /*
                if (nowOffset < 0 && !flag || nowOffset > 0 && flag) {
                    System.out.println(StringUtil.byteToString(traceId) + " " + sb.toString());
                }
                 */

                // 返回值 < 0, 说明当前 traceId 有问题
                if (nowOffset < 0) {
                    // System.out.println("No");
                    nowOffset = -nowOffset;
                    errorCount.incrementAndGet();
//                    long start = truthOffset + preOffset - LENGTH_PER_READ;
//                    long end = truthOffset + nowOffset - LENGTH_PER_READ;
                    buckets[bucketIndex].addNewSpan(preOffset, nowOffset, true);
                } else {
                    // System.out.println("Yes");
//                    long start = truthOffset + preOffset - LENGTH_PER_READ;
//                    long end = truthOffset + nowOffset - LENGTH_PER_READ;
                    buckets[bucketIndex].addNewSpan(preOffset, nowOffset, false);
                }

                // 窗口操作, 当前写 nodeIndex
                // 取出2w记录之前的数据
                int pre = nodes[nodeIndex].bucketIndex;
                // 如果已经有数据了
                if (pre != -1) {
                    buckets[pre].checkAndUpload(bytes, nodes[nodeIndex].endOffset);
                }
                // 覆盖写
                // System.out.print(String.format("Node:%d Pre:%d  ", nodeIndex, pre));
                nodes[nodeIndex].bucketIndex = bucketIndex;
                // nodes[nodeIndex].endOffset = truthOffset + nowOffset - LENGTH_PER_READ;
                nodes[nodeIndex].endOffset = nowOffset;
                nodeIndex = (nodeIndex + 1) % WINDOW_SIZE;

                nowOffset++;
            }

            // 如果太长了要从头开始写
            if (logOffset > BYTES_LENGTH + LENGTH_PER_READ) {
                // 拷贝末尾的数据
                for (int i = nowOffset; i < logOffset; i++) {
                    bytes[i - BYTES_LENGTH] = bytes[i];
                }
                nowOffset -= BYTES_LENGTH;
                logOffset -= BYTES_LENGTH;
                truthOffset += BYTES_LENGTH;
                // log.info("rewrite");
            }
        }

        for (int i = nodeIndex; i < nodeIndex + WINDOW_SIZE; i++) {
            int now = i % 20000;
            int pre = nodes[now].bucketIndex;
            buckets[pre].checkAndUpload(bytes, nodes[now].endOffset);
        }
        log.info("Client pull data finish...");
        log.info("errorCount: " + errorCount);
    }

    // 查询
    public static void queryRecord(byte[] traceId) throws InterruptedException, IOException {
        queryCount.incrementAndGet();
        int bucketIndex = StringUtil.byteToHex(traceId, 0, 5);
        // log.info("query: " + new String(traceId));
        buckets[bucketIndex].tryResponse(traceId.toString());
    }

    // 上传调用链
    public static void upload(Message message) {
        uploadCount.incrementAndGet();
        future.channel().writeAndFlush(message);
    }

    // 查询响应
    public static void response(Message message) {
        responseCount.incrementAndGet();
        future.channel().writeAndFlush(message);
    }

    public static void setOffsetAndRun(long length) {
        contentLength = length;
        log.info(HttpHeaderNames.CONTENT_LENGTH.toString() + ": " + length);
        long blockSize = length / nThreads;
        for (int i = 0; i < nThreads; i++) {
            ClientService service = services.get(i);
            service.startOffset = i * blockSize;
            service.finishOffset = (i + 1) * blockSize - 1;
            if (i == nThreads - 1) {
                service.finishOffset = length - 1;
            }
            service.start();
        }
    }

    // 只初始化一次
    public static void init() {
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
            buckets[i] = new Bucket();
        }

        client = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(2, 5, TimeUnit.MINUTES))
                .build();

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
        Map<String, List<String>> headerFields = httpConnection.getHeaderFields();
        Iterator iterator = headerFields.keySet().iterator();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            List values = headerFields.get(key);
            log.info(key + ":" + values.toString());
        }
        contentLength = httpConnection.getContentLengthLong();
        httpConnection.disconnect();
    }

    private static OkHttpClient client;

    public static byte[] query(String requestOffset) throws IOException {
        // log.info(requestOffset);
        Request request = new Request.Builder()
                .url(url)
                .header("range", requestOffset)
                .build();

        // 返回的结果
        byte[] bytes = client.newCall(request).execute().body().bytes();

        // 结果合并处理
        int index = 0, length = bytes.length;
        byte[] result = new byte[length];
        int pos = 0;

        while (index < length) {
            if (bytes[index] == LINE_SEPARATOR || bytes[index] == CR_SEPARATOR
                    || bytes[index] == MINUS_SEPARATOR || bytes[index] == C_SEPARATOR) {
                for ( ; index < length; index++) {
                    if (bytes[index] == LINE_SEPARATOR) {
                        break;
                    }
                }
                index++;
            } else {
                for ( ; index < length; index++) {
                    result[pos++] = bytes[index];
                    if (bytes[index] == LINE_SEPARATOR) {
                        break;
                    }
                }
                index++;
            }
        }
        // log.info(new String(result, 0, pos));
        return new String(result, 0, pos).getBytes();
    }

    @Override
    public void run() {
        try {
            pullData();
            // log.info("errorCount: " + errorCount);
        } catch (IOException e) {
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
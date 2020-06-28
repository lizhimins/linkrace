package com.alirace.client;

import com.alirace.util.NumberUtil;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alirace.client.ClientMonitor.*;
import static com.alirace.constant.Constant.*;

public class ClientService extends Thread {

    private static final Logger log = LoggerFactory.getLogger(ClientService.class);

    // 实际工作线程
    private int threadId = 0;
    public static ClientService[] services = new ClientService[nThreads];

    // 数据获取地址 + 开始/结束为止, 由 CommonController 传入
    public static String path;
    private static URL url;
    private long startOffset = 0L;
    private long finishOffset = 0L;

    // 机器同步算法, 防止快机太快
    public int readBlockTimes = 0;
    public int otherBlockTimes = 0;

    // 字节缓冲区和处理指针
    private int preOffset = 0; // 起始偏移 左指针
    private int nowOffset = 0; // 当前偏移
    private int logOffset = 0; // 日志偏移 右指针
    private byte[] bytes = new byte[BYTES_SIZE];

    // HashMap, 低 32 字节保存 hashcode, 高 32 字节保存行号, 单例
    private int[] bucketStatus = new int[BUCKET_NUM];
    private long[][] buckets = new long[BUCKET_NUM][BUCKET_CAP];

    // 偏移表
    private long[] offsetStatus = new long[OFFSET_NUM];;
    private long[][] offset = new long[OFFSET_NUM][OFFSET_CAP];;

    // 偏移表行号 行号原子增加
    private AtomicInteger maxLineIndex = new AtomicInteger(-1);

    // 滑动窗口, 大小配置为 2万
    private int windowIndex = 0;
    private long[] window = new long[WINDOW_SIZE];

    public int errorCount = 0;

    // 构造函数
    public ClientService(String name, int threadId) {
        super(name);
        for (int i = 0; i < BYTES_SIZE; i++) {
            bytes[i] = '\n';
        }
        this.threadId = threadId;
    }

    // 获得所在行的编号, 若不存在则会创建新的行注意该方法会自动推进 nowOffset
    private int queryLineIndex() {
//        StringBuffer sb = new StringBuffer();
//        for (int i = preOffset; i < preOffset + 16; i++) {
//            if (bytes[i] == LOG_SEPARATOR) {
//                break;
//            }
//            sb.append((char) (int) bytes[i]);
//        }
//        if (threadId == 1) {
//            System.out.println(sb.toString());
//        }

        // 根据前几位计算桶的编号
        int bucketIndex = 0; byte b;
        for (int i = 0; i < 5; i++) {
            b = bytes[nowOffset++];
            if ('0' <= b && b <= '9') {
                bucketIndex = bucketIndex * 16 + ((int) b - '0');
            } else {
                bucketIndex = bucketIndex * 16 + ((int) b - 'a') + 10;
            }
        }

        // 处理到尾巴
        int hash = 0;
        while ((b = bytes[nowOffset]) != LOG_SEPARATOR) {
            hash = hash * 31 + b;
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

    private boolean checkTags() {
        int endIndex = bytes[nowOffset] == 38 ? nowOffset - 2 : nowOffset - 1;

        byte tmp;
        // & 38        | 124
        while (true) {
            tmp = bytes[endIndex - 7];
            if (tmp == 38 || tmp == 124) {
                return true;
            }
            tmp = bytes[endIndex - 20];
            if ((tmp == 38 || tmp == 124) && bytes[endIndex - 19] == 104) {
                return bytes[endIndex - 2] != 50;
            }
            if (bytes[endIndex] == 38) {
                endIndex = endIndex - 1;
                continue;
            }

            // ?id=    63 105 100 61
            if (bytes[endIndex - 4] == 63) {
                endIndex -= 43;
            } else {
                endIndex -= 10;
            }

            while (true) {
                tmp = bytes[endIndex--];
                if (tmp == 38) {
                    break;
                } else if (tmp == 124) {
                    return false;
                }
            }
        }
    }
    
    // 检查标签中是否包含错误
    private boolean checkTags1() {
        int endIndex = nowOffset;
        while (true) {
            // "error=1" :  101 114 114 111 114 61 49
            if (bytes[endIndex] == 49) {
                if (bytes[endIndex - 2] == 114) {
                    if (bytes[endIndex - 6] == 101) {
                        return true;
                    }
                }
            }

            // "http.status_code="  :  104 116 116 112 46 115 116 97 116 117 115 95 99 111 100 101 61
            if (bytes[endIndex] == 61) {
                if (bytes[endIndex - 16] == 104) {
                    return bytes[endIndex + 1] != 50 || bytes[endIndex + 2] != 48 || bytes[endIndex + 3] != 48;
                }
            }

            if (bytes[endIndex] == 124) {
                return false;
            }
            --endIndex;
        }
    }

    private static int totalSpan = 0;
    // 每处理一条日志, 就需要调用一次这个函数
    private void handleSpan() throws Exception {
        totalSpan++;

        // 左指针 = 当前指针
        preOffset = nowOffset;

        // 查询行号: traceId -> nowLine
        int nowLine = queryLineIndex();

        // System.out.println((char) (int) bytes[nowOffset]);
        // 处理时间戳, spanId
        nowOffset += 110;

        while (bytes[nowOffset] != LINE_SEPARATOR) {
            nowOffset++;
        }
        // 滑过中间部分
//        int sep = 0;
//        while (sep < 6) {
//            if (bytes[nowOffset] == LOG_SEPARATOR) {
//                sep++;
//            }
//            nowOffset++;
//        }
//        nowOffset++;
        // System.out.println((char) (int) bytes[nowOffset]);

        // 检查错误
        if (checkTags()) {
            errorCount++; // 错误数量 + 1

//            for (int i = preOffset; i < nowOffset; i++) {
//                System.out.print((char) (int) bytes[i]);
//            }
//            System.out.println();

            offsetStatus[nowLine] |= (0x1L << 32); // 错误打标
        }
        // log.info(preOffset + "-" + nowOffset);

        long status = offsetStatus[nowLine];
        offset[nowLine][(int) status] = NumberUtil.combineInt2Long(preOffset, nowOffset);
        // log.info(lineId + "|" + spanNum + "|" + tmp + "|" + offset[lineId][0]);

        // 保存到同一个 long 上
        // offset 数组的第一个格子, 高位保存状态, 低位保存数据条数
        offsetStatus[nowLine]++;

        // 滑动窗口
        long data = window[windowIndex];
        if (data != 0L) {
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

    @Override
    public void run() {
        long start = System.currentTimeMillis();

        try {
            pullData();
            ClientMonitor.printStatus();
            log.info("total: " + totalSpan);
            long syncValue = NumberUtil.combineInt2Long(threadId, Integer.MAX_VALUE);
            NettyClient.sendSync(syncValue);
            NettyClient.finish("\n".getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }

        long end = System.currentTimeMillis();
        log.info("It cost time: " + (end - start) + " ms.");
    }

    private void handleFirst() {
        // 跳过最前面的一条
        while (bytes[nowOffset] != LINE_SEPARATOR) {
            nowOffset++;
        }
        nowOffset++;

        while (nowOffset < CROSS_RANGE * 2) {
            // 左指针 = 当前指针
            preOffset = nowOffset;

            // 查询行号: traceId -> nowLine
            int nowLine = queryLineIndex();

            // System.out.println((char) (int) bytes[nowOffset]);
            // 处理时间戳, spanId
            nowOffset += 110;

            while (bytes[nowOffset] != LINE_SEPARATOR) {
                nowOffset++;
            }

            // 检查错误
            if (checkTags()) {
                errorCount++; // 错误数量 + 1
                offsetStatus[nowLine] |= (0x1L << 32); // 错误打标
            }
            // log.info(preOffset + "-" + nowOffset);

            long status = offsetStatus[nowLine];
            offset[nowLine][(int) status] = NumberUtil.combineInt2Long(preOffset, nowOffset);
            // log.info(lineId + "|" + spanNum + "|" + tmp + "|" + offset[lineId][0]);

            // 保存到同一个 long 上
            // offset 数组的第一个格子, 高位保存状态, 低位保存数据条数
            offsetStatus[nowLine]++;

            // 循环覆盖写
            window[windowIndex] = NumberUtil.combineInt2Long(nowLine, (int) (offsetStatus[nowLine]));
            windowIndex = (windowIndex + 1) % WINDOW_SIZE;

            // 最后一个符号是 \n
            nowOffset++;
        }
    }

    // BIO 读取数据
    public void pullData() throws Exception {
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        long start = threadId == 0 ? startOffset : startOffset - CROSS_RANGE;
        long finish = startOffset == 0 ? finishOffset + CROSS_RANGE : finishOffset;
        log.info(String.format("Start receive file: %10d-%10d, Data path: %s", start, finish, path));
        String range = String.format("bytes=%d-%d", start, finish);
        httpConnection.setRequestProperty(HttpHeaderNames.RANGE.toString(), range);
        InputStream input = httpConnection.getInputStream();

        int readByteCount;
        while (logOffset < PRE_DOWNLOAD_LENGTH) {
            readByteCount = input.read(bytes, logOffset, LENGTH_PER_READ);
            logOffset += readByteCount;
        }

        // 如果不是第一个线程的话
        if (threadId != 0) {
            handleFirst();
        }

        while (true) {
            // 读入一小段数据
            readByteCount = input.read(bytes, logOffset, LENGTH_PER_READ);
            // syncBlock();

            // 文件结束退出
            if (readByteCount == -1) {
                while (nowOffset < logOffset) {
                    handleSpan();
                }
                break;
            }

            // 日志坐标右移
            logOffset += readByteCount;

            // 循环处理所有数据
            while (nowOffset + LENGTH_PER_READ < logOffset) {
                handleSpan();
            }

            // 如果太长了要从头开始写, 拷贝末尾的数据到头部
            if (logOffset >= BYTES_LENGTH) {
                for (int i = nowOffset; i <= logOffset; i++) {
                    bytes[i - nowOffset + CROSS_RANGE * 4] = bytes[i];
                }
                logOffset = logOffset - nowOffset + CROSS_RANGE * 4;
                nowOffset = CROSS_RANGE * 4;
                // log.info("rewrite");
            }
        }

        if (threadId != 0) {
            for (int i = windowIndex; i < windowIndex + WINDOW_SIZE; i++) {
                int now = i % 20000;
                int preLineId = (int) (window[now] >> 32);
                int preLength = (int) window[now];
                int nowlength = (int) offsetStatus[preLineId];
                if (preLength == nowlength) {
                    queryAndUpload(preLineId);
                }
            }
        } else {
            for (int i = windowIndex; i < windowIndex + WINDOW_SIZE; i++) {
                int now = i % 20000;
                int preLineId = (int) (window[now] >> 32);
                int preLength = (int) window[now];
                int nowlength = (int) offsetStatus[preLineId];
                if (preLength == nowlength) {
                    int startPos = (int) (offset[preLineId][0] >> 32);
                    byte[] traceId = new byte[16];
                    int index = 0;
                    for (int j = 0; j < 16; j++) {
                        traceId[j] = bytes[startPos + j];
                    }
                    int lineId = services[1].queryLineId(traceId);
                    // log.info(startPos + " " + lineId);
                    services[1].queryAndUpload(lineId);
                }
            }
        }

        input.close();
        httpConnection.disconnect();
        log.info("Client pull data finish...");
    }

    // 查询
    public void queryAndUpload(int lineId) {
        // 获得这一行最新的状态 0x1 错误 0x10000 表示结束
        int status = (int) (offsetStatus[lineId] >> 32);
        // log.info(String.format("%h", status));

        // 有错误
        if (status != 0) {
            byte[] body = getDataByLineId(lineId);
            if (status == 0x00000001) {
                NettyClient.upload(body);
            }
            if (status == 0x00000100) {
                NettyClient.response(body);
            }
            if (status == 0x00000101) {
                NettyClient.response(body);
                // response("\r".getBytes());
            }
            // log.info(new String(body));
        }

        offsetStatus[lineId] |= (0x1L << 48); // 标记结束
    }

    public int queryLineId(byte[] traceId) {
        // 根据前几位计算桶的编号
        int bucketIndex = 0; byte b;
        for (int i = 0; i < 5; i++) {
            b = traceId[i];
            if ('0' <= b && b <= '9') {
                bucketIndex = bucketIndex * 16 + ((int) b - '0');
            } else {
                bucketIndex = bucketIndex * 16 + ((int) b - 'a') + 10;
            }
        }

        // 处理到尾巴
        int hash = 0;
        for (int i = 5; i < 16; i++) {
            if (traceId[i] == LOG_SEPARATOR) {
                break;
            }
            hash = hash * 31 + traceId[i];
        }

        // 检测是否已经出现过
        int depth = bucketStatus[bucketIndex];
        for (int i = 0; i < depth; i++) {
            long value = buckets[bucketIndex][i];
            if (hash == (int) value) {
                // log.info(String.format("bucketIndex: %6d, hashCode: %6d, ele: %6d", bucketIndex, hash, elements));
                return (int) (value >> 32);
            }
        }
        return -1;
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

        // 处理到尾巴
        int hash = 0; int k = 5; int length = traceId.length;
        while (k < length) {
            hash = hash * 31 + traceId[k];
            k++;
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
        } else {
            // log.info(new String(traceId) + " existent");
            // offsetStatus[lineId] |= ((0x1L) << 40); // 标记为需要被动上传
            tryResponse(traceId, lineId);
        }

//        log.info(new String(traceId) + " "
//                + String.format("bucketIndex: %6d, hashCode: %6d", bucketIndex, hash)
//                + String.format(" %s", " " + lineId));
    }

    public byte[] getDataByLineId(int lineId) {
        int spanNum = (int) (offsetStatus[lineId]); // 数据条数

        int startIndex = (int) (offset[lineId][0] >> 32);

        int traceIdLength = 0;
        while (bytes[startIndex++] != LOG_SEPARATOR) {
            traceIdLength++;
        }
        traceIdLength++;

        int pos = 0;
        long[] timeTMP = new long[108];
        long[] offsetTMP = new long[108];

        int length = 0; long values = 0;
        for (int i = 0; i < spanNum; i++) {
            long spanOffset = offset[lineId][i];
            int start = (int) (spanOffset >> 32);
            int finish = (int) spanOffset;
            length += finish - start + 1;
            for (int j = start + traceIdLength; j < start + traceIdLength + 16; j++) {
                values <<= 4; values |= ((bytes[j] - '0') & 0xff);
            }

            // log.info(String.format("%x", values));

            timeTMP[pos] = values;
            offsetTMP[pos] = spanOffset;
            pos++;
        }

        NumberUtil.bubbleSort(timeTMP, offsetTMP, spanNum);

//        System.out.print("TIME: ");
//        for (int i = 0; i < spanNum; i++) {
//            System.out.print(String.format("%x ", timeTMP[i]));
//        }
//        System.out.println();
//
//        System.out.print("offset: ");
//        for (int i = 0; i < spanNum; i++) {
//            System.out.print(offsetTMP[i] + " ");
//        }
//        System.out.println();

        int index = 0;
        byte[] body = new byte[length];
        for (int i = 0; i < spanNum; i++) {
            long spanOffset = offsetTMP[i];
            int start = (int) (spanOffset >> 32);
            int finish = (int) spanOffset;
            for (int j = start; j <= finish; j++) {
                body[index] = bytes[j];
                index++;
            }
        }
        return body;
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
            byte[] body = getDataByLineId(lineId);

            boolean isError = false;
            for (int i = 0; i < traceId.length; i++) {
                if (traceId[i] != body[i]) {
                    isError = true;
                    break;
                }
            }
            // TODO: 校验数据, 校验不通过直接丢弃
            if (!isError) {
                NettyClient.response(body);
            } else {
                NettyClient.response("\n".getBytes());
                log.error("DROP DATA: " + new String(traceId));
                // log.info(new String(body));
            }
            // log.info(new String(body));
        } else {
            // 还没结束的话只需要标记一下有错误就可以了
            offsetStatus[lineId] |= ((0x1L) << 40); // 标记为第二种错误
        }
    }

    public static void setOffsetAndRun(long length) {
        long blockSize = length / nThreads;
        log.info(HttpHeaderNames.CONTENT_LENGTH.toString() + ": " + length + ", blockSize: " + blockSize);
        for (int i = 0; i < nThreads; i++) {
            services[i].startOffset = i * blockSize;
            services[i].finishOffset = (i != nThreads-1) ? (i + 1) * blockSize - 1 : length - 1;
            services[i].start();
        }
    }

    public void syncBlock() throws InterruptedException {
        readBlockTimes++;
        while (readBlockTimes - otherBlockTimes > SYNC_GAP) {
            TimeUnit.MILLISECONDS.sleep(1);
        }
        if (readBlockTimes % 4 == 0) {
            long syncValue = NumberUtil.combineInt2Long(threadId, readBlockTimes);
            NettyClient.sendSync(syncValue);
        }
    }

    // 只初始化一次
    public static void init() throws Exception {
        log.info("ClientService start...");

        for (int i = 0; i < nThreads; i++) {
            services[i] = new ClientService(String.format("Client %d", i), i);
        }

        // 监控服务
        ClientMonitor.start();

        // 在最后启动 netty 进行通信
        NettyClient.startNetty();
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
}
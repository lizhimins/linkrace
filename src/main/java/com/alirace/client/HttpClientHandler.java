package com.alirace.client;

import com.alirace.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static com.alirace.client.HttpClient.*;

public class HttpClientHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(HttpClientHandler.class);

    private static final byte LOG_SEPARATOR = (byte) '|';
    private static final byte LINE_SEPARATOR = (byte) '\n'; // 行尾分隔符

    private CompositeByteBuf compositeByteBuf;

    private long startOffset = 0;
    private long finishOffset = 0;

    private long preOffset = -1; // 起始偏移
    private long nowOffset = -1; // 当前偏移
    private long logOffset = -1; // 日志偏移

    // 保存 traceId
    private int pos = 0;
    private byte[] traceId = new byte[32];
    private int bucketIndex = 0;

    private byte b;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpContent) {
            // 调试用延迟
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            HttpContent httpContent = (HttpContent) msg;
            // log.info("packet: " + httpContent.content().writerIndex() + " ");

            // 最后一个空数据包
            if (httpContent instanceof LastHttpContent) {
                // System.out.println(byteBuf.toString(StandardCharsets.UTF_8));
                log.info("finish");
            }

            ByteBuf content = httpContent.content();

            if (preOffset == -1) {
                compositeByteBuf.addComponent(true, content);
                preOffset = startOffset;
                nowOffset = startOffset;
                logOffset = startOffset + content.writerIndex();
                return;
            }

            // 加入新的
            compositeByteBuf.addComponent(true, content);
            logOffset += content.capacity();

            if (compositeByteBuf.numComponents() > 20) {
                compositeByteBuf.discardReadBytes();
            }

            log.info(compositeByteBuf.toString());

            // 如果是从中间开始的话要丢弃半条日志
            if (preOffset == startOffset && startOffset != 0) {
                while ((b = compositeByteBuf.readByte()) != LINE_SEPARATOR) {
                    nowOffset++;
                }
                nowOffset++;
            }

//            while (compositeByteBuf.numComponents() > 10) {
//                 compositeByteBuf.discardReadBytes();
//            }

            while (nowOffset + BLOCK_SIZE < logOffset
                    && compositeByteBuf.readerIndex() + BLOCK_SIZE < compositeByteBuf.writerIndex()) {
                preOffset = nowOffset;

                // traceId 部分处理
                pos = 0;
                while ((b = compositeByteBuf.readByte()) != LOG_SEPARATOR) {
                    traceId[pos] = b;
                    pos++;
                    nowOffset++;
                }
                traceId[pos] = LINE_SEPARATOR; // 行尾

                // System.out.println(StringUtil.byteToString(traceId) + " ");

                while ((b = compositeByteBuf.readByte()) != LINE_SEPARATOR) {
                    nowOffset++;
                }
                nowOffset++;
            }
            return;
        }

        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;
            compositeByteBuf = ctx.alloc().compositeBuffer(256);

            if (HttpUtil.isContentLengthSet(response) && HttpClient.contentLength == 0) {
                HttpClient.contentLength = HttpUtil.getContentLength(response);
                // HttpClient.contentLength = 20000;
                log.info(HttpHeaderNames.CONTENT_LENGTH.toString() + ": " + HttpClient.contentLength);
                ClientService.startThread();
                return;
            }

            String range = response.headers().get(HttpHeaderNames.CONTENT_RANGE);
            int divIndex = range.indexOf("/");
            int minusIndex = range.indexOf("-");
            long contentLength = Long.parseLong(range.substring(divIndex + 1, range.length()));
            if (contentLength == HttpClient.contentLength) {
                startOffset = Long.parseLong(range.substring(6, minusIndex));
                finishOffset = Long.parseLong(range.substring(minusIndex + 1, divIndex));
                log.info(String.format("Start receive file: %12d-%12d", startOffset, finishOffset));
            }
        }
    }
}

// byteBuf.setIndex(0, 20);
// byteBuf.discardReadBytes();
// System.out.println(byteBuf.toString(StandardCharsets.UTF_8));
// for (int i = 0; i < 30; i++) {
// System.out.print((char) (int) byteBuf.getByte(i));
// }


// System.out.println("Server said:" + byteBuf);
// ctx.close();


//                // 调试用延迟
//                try {
//                    TimeUnit.MILLISECONDS.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }

//        int readByteCount = input.read(bytes, logOffset, LENGTH_PER_READ);
//        logOffset += readByteCount;
//
//        while (true) {
//            // 尝试读入一次
//            readByteCount = input.read(bytes, logOffset, LENGTH_PER_READ);
//
//            // 文件结束退出
//            if (readByteCount == -1) {
//                break;
//            }
//
//            // 日志坐标右移
//            logOffset += readByteCount;
//
//            while (nowOffset + LENGTH_PER_READ < logOffset) {
//                preOffset = nowOffset;
//
//                // 调试用延迟
////                try {
////                    TimeUnit.MILLISECONDS.sleep(1);
////                } catch (InterruptedException e) {
////                    e.printStackTrace();
////                }
//
//                // traceId 部分处理
//                pos = 0;
//                while (bytes[nowOffset] != LOG_SEPARATOR) {
//                    traceId[pos] = bytes[nowOffset];
//                    pos++;
//                    nowOffset++;
//                }
//                traceId[pos] = (byte) '\n';
//
//                // System.out.print(StringUtil.byteToString(traceId) + " ");
//
//                // 计算桶的索引
//                bucketIndex = StringUtil.byteToHex(traceId, 0, 5);
//                // System.out.println(String.format("index: %d ", bucketIndex));
//
//                // 将 traceId
//                boolean isSame = buckets[bucketIndex].isSameTraceId(traceId);
//
//                if (!isSame) {
//                    buckets[bucketIndex].setTraceId(traceId);
//                }
//
//                // 滑过中间部分
//                for (int sep = 0; sep < 8; nowOffset++) {
//                    if (bytes[nowOffset] == LOG_SEPARATOR) {
//                        sep++;
//                    }
//                }
//
//                nowOffset = AhoCorasickAutomation.find(bytes, nowOffset - 1);
//
//                // 返回值 < 0, 说明当前 traceId 有问题
//                if (nowOffset < 0) {
//                    // System.out.println("No");
//                    nowOffset = -nowOffset;
//                    errorCount.incrementAndGet();
//                    long start = roundOffset + preOffset - LENGTH_PER_READ;
//                    long end = roundOffset + nowOffset - LENGTH_PER_READ;
//                    buckets[bucketIndex].addNewSpan(start, end, true);
//                } else {
//                    // System.out.println("Yes");
//                    long start = roundOffset + preOffset - LENGTH_PER_READ;
//                    long end = roundOffset + nowOffset - LENGTH_PER_READ;
//                    buckets[bucketIndex].addNewSpan(start, end, false);
//                }
//
//                // 窗口操作, 当前写 nodeIndex
//                // 先取出数据
//                int preBucketIndex = nodes[nodeIndex].bucketIndex;
//                long preStartOffset = nodes[nodeIndex].startOffset;
//
//                // 如果已经有数据了
//                if (preBucketIndex != -1) {
//                    buckets[preBucketIndex].checkAndUpload(preStartOffset);
//                }
//                nodes[nodeIndex].bucketIndex = bucketIndex;
//                nodes[nodeIndex].startOffset = preOffset;
//                nodeIndex = (nodeIndex + 1) % WINDOW_SIZE;
//
//                nowOffset++;
//            }
//
//            // 如果太长了要从头开始写
//            if (logOffset > BYTES_LENGTH + LENGTH_PER_READ) {
//                // 拷贝末尾的数据
//                for (int i = nowOffset; i < logOffset; i++) {
//                    bytes[i - BYTES_LENGTH] = bytes[i];
//                }
//                nowOffset -= BYTES_LENGTH;
//                logOffset -= BYTES_LENGTH;
//                roundOffset += BYTES_LENGTH;
//                // log.info("rewrite");
//            }
//        }
//        log.info("Client pull data finish...");
//        log.info("errorCount: " + errorCount);
//        // log.info("traceCount: " + traceCount.size());
//    }
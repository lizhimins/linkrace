package com.alirace.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

import static com.alirace.client.HttpClient.BYTES_LENGTH;

public class HttpClientHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(HttpClientHandler.class);

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

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;

            // 最后一个空数据包
            if (httpContent instanceof LastHttpContent) {
                // System.out.println(byteBuf.toString(StandardCharsets.UTF_8));
                log.info("finish");
            }

            ByteBuf content = httpContent.content();

            if (preOffset == -1) {
                compositeByteBuf.addComponent(true, content);
                preOffset = startOffset;
                logOffset = startOffset + content.writerIndex();
                return;
            }

            // 当前处理指针指向左节点
            nowOffset = preOffset;

            compositeByteBuf.addComponent(true, content);
            compositeByteBuf.removeComponent(0);

            return;
        }

        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;
            compositeByteBuf = ctx.alloc().compositeBuffer(1024);

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
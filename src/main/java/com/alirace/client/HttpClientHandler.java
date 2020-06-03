package com.alirace.client;

import com.alirace.netty.ByteBufToBytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class HttpClientHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(ClientService.class);

    private ByteBufToBytes reader = new ByteBufToBytes(8 * 1024 * 1024);

    private ByteBuf temp;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;
            if (HttpUtil.isContentLengthSet(response) && ClientService.contentLength == 0) {
                ClientService.setOffsetAndRun(HttpUtil.getContentLength(response));
                return;
            }
        }

        if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;
            ByteBuf content = httpContent.content();
            // reader.reading(content);

//            if (r) {
//                String resultStr = new String(reader.readFull());
//                System.out.println("Server said:" + resultStr);
//                String[] split = resultStr.split("\n");
//                for (int i = 0; i < split.length; i++) {
//                    if (split[i] == null || split.length == 0 || "\r".equals(split[i]) || split[i].startsWith("--") || split[i].startsWith("Con")) {
//                        continue;
//                    }
//                    log.info(split[i]);
//                }
//                ctx.close();
//            }
            byte[] bytes = new byte[content.readableBytes()];
            content.readBytes(bytes);
            content.release();
            String[] split = new String(bytes).split("\n");
            for (int i = 0; i < split.length; i++) {
                if (split[i] == null || split.length == 0 || "\r".equals(split[i]) || split[i].startsWith("--") || split[i].startsWith("Con")) {
                    continue;
                }
                log.info(split[i]);
            }
        }
    }
}
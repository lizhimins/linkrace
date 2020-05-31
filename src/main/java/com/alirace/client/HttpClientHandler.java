package com.alirace.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;

import static com.alirace.client.HttpClient.BYTES_LENGTH;

public class HttpClientHandler extends ChannelInboundHandlerAdapter {

    private ByteBuf byteBuf = Unpooled.buffer(BYTES_LENGTH);

    private long contentLength = 0;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;
            System.out.println("CONTENT_TYPE:" + response.headers().get(HttpHeaderNames.CONTENT_TYPE));
            if (HttpUtil.isContentLengthSet(response)) {
                contentLength = HttpUtil.getContentLength(response);
                System.out.println(contentLength);
            }
        }

        if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;
            ByteBuf content = httpContent.content();
            System.out.println(content.writerIndex());

            if (byteBuf.writerIndex() > 1000) {
                // byteBuf.setIndex(0, 20);
                // byteBuf.discardReadBytes();
                //System.out.println(byteBuf.toString(StandardCharsets.UTF_8));
                for (int i = 0; i < 30; i++) {
                    System.out.print((char) (int) byteBuf.getByte(i));
                }
            }

            byteBuf.writeBytes(content);
            content.release();

            // System.out.println("Server said:" + byteBuf);
            // ctx.close();
        }
    }
}
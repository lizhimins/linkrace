package com.alirace.client;

import com.alirace.netty.ByteBufToBytes;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;

public class HttpClientHandler extends ChannelInboundHandlerAdapter {
    private ByteBufToBytes reader;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
//        if (msg instanceof HttpResponse)
//        {
//            HttpResponse response = (HttpResponse) msg;
//            System.out.println("CONTENT_TYPE:" + response.headers().get(HttpHeaderNames.CONTENT_TYPE));
//            System.out.println();
//        }
        if (msg instanceof HttpContent) {
            HttpContent content = (HttpContent) msg;
            ByteBuf buf = content.content();
//            System.out.println(buf.toString(io.netty.util.CharsetUtil.UTF_8));
//            System.out.println();
            buf.release();
        }

//        if (msg instanceof HttpResponse) {
//            HttpResponse response = (HttpResponse) msg;
//            System.out.println("CONTENT_TYPE:" + response.headers().get(HttpHeaderNames.CONTENT_TYPE));
//            if (HttpUtil.isContentLengthSet(response)) {
//                reader = new ByteBufToBytes((int) HttpUtil.getContentLength(response));
//            }
//        }
//        if (msg instanceof HttpContent) {
//            HttpContent httpContent = (HttpContent) msg;
//            ByteBuf content = httpContent.content();
//            reader.reading(content);
//            content.release();
//            if (reader.isEnd()) {
//                String resultStr = new String(reader.readFull());
//                System.out.println("Server said:" + resultStr);
//                ctx.close();
//            }
//        }
    }
}
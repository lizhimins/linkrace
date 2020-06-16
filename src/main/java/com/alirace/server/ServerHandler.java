package com.alirace.server;

import com.alirace.controller.CommonController;
import com.alirace.model.Message;
import com.alirace.model.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

import static com.alirace.server.ServerService.*;

/**
 * 数据合并处理器
 */
public class ServerHandler extends SimpleChannelInboundHandler<Object> {

    /**
     * 所有的活动用户
     */
    public static final ChannelGroup group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static final Logger log = LoggerFactory.getLogger(ServerHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object obj) throws Exception {
        Channel channel = ctx.channel();

        Message message = (Message) obj;
        // log.info("Client->Server: " + channel.remoteAddress() + " " + message.toString());

        if (MessageType.PASS.getValue() == message.getType()) {
            byte[] body = message.getBody();
            StringBuffer buffer = new StringBuffer(16);
            for (int i = 0; i < 16; i++) {
                if (body[i] == (byte) '|') {
                    break;
                }
                buffer.append((char) (int) body[i]);
            }
            String traceId = buffer.toString();
            ByteBuf byteBuf = mergeMap2.get(traceId);
            if (byteBuf == null) {
                byteBuf = Unpooled.buffer();
                mergeMap2.put(traceId, byteBuf);
            }
            byteBuf.writeBytes(body);
        }

        // 如果是上传数据
        if (MessageType.UPLOAD.getValue() == message.getType()) {
            byte[] body = message.getBody();
            StringBuffer buffer = new StringBuffer(16);
            for (int i = 0; i < 16; i++) {
                if (body[i] == (byte) '|') {
                    break;
                }
                buffer.append((char) (int) body[i]);
            }
            String traceId = buffer.toString();
            // log.info(traceId);

            // 向其他机器广播查询请求
            for (Channel ch : group) {
                if (ch != channel) {
                    queryRequestCount.incrementAndGet();
                    Message query = new Message(MessageType.QUERY.getValue(), traceId.getBytes());
                    ch.writeAndFlush(query);
                }
            }

            // 如果当前内存中不包含 traceId 的调用链路就放入内存, 如果存在的话就合并调用链, 然后刷盘
            byte[] result = mergeMap.putIfAbsent(traceId, body);
            if (result != null) {
                ServerService.flushResult(traceId, body, result);
            }
            return;
        }

        if (MessageType.RESPONSE.getValue() == message.getType()) {
            queryResponseCount.incrementAndGet();
        }

        // 如果日志流已经上报完, 只等数据回查的话
        if (MessageType.FINISH1.getValue() == message.getType()) {
            if (finish1.incrementAndGet() == TOTAL_SERVICES_COUNT) {
                int size = mergeMap.size();
                StringBuffer sb = new StringBuffer();
                Iterator<Map.Entry<String, byte[]>> iterator = mergeMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    sb.append(iterator.next().getKey() + ",");
                }
                log.info("Broadcast: " + sb.toString());
                Message response = new Message(MessageType.FINISH1.getValue(), sb.toString().getBytes());
                for (Channel ch : group) {
                    ch.writeAndFlush(response);
                }
            }
        }

        if (MessageType.FINISH2.getValue() == message.getType()) {
            if (finish2.incrementAndGet() == TOTAL_SERVICES_COUNT) {
                log.info("all finish");
                ServerService.flushResult2();
                uploadData();
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        group.add(channel);
        log.info(String.format("Now %d client connected to this server, %s", group.size(), channel.remoteAddress()));
        if (group.size() == MACHINE_NUM) {
            // 向所有机器广播 READY 信号
            for (Channel ch : group) {
                Message query = new Message(MessageType.READY.getValue(), "READY".getBytes());
                ch.writeAndFlush(query);
            }
            // 标记 ready 接口
            CommonController.setReady();
            log.info("Server start data merge...");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
        Channel channel = ctx.channel();
        log.error("[" + channel.remoteAddress() + "] dis connect...");
        e.printStackTrace();
        ctx.close().sync();
    }
}

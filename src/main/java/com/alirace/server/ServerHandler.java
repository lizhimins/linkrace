package com.alirace.server;

import com.alirace.controller.CommonController;
import com.alirace.model.Message;
import com.alirace.model.MessageType;
import com.alirace.model.Record;
import com.alirace.util.SerializeUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        // 动态代理
        // 如果是上传数据
        if (MessageType.UPLOAD.getValue() == message.getType()) {
            // 反序列化得到数据
            byte[] body = message.getBody();
            StringBuffer traceId = new StringBuffer(20);
            for (int i = 0; i < 20; i++) {
                if (body[i] == (byte) '|') {
                    break;
                }
                traceId.append((char) (int) body[i]);
            }
            // log.info(traceId.toString());

            // 向其他机器广播查询请求
            for (Channel ch : group) {
                if (ch != channel) {
                    queryRequestCount.incrementAndGet();
                    Message query = new Message(MessageType.QUERY.getValue(), traceId.toString().getBytes());
                    ch.writeAndFlush(query);
                }
            }

//            Record result = mergeMap.get(traceId);
//            // 如果当前内存中不包含 traceId 的调用链路就放入内存, 如果存在的话就合并调用链, 然后刷盘
//            if (result == null) {
//                mergeMap.put(traceId, record);
//            } else {
//                result.merge(record);
//                ServerService.flushResult(traceId, result);
//            }
            return;
        }

//        if (MessageType.PASS.getValue() == message.getType()) {
//            // 反序列化得到数据
//            Record record = SerializeUtil.deserialize(message.getBody(), Record.class);
//            String traceId = record.getTraceId();
//            // log.info(traceId);
//            Record result = mergeMap.get(traceId);
//            if (result == null) {
//                mergeMap.put(traceId, record);
//            } else {
//                result.merge(record);
//                ServerService.flushResult(traceId, result);
//            }
//        }

//        // 如果日志流已经上报完, 只等数据回查的话
//        if (MessageType.FINISH.getValue() == message.getType()) {
//            if (doneServicesCount.incrementAndGet() == TOTAL_SERVICES_COUNT) {
//                // 广播结束信号
//                for (Channel ch : group) {
//                    Message query = new Message(MessageType.NO_MORE_UPLOAD.getValue(), "EOF".getBytes());
//                    ch.writeAndFlush(query);
//                }
//            }
//        }
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

package com.alirace.server;

import com.alirace.controller.CommonController;
import com.alirace.model.Message;
import com.alirace.model.MessageType;
import com.alirace.util.HttpUtil;
import com.alirace.util.MD5Util;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
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
@ChannelHandler.Sharable
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

            // if (traceId.equals("1c2b9d10fde34")) {
            //     System.out.println(new String(body));
            // }

            // 向其他机器广播查询请求
            for (Channel ch : group) {
                if (ch != channel) {
                    queryRequestCount++;
                    Message query = new Message(MessageType.QUERY.getValue(), traceId.getBytes());
                    ch.writeAndFlush(query);
                }
            }

            // 如果当前内存中不包含 traceId 的调用链路就放入内存, 如果存在的话就合并调用链, 然后刷盘
            byte[] result = mergeMap.putIfAbsent(traceId, body);
            if (result != null) {
                String md5 = flushResult(body, result);
                resultMap.put(traceId, md5);
                // ServerService.addResult(traceId, md5);
                mergeMap.remove(traceId);
            }
            return;
        }

        if (MessageType.RESPONSE.getValue() == message.getType()) {
            queryResponseCount++;
            byte[] body = message.getBody();
            // 空数据直接退出
            if (body[0] == '\n' || body[0] == '\r') {
                log.error("ERROR LOG...");
                return;
            }

            StringBuffer buffer = new StringBuffer(16);
            for (int i = 0; i < message.getLength(); i++) {
                if (body[i] == (byte) '|') {
                    break;
                }
                buffer.append((char) (int) body[i]);
            }
            String traceId = buffer.toString();

//            if (traceId.equals("1c2b9d10fde34")) {
//                System.out.println(new String(body));
//            }
            // log.error("RECEIVE: " + traceId);
//            if (message.getLength() < 32) {
//                ServerService.flushResult(mergeMap.get(traceId));
//                mergeMap.remove(traceId);
//                return;
//            }

            byte[] result = mergeMap.get(traceId);
            if (result != null) {
                String md5 = flushResult(body, result);
                resultMap.put(traceId, md5);
                // ServerService.addResult(traceId, md5);
                mergeMap.remove(traceId);
            }
        }

        // 同步算法
        if (MessageType.SYNC.getValue() == message.getType()) {
            for (Channel ch : group) {
                if (ch != channel) {
                    ch.writeAndFlush(message);
                }
            }
        }

        if (MessageType.FINISH.getValue() == message.getType()) {
            if (++finishCount == 4) {
                for (Channel ch : group) {
                    ch.writeAndFlush(message);
                }
            }
        }

        if (MessageType.DONE.getValue() == message.getType()) {
            if (++doneCount == 2) {
                Iterator<Map.Entry<String, byte[]>> iterator = mergeMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, byte[]> entry = iterator.next();
                    byte[] body = entry.getValue();
                    StringBuffer buffer = new StringBuffer(16);
                    for (int i = 0; i < 16; i++) {
                        if (body[i] == (byte) '|') {
                            break;
                        }
                        buffer.append((char) (int) body[i]);
                    }
                    String traceId = buffer.toString();
                    String md5 = MD5Util.byteToMD5(entry.getValue());
                    // String md5 = ServerService.flushResult3(entry.getValue());
                    resultMap.put(traceId, md5);
                    // HttpUtil.post(traceId, md5);
                    // log.info(new String(entry.getValue()));
                    // iterator.remove();
                }
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
            // ServerService.buildLink();
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

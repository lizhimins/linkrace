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

import java.util.concurrent.TimeUnit;

/**
 * 数据合并处理器
 */
public class ServerHandler extends SimpleChannelInboundHandler<Object> {

    /**
     * 所有的活动用户
     */
    public static final ChannelGroup group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static final Logger log = LoggerFactory.getLogger(ServerHandler.class);
    private static final int MACHINE_NUM = 2;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object obj) throws Exception {
        Channel channel = ctx.channel();

        Message message = (Message) obj;

        // 如果是上传数据
        if (MessageType.UPLOAD.getValue() == message.getType()) {
            Record record = SerializeUtil.deserialize(message.getBody(), Record.class);
            log.info(record.toString());
        }
    }

    /**
     * 阻塞, 等待其他机器上线
     */
    private void waitAllMachineOnline() {
        while (group.size() < MACHINE_NUM) {
            try {
                // log.info("System waiting...");
                TimeUnit.SECONDS.sleep(1L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    /**
     * 处理新加的消息通道
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        group.add(channel);
        log.info("Number of machines currently connected to this server: "
                + group.size() + " " + channel.remoteAddress());
        if (group.size() == MACHINE_NUM) {
            // 标记 ready 接口
            CommonController.isReady.set(true);
            log.info("Server start data merge...");
        }
    }

    /**
     * 处理退出消息通道
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
//        for (Channel ch : group) {
//            if (ch == channel) {
//                ch.writeAndFlush("[" + channel.remoteAddress() + "] leaving");
//            }
//        }
        group.remove(channel);
    }

    /**
     * 在建立连接时发送消息
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        Channel channel = ctx.channel();
//        boolean active = channel.isActive();
//        if (active) {
//            System.out.println("[" + channel.remoteAddress() + "] is online");
//        } else {
//            System.out.println("[" + channel.remoteAddress() + "] is offline");
//        }
//        ctx.writeAndFlush("[server]: welcome");
//        log.info("Number of machines currently connected to this server: "
//                + group.size() + " " + channel.remoteAddress());
    }

    /**
     * 退出时发送消息
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
//        Channel channel = ctx.channel();
//        if (!channel.isActive()) {
//            System.out.println("[" + channel.remoteAddress() + "] is offline");
//        } else {
//            System.out.println("[" + channel.remoteAddress() + "] is online");
//        }
    }

    /**
     * 异常捕获
     *
     * @param ctx
     * @param e
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
        Channel channel = ctx.channel();
        log.error("[" + channel.remoteAddress() + "] dis connect...");
        e.printStackTrace();
        ctx.close().sync();
    }
}

package com.alirace.server;

import com.alirace.controller.CommonController;
import com.alirace.model.Message;
import com.alirace.model.MessageType;
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
        // log.info("Client->Server: " + channel.remoteAddress() + " " + message.toString());

        // 动态代理
        // 如果是上传数据
        if (MessageType.UPLOAD.getValue() == message.getType()) {
            return;
        }

        // 如果是进度汇报数据
        if (MessageType.STATUS.getValue() == message.getType()) {
            return;
        }

        // 如果日志流已经上报完, 只等数据回查的话
        if (MessageType.FINISH.getValue() == message.getType()) {
            return;
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

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        group.add(channel);
        log.info(String.format("Now %d client connected to this server, %s", group.size(), channel.remoteAddress()));
        if (group.size() == MACHINE_NUM) {
            // 标记 ready 接口
            CommonController.isReady.set(true);
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

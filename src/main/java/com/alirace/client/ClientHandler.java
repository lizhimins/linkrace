package com.alirace.client;

import com.alirace.controller.CommonController;
import com.alirace.model.Message;
import com.alirace.model.MessageType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

import static com.alirace.client.ClientService.*;

/**
 * My ClientHandler.
 */
public class ClientHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object obj) throws Exception {
        Channel channel = ctx.channel();
        Message message = (Message) obj;

        // 如果收到查询请求
        if (MessageType.QUERY.getValue() == message.getType()) {
            // log.info(new String(message.getBody()));
            ClientMonitor.queryCount.incrementAndGet();
            return;
        }

//        // 如果收到查询请求
//        f (MessageType.FINISH1.getValue() == message.getType()) {
////
////        }

        if (MessageType.SYNC.getValue() == message.getType()) {
            long syncValue = Long.parseLong(new String(message.getBody()));
            int serviceId = (int) (syncValue >> 32);
            int otherTimes = (int) syncValue;
            log.info(String.format("RECV: %x", syncValue));
            services[serviceId].otherBlockTimes = otherTimes;
        }

        if (MessageType.FINISH.getValue() == message.getType()) {
//            Iterator<Map.Entry<Integer, byte[]>> iterator = queryArea.entrySet().iterator();
//            while (iterator.hasNext()) {
//                byte[] value = iterator.next().getValue();
//                // log.info(new String(value));
//                response(value);
//            }
//            ClientService.done("\n".getBytes());
        }

        // 如果收到开始信号请求
        if (MessageType.READY.getValue() == message.getType()) {
            CommonController.setReady();
            return;
        }
    }

    /**
     * 处理新加的消息通道
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        // Channel channel = ctx.channel();
        // CommonController.isReady = true;
        // log.info("Client connect to server success...");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // doConnect();
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
        System.out.println("[" + channel.remoteAddress() + "] disConnect");
        e.printStackTrace();
        ctx.close().sync();
        NettyClient.doConnect();
    }
}

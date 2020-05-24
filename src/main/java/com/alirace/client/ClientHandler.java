package com.alirace.client;

import com.alirace.controller.CommonController;
import com.alirace.model.Message;
import com.alirace.model.MessageType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.alirace.client.ClientService.doConnect;

/**
 * My ClientHandler.
 */
public class ClientHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object obj) throws Exception {
        Channel channel = ctx.channel();
        Message message = (Message) obj;

        // 动态代理
        // 如果收到查询请求
        if (MessageType.QUERY.getValue() == message.getType()) {
            String traceId = new String(message.getBody());
            // 调用查询服务上传查询结果
            ClientService.queryRecord(traceId);
            return;
        }

        // 如果收到开始信号请求
        if (MessageType.START.getValue() == message.getType()) {
            CommonController.setReady();
            return;
        }

        // 如果收到开始信号请求
        if (MessageType.NO_MORE_UPLOAD.getValue() == message.getType()) {
//            int num = 0;
//            Iterator<Map.Entry<String, Boolean>> iterator = waitMap.entrySet().iterator();
//            while (iterator.hasNext()) {
//                Map.Entry<String, Boolean> entry = iterator.next();
//                if (!entry.getValue()) {
//                    num++;
//                }
//            }
            int num = (int) (ClientMonitor.queryCount.get() - ClientMonitor.responseCount.get());
            // ClientService.response(num);
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
        // e.printStackTrace();
        ctx.close().sync();
        doConnect();
    }
}

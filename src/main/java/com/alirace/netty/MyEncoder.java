/**
 *
 */
package com.alirace.netty;

import com.alirace.model.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * My Encoder.
 */
public class MyEncoder extends MessageToByteEncoder<Message> {

    private static final Logger log = LoggerFactory.getLogger(MyEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, Message message, ByteBuf out) throws Exception {
        if (message == null) {
            throw new Exception("The encode message is null");
        }
//        if (MessageType.UPLOAD.getValue() == message.getType()) {
//            Record record = SerializeUtil.deserialize(message.getBody(), Record.class);
//            String traceId = record.getTraceId();
//            // TODO: SEND
//            // System.out.println("Send: " + traceId);
//        }
        out.writeByte(message.getType());
        out.writeInt(message.getLength());
        out.writeBytes(message.getBody());
    }
}
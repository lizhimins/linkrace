package com.alirace.netty;

import com.alirace.model.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * My Decoder.
 */
public class MyDecoder extends LengthFieldBasedFrameDecoder {
    private static final int HEADER_SIZE = 5;

    // 帧长度解码设置
    private static final int MAX_FRAME_LENGTH = 1024 * 1024;  //最大长度
    private static final int LENGTH_FIELD_LENGTH = 4;  //长度字段所占的字节数
    private static final int LENGTH_FIELD_OFFSET = 1;  //长度偏移
    private static final int LENGTH_ADJUSTMENT = 0;
    private static final int INITIAL_BYTES_TO_STRIP = 0;

    public MyDecoder() {
        super(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP, false);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        //在这里调用父类的方法,实现指得到想要的部分,我在这里全部都要,也可以只要body部分
        in = (ByteBuf) super.decode(ctx, in);
        if (in == null) {
            return null;
        }

        if (in.readableBytes() < HEADER_SIZE) {
            throw new Exception("字节数不足");
        }

        //读取type字段
        byte type = in.readByte();
        // 读取消息的长度
        int bodyLength = in.readInt();
        if (in.readableBytes() != bodyLength) {
            throw new Exception("长度错误");
        }

        //读取body
        byte[] bytes = new byte[bodyLength];
        in.readBytes(bytes);
        in.release();
        return new Message(type, bytes);
    }
}

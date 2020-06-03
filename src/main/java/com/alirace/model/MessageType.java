package com.alirace.model;

/**
 * Message Type.
 */
public enum MessageType {

    // 客户端->服务端
    UPLOAD((byte) 0x00), // 主动上传
    RESPONSE((byte) 0x09), // 查询响应
    FINISH((byte) 0x0F),

    // 客户端<-服务端
    READY((byte) 0x10), // 广播 ready 信号
    QUERY((byte) 0x19); // 查询

    private byte value;

    private MessageType(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }
}

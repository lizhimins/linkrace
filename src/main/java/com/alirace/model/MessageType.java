package com.alirace.model;

/**
 * Message Type.
 */
public enum MessageType {

    // 客户端->服务端
    UPLOAD((byte) 0x00), // 主动上传
    PASS((byte) 0x01), // 数据正确
    RESPONSE((byte) 0x09), // 查询响应
    WAIT((byte) 0x0C), // 同步信号

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

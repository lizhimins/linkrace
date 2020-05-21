package com.alirace.model;

/**
 * Message Type.
 */
public enum MessageType {

    // 客户端->服务端
    UPLOAD((byte) 0x00),
    RESPONSE((byte) 0x01),
    STATUS((byte) 0x0A),
    FINISH((byte) 0x0F),

    // 客户端<-服务端
    QUERY((byte) 0x11),
    SYNC((byte) 0x1A),
    MONITOR((byte) 0x12);

    private byte value;

    private MessageType(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }
}

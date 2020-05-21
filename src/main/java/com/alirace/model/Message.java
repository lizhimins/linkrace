package com.alirace.model;

/**
 * Message
 */
public class Message {

    // 消息类型
    private byte type;

    // 消息长度
    private int length;

    // 消息体
    private byte[] body;

    public Message() {
    }

    public Message(byte type, byte[] body) {
        this.type = type;
        this.length = body.length;
        this.body = body;
    }

    @Override
    public String toString() {
        if (MessageType.QUERY.getValue() == type) {
            return "Message{" + "type=" + String.valueOf(type) + ", " + new String(body) + "}";
        }
        return "Message{" + "type=" + type + ", length=" + length + "}";
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }
}

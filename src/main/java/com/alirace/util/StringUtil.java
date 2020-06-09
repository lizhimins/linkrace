package com.alirace.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class StringUtil {

    private static Charset cs = StandardCharsets.UTF_8;

    public static String byteToString(byte[] bytes) {
        StringBuffer sb = new StringBuffer(16);
        for (int i = 0; i < 16; i++) {
            char ch = (char) (int) bytes[i];
            if ('\n' != ch) {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    public static long byteToHex(byte[] bytes, int start, int end) {
        long sum = 0;
        for (int i = start; i < end; i++) {
            // System.out.println(bytes[i]);
            if ('0' <= bytes[i] && bytes[i] <= '9') {
                sum = sum << 4 + ((int) bytes[i] - '0');
            } else {
                sum = sum << 4 + ((int) bytes[i] - 'a') + 10;
            }
        }
        return sum;
    }

    public static char byteToChar(byte data) {
        return (char) (int) data;
    }

    public static byte[] getBytes(char[] chars) {
        CharBuffer cb = CharBuffer.allocate(chars.length);
        cb.put(chars);
        cb.flip();
        ByteBuffer bb = cs.encode(cb);
        return bb.array();
    }

    public static char[] getChars(byte[] bytes) {
        Charset cs = StandardCharsets.UTF_8;
        ByteBuffer bb = ByteBuffer.allocate(bytes.length);
        bb.put(bytes).flip();
        CharBuffer cb = cs.decode(bb);
        return cb.array();
    }

    public static byte[] charToByte(char c) {
        byte[] b = new byte[2];
        b[0] = (byte) ((c & 0xFF00) >> 8);
        b[1] = (byte) (c & 0xFF);
        return b;
    }

    public static char byteToChar(byte[] b) {
        int hi = (b[0] & 0xFF) << 8;
        int lo = b[1] & 0xFF;
        return (char) (hi | lo);
    }

    public static void main(String[] args) {
        // char[] <===> byte[]
        char[] c = getChars(new byte[]{65, 2, 3});
        System.out.println(Arrays.toString(c));
        byte[] b = getBytes(c);
        System.out.println(Arrays.toString(b));

        // char <===> byte[]
        byte[] b2 = charToByte('A');
        System.out.println(Arrays.toString(b2));
        char c2 = byteToChar(b2);
        System.out.println(c2);
    }
}

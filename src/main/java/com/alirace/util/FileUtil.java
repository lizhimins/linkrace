package com.alirace.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class FileUtil {

    private static FileOutputStream outputStream;

    private static String fileName = "C:/resources/1.TXT";

    public static void init() throws IOException {
        outputStream = new FileOutputStream(fileName);
    }

    public static void write(byte[] strToBytes) throws IOException {
        outputStream.write(strToBytes);
    }

    public static void write(byte b) throws IOException {
        outputStream.write(b);
    }

    public static void close() throws IOException {
        outputStream.close();
    }

    public static void main(String[] args) throws IOException {
        init();
        write("1".getBytes());
        close();
    }
}

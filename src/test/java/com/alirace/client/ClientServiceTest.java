package com.alirace.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ClientServiceTest {

    private static final byte LOG_SEPARATOR = (byte) '|';
    private static final byte CR_SEPARATOR = (byte) '\r';
    private static final byte MINUS_SEPARATOR = (byte) '-';
    private static final byte LINE_SEPARATOR = (byte) '\n'; // key:value 分隔符
    private static final byte C_SEPARATOR = (byte) 'C';

    private static OkHttpClient client= new OkHttpClient.Builder()
            .connectionPool(new ConnectionPool(2, 5, TimeUnit.MINUTES))
            .build();

    @Test
    public void query() throws IOException {
        String range = "bytes=1072465-1072726,1180131-1180422,1233460-1233737,1312064-1312354";
        URL url = new URL("http://10.66.1.107:" + "8004" + "/trace1.data");

        Request request = new Request.Builder()
                .url(url)
                .header("range", range)
                .build();
        byte[] bytes = client.newCall(request).execute().body().bytes();

        // System.out.println(new String(bytes));

        String[] split = new String(bytes).split("\n");
        for (int i = 0; i < split.length; i++) {
            byte k = (byte) (int) split[i].charAt(0);
            if (k == LINE_SEPARATOR || k == CR_SEPARATOR
                    || k == MINUS_SEPARATOR || k == C_SEPARATOR) {
                continue;
            }
            System.out.println(split[i]);
        }

        int index = 0, length = bytes.length;

        byte[] result = new byte[length];
        int pos = 0;

        byte b;
        while (index < length) {
            if (bytes[index] == LINE_SEPARATOR || bytes[index] == CR_SEPARATOR
                    || bytes[index] == MINUS_SEPARATOR || bytes[index] == C_SEPARATOR) {
                for ( ; index < length; index++) {
                    if (bytes[index] == LINE_SEPARATOR) {
                        break;
                    }
                }
                index++;
            } else {
                for ( ; index < length; index++) {
                    result[pos++] = bytes[index];
                    if (bytes[index] == LINE_SEPARATOR) {
                        break;
                    }
                }
                index++;
            }
        }
        System.out.println(new String(result, 0, pos));
    }
}
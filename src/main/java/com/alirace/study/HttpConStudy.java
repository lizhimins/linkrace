package com.alirace.study;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

public class HttpConStudy {

    private static byte[] bytes = new byte[4096];

    public static void main(String[] args) throws IOException {
        URL url = new URL("http://10.66.1.107:" + "8004" + "/trace1.data");
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        httpConnection.setRequestProperty("range", "bytes=0-10,15-20,35-50");
        InputStream input = httpConnection.getInputStream();

        input.read(bytes);

        System.out.println(new String(bytes));

    }
}

package com.alirace.model;

import org.junit.Test;

public class TagTest extends Tag {

    @Test
    public void staticValue() {
//        for (int i = 0; i < HTTP_STATUS_CODE.length; i++) {
//            System.out.print(HTTP_STATUS_CODE[i] + " ");
//        }
//        System.out.println();
//        int k[] = Tag.computeFailure(HTTP_STATUS_CODE);
//        for (int i = 0; i < k.length; i++) {
//            System.out.print(k[i] + " ");
//        }
    }


    @Test
    public void lengthTest() {
        System.out.println("http.status_code=".length());
        System.out.println("error=1".length());
        System.out.println("http".getBytes()[1]);
    }
}

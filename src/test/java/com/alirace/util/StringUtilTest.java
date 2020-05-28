package com.alirace.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class StringUtilTest {

    @Test
    public void byteToHex() {
        byte[] bytes = "69fe7".getBytes();
        assertEquals(434151, StringUtil.byteToHex(bytes, 0, 5));
    }
}
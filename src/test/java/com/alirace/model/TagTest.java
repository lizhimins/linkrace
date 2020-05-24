package com.alirace.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TagTest {

    @Test
    public void isError() {
        assertEquals(false, Tag.isError("http.status_code=200"));
        assertEquals(true, Tag.isError("http.status_code=0"));
        assertEquals(false, Tag.isError("&&http.status_code=200"));
        assertEquals(false, Tag.isError("error=0"));
        assertEquals(true, Tag.isError("error=1"));
        assertEquals(false, Tag.isError("http.status_code=200&key=value"));
    }
}

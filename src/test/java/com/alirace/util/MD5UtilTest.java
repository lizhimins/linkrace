package com.alirace.util;

import org.junit.Assert;
import org.junit.Test;

public class MD5UtilTest {

    @Test
    public void strToMd5() {
        Assert.assertEquals(MD5Util.strToMd5("A").toLowerCase(), "7fc56270e7a70fa81a5935b72eacbe29");
        Assert.assertEquals(MD5Util.strToMd5("B").toLowerCase(), "9d5ed678fe57bcca610140957afab571");
    }
}

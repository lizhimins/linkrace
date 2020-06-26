package com.alirace.constant;

public class Constant {

    public static final int nThreads = 2;
    public static final byte LOG_SEPARATOR = (byte) '|';
    public static final byte LINE_SEPARATOR = (byte) '\n';

    public static final byte[] HTTP_STATUS_CODE = "http.status_code=200".getBytes();
    public static final byte[] ERROR_EQUAL_1 = "error=1".getBytes();

    public static final int BYTES_LENGTH = (512 * 1024 * 1024) / nThreads;
    public static final int LENGTH_PER_READ = 1024 * 1024; // 每一次读 1M 2.8秒
    public static final int BYTES_SIZE = BYTES_LENGTH + 4 * LENGTH_PER_READ;
    public static final int CROSS_RANGE = 4 * 1024 * 1024; // 交叉区, 最多8M

    public static final int BUCKET_NUM = 0x1 << 20;
    public static final int BUCKET_CAP = 16;

    public static final int OFFSET_NUM = 110_0000 / nThreads;
    public static final int OFFSET_CAP = 108;

    public static final int WINDOW_SIZE = 20000;
}

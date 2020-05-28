package com.alirace.model;

/**
 * 链路信息中 tag 信息
 */
public class Tag {

    // tags 之间的分隔符
    private static final String TAG_SEPARATOR = "&";

    // key:value 分隔符
    private static final String KV_SEPARATOR = "=";

    // 错误码
    private static final String HTTP_STATUS_CODE = "http.status_code";

    private static final String ERROR = "error";

    // 返回 tag 里面是不是包含错误
    public static boolean isError(String tagStr) {
        // 如果为空返回无错误
        if (tagStr == null || tagStr.length() == 0) {
            return false;
        }
        String[] tagList = tagStr.split(TAG_SEPARATOR);
        for (String item : tagList) {
            // 兼容处理1, 空 tag
            if (item == null || item.length() == 0) {
                continue;
            }
            int index = item.indexOf(KV_SEPARATOR);
            // 兼容处理2, 找不到分隔符
            if (index == -1) {
                continue;
            }
            String key = item.substring(0, index);
            String value = item.substring(index + 1);
            if (HTTP_STATUS_CODE.equals(key) && !"200".equals(value)) {
                return true;
            }
            if (ERROR.equals(key) && "1".equals(value)) {
                return true;
            }
        }
        return false;
    }
}
//    // tags 之间的分隔符
//    public static final byte TAG_SEPARATOR = (byte) '&';
//
//    // key:value 分隔符
//    public static final byte KV_SEPARATOR = (byte) '=';
//
//    public static final byte[] HTTP_STATUS_CODE = "http.status_code=".getBytes();
//    public static final byte[] ERROR = "error=1".getBytes();
//    public static int[] ERROR_FAIL = computeFailure(ERROR);
//    public static int[] HTTP_STATUS_CODE_FAIL = computeFailure(HTTP_STATUS_CODE);
//
//    // 对 tag 进行检查
//    private int[] failure;
//    private int matchPoint;
//    private byte[] bytePattern;
//
//    /**
//     * 取到字节流中指定字节流的位置
//     * The Knuth-Morris-Pratt Pattern Matching Algorithm can be used to search a byte array.
//     * Search the data byte array for the first occurrence
//     * of the byte array pattern.
//     */
//    public static int indexOf(byte[] data, int start, byte[] pattern) {
//        int[] failure = computeFailure(pattern);
//        int j = 0;
//        for (int i = start; data[i] != (byte) '\n'; i++) {
//            while (j > 0 && pattern[j] != data[i]) {
//                j = failure[j - 1];
//            }
//            if (pattern[j] == data[i]) {
//                j++;
//            }
//            if (j == pattern.length) {
//                return i + 1;
//                //return i - pattern.length + 1;
//            }
//        }
//        return -1;
//    }
//
//    /**
//     * Computes the failure function using a boot-strapping process,
//     * where the pattern is matched against itself.
//     */
//    public static int[] computeFailure(byte[] pattern) {
//        int[] failure = new int[pattern.length];
//        int j = 0;
//        for (int i = 1; i < pattern.length; i++) {
//            while (j > 0 && pattern[j] != pattern[i]) {
//                j = failure[j - 1];
//            }
//            if (pattern[j] == pattern[i]) {
//                j++;
//            }
//            failure[i] = j;
//        }
//        return failure;
//    }
//
//    /**
//     * Method indexOf …
//     *
//     * @param text       of type byte[]
//     * @param startIndex of type int
//     * @return int
//     */
//
//    public int indexOf(byte[] text, int startIndex) {
//
//        int j = 0;
//
//        if (text.length == 0 || startIndex > text.length) return -1;
//
//
//        for (int i = startIndex; i < text.length; i++) {
//
//            while (j > 0 && bytePattern[j] != text[i]) {
//
//                j = failure[j - 1];
//
//            }
//
//            if (bytePattern[j] == text[i]) {
//
//                j++;
//
//            }
//
//            if (j == bytePattern.length) {
//
//                matchPoint = i –bytePattern.length + 1;
//
//                return matchPoint;
//
//            }
//
//        }
//
//        return -1;
//
//    }
//
//    /**
//     * Method computeFailure4Byte …
//     *
//     * @param patternStr of type byte[]
//     */
//
//    public void computeFailure4Byte(byte[] patternStr) {
//        bytePattern = patternStr;
//        int j = 0;
//        int len = bytePattern.length;
//        failure = new int[len];
//        for (int i = 1; i < len; i++) {
//            while (j > 0 && bytePattern[j] != bytePattern[i]) {
//                j = failure[j - 1];
//            }
//            if (bytePattern[j] == bytePattern[i]) {
//                j++;
//            }
//            failure[i] = j;
//        }
//    }
//
//}

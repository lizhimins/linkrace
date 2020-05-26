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

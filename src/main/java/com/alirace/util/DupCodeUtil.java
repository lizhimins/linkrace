package com.alirace.util;

public class DupCodeUtil {

//        while (bytes[nowOffset] != LINE_SEPARATOR) {
////            // "error=1" :  101 114 114 111 114 61 49
////            if (bytes[nowOffset] == 101) {
////                if (bytes[nowOffset + 4] == 114) {
////                    if (bytes[nowOffset + 6] == 49) {
////                        return true;
////                    }
////                }
////            }
////
////            // "http.status_code="  :  104 116 116 112 46 115 116 97 116 117 115 95 99 111 100 101 61
////            if (bytes[nowOffset] == 104) {
////                if (bytes[nowOffset + 16] == 61) {
////                    return bytes[nowOffset + 17] != 50
////                            || bytes[nowOffset + 18] != 48
////                            || bytes[nowOffset + 19] != 48;
////                }
////            }
////            nowOffset++;
////        }
////        return false;


//        while (bytes[nowOffset] != LINE_SEPARATOR) {
//            if (       bytes[nowOffset]      == HTTP_STATUS_CODE[0]
//                    && bytes[nowOffset + 1]  == HTTP_STATUS_CODE[1]
//                    && bytes[nowOffset + 2]  == HTTP_STATUS_CODE[2]
//                    && bytes[nowOffset + 3]  == HTTP_STATUS_CODE[3]
//                    && bytes[nowOffset + 4]  == HTTP_STATUS_CODE[4]
//                    && bytes[nowOffset + 5]  == HTTP_STATUS_CODE[5]
//                    && bytes[nowOffset + 6]  == HTTP_STATUS_CODE[6]
//                    && bytes[nowOffset + 7]  == HTTP_STATUS_CODE[7]
//                    && bytes[nowOffset + 8]  == HTTP_STATUS_CODE[8]
//                    && bytes[nowOffset + 9]  == HTTP_STATUS_CODE[9]
//                    && bytes[nowOffset + 10] == HTTP_STATUS_CODE[10]
//                    && bytes[nowOffset + 11] == HTTP_STATUS_CODE[11]
//                    && bytes[nowOffset + 12] == HTTP_STATUS_CODE[12]
//                    && bytes[nowOffset + 13] == HTTP_STATUS_CODE[13]
//                    && bytes[nowOffset + 14] == HTTP_STATUS_CODE[14]
//                    && bytes[nowOffset + 15] == HTTP_STATUS_CODE[15]
//                    && bytes[nowOffset + 16] == HTTP_STATUS_CODE[16]
//            ) {
//                if (bytes[nowOffset + 17] != HTTP_STATUS_CODE[17]
//                    || bytes[nowOffset + 18] != HTTP_STATUS_CODE[18]
//                    || bytes[nowOffset + 19] != HTTP_STATUS_CODE[19]
//                ) {
//                    return true;
//                }
//            }
//
//            if (       bytes[nowOffset]     == ERROR_EQUAL_1[0]
//                    && bytes[nowOffset + 1] == ERROR_EQUAL_1[1]
//                    && bytes[nowOffset + 2] == ERROR_EQUAL_1[2]
//                    && bytes[nowOffset + 3] == ERROR_EQUAL_1[3]
//                    && bytes[nowOffset + 4] == ERROR_EQUAL_1[4]
//                    && bytes[nowOffset + 5] == ERROR_EQUAL_1[5]
//                    && bytes[nowOffset + 6] == ERROR_EQUAL_1[6]) {
//                return true;
//            }
//
//            nowOffset++;
//        }
//        return false;
//    }

//    // 检查标签中是否包含错误
//    private boolean checkTags() {
//        int endIndex = nowOffset;
//        while (true) {
//            // "error=1" :  101 114 114 111 114 61 49
//            if (bytes[endIndex] == 49) {
////                if (bytes[endIndex - 1] == 61) {
//                if (bytes[endIndex - 2] == 114) {
////                        if (bytes[endIndex - 3] == 111) {
////                            if (bytes[endIndex - 4] == 114) {
////                                if (bytes[endIndex - 5] == 114) {
//                    if (bytes[endIndex - 6] == 101) {
//                        return true;
//                    }
////                                }
////                            }
////                        }
//                }
////                }
//            }
//
//            // "http.status_code="  :  104 116 116 112 46 115 116 97 116 117 115 95 99 111 100 101 61
//            if (bytes[endIndex] == 61) {
//                if (bytes[endIndex - 16] == 104) {
//                    return bytes[endIndex + 1] != 50
//                            || bytes[endIndex + 2] != 48
//                            || bytes[endIndex + 3] != 48;
//                }
//            }
//
//            if (bytes[endIndex] == 124) {
//                return false;
//            }
//            --endIndex;
//        }
//    }
}

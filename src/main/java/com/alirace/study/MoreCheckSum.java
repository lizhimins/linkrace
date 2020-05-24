package com.alirace.study;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MoreCheckSum {

    private static final Logger log = LoggerFactory.getLogger(MoreCheckSum.class);
    // 结果对比用
    public static HashMap<String, String> resultMap = new HashMap<>();
    private static String resultFileName = "checkSum.data";

    public static void main(String[] args) throws Exception {
        readCheckSum();
    }

    public static void writeCheckSum(String logFileName) throws Exception {

    }

    public static void readCheckSum() throws IOException {
        // file path
        String basePath = MoreCheckSum.class.getResource("/").getPath();
        String path = basePath + resultFileName;
        File file = new File(path);

        // read file
        long start = System.currentTimeMillis();
        String fileStr = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        long end = System.currentTimeMillis();

        JSONObject json = JSONObject.parseObject(fileStr);

        Iterator<Map.Entry<String, Object>> iterator = json.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            String traceId = entry.getKey();
            String md5 = (String) entry.getValue();
            // System.out.println(traceId + ":" + md5);
            resultMap.put(traceId, md5);
        }

        log.info("read " + resultFileName + ": " + (end - start) + " ms, " + "file record num: " + resultMap.size());
    }
}

package com.alirace.study;

import com.alirace.model.TraceLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MoreTraceData {

    private static final String LOG_SEPARATOR = "|";

    private static final Logger log = LoggerFactory.getLogger(MoreTraceData.class);

    // 读日志数据
    private static List<String> logList = new ArrayList<>();

    private static int logOffset = 0;

    private static int[] count = new int[40960];

    public static void MoreTraceData(String logFileName) {
        // file path
        String basePath = MoreTraceData.class.getResource("/").getPath().substring(1);
        String pathStr = basePath + logFileName;
        log.info("Client start to load file: " + pathStr);
        Path path = Paths.get(pathStr);
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                // log.info(line);
                // logList.add(line);
                String traceId = TraceLog.getTraceId(line);
                int index = Math.abs(traceId.hashCode() % 4096);
                // System.out.println(index);
                if (index == 4032) {
                    logList.add(line);
                }
                count[index]++;
            }
        } catch (IOException ioe) {
            log.error(String.valueOf(ioe));
        }
        log.info("Client file load finished, size: " + logList.size());
    }

    public static void writeTraceData(String logFileName) throws IOException {
        // file path
        String basePath = MoreTraceData.class.getResource("/").getPath().substring(1);
        String pathStr = basePath + logFileName;

        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(pathStr));
            // 写出数据
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < logList.size(); j++) {
                    String traceLog = logList.get(j);
                    String traceId = TraceLog.getTraceId(traceLog) + i;
                    int index = traceLog.indexOf(LOG_SEPARATOR);
                    String traceBody = traceLog.substring(index);
                    out.write(traceId + traceBody + "\n");
                }
            }
            log.info("文件创建成功！");
        } catch (IOException e) {
        } finally {
            out.close();
        }
    }

    private static void hashCount() {
//        for (int i = 0; i < logList.size(); i++) {
//            String traceLog = logList.get(i);
//            String traceId = TraceLog.getTraceId(traceLog) + i;
//            int hash1 = (traceId.indexOf(0) - '0') << 8;
//            int hash2 = (traceId.indexOf(1) - '0') << 4;
//            int hash3 = (traceId.indexOf(2) - '0');
//            int index = hash1 + hash2 + hash3;
//            count[index]++;
//        }

        for (int i = 0; i < 4096; i++) {
            System.out.println(i + ": " + count[i]);
        }

        for (int i = 0; i < logList.size(); i++) {
            System.out.println(logList.get(i));
        }
    }

    public static void main(String[] args) throws IOException {
        // 读取服务
        MoreTraceData("trace1.data");
        hashCount();
        //writeTraceData("trace3.data");
        log.info("traceId num: " + logList.size() + "");
    }
}

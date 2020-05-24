package com.alirace.study;

import com.alirace.model.Tag;
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
import java.util.HashMap;
import java.util.List;

public class MoreTraceData extends Thread {

    private static final String LOG_SEPARATOR = "|";

    private static final Logger log = LoggerFactory.getLogger(MoreTraceData.class);

    // 读日志数据
    private static List<String> logList = new ArrayList<>();

    private static int logOffset = 0;

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
                logList.add(line);
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
                    String traceBody = traceLog.substring(index, traceLog.length());
                    out.write(traceId + traceBody + "\n");
                }
            }
            log.info("文件创建成功！");
        } catch (IOException e) {
        } finally {
            out.close();
        }
    }

    public static void main(String[] args) throws IOException {
        // 读取服务
        MoreTraceData("trace1.data");
        writeTraceData("trace3.data");
        log.info("traceId num: " + logList.size() + "");
    }
}

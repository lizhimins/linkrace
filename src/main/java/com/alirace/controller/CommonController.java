package com.alirace.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;
import com.alirace.Application;
import com.alirace.client.ClientService;
import com.alirace.util.HttpUtil;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.alirace.Application.CLIENT_PROCESS_PORT1;
import static com.alirace.Application.CLIENT_PROCESS_PORT2;

@RestController
public class CommonController {

    // 程序是否准备完成
    public static volatile AtomicBoolean isReady = new AtomicBoolean(false);
    private static Integer DATA_SOURCE_PORT = 8000;
    private static AtomicBoolean isBeginning = new AtomicBoolean(false);
    private static String json;

    public static void setReady() {
//        try {
//            Thread.sleep(2990);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        isReady.compareAndSet(false, true);
    }

    public static Integer getDataSourcePort() {
        return DATA_SOURCE_PORT;
    }

    private static String getPath() {
        String port = Application.getSystemPort();
        if (System.getProperty("local") != null) {
            if (CLIENT_PROCESS_PORT1.equals(port)) {
                return "http://47.100.53.20:" + "8004" + "/trace1.data";
            }
            if (CLIENT_PROCESS_PORT2.equals(port)) {
                return "http://47.100.53.20:" + "8004" + "/trace2.data";
            }
        }
        if (CLIENT_PROCESS_PORT1.equals(port)) {
            return "http://127.0.0.1:" + CommonController.getDataSourcePort() + "/trace1.data";
        }
        if (CLIENT_PROCESS_PORT2.equals(port)) {
            return "http://127.0.0.1:" + CommonController.getDataSourcePort() + "/trace2.data";
        }
        return null;
    }

    @RequestMapping("/start")
    public String start() {
        return "suc";
    }

    @RequestMapping("/ready")
    public String ready() {
        return isReady.get() ? "suc" : "fail";
    }

    @RequestMapping("/setParameter")
    public String setParamter(@RequestParam Integer port) throws IOException {
        DATA_SOURCE_PORT = port;
        if (isBeginning.compareAndSet(false, true)) {
            // 开始读入数据
            if (Application.isClientProcess()) {
                // 放入文件地址
                ClientService.setPathAndPull(getPath());
            }

//            if (Application.isBackendProcess()) {
//                HttpUtil.init();
//            }
        }
        return "suc";
    }

    @PostMapping(value = "/api/finished")
    public String callFinish(@RequestParam String result) {
        // System.out.println(result);
        json = result;
        Map<String, String> checkSumMap = (Map) JSON.parseObject(result, new TypeReference<Map<String, String>>() {
        }, new Feature[0]);

        if (checkSumMap == null) {
            System.out.println("fail: " + result);
        } else {
            System.out.println("receive: " + result);
        }

        Iterator<Map.Entry<String, String>> iterator = checkSumMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String key = entry.getKey();
            String value = entry.getValue();
            iterator.remove();
        }
        return result;
    }


    @GetMapping(value = "/result")
    public String callFinish() {
        return json;
    }
}

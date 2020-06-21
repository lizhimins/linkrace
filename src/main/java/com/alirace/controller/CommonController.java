package com.alirace.controller;

import com.alirace.Application;
import com.alirace.client.ClientService;
import com.alirace.util.HttpUtil;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.alirace.Application.CLIENT_PROCESS_PORT1;
import static com.alirace.Application.CLIENT_PROCESS_PORT2;

@RestController
public class CommonController {

    // 程序是否准备完成
    public static volatile AtomicBoolean isReady = new AtomicBoolean(false);
    private static Integer DATA_SOURCE_PORT = 8002;
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
                return "http://10.66.1.107:" + "8004" + "/trace1.data";
            }
            if (CLIENT_PROCESS_PORT2.equals(port)) {
                return "http://10.66.1.107:" + "8004" + "/trace2.data";
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

            if (Application.isBackendProcess()) {
                HttpUtil.init();
            }
        }
        return "suc";
    }

    @PostMapping(value = "/api/finish")
    public String callFinish(@RequestBody String result) {
        System.out.println("result:" + result);
        json = result;
        return result;
    }

    @GetMapping(value = "/result")
    public String callFinish() {
        return json;
    }
}

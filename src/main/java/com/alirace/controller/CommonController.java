package com.alirace.controller;

import com.alirace.Application;
import com.alirace.client.ClientService;
import com.alirace.client.PullService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.alirace.Application.CLIENT_PROCESS_PORT1;
import static com.alirace.Application.CLIENT_PROCESS_PORT2;

@RestController
public class CommonController {

    // 程序是否准备完成
    public static volatile AtomicBoolean isReady = new AtomicBoolean(false);
    private static final String HOST = "localhost";
    private static Integer DATA_SOURCE_PORT = 0;

    public static Integer getDataSourcePort() {
        return DATA_SOURCE_PORT;
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
    public String setParamter(@RequestParam Integer port) throws IOException, InterruptedException {
        DATA_SOURCE_PORT = port;
        // 开始读入数据
        if (Application.isClientProcess()) {
            PullService.path = getPath();
            PullService.start();
        }
        return "suc";
    }

    private static String getPath() {
        String port = Application.getSystemPort();
        if (CLIENT_PROCESS_PORT1.equals(port)) {
            return "http://localhost:" + CommonController.getDataSourcePort() + "/trace1.data";
        }
        if (CLIENT_PROCESS_PORT2.equals(port)) {
            return "http://localhost:" + CommonController.getDataSourcePort() + "/trace2.data";
        }
        return null;
    }
}

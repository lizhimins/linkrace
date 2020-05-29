package com.alirace.controller;

import com.alirace.Application;
import com.alirace.client.ClientService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.alirace.Application.CLIENT_PROCESS_PORT1;
import static com.alirace.Application.CLIENT_PROCESS_PORT2;

@RestController
public class CommonController {

    private static final String HOST = "localhost";
    // 程序是否准备完成
    public static volatile AtomicBoolean isReady = new AtomicBoolean(false);
    private static Integer DATA_SOURCE_PORT = 0;
    private static AtomicBoolean isBeginning = new AtomicBoolean(false);

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
        if (CLIENT_PROCESS_PORT1.equals(port)) {
            // return "http://localhost:" + CommonController.getDataSourcePort() + "/trace1.data";
            // return "http://localhost:" + "8004" + "/trace1.data";
            return "http://10.66.1.107:" + "8004" + "/trace1.data";
        }
        if (CLIENT_PROCESS_PORT2.equals(port)) {
            // return "http://localhost:" + CommonController.getDataSourcePort() + "/trace2.data";
            return "http://localhost:" + "8004" + "/trace2.data";
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
    public String setParamter(@RequestParam Integer port) throws IOException, InterruptedException {
        DATA_SOURCE_PORT = port;
        if (isBeginning.compareAndSet(false, true)) {
            // 开始读入数据
            if (Application.isClientProcess()) {
                // 放入文件地址
                ClientService.setPath(getPath());
                // 开始处理数据
                ClientService.getClientService().start();
            }
        }
        return "suc";
    }
}

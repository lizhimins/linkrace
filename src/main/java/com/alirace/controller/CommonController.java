package com.alirace.controller;

import com.alirace.Application;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicBoolean;

@RestController
public class CommonController {

    public static volatile AtomicBoolean isReady = new AtomicBoolean(false);
    private static String HOST = "localhost";
    private static Integer DATA_SOURCE_PORT = 0;

    public static Integer getDataSourcePort() {
        return DATA_SOURCE_PORT;
    }

    @RequestMapping("/ready")
    public String ready() {
        return isReady.get() ? "suc" : "fail";
    }

    @RequestMapping("/setParameter")
    public String setParamter(@RequestParam Integer port) throws InterruptedException {
        DATA_SOURCE_PORT = port;
        if (Application.isClientProcess()) {
            //FilterService.start();
        }
        return "suc";
    }

    @RequestMapping("/start")
    public String start() {
        return "suc";
    }
}

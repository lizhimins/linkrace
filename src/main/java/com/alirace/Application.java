package com.alirace;

import com.alirace.client.ClientService;
import com.alirace.server.ServerService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@EnableAutoConfiguration
@ComponentScan(basePackages = "com.alirace.controller")
public class Application {

    // 端口常量直接定义在这里
    public static final String CLIENT_PROCESS_PORT1 = "8000";
    public static final String CLIENT_PROCESS_PORT2 = "8001";
    public static final String BACKEND_PROCESS_PORT1 = "8002";
    public static final String BACKEND_PROCESS_PORT2 = "8003";
    public static final String BACKEND_PROCESS_PORT3 = "8004";

    private static String systemPort = BACKEND_PROCESS_PORT1;

    public static String getSystemPort() {
        return System.getProperty("server.port", "8080");
    }

    public static boolean isClientProcess() {
        String port = getSystemPort();
        if (CLIENT_PROCESS_PORT1.equals(port) || CLIENT_PROCESS_PORT2.equals(port)) {
            return true;
        }
        return false;
    }

    public static boolean isBackendProcess() {
        String port = getSystemPort();
        if (BACKEND_PROCESS_PORT1.equals(port)) {
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        String port = getSystemPort();
        // 启动 springboot
        SpringApplication.run(Application.class, "--server.port=" + port);
        if (isBackendProcess()) {
            ServerService.start();
        }
        if (isClientProcess()) {
            ClientService.init();
        }
    }
}

package com.alirace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@EnableAutoConfiguration
@ComponentScan(basePackages = "com.alirace")
public class Application {

    // 端口常量直接定义在这里
    public static final String CLIENT_PROCESS_PORT1 = "8000";
    public static final String CLIENT_PROCESS_PORT2 = "8001";
    public static final String BACKEND_PROCESS_PORT1 = "8002";
    public static final String BACKEND_PROCESS_PORT2 = "8003";
    public static final String BACKEND_PROCESS_PORT3 = "8004";
    public static int BATCH_SIZE = 20000;
    public static int PROCESS_COUNT = 2;

    private static String systemPort = BACKEND_PROCESS_PORT1;

    public static String getSystemPort() {
        return systemPort;
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
        systemPort = args[0];
        int port = Integer.parseInt(systemPort);

        SpringApplication.run(Application.class, "--server.port=" + port);
        if (isBackendProcess()) {
            //CollectService.init(BACKEND_PROCESS_PORT2);
        }
        if (isClientProcess()) {
            //FilterService.init(BACKEND_PROCESS_PORT2);
        }
    }
}

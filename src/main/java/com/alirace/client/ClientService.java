package com.alirace.client;

import com.alirace.server.ServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Cache;

import java.util.ArrayList;
import java.util.List;

public class ClientService {

    private static final Logger log = LoggerFactory.getLogger(ServerService.class);

    public static List<CacheService> services = new ArrayList<>();

    public static void start() {
        log.info("Client initializing start...");
        ClientMonitorService.start();
        for (int i = 0; i < 2; i++) {
            CacheService cacheService = new CacheService();
            cacheService.init();
            services.add(cacheService);
        }
        log.info("Client initializing finish...");
    }
}

//package com.alirace.util;
//
//import com.alirace.model.Tag;
//import org.junit.Test;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//
//public class AhoCorasickAutomationTest {
//
//    private static AhoCorasickAutomation aca = new AhoCorasickAutomation();
//
//    @Test
//    public void find() {
//
//        // String text = "&component=java-spring-rest-template&span.kind=client&http.url=http://localhost:9001/getItem?id=5&peer.port=9001&http.method=GET";
//        // String text = "&component=java-spring-rest-template&span.kind=client&http.url=http://localhost:9001/getItem?id=5&peer.port=9001&http.method=GET";
//        String text = "http.status_code=200&v=j&s&s&sa";
//
//        HashMap<String, List<Integer>> result = aca.find(text);
////
////        System.out.println(text);
////
////        for (Map.Entry<String, List<Integer>> entry : result.entrySet()) {
////            System.out.println(entry.getKey() + " : " + entry.getValue());
////        }
//    }
//
//    @Test
//    public void find2() {
//
//        // String text = "&component=java-spring-rest-template&span.kind=client&http.url=http://localhost:9001/getItem?id=5&peer.port=9001&http.method=GET";
//        // String text = "&component=java-spring-rest-template&span.kind=client&http.url=http://localhost:9001/getItem?id=5&peer.port=9001&http.method=GET";
//        List<String> list = new ArrayList<>();
//        list.add("|http.status_code=200\nabcde");
//        list.add("|http.status_code=200&error=1&v=j&s&s&sa\n");
//        list.add("|http.status_code=201&error=0&v=j&s&s&sa\n");
//        list.add("|http.status_code=200&v=j&s&s&sa\n");
//
//
//        for (int i = 0; i < list.size(); i++) {
////            System.out.println("CASE: " + i + " " + list.get(i).length());
////            System.out.println(list.get(i).replace('\n', '_'));
////            System.out.println(aca.find(list.get(i).getBytes(), 0));
////            System.out.println();
//        }
//    }
//
//    @Test
//    public void find3() {
//        List<String> list = new ArrayList<>();
//        list.add("74ed065dc42bfd3a|1590216545778521|63605ded4e755c95|7a6305ae3050d429|951|OrderCenter|postHandleData|192.168.9.210|db.instance=db&component=java-jdbc&db.type=h2&span.kind=client&__sql_id=1af9zkk&peer.address=localhost:8082");
//        list.add("74ed065dc42bfd3a|1590216545778523|28629b46780e4df4|7a6305ae3050d429|952|LogisticsCenter|db.AlertDao.listByTitleAndUserId(..)|192.168.9.211|http.status_code=200&http.url=http://localhost:9003/getAddress&component=java-web-servlet&span.kind=server&http.method=GET");
//        list.add("74ed065dc42bfd3a|1590216545778525|383bdddf04e62a65|7a6305ae3050d429|953|InventoryCenter|db.ArmsAppDao.selectByComplex(..)|192.168.9.212|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://tracing.console.aliyun.com/getOrder?id=1&peer.port=9002&http.method=GET");
//        list.add("74ed065dc42bfd3a|1590216545778537|1a37264aed8e4969|7a6305ae3050d429|959|InventoryCenter|TraceSegmentService/collect|192.168.9.218|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://tracing.console.aliyun.com/createOrder?id=5&peer.port=9002&http.method=GET");
//        list.add("74ed065dc42bfd3a|1590216545778545|2e1cdfa361d2a720|7a6305ae3050d429|963|OrderCenter|ServiceNameDiscoveryService/discovery|192.168.9.222|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://tracing.console.aliyun.com/getInventory?id=5&peer.port=9005&http.method=GET");
//        list.add("74ed065dc42bfd3a|1590216545778551|75e984af5323e91|7a6305ae3050d429|966|Frontend|db.ArmsAppDao.getAppListByUserId(..)|192.168.9.225|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://tracing.console.aliyun.com/getOrder?id=2&peer.port=9002&http.method=GET");
//        list.add("74ed065dc42bfd3a|1590216545778555|744970d685bcad96|7a6305ae3050d429|968|ItemCenter|tlog.queryPersistenceData|192.168.9.227|&component=java-web-servlet&span.kind=server&sampler.type=const&sampler.param=1&http.url=http://localhost:8081/buyItem&http.method=GET&");
//        list.add("74ed065dc42bfd3a|1590216545778557|4f78e0e1d2b77fda|7a6305ae3050d429|969|OrderCenter|DoSelectTopoInfo|192.168.9.228|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://localhost:9001/getItem?id=2&peer.port=9001&http.method=GET");
//        list.add("74ed065dc42bfd3a|1590216545778565|497d16a2f278832f|7a6305ae3050d429|973|PromotionCenter|DoCheckApplicationExist|192.168.9.232|&component=java-web-servlet&span.kind=server&http.url=http://tracing.console.aliyun.com/createOrder&bizErr=1-InventoryNotEmpty&entrance=mobile&http.method=GET&userId=123888&");
//        list.add("74ed065dc42bfd3a|1590216545778567|92cec186ff8ee53|7a6305ae3050d429|974|ItemCenter|db.ArmsAppDao.selectByPidTypeRegionIdUserId(..)|192.168.9.233|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://tracing.console.aliyun.com/createOrder?id=2&peer.port=9002&http.method=GET");
//        list.add("74ed065dc42bfd3a|1590216545778569|430a9dcd94047abc|7a6305ae3050d429|975|OrderCenter|DoCheckStatus|192.168.9.234|http.status_code=200&component=java-web-servlet&span.kind=server&http.url=http://tracing.console.aliyun.com/createOrder&entrance=pc&http.method=GET&userId=8263");
//        list.add("74ed065dc42bfd3a|1590216545778575|29ed34e554971bf9|7a6305ae3050d429|978|Frontend|db.UserDao.getUser(..)|192.168.9.237|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://localhost:9001/getItem?id=4&peer.port=9001&http.method=GET");
//        list.add("74ed065dc42bfd3a|1590216545778583|4d0ec4b5e1e93592|7a6305ae3050d429|982|LogisticsCenter|db.AlertDao.listByTitleAndUserIdAndFilterStr(..)|192.168.9.241|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://localhost:9001/getItem?id=3&peer.port=9001&http.method=GET");
//        list.add("74ed065dc42bfd3a|1590216545778587|14ab1f84c02a653d|7a6305ae3050d429|984|Frontend|/nginx_status|192.168.9.243|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://localhost:9003/getAddress?id=3&peer.port=9003&http.method=GET");
//        list.add("74ed065dc42bfd3a|1590216545778589|477790887b801f52|7a6305ae3050d429|985|PromotionCenter|/agent/gRPC|192.168.9.244|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://tracing.console.aliyun.com/getInventory?id=3&peer.port=9005&http.method=GET&error=1");
//        list.add("74ed065dc42bfd3a|1590216545778591|3daa32485ce4dda5|74ed065dc42bfd3a|986|ItemCenter|DoGetAppsGrid|192.168.9.245|http.status_code=200&component=java-web-servlet&span.kind=server&http.url=http://tracing.console.aliyun.com/createOrder&entrance=pc&http.method=GET&userId=110");
//        list.add("74ed065dc42bfd3a|1590216545778593|39fea2a224f0907f|3daa32485ce4dda5|987|OrderCenter|DoGetInnerCallChain|192.168.9.246|http.status_code=200&component=java-web-servlet&span.kind=server&http.url=http://tracing.console.aliyun.com/createOrder&entrance=pc&http.method=GET&userId=8970");
//        list.add("74ed065dc42bfd3a|1590216545778595|7664b7b8b418033|3daa32485ce4dda5|988|LogisticsCenter|DoGetTraceTaskAutoStart|192.168.9.247|&component=java-spring-rest-template&span.kind=client&http.url=http://tracing.console.aliyun.com/getOrder?id=3&peer.port=9002&http.method=GET&http.status_code=400");
//
//        for (int i = 0; i < list.size(); i++) {
//            list.set(i, list.get(i) + "\n");
//            System.out.println("CASE: " + i + " " + list.get(i).length());
//            System.out.println(list.get(i).replace('\n', '_'));
//
//            String[] split = list.get(i).split("\\|");
//            System.out.println(Tag.isError(split[8]) ? "Yes" : "No");
//
//            System.out.println(aca.find(list.get(i).getBytes(), 0));
//            System.out.println();
//        }
//    }
//
//    @Test
//    public void find4() {
//        List<String> list = new ArrayList<>();
//        list.add("8dc1e1c1293aa3e|1590216545677239|51d46b3cda75ad0a|6a843d720f7b16fc|522|LogisticsCenter|ServiceNameDiscoveryService/discovery|192.168.98.91|http.status_code=200&component=java-web-servlet&span.kind=server&http.url=http://tracing.console.aliyun.com/createOrder&entrance=pc&http.method=GET&userId=1450&error=1");
//        for (int i = 0; i < list.size(); i++) {
//            list.set(i, list.get(i) + "\n");
//            System.out.println("CASE: " + i + " " + list.get(i).length());
//            System.out.println(list.get(i).replace('\n', '_'));
//
//            String[] split = list.get(i).split("\\|");
//            System.out.println(split[8]);
//            System.out.println(Tag.isError(split[8]) ? "Yes" : "No");
//
//            System.out.println(aca.find(list.get(i).getBytes(), 0));
//            System.out.println();
//        }
//    }
//}
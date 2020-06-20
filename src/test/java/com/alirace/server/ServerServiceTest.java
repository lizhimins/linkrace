package com.alirace.server;

import com.alirace.util.MD5Util;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledHeapByteBuf;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class ServerServiceTest {

    @Test
    public void flushResult() {
//        String truth = ServerService.flushResult(bodyStr.getBytes());
//        String md5 = MD5Util.byteToMD5(bodyStr.getBytes());
//        System.out.println(truth);
//        System.out.println(md5);

//        Assert.assertEquals(truth, md5);
    }

    @Test
    public void flushResult2() {
        String truth = MD5Util.byteToMD5(bodyStr.getBytes());
        String md5 = ServerService.flushResult(bodyStr2.getBytes(), bodyStr3.getBytes());
//        System.out.println(truth);
//        System.out.println(md5);
        Assert.assertEquals(truth, md5);
    }



    @Test
    public void benchMark2() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10_000; i++) {
            String md5 = ServerService.flushResult(bodyStr2.getBytes(), bodyStr3.getBytes());
        }
        long end = System.currentTimeMillis();
        System.out.println("2 It cost: " + (end - start) + " ms");
    }

    @Test
    public void benchMark1() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10_000; i++) {
            String md5 = ServerService.flushResult(bodyStr2.getBytes(), bodyStr3.getBytes());
        }
        long end = System.currentTimeMillis();
        System.out.println("1 It cost: " + (end - start) + " ms");
    }

    private static String bodyStr = ""
            + "447eb726d7d5e8c9|1590216545652169|447eb726d7d5e8c9|0|1261|LogisticsCenter|DoQueryStatData|192.168.58.85|biz=fxtius&sampler.type=const&sampler.param=1\n"
            + "447eb726d7d5e8c9|1590216545652172|521f164a0650351|447eb726d7d5e8c9|1258|InventoryCenter|DoSearchAlertByName|192.168.58.86|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://localhost:9004/getPromotion?id=4&peer.port=9004&http.method=GET\n"
            + "447eb726d7d5e8c9|1590216545652175|347dafb6970e12b8|521f164a0650351|1255|Frontend|TraceSegmentReportService/collect|192.168.58.87|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://localhost:9003/getAddress?id=4&peer.port=9003&http.method=GET\n"
            + "447eb726d7d5e8c9|1590216545652178|14caa7851514f1aa|521f164a0650351|1252|PromotionCenter|DoGetDatas|192.168.58.88|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://tracing.console.aliyun.com/getInventory?id=4&peer.port=9005&http.method=GET\n"
            + "447eb726d7d5e8c9|1590216545652181|a2500deaee4d02f|521f164a0650351|1249|ItemCenter|/status.html|192.168.58.89|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://localhost:9001/getItem?id=3&peer.port=9001&http.method=GET\n"
            + "447eb726d7d5e8c9|1590216545652184|188156abbc17dfcf|521f164a0650351|1246|OrderCenter|noToName2|192.168.58.90|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://localhost:9004/getPromotion?id=3&peer.port=9004&http.method=GET\n"
            + "447eb726d7d5e8c9|1590216545652187|1fdf6f1c165e1167|521f164a0650351|1243|LogisticsCenter|processZipkin|192.168.58.91|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://localhost:9003/getAddress?id=3&peer.port=9003&http.method=GET\n"
            + "447eb726d7d5e8c9|1590216545652190|8732a74a7afec5e|521f164a0650351|1240|InventoryCenter|Register/doEndpointRegister|192.168.58.92|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://tracing.console.aliyun.com/getInventory?id=3&peer.port=9005&http.method=GET\n"
            + "447eb726d7d5e8c9|1590216545652193|32fef78ec5c82ab0|521f164a0650351|1237|Frontend|sls.getOperator|192.168.58.93|http.status_code=200&component=java-web-servlet&span.kind=server&http.url=http://tracing.console.aliyun.com/createOrder&entrance=pc&http.method=GET&userId=110\n"
            + "447eb726d7d5e8c9|1590216545652196|7d610a59da2e8962|521f164a0650351|1234|PromotionCenter|DoGetTProfInteractionSnapshot|192.168.58.94|http.status_code=200&component=java-web-servlet&span.kind=server&http.url=http://tracing.console.aliyun.com/createOrder&entrance=pc&http.method=GET&userId=8970\n"
            + "447eb726d7d5e8c9|1590216545652199|43709914b27298c|521f164a0650351|1231|ItemCenter|db.ArmsAppDao.getAppListByUserIdAllRegion(..)|192.168.58.95|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://tracing.console.aliyun.com/getOrder?id=3&peer.port=9002&http.method=GET\n"
            + "447eb726d7d5e8c9|1590216545652202|1c666119dee0dd90|521f164a0650351|1228|OrderCenter|postHandleData|192.168.58.96|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://tracing.console.aliyun.com/createOrder?id=3&peer.port=9002&http.method=GET\n"
            + "447eb726d7d5e8c9|1590216545652205|15b5b42245efdd6b|521f164a0650351|1225|LogisticsCenter|db.AlertDao.listByTitleAndUserId(..)|192.168.58.97|&component=java-web-servlet&span.kind=server&http.url=http://tracing.console.aliyun.com/getOrder&http.method=GET&&error=1\n"
            + "447eb726d7d5e8c9|1590216545652208|4557ced6f95e1cc|447eb726d7d5e8c9|1222|InventoryCenter|DoSearchAlertTemplates|192.168.58.98|&component=java-spring-rest-template&span.kind=client&http.url=http://tracing.console.aliyun.com/getOrder?id=4&peer.port=9002&http.method=GET\n"
            + "447eb726d7d5e8c9|1590216545652211|6999f212cdb17d03|4557ced6f95e1cc|1219|Frontend|db.ArmsAppDao.selectByComplex(..)|192.168.58.99|&component=java-web-servlet&span.kind=server&http.url=http://tracing.console.aliyun.com/createOrder&entrance=pc&http.method=GET&userId=26758&http.status_code=400\n";

    private static String bodyStr2 = ""
            + "447eb726d7d5e8c9|1590216545652169|447eb726d7d5e8c9|0|1261|LogisticsCenter|DoQueryStatData|192.168.58.85|biz=fxtius&sampler.type=const&sampler.param=1\n"
            + "447eb726d7d5e8c9|1590216545652175|347dafb6970e12b8|521f164a0650351|1255|Frontend|TraceSegmentReportService/collect|192.168.58.87|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://localhost:9003/getAddress?id=4&peer.port=9003&http.method=GET\n"
            + "447eb726d7d5e8c9|1590216545652181|a2500deaee4d02f|521f164a0650351|1249|ItemCenter|/status.html|192.168.58.89|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://localhost:9001/getItem?id=3&peer.port=9001&http.method=GET\n"
            + "447eb726d7d5e8c9|1590216545652187|1fdf6f1c165e1167|521f164a0650351|1243|LogisticsCenter|processZipkin|192.168.58.91|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://localhost:9003/getAddress?id=3&peer.port=9003&http.method=GET\n"
            + "447eb726d7d5e8c9|1590216545652190|8732a74a7afec5e|521f164a0650351|1240|InventoryCenter|Register/doEndpointRegister|192.168.58.92|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://tracing.console.aliyun.com/getInventory?id=3&peer.port=9005&http.method=GET\n"
            + "447eb726d7d5e8c9|1590216545652193|32fef78ec5c82ab0|521f164a0650351|1237|Frontend|sls.getOperator|192.168.58.93|http.status_code=200&component=java-web-servlet&span.kind=server&http.url=http://tracing.console.aliyun.com/createOrder&entrance=pc&http.method=GET&userId=110\n"
            + "447eb726d7d5e8c9|1590216545652202|1c666119dee0dd90|521f164a0650351|1228|OrderCenter|postHandleData|192.168.58.96|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://tracing.console.aliyun.com/createOrder?id=3&peer.port=9002&http.method=GET\n"
            + "447eb726d7d5e8c9|1590216545652205|15b5b42245efdd6b|521f164a0650351|1225|LogisticsCenter|db.AlertDao.listByTitleAndUserId(..)|192.168.58.97|&component=java-web-servlet&span.kind=server&http.url=http://tracing.console.aliyun.com/getOrder&http.method=GET&&error=1\n"
            + "447eb726d7d5e8c9|1590216545652208|4557ced6f95e1cc|447eb726d7d5e8c9|1222|InventoryCenter|DoSearchAlertTemplates|192.168.58.98|&component=java-spring-rest-template&span.kind=client&http.url=http://tracing.console.aliyun.com/getOrder?id=4&peer.port=9002&http.method=GET\n";

    private static String bodyStr3 = ""
            + "447eb726d7d5e8c9|1590216545652172|521f164a0650351|447eb726d7d5e8c9|1258|InventoryCenter|DoSearchAlertByName|192.168.58.86|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://localhost:9004/getPromotion?id=4&peer.port=9004&http.method=GET\n"
            + "447eb726d7d5e8c9|1590216545652178|14caa7851514f1aa|521f164a0650351|1252|PromotionCenter|DoGetDatas|192.168.58.88|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://tracing.console.aliyun.com/getInventory?id=4&peer.port=9005&http.method=GET\n"
            + "447eb726d7d5e8c9|1590216545652184|188156abbc17dfcf|521f164a0650351|1246|OrderCenter|noToName2|192.168.58.90|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://localhost:9004/getPromotion?id=3&peer.port=9004&http.method=GET\n"
            + "447eb726d7d5e8c9|1590216545652196|7d610a59da2e8962|521f164a0650351|1234|PromotionCenter|DoGetTProfInteractionSnapshot|192.168.58.94|http.status_code=200&component=java-web-servlet&span.kind=server&http.url=http://tracing.console.aliyun.com/createOrder&entrance=pc&http.method=GET&userId=8970\n"
            + "447eb726d7d5e8c9|1590216545652199|43709914b27298c|521f164a0650351|1231|ItemCenter|db.ArmsAppDao.getAppListByUserIdAllRegion(..)|192.168.58.95|http.status_code=200&component=java-spring-rest-template&span.kind=client&http.url=http://tracing.console.aliyun.com/getOrder?id=3&peer.port=9002&http.method=GET\n"
            + "447eb726d7d5e8c9|1590216545652211|6999f212cdb17d03|4557ced6f95e1cc|1219|Frontend|db.ArmsAppDao.selectByComplex(..)|192.168.58.99|&component=java-web-servlet&span.kind=server&http.url=http://tracing.console.aliyun.com/createOrder&entrance=pc&http.method=GET&userId=26758&http.status_code=400\n";

}
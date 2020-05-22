package com.alirace.server;

import com.alibaba.fastjson.JSON;
import com.alirace.model.Record;
import com.alirace.netty.MyDecoder;
import com.alirace.netty.MyEncoder;
import com.alirace.util.HttpUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerService implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ServerService.class);

    // 用来存放待合并的数据 traceId -> record
    private static final int MAX_HASHMAP_SIZE = 1024 * 16;
    public static ConcurrentHashMap<String, Record> mergeMap = new ConcurrentHashMap(MAX_HASHMAP_SIZE);

    // 用来存放剩下的数据 traceId -> md5
    public static ConcurrentHashMap<String, String> resultMap = new ConcurrentHashMap(MAX_HASHMAP_SIZE);

    // 发出的查询数量 和 收到的响应数量, 需要支持并发
    public static AtomicInteger queryRequestNum = new AtomicInteger(0);
    public static AtomicInteger queryResponseNum = new AtomicInteger(0);

    // 客户端状态机, 已经完成读入任务的机器数量
    public static AtomicInteger readFinishMachineNum = new AtomicInteger(0);
    // 监听的端口号
    private static int PORT = 8003;

    public static void start() {
        log.info("Server initializing start...");
        // 状态监控服务
        ServerMonitor.start();

        Thread thread = new Thread(new ServerService(), "ServerService");
        thread.start();
    }

    public static void startNetty() throws Exception {
        log.info("Server start listen at " + PORT);
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast("decoder", new MyDecoder());
                            pipeline.addLast("encoder", new MyEncoder());
                            pipeline.addLast(new ServerHandler());
                        }
                    });
            // bind port
            ChannelFuture future = bootstrap.bind(PORT).sync();
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    // 结果转移 + 刷盘
    public static void flushResult(Record record) {
        // log.info(record.toString());
        // String traceId = record.getTraceId();
        // Iterator<TraceLog> iterator = record.getLinkedList().iterator();
//        StringBuffer sb = new StringBuffer();
//        while (iterator.hasNext()) {
//            sb.append(iterator.next().toString());
//            sb.append("\n");
//            iterator.remove();
//        }
//        // String truth = VisualizationCheckSum.resultMap.get(traceId);
//        String md5 = MD5Util.strToMd5(sb.toString()).toUpperCase();
        // String ans = md5.equals(truth) ? "Yes" : "No";
        // log.info(String.format("TraceId: %16s, MD5: %32s, Cal: %32s %3s %d", traceId, truth, md5, ans, count++));
        // log.info(String.format("TraceId: %16s, MD5: %4s, Cal: %4s %3s %d",
        //        traceId, truth.substring(0, 4), md5.substring(0, 4), ans, CollectService.totalYes));
        // if (ans == "Yes") {
        //     CollectService.totalYes++;
        // } else {
        //    log.info(record.toString());
        // }
//        resultMap.put(traceId, md5);
//        CollectService.mergeMap.remove(traceId);
    }

    // http 调用上传接口
    public static void uploadData() {
        try {
            String result = JSON.toJSONString(resultMap);
            RequestBody body = new FormBody.Builder()
                    .add("result", result).build();
            String url = String.format("http://localhost:%s/api/finished", "9000");
            // String url = String.format("http://localhost:%s/api/finished", CommonController.getDataSourcePort());
            Request request = new Request.Builder().url(url).post(body).build();
            Response response = HttpUtil.callHttp(request);
            if (response.isSuccessful()) {
                response.close();
                log.warn("Server success to sendCheckSum, result.");
                return;
            }
            log.warn("fail to sendCheckSum:" + response.message());
            response.close();
        } catch (Exception e) {
            log.warn("fail to call finish", e);
        }
        log.info("Server data upload success...");
    }

    @Override
    public void run() {
        try {
            startNetty();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

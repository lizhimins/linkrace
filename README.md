# 首届云原生编程挑战赛1

## 题目与链接
[实现一个分布式统计和过滤的链路追踪](https://tianchi.aliyun.com/competition/entrance/231790/information)

## 题目解读
数据来源：采集自分布式系统中的多个节点上的调用链数据，每个节点一份数据文件。数据格式进行了简化，每行数据(即一个span)包含如下信息：

traceId | startTime | spanId | parentSpanId | duration | serviceName | spanName | host | tags

具体各字段的：

traceId：全局唯一的Id，用作整个链路的唯一标识与组装  
startTime：调用的开始时间  
spanId: 调用链中某条数据(span)的id  
parentSpanId: 调用链中某条数据(span)的父亲id，头节点的span的parantSpanId为0
duration：调用耗时  
serviceName：调用的服务名  
spanName：调用的埋点名  
host：机器标识，比如ip，机器名  
tags: 链路信息中tag信息，存在多个tag的key和value信息。格式为key1:val1&key2:val2&key3:val3 比如 http.status_code:200&error:1  

d614959183521b4b|1587457762873000|d614959183521b4b|0|311601|order|getOrder|192.168.1.3|http.status_code:200

d614959183521b4b|1587457762877000|69a3876d5c352191|d614959183521b4b|305265|Item|getItem|192.168.1.3|http.status_code:200

文件2有

d614959183521b4b|1587457763183000|dffcd4177c315535|d614959183521b4b|980|Loginc|getLogisticy|192.168.1.2|http.status_code:200

d614959183521b4b|1587457762878000|52637ab771da6ae6|d614959183521b4b|304284|Loginc|getAddress|192.168.1.2|http.status_code:200

1.2、数据流
## 生成测试文件
### 化简背景
单个服务与多个服务

## 消息格式

类型	| 名称		 | 字节序列	| 取值范围	  | 备注
--- 	| ----- 	 | ---------| ---------   |----
消息头	| msgType	 | 0 		|0x00-0xff    |消息类型
消息头	| len		 |1-4		|0-2147483647 |消息体长度
消息体	| body		 |变长		|0-           |消息体

私有通信协议:

类型 | 消息体格式 | body 内容
--- | --- | ---
0x00| Upload | Record 的序列化
0x01| Long | 当前处理的时间戳偏移量, 用来控制双机同步
0x02| String | finish flag, 请求上传完成
0x10| Query | String traceId, 汇总服务向过滤服务缓存查询
0x11| Long | 0x01 的响应, 同步消费情况
0x12| WatchData | 蓝色监控数据下发
0x13| String | 让客户端关闭连接, 客户端结束

服务器收到两个客户端完成信号, 这时候应该所有查询都已经下发下去了, 客户端标记自己完成。
接下来还有一些数据是要合并到hashHap里面的, 

## 数据分析
文件1 log size: 1514950, Error spanId num: 393 , Error traceId num: 393  
文件2 log size: 1514950, Error spanId num: 1207 , Error traceId num: 800  
合并 total Error spanId num: 1600 , Error traceId num: 800  

需要做的事情:
tag 字段需要做兼容处理

处理时序:
启动服务器, 客户端等待  
客户端发送开始, 等待服务端批准  
收到第一个0x11, 服务器批准, 双机开始读入

文件1 最大长度 132-344
文件2 最大长度 128-344
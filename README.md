核心模块

timer-core

负责：提供系统的核心接口和基础实现，包括时间轮算法、任务模型定义
主要组件：时间轮实现、任务接口定义、事件总线
依赖：基础工具类


timer-common

负责：通用工具类、常量定义、异常处理
主要组件：工具类、常量、异常定义
依赖：无



存储模块

timer-storage

负责：任务持久化、存储接口定义
主要组件：存储接口、数据模型
依赖：timer-common


timer-storage-mysql

负责：MySQL存储实现
主要组件：MySQL持久化实现
依赖：timer-storage, timer-common


timer-storage-redis

负责：Redis缓存实现
主要组件：Redis缓存实现
依赖：timer-storage, timer-common



调度模块

timer-scheduler

负责：任务调度核心逻辑
主要组件：调度器、调度策略、负载均衡
依赖：timer-core, timer-storage


timer-executor

负责：任务执行、重试、超时处理
主要组件：执行器、线程池管理、任务上下文
依赖：timer-core, timer-common



分布式协调模块

timer-registry

负责：服务注册发现、集群管理
主要组件：注册中心客户端、节点管理
依赖：timer-common


timer-consistency

负责：分布式一致性、选主
主要组件：分布式锁、选主算法
依赖：timer-registry, timer-common



通信模块

timer-transport

负责：定义通信接口
主要组件：通信协议、序列化
依赖：timer-common


timer-transport-http

负责：HTTP通信实现
主要组件：HTTP客户端与服务端
依赖：timer-transport, timer-common


timer-transport-rpc

负责：RPC通信实现
主要组件：RPC客户端与服务端
依赖：timer-transport, timer-common



客户端模块

timer-client

负责：客户端API封装
主要组件：客户端API、任务提交
依赖：timer-transport, timer-common



监控和管理模块

timer-monitor

负责：系统监控、指标收集
主要组件：监控指标、埋点
依赖：timer-common


timer-admin

负责：管理控制台
主要组件：Web界面、REST API
依赖：timer-client, timer-monitor



示例和测试模块

timer-example

负责：示例代码
主要组件：使用示例
依赖：timer-client


timer-test

负责：集成测试
主要组件：测试用例
依赖：所有相关模块



启动模块

timer-bootstrap

负责：系统引导和启动
主要组件：启动器、配置加载
依赖：各个核心模块
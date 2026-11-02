# 🌱 银发无忧社区公益养老平台 (Silver Community)

> 基于 Spring Cloud Alibaba 微服务架构与 uni-app 跨平台前端构建的社区公益养老服务平台

## 📖 项目简介

随着我国老龄化程度不断加深，社区居家养老成为主流趋势。然而，传统的社区养老服务普遍存在**信息不对称、服务过程不透明、多方协作能力弱**等痛点。

本项目旨在构建一个面向 **“老人 — 子女（监护人） — 志愿者 — 社区管理员”** 四端协作的公益养老平台。系统实现了工单的**发布、审核、接单、组队、完成、验收、撤销**全流程闭环管理，并内置了 SOS 紧急求助与工单状态变更通知机制，让社区养老服务更高效、更透明、更温暖。

## 🏗️ 系统架构

本项目采用**前后端分离**与**微服务**架构设计，保证系统的高内聚与低耦合。

```text
┌────────────────────────────────────────────────────────┐
│                    前端 (uni-app)                      │
│     首页 | 登录注册 | 老人端 | 子女端 | 志愿者端 | 管理端     │
└─────────────────────────┬──────────────────────────────┘
                          │ HTTP (RESTful API)
┌─────────────────────────▼──────────────────────────────┐
│               统一网关 (silver-gateway)                 │
│            端口: 8080 | 路由转发 | 跨域处理               │
└───────────┬─────────────────────────────┬──────────────┘
            │                             │
┌───────────▼───────────┐     ┌───────────▼───────────┐
│    用户服务 (8081)     │     │    工单服务 (8082)     │
│  (service-user)       │     │  (service-order)      │
│  用户/健康/绑定关系    │◄────┤  工单/公告/告警/通知    │
└───────────┬───────────┘Feign└───────────┬───────────┘
            │                             │
            └─────────────┬───────────────┘
                          │
┌─────────────────────────▼──────────────────────────────┐
│                   Nacos 注册中心 (8848)                 │
└─────────────────────────┬──────────────────────────────┘
                          │ JDBC
┌─────────────────────────▼──────────────────────────────┐
│                   MariaDB 数据库 (3306)                 │
└────────────────────────────────────────────────────────┘
```

<img width="1369" height="765" alt="image" src="https://github.com/user-attachments/assets/8e7978f2-9a35-432b-99bb-13ce03bd5f76" />


## ✨ 核心亮点与特色功能

- 多角色协同与代发机制：子女可绑定老人，不仅能随时查看老人的健康档案和工单记录，还能远程“代发工单”，解决异地尽孝难题。

- 全生命周期工单状态机：涵盖 待审核、审核中、待接单、待组队、进行中、待验收、已完成、已取消、已拒绝、已撤销 10种状态，精准适配现实业务流转。

- 多人组队接单模式：支持一个工单需求多人（如多名志愿者陪诊）。志愿者接单后进入“待组队”，人数凑齐自动流转为“进行中”。

- 并发审核防重机制：引入“审核锁定”。普通管理员点击进入工单详情时，该工单对其他普通管理员变为“审核中”只读状态。超级管理员（Admin）不受限制，且拥有强行分派和一键撤销的特权。

- SOS 一键紧急求助：老人端提供醒目的一键求助按钮，触发后通过服务间调用，向绑定的所有子女推送紧急告警消息。

- 主动状态通知：当已接单的工单被发布者修改或取消，或审核状态发生变更时，系统会通过轮询机制向志愿者发送站内通知。

<img width="1624" height="855" alt="image" src="https://github.com/user-attachments/assets/53cba61b-16dc-4dfe-9853-0bf8b4257bfb" />

<img width="1354" height="967" alt="image" src="https://github.com/user-attachments/assets/9c85d015-ee27-4575-8328-216891e180dc" />


## 🛠️ 技术栈

### 后端技术

核心框架：JDK 17, Spring Boot 2.6.13

微服务架构：Spring Cloud 2021.0.5, Spring Cloud Alibaba 2021.0.5.0

注册中心：Nacos Server 2.2.3

网关：Spring Cloud Gateway

远程调用：OpenFeign + LoadBalancer

持久层：MyBatis-Plus 3.5.3.1

数据库：MariaDB 10.11 (兼容 MySQL 8.0+)

构建工具：Maven 3.9+

### 前端技术

框架：uni-app (Vue 3 / Vue 2 均可支持)

开发工具：HBuilder X

状态管理与存储：sessionStorage (H5端关闭即清除)

## 📂 项目目录结构

```text
silver-community/                  # 后端父工程
├── silver-common/                 # 公共模块 (实体类 DTO R Feign接口)
├── silver-gateway/                # 网关微服务 (端口 8080)
├── service-user/                  # 用户微服务 (端口 8081)
└── service-order/                 # 工单微服务 (端口 8082)

silver-frontend/                   # 前端 uni-app 项目
├── utils/                         # 封装请求 (request.js) 与 存储 (storage.js)
├── static/                        # 静态资源
└── pages/                         # 页面目录
    ├── index/                     # 首页 (含最新公告与社区需求)
    ├── login/                     # 登录页
    ├── register/                  # 注册页 (分身份注册)
    ├── elder/                     # 老人端主页
    ├── guardian/                  # 子女端主页及档案页
    ├── volunteer/                 # 志愿者端主页
    ├── admin/                     # 管理员控制台及用户详情
    ├── news/                      # 新闻公告列表与详情
    └── order/                     # 工单列表与工单详情
```

## 🚀 快速启动指南

1. 数据库准备
   新建一个名为 silver_community 的数据库（字符集 utf8mb4），并依次执行项目根目录下的 SQL 脚本建表（或直接导入项目提供的 silver.sql 文件）。注意：需执行 UPDATE sys_user SET role = 5 WHERE username = 'admin'; 将初始管理员升级为超级管理员。

2. 启动 Nacos 注册中心
   下载并解压 Nacos Server (推荐 2.2.3 版本)。

进入 bin 目录，打开命令行执行：

```bash
startup.cmd -m standalone
```

浏览器访问 http://localhost:8848/nacos，默认账号密码均为 nacos。

3. 启动后端微服务
   使用 IntelliJ IDEA 打开后端父工程 silver-community。

配置 Maven 依赖并刷新（建议配置阿里云镜像）。

修改 service-user 和 service-order 中的 application.yml，将数据库密码修改为你本地的 MariaDB 密码。

依次启动：

UserApplication (8081)

OrderApplication (8082)

GatewayApplication (8080)

4. 启动前端项目
   使用 HBuilder X 打开前端目录 silver-frontend。

修改 utils/request.js 中的 BASE_URL（默认 http://localhost:8080/api）。

点击菜单栏 运行 -> 运行到浏览器 -> Chrome。

## 🧪 测试账号说明

系统内置了不同角色的基础测试账号（密码均为 123456），方便快速体验各端功能：

超级管理员：admin

普通管理员：root

老人：注册时选择“老人”身份

子女（监护人）：注册时选择“子女”身份

志愿者：注册时选择“志愿者”身份

## ⭐部分项目展示

- 老人端
<img width="394" height="671" alt="image" src="https://github.com/user-attachments/assets/2084aa90-4a8a-44b7-9b89-e2bd75684f1a" />
<img width="394" height="697" alt="image" src="https://github.com/user-attachments/assets/100d23f1-a0f9-499d-b4e6-55c5b16de816" />
<img width="459" height="689" alt="image" src="https://github.com/user-attachments/assets/6df485f6-2a33-4e06-8c45-e915f49f1c65" />
<img width="799" height="211" alt="image" src="https://github.com/user-attachments/assets/d4a2d41b-dbfb-4675-82cf-dae9f5db6451" />


- 监护人端
<img width="450" height="642" alt="image" src="https://github.com/user-attachments/assets/cd6aae74-69e6-4a44-97ab-a6bcf1be749d" />
<img width="438" height="642" alt="image" src="https://github.com/user-attachments/assets/6f0dd5df-128b-4a1e-b849-4d2b79c1948d" />
<img width="387" height="637" alt="image" src="https://github.com/user-attachments/assets/144f19fb-12b3-4f96-880f-80793d162aa2" />
<img width="380" height="642" alt="image" src="https://github.com/user-attachments/assets/82b633f1-2b27-4947-aedd-fa06b7a25c9a" />


- 志愿者端

<img width="365" height="614" alt="image" src="https://github.com/user-attachments/assets/3d95a113-b598-4d7b-bf14-529224fc8f8a" />
<img width="362" height="598" alt="image" src="https://github.com/user-attachments/assets/8da9766e-8b35-4868-a582-7cd34d2042fb" />
<img width="353" height="253" alt="image" src="https://github.com/user-attachments/assets/b10b8d32-77cf-46d9-bd3b-ed6ba30ff14f" />
<img width="370" height="622" alt="image" src="https://github.com/user-attachments/assets/e9ab08b9-681d-4ec9-8d58-44522658920e" />
<img width="467" height="242" alt="image" src="https://github.com/user-attachments/assets/d08cb7cf-95bd-445a-9165-d8000039534c" />
<img width="433" height="727" alt="image" src="https://github.com/user-attachments/assets/c8f1a8cc-3170-4a4d-902f-bff19f021fd5" />


- 管理员端

<img width="750" height="325" alt="image" src="https://github.com/user-attachments/assets/5af7693a-6b6c-40c6-8235-249a6a44e610" />

<img width="368" height="653" alt="image" src="https://github.com/user-attachments/assets/47c32bb8-e512-4ed8-9a4a-66cd32f12bb9" />

<img width="447" height="587" alt="image" src="https://github.com/user-attachments/assets/39a15e99-bf5f-4c2f-a928-931b6f54cace" />

<img width="408" height="516" alt="image" src="https://github.com/user-attachments/assets/28ae9562-4c54-4b02-b1df-1dbc922a2541" />

<img width="420" height="537" alt="image" src="https://github.com/user-attachments/assets/a9c1c99f-108d-459f-bc3b-42017319645a" />

<img width="489" height="627" alt="image" src="https://github.com/user-attachments/assets/1ada3b2e-2f79-48e9-9eec-ad6c3dd73665" />



## ⚠️ 注意事项

本系统作为学习设计项目，暂未接入 Redis，Token 认证机制采用简易版（核心关注点在业务流转与微服务架构）。生产环境请务必加入 Spring Security + JWT + Redis 完善鉴权。

微服务间的服务发现和调用依赖 Nacos，请务必保证在启动微服务前 Nacos 已经处于运行状态。

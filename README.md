# 渔具秒杀商城（project-a-trade）

基于 **Spring Boot 4.0.7 + Java 17 + MyBatis-Plus 3.5.17 + Spring Security 7 + Redis + RabbitMQ（延迟消息）** 的交易类项目，覆盖「商品 → 库存 → 秒杀 → 订单状态机 → 支付回调 → 超时关单 → 退款」完整闭环。

- 后端端口：**8080**，接口统一前缀 `/api`，共 **29 个接口**
- 前端端口：**5173**（Vue 3 + Vite 3 + Element Plus + Pinia + Axios）
- 数据库：`trade_db`（**12 张表**），Redis 使用 **db=0**
- 默认账号（明文密码均为 `123456`）：`buyer1`（买家）、`admin`（运营管理员）

---

## 1. 技术栈

| 层 | 技术 |
|---|---|
| 框架 | Spring Boot 4.0.7（`spring-boot-starter-webmvc`） |
| 语言 | Java 17 |
| 持久层 | MyBatis-Plus 3.5.17（`mybatis-plus-spring-boot4-starter` + `mybatis-plus-jsqlparser`）、MySQL 8.0 |
| 安全 | Spring Security 7（无状态 JWT + Redis 白名单 + `@PreAuthorize`） |
| 缓存 | Redis（Lettuce）：库存预扣、幂等、商品缓存、分布式锁、限流、Token 白名单 |
| 消息 | RabbitMQ（TTL + 死信队列延迟关单；`app.mq.enabled=false` 时由 `@Scheduled` 兜底） |
| 令牌 | JJWT 0.11.5（HS256） |
| 其他 | Lombok、Jakarta Validation |

> Boot 4 的 Web 启动器叫 `spring-boot-starter-webmvc`；MyBatis-Plus 对 Boot 4 提供 `mybatis-plus-spring-boot4-starter`，与 Boot 3 示例不同。

---

## 2. 目录结构

```
project-a-trade/
├── backend/                         # 单 Maven 模块，包 com.pyh.trade
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/pyh/trade/
│       │   ├── common/              Result/ResultCode/BusinessException/GlobalExceptionHandler/PageResult/Constants
│       │   ├── config/              SecurityConfig/MybatisPlusConfig/RedisConfig/RabbitMQConfig/WebMvcConfig
│       │   ├── controller/          Auth/Product/Seckill/Order/Pay/Refund/Admin*/Stats
│       │   ├── service/ + impl/     业务实现，@Transactional 边界
│       │   ├── mapper/              BaseMapper + 自定义条件 UPDATE
│       │   ├── entity/ dto/ vo/     12 张表实体、入参、出参
│       │   ├── security/            JwtAuthenticationFilter/JwtUtils/LoginUser/EntryPoint/AccessDeniedHandler
│       │   ├── mq/                  OrderDelayProducer/OrderCloseConsumer/OrderDelayMessage
│       │   ├── task/                OrderCloseTask/StockReconcileTask
│       │   └── utils/               RedisKeys/IdGenerator/SecurityUtils
│       └── resources/
│           ├── application.yml
│           ├── application-dev.yml.example
│           ├── lua/                 seckill_deduct.lua / rate_limit.lua / unlock.lua
│           └── mapper/              StatsMapper.xml
├── frontend/                        # Vue 3 + Vite 3
├── sql/schema.sql                   # 建库 + 12 张表
├── sql/data.sql                     # 种子数据（可直接演示秒杀/支付/退款）
├── docs/                            # 项目文件结构 / 运行介绍 / 功能介绍
└── README.md
```

---

## 3. 三步启动

### 第 1 步：建库导数据

```bash
mysql -uroot -proot < sql/schema.sql
mysql -uroot -proot < sql/data.sql
```

### 第 2 步：启动 Redis 与后端

```bash
# Redis（任选其一；本项目用 db=0）
docker run -d --name trade-redis -p 6379:6379 redis:7-alpine

cd backend
mvn clean package -DskipTests
java -jar target/trade-1.0.0.jar
```

- 本机没有 RabbitMQ 时保持 `app.mq.enabled=false`（默认），关单由 `OrderCloseTask` 每分钟扫表兜底。
- 数据库账号密码、JWT 密钥、支付回调密钥都可用环境变量覆盖：`DB_USERNAME` / `DB_PASSWORD` / `JWT_SECRET` / `PAY_SECRET`。

### 第 3 步：启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173`，Vite 已把 `/api` 代理到 `http://localhost:8080`。

---

## 4. 默认账号与演示路径

| 账号 | 密码 | 角色 | 用途 |
|---|---|---|---|
| `buyer1` | `123456` | 买家 | 商品、秒杀、订单、支付、退款 |
| `admin` | `123456` | 运营管理员 | 商品/库存/活动/订单/退款/报表 |

**主流程验证**

1. `buyer1` 登录 → 商品列表 → 商品详情；
2. 秒杀页 → 秒杀下单 → 我的订单（待支付）；
3. 订单详情 → 模拟支付（走 `/api/pay/mock/{orderNo}`，内部复用支付回调验签/幂等/状态机）；
4. 订单状态变为「已支付」→ 申请退款；
5. `admin` 登录 → 退款审核 → 订单变为「已退款」；
6. `admin` 配置秒杀活动 → 预热库存 → 前端秒杀页库存实时变化。

> 支付回调是**本地模拟网关**（HMAC-SHA256 验签），不接真实微信/支付宝；接口与状态机是真实的。

---

## 5. 关键设计（面试可讲）

- **秒杀防超卖**：Redis Lua 原子「判库存 + 判重复 + DECR」→ DB 条件 UPDATE 兜底 → 唯一索引 `(user_id, seckill_sku_id)`；失败补偿 Redis。
- **订单状态机**：`0 待支付 → 1 已支付 → 2 已发货 → 3 已完成`，`0 → 4 已关闭`，`1/2/3 → 5 退款中 → 6 已退款`；所有流转带 `WHERE status=#{from}`。
- **支付回调防重**：验签 → `SETNX lock:pay:{orderNo}` → `t_payment_record.channel_trade_no` 唯一索引 → 状态机 CAS。
- **延迟关单**：RabbitMQ 队列级 TTL 30 分钟 + 死信交换机；消费者手动 ACK + `t_mq_consume_log` 幂等；MQ 关闭时走 `@Scheduled`。
- **缓存**：商品详情 Redis Hash + 逻辑过期 + 互斥重建；空值缓存防穿透。
- **认证**：Spring Security 无状态过滤器链 + JWT（只放 userId/jti/ver）+ Redis 白名单，登出即失效。

---

## 6. 文档

- [项目文件结构](docs/项目文件结构.md)
- [项目运行介绍](docs/项目运行介绍.md)
- [项目功能介绍](docs/项目功能介绍.md)
- [接口文档（双项目）](../接口文档.md)

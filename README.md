# fish-trade-mall · 渔具秒杀商城

> **A full-stack seckill & trade platform** — Spring Boot 4 + Vue 3 + Redis + RabbitMQ.
> 面向钓友的渔具限时秒杀与交易闭环，覆盖商品、库存、秒杀、订单状态机、支付回调、超时关单与退款。

---

## 项目简介

钓具秒杀和普通商城不同：库存少、瞬时流量高、订单有时限，超时未支付要自动关单并回补库存。
本项目把这套线下交易流程做成了可演示的线上系统：

- 买家：注册登录、浏览商品与秒杀场次、秒杀下单、查看订单、模拟支付、申请退款
- 运营：商品与 SKU 管理、库存调整、秒杀活动配置与预热、订单查询、退款审核、销售看板

## 文档索引

| 文档 | 内容 |
|---|---|
| [docs/项目文件结构.md](docs/项目文件结构.md) | 逐文件职责、分层结构、规模速查 |
| [docs/项目运行介绍.md](docs/项目运行介绍.md) | 环境、建库、启动、默认账号、主流程验证、12 条排错 |
| [docs/项目功能介绍.md](docs/项目功能介绍.md) | 角色×模块、用户故事、技术实现、涉及表/接口 |
| [sql/schema.sql](sql/schema.sql) · [sql/data.sql](sql/data.sql) | 12 张表建表脚本与可演示种子数据 |

## 功能概览

| 角色 | 主要功能 |
|---|---|
| 买家 | 注册登录、登出、当前用户；商品列表/详情；秒杀场次/详情/下单；订单列表/详情/取消；模拟支付；退款申请与进度 |
| 运营管理员 | 商品/SKU/库存 CRUD；秒杀活动创建与预热；订单查询；退款审核；销售看板（今日销售额、订单数、待退款、库存预警、7 日趋势） |

## 技术栈

| 层次 | 技术 |
|---|---|
| 前端 | Vue 3.2.38、Vite 3.0.9、Vue Router 4.1.5、Pinia 2.1.7、Element Plus 2.14.3、Axios 1.18.1 |
| 后端 | Spring Boot 4.0.7（Java 17）、Spring Security 7、MyBatis-Plus 3.5.17（boot4 starter + jsqlparser）、JJWT 0.11.5、Lombok、Jakarta Validation |
| 中间件 | Redis（Lettuce，db=0）、RabbitMQ（可选，延迟关单） |
| 数据库 | MySQL 8.0、HikariCP |
| 构建 | Maven、npm |

## 目录结构

```text
project-a-trade/
├─ backend/                             单 Maven 模块，包 com.pyh.trade
│  ├─ src/main/java/com/pyh/trade/
│  │  ├─ controller/                    11 个 Controller，29 个接口
│  │  ├─ service/ + impl/               业务实现，@Transactional 边界
│  │  ├─ mapper/                        BaseMapper + 条件 UPDATE
│  │  ├─ entity/ dto/ vo/               12 张表实体、入参、出参
│  │  ├─ security/                      JWT 过滤器、白名单、401/403 出口
│  │  ├─ mq/                            延迟关单生产者/消费者
│  │  ├─ task/                          OrderCloseTask、StockReconcileTask
│  │  └─ config/ common/ utils/         配置、统一返回、Redis Key、签名工具
│  └─ src/main/resources/
│     ├─ application.yml                端口 8080、数据源、Redis、业务开关
│     ├─ application-dev.yml.example    本地开发配置样例
│     ├─ lua/                           3 个 Lua：预扣 / 限流 / 解锁
│     └─ mapper/StatsMapper.xml         看板统计 SQL
├─ frontend/                            Vue 3 + Vite 3（12 个页面 / 14 条路由）
├─ sql/                                 schema.sql（12 表）、data.sql（种子数据）
├─ docs/                                项目文件结构 / 运行介绍 / 功能介绍
└─ README.md
```

## 核心设计要点

1. **秒杀防超卖**：`SeckillServiceImpl` 先执行 `lua/seckill_deduct.lua`，在一次 Lua 调用里完成
   「判重复 + 判库存 + DECR」；DB 侧再用 `UPDATE ... WHERE stock >= 1` 条件扣减与唯一索引
   `(user_id, seckill_sku_id)` 兜底；DB 失败时补偿 Redis 库存并删除幂等标记。
2. **订单状态机**：`0 待支付 → 1 已支付 → 2 已发货 → 3 已完成`，`0 → 4 已关闭`，
   `1/2/3 → 5 退款中 → 6 已退款`；所有流转带 `WHERE status = #{from}`，影响 0 行即并发冲突。
3. **支付回调防重**：HMAC-SHA256 验签 → `SETNX lock:pay:{orderNo}` → `t_payment_record`
   的 `channel_trade_no` 唯一索引 → 订单状态机 CAS。`/api/pay/mock` 是本地模拟网关，走同一套回调逻辑。
4. **延迟关单**：RabbitMQ 队列级 TTL 30 分钟 + 死信交换机；消费者手动 ACK，
   `t_mq_consume_log` 做消费幂等；`app.mq.enabled=false` 时由 `@Scheduled OrderCloseTask` 扫表兜底。
5. **缓存**：商品详情用 Redis Hash + 逻辑过期（`data` + `expireAt`），过期后互斥重建、
   抢不到锁返回旧值；空值缓存 60 秒防穿透；更新商品直接删 key。
6. **无状态认证**：Spring Security 过滤器链 + JWT（只放 `userId/jti/ver`）+ Redis 白名单；
   登出删白名单，改密/禁用自增 `tokenVersion`；`@PreAuthorize` 覆盖运营接口。
7. **工程规范**：统一 `Result<T>`、全局异常分类、`@Transactional` 只在 service 实现、
   金额 `BigDecimal + HALF_UP`、时间 `LocalDateTime`、Controller 不写业务。

## 本地运行

### 1. 准备数据库

```bash
mysql -uroot -p < sql/schema.sql
mysql -uroot -p trade_db < sql/data.sql
```

`data.sql` 会写入 2 个演示账号、3 个商品、1 个秒杀活动（2 个 SKU）和 3 条不同状态的订单：

| 角色 | 账号 | 密码 | 入口 |
|---|---|---|---|
| 买家 | `buyer1` | `123456` | `http://localhost:5173` |
| 运营管理员 | `admin` | `123456` | `http://localhost:5173/admin/dashboard` |

### 2. 启动 Redis

```bash
docker run -d --name trade-redis -p 6379:6379 redis:7-alpine
```

项目 A 使用 **db=0**。没有 Docker 时，本机 Redis 7（或兼容 5.0+）监听 6379 即可。

### 3. 启动后端

```bash
cd backend
mvn clean package -DskipTests
java -jar target/trade-1.0.0.jar
# 或 mvn spring-boot:run
```

默认监听 `http://localhost:8080`，接口统一前缀 `/api`。数据库/Redis/密钥均可通过环境变量覆盖：

| 变量 | 说明 | 默认值 |
|---|---|---|
| `DB_HOST` / `DB_PORT` | MySQL 地址/端口 | `localhost` / `3306` |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号/密码 | `root` / `root` |
| `REDIS_HOST` / `REDIS_PORT` | Redis 地址/端口 | `localhost` / `6379` |
| `JWT_SECRET` | JWT 签名密钥（≥32 字节） | 本地开发默认值 |
| `PAY_SECRET` | 支付回调 HMAC 密钥 | 本地开发默认值 |
| `MQ_ENABLED` | 是否启用 RabbitMQ 延迟关单 | `false` |

> 本机没装 RabbitMQ 时保持 `MQ_ENABLED=false`，关单由 `@Scheduled` 兜底；装好后置 `true` 即走延迟队列。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173`，`/api` 已通过 Vite 代理到 `http://localhost:8080`。

### 5. 打包

```bash
# 前端：产物在 frontend/dist/
cd frontend && npm run build

# 后端：产物为 backend/target/trade-1.0.0.jar
cd backend && mvn clean package -DskipTests
```

## 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 16+
- MySQL 8.0+
- Redis 6.0+（5.0.14 便携版也可运行本项目）
- RabbitMQ 3.8+（可选；不装则走定时任务兜底）

## 说明

- 支付回调是**本地模拟网关**（HMAC-SHA256），不接真实微信/支付宝；接口、验签、幂等与状态机是真实的。
- 默认账号密码仅用于演示；生产请用环境变量覆盖，`application-dev.yml` 已被 `.gitignore` 排除。
- 仓库不包含任何真实用户数据、密钥或第三方服务凭据。
- 本项目为个人学习与作品展示用途。

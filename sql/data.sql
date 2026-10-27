-- =============================================================
-- 项目 A：渔具秒杀商城 · 种子数据
-- 执行方式：
--   mysql -uroot -proot trade_db < project-a-trade\sql\data.sql
-- 前置：先执行 schema.sql
-- 说明：
--   1. 所有账号明文密码都是 123456，库里存的是 BCrypt 摘要
--      （已用 BCryptPasswordEncoder 验证 matches("123456", hash) == true）
--   2. 秒杀活动时间窗故意设成「过去 ~ 2027 年」，保证任何时候启动都能演示抢购
--   3. 订单预置 3 种状态：已支付 / 已关闭 / 退款中，配合运营看板与退款审核演示
-- =============================================================

USE trade_db;
SET NAMES utf8mb4;

DELETE FROM t_stock_log;
DELETE FROM t_mq_consume_log;
DELETE FROM t_refund;
DELETE FROM t_payment_record;
DELETE FROM t_order_item;
DELETE FROM t_order;
DELETE FROM t_seckill_sku;
DELETE FROM t_seckill_activity;
DELETE FROM t_stock;
DELETE FROM t_sku;
DELETE FROM t_product;
DELETE FROM t_user;

-- -------------------------------------------------------------
-- 用户：buyer1 买家 / admin 运营（密码均为 123456）
-- -------------------------------------------------------------
INSERT INTO t_user (id, username, password, nickname, phone, status, create_time, update_time) VALUES
(1, 'buyer1', '$2a$10$o74fBgz2GPZhn2zdNWBcpeJcUKTP9r0Ia4KsgTfjAn2KJ89nwYccq', '钓鱼佬小王', '13800000001', 1, NOW(), NOW()),
(2, 'admin',  '$2a$10$o74fBgz2GPZhn2zdNWBcpeJcUKTP9r0Ia4KsgTfjAn2KJ89nwYccq', '运营小明', '13800000002', 1, NOW(), NOW());

-- -------------------------------------------------------------
-- 商品：3 个渔具商品
-- -------------------------------------------------------------
INSERT INTO t_product (id, name, subtitle, cover, price, status, deleted, create_time, update_time) VALUES
(1, '碳素超轻台钓竿', '28 调 · 5.4 米 · 手感轻到怀疑人生', 'https://img.example.com/rod.jpg',    199.00, 1, 0, NOW(), NOW()),
(2, '全金属远投纺车轮', '11 轴 · 5.2:1 速比 · 海钓淡水通吃', 'https://img.example.com/reel.jpg',   189.00, 1, 0, NOW(), NOW()),
(3, '高灵敏纳米浮漂三件套', '吃铅 1.8g · 加粗醒目尾 · 日夜两用', 'https://img.example.com/float.jpg',   89.00, 1, 0, NOW(), NOW());

-- -------------------------------------------------------------
-- SKU：每个商品 2 个规格
-- -------------------------------------------------------------
INSERT INTO t_sku (id, product_id, name, price, status, create_time, update_time) VALUES
(1, 1, '5.4 米 / 28 调', 199.00, 1, NOW(), NOW()),
(2, 1, '6.3 米 / 28 调', 239.00, 1, NOW(), NOW()),
(3, 2, '2000 型 / 浅线杯', 189.00, 1, NOW(), NOW()),
(4, 2, '3000 型 / 深线杯', 209.00, 1, NOW(), NOW()),
(5, 3, '套装 A（3 支装）', 89.00, 1, NOW(), NOW()),
(6, 3, '套装 B（5 支装）', 129.00, 1, NOW(), NOW());

-- -------------------------------------------------------------
-- 库存：可用 + 锁定
-- -------------------------------------------------------------
INSERT INTO t_stock (id, sku_id, total, available, locked, version, create_time, update_time) VALUES
(1, 1, 100, 92, 8, 0, NOW(), NOW()),
(2, 2,  60, 60, 0, 0, NOW(), NOW()),
(3, 3, 120, 118, 2, 0, NOW(), NOW()),
(4, 4,  80, 80, 0, 0, NOW(), NOW()),
(5, 5, 200, 200, 0, 0, NOW(), NOW()),
(6, 6, 150, 145, 5, 0, NOW(), NOW());

-- -------------------------------------------------------------
-- 秒杀活动 + 秒杀 SKU（sku 1 与 sku 5 参加秒杀）
-- 注意：活动创建后必须调 /api/admin/seckill/activities/1/preheat 预热 Redis，
--       未预热也能下单（下单链路会自动用 DB 库存回填），但演示时建议先调一次
-- -------------------------------------------------------------
INSERT INTO t_seckill_activity (id, name, start_time, end_time, status, create_time, update_time) VALUES
(1, '秋季渔具秒杀专场', DATE_SUB(NOW(), INTERVAL 1 DAY), '2027-12-31 23:59:59', 1, NOW(), NOW());

INSERT INTO t_seckill_sku (id, activity_id, sku_id, seckill_price, stock, version, create_time, update_time) VALUES
(1, 1, 1, 99.00, 50, 0, NOW(), NOW()),
(2, 1, 5, 39.90, 30, 0, NOW(), NOW());

-- -------------------------------------------------------------
-- 订单：1 已支付 / 1 已关闭（超时） / 1 退款中
-- 普通订单 activity_id 与 seckill_sku_id 为 NULL，不受 uk_user_seckill 影响
-- -------------------------------------------------------------
INSERT INTO t_order (id, order_no, user_id, activity_id, seckill_sku_id, total_amount, pay_amount, status,
                     pay_time, close_time, version, create_time, update_time) VALUES
(1, 'SO20261026093000000001', 1, NULL, NULL, 199.00, 199.00, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, 0,
 DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 'SO20261026093000000002', 1, NULL, NULL, 189.00, 189.00, 4, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), 0,
 DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(3, 'SO20261026093000000003', 1, NULL, NULL,  89.00,  89.00, 5, DATE_SUB(NOW(), INTERVAL 6 HOUR), NULL, 0,
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 6 HOUR));

INSERT INTO t_order_item (id, order_id, sku_id, product_name, sku_name, price, quantity) VALUES
(1, 1, 1, '碳素超轻台钓竿', '5.4 米 / 28 调', 199.00, 1),
(2, 2, 3, '全金属远投纺车轮', '2000 型 / 浅线杯', 189.00, 1),
(3, 3, 5, '高灵敏纳米浮漂三件套', '套装 A（3 支装）', 89.00, 1);

-- 库存流水：3 笔下单扣减
INSERT INTO t_stock_log (id, order_no, sku_id, type, quantity, create_time) VALUES
(1, 'SO20261026093000000001', 1, 1, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 'SO20261026093000000002', 3, 1, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(3, 'SO20261026093000000003', 5, 1, 1, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- 支付流水：订单 1、3 有支付记录
INSERT INTO t_payment_record (id, order_no, channel_trade_no, amount, status, raw_body, create_time) VALUES
(1, 'SO20261026093000000001', 'MOCK202610260930000001', 199.00, 1, '{"orderNo":"SO20261026093000000001","channel":"MOCK"}', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 'SO20261026093000000003', 'MOCK202610260930000002',  89.00, 1, '{"orderNo":"SO20261026093000000003","channel":"MOCK"}', DATE_SUB(NOW(), INTERVAL 6 HOUR));

-- 退款单：订单 3 待审核，用于演示运营端「退款审核」
INSERT INTO t_refund (id, refund_no, order_no, user_id, amount, reason, status, audit_remark, create_time, update_time) VALUES
(1, 'RF20261026120000000001', 'SO20261026093000000003', 1, 89.00, '浮漂吃铅和描述不符，申请退款', 0, NULL,
 DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_SUB(NOW(), INTERVAL 5 HOUR));

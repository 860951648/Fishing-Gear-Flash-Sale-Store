-- =============================================================
-- 项目 A：渔具秒杀商城 · 建库建表脚本
-- 执行方式：
--   mysql -uroot -proot < project-a-trade\sql\schema.sql
-- 说明：
--   1. 金额统一 DECIMAL(10,2)，时间统一 DATETIME，状态统一 TINYINT 且注释写明含义
--   2. 乐观锁字段 version 出现在 t_stock / t_seckill_sku / t_order
--   3. 逻辑删除字段 deleted 只出现在 t_product（MyBatis-Plus @TableLogic）
--   4. 唯一索引即幂等闸门：uk_order_no、uk_user_seckill、uk_channel_trade_no、
--      uk_activity_sku、uk_message_id、uk_order_type
-- =============================================================

CREATE DATABASE IF NOT EXISTS trade_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE trade_db;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS t_stock_log;
DROP TABLE IF EXISTS t_mq_consume_log;
DROP TABLE IF EXISTS t_refund;
DROP TABLE IF EXISTS t_payment_record;
DROP TABLE IF EXISTS t_order_item;
DROP TABLE IF EXISTS t_order;
DROP TABLE IF EXISTS t_seckill_sku;
DROP TABLE IF EXISTS t_seckill_activity;
DROP TABLE IF EXISTS t_stock;
DROP TABLE IF EXISTS t_sku;
DROP TABLE IF EXISTS t_product;
DROP TABLE IF EXISTS t_user;

-- -------------------------------------------------------------
-- 1. 用户表
-- -------------------------------------------------------------
CREATE TABLE t_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(32)  NOT NULL COMMENT '登录名',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码摘要',
    nickname    VARCHAR(32)  DEFAULT NULL COMMENT '昵称',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 正常 0 禁用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- -------------------------------------------------------------
-- 2. 商品表
-- -------------------------------------------------------------
CREATE TABLE t_product (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(60)   NOT NULL COMMENT '商品名称',
    subtitle    VARCHAR(120)  DEFAULT NULL COMMENT '副标题/卖点',
    cover       VARCHAR(255)  DEFAULT NULL COMMENT '封面图地址',
    price       DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '展示价',
    status      TINYINT       NOT NULL DEFAULT 1 COMMENT '1 上架 0 下架',
    deleted     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0 未删 1 已删',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品表';

-- -------------------------------------------------------------
-- 3. SKU 表
-- -------------------------------------------------------------
CREATE TABLE t_sku (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    product_id  BIGINT        NOT NULL COMMENT '商品 id',
    name        VARCHAR(60)   NOT NULL COMMENT '规格名',
    price       DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'SKU 价格',
    status      TINYINT       NOT NULL DEFAULT 1 COMMENT '1 启用 0 停用',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_product_id (product_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'SKU 表';

-- -------------------------------------------------------------
-- 4. 库存表（可用/锁定分离 + 乐观锁）
-- -------------------------------------------------------------
CREATE TABLE t_stock (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    sku_id      BIGINT   NOT NULL COMMENT 'SKU id',
    total       INT      NOT NULL DEFAULT 0 COMMENT '总库存',
    available   INT      NOT NULL DEFAULT 0 COMMENT '可用库存',
    locked      INT      NOT NULL DEFAULT 0 COMMENT '锁定库存',
    version     INT      NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sku_id (sku_id),
    KEY idx_available (available)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'SKU 库存表';

-- -------------------------------------------------------------
-- 5. 秒杀活动表
-- -------------------------------------------------------------
CREATE TABLE t_seckill_activity (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(60) NOT NULL COMMENT '活动名称',
    start_time  DATETIME    NOT NULL COMMENT '开始时间',
    end_time    DATETIME    NOT NULL COMMENT '结束时间',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '1 上线 0 下线',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_time (start_time, end_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '秒杀活动表';

-- -------------------------------------------------------------
-- 6. 秒杀 SKU 表（活动 × SKU 唯一）
-- -------------------------------------------------------------
CREATE TABLE t_seckill_sku (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    activity_id   BIGINT        NOT NULL COMMENT '活动 id',
    sku_id        BIGINT        NOT NULL COMMENT 'SKU id',
    seckill_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '秒杀价',
    stock         INT           NOT NULL DEFAULT 0 COMMENT '秒杀剩余库存',
    version       INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_activity_sku (activity_id, sku_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '秒杀 SKU 表';

-- -------------------------------------------------------------
-- 7. 订单主表
--    uk_user_seckill 是秒杀「一人一单」的最后一道闸门；
--    普通订单 seckill_sku_id 为 NULL，MySQL 唯一索引允许多个 NULL，不影响普通订单
-- -------------------------------------------------------------
CREATE TABLE t_order (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no        VARCHAR(40)    NOT NULL COMMENT '订单号',
    user_id         BIGINT         NOT NULL COMMENT '买家 id',
    activity_id     BIGINT         DEFAULT NULL COMMENT '秒杀活动 id',
    seckill_sku_id  BIGINT         DEFAULT NULL COMMENT '秒杀 SKU id',
    total_amount    DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '订单原价',
    pay_amount      DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
    status          TINYINT        NOT NULL DEFAULT 0 COMMENT '0 待支付 1 已支付 2 已发货 3 已完成 4 已关闭 5 退款中 6 已退款',
    pay_time        DATETIME       DEFAULT NULL COMMENT '支付时间',
    close_time      DATETIME       DEFAULT NULL COMMENT '关闭时间',
    version         INT            NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    UNIQUE KEY uk_user_seckill (user_id, seckill_sku_id),
    KEY idx_user_status (user_id, status),
    KEY idx_status_create (status, create_time),
    KEY idx_pay_time (pay_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单主表';

-- -------------------------------------------------------------
-- 8. 订单明细表
-- -------------------------------------------------------------
CREATE TABLE t_order_item (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id     BIGINT         NOT NULL COMMENT '订单 id',
    sku_id       BIGINT         NOT NULL COMMENT 'SKU id',
    product_name VARCHAR(60)    NOT NULL COMMENT '商品名快照',
    sku_name     VARCHAR(60)    NOT NULL COMMENT 'SKU 名快照',
    price        DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '成交单价',
    quantity     INT            NOT NULL DEFAULT 1 COMMENT '数量',
    PRIMARY KEY (id),
    KEY idx_order_id (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单明细表';

-- -------------------------------------------------------------
-- 9. 支付流水表（channel_trade_no 唯一 = 重复回调的第二道幂等闸门）
-- -------------------------------------------------------------
CREATE TABLE t_payment_record (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no         VARCHAR(40)    NOT NULL COMMENT '订单号',
    channel_trade_no VARCHAR(64)    NOT NULL COMMENT '渠道交易号',
    amount           DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    status           TINYINT        NOT NULL DEFAULT 1 COMMENT '1 成功 0 失败',
    raw_body         VARCHAR(1000)  DEFAULT NULL COMMENT '回调原始报文',
    create_time      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_channel_trade_no (channel_trade_no),
    KEY idx_order_no (order_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '支付流水表';

-- -------------------------------------------------------------
-- 10. 退款单表
-- -------------------------------------------------------------
CREATE TABLE t_refund (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    refund_no    VARCHAR(40)    NOT NULL COMMENT '退款单号',
    order_no     VARCHAR(40)    NOT NULL COMMENT '订单号',
    user_id      BIGINT         NOT NULL COMMENT '申请人 id',
    amount       DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '退款金额',
    reason       VARCHAR(200)   NOT NULL COMMENT '退款原因',
    status       TINYINT        NOT NULL DEFAULT 0 COMMENT '0 待审核 1 审核通过 2 审核驳回',
    audit_remark VARCHAR(200)   DEFAULT NULL COMMENT '审核备注',
    create_time  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_no (refund_no),
    KEY idx_order_no (order_no),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '退款单表';

-- -------------------------------------------------------------
-- 11. MQ 消费幂等表
-- -------------------------------------------------------------
CREATE TABLE t_mq_consume_log (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    message_id   VARCHAR(64) NOT NULL COMMENT '消息唯一 id',
    consume_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消费时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_message_id (message_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'MQ 消费幂等表';

-- -------------------------------------------------------------
-- 12. 库存流水表（同一订单同一动作只能回滚一次）
-- -------------------------------------------------------------
CREATE TABLE t_stock_log (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no    VARCHAR(40) NOT NULL COMMENT '订单号',
    sku_id      BIGINT      NOT NULL COMMENT 'SKU id',
    type        TINYINT     NOT NULL COMMENT '1 下单扣减 2 退款回滚 3 关单回滚',
    quantity    INT         NOT NULL DEFAULT 1 COMMENT '数量',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_type (order_no, type),
    KEY idx_sku_id (sku_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库存流水表';

SET FOREIGN_KEY_CHECKS = 1;

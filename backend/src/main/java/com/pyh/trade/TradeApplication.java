package com.pyh.trade;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 项目 A「渔具秒杀商城」启动类。
 * <p>@MapperScan 扫 Mapper 接口；@EnableScheduling 打开 OrderCloseTask（MQ 关闭时的延迟关单兜底）
 * 与 StockReconcileTask（每日库存对账）。
 */
@SpringBootApplication
@MapperScan("com.pyh.trade.mapper")
@EnableScheduling
public class TradeApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradeApplication.class, args);
    }
}

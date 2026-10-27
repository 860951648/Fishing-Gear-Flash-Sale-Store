package com.pyh.trade.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 销售趋势出参（按天）。
 * <p>date 为 yyyy-MM-dd 字符串（SQL 里 DATE_FORMAT 出来），amount 为当天支付金额合计；
 * 直接给 ECharts / el-table 用。由 StatsMapper 的 XML 聚合 SQL 返回、StatsServiceImpl 补齐空日期。
 */
@Data
public class SalesTrendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String date;

    private BigDecimal amount;

    public SalesTrendVO() {
    }

    public SalesTrendVO(String date, BigDecimal amount) {
        this.date = date;
        this.amount = amount;
    }
}

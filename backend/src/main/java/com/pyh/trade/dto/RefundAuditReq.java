package com.pyh.trade.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 运营端退款审核入参。
 * <p>由 AdminRefundController#audit 接收，RefundServiceImpl 在一个事务里
 * 更新退款单状态 + 订单状态机流转（通过→6 已退款；驳回→回滚到已支付）+ 回滚库存。
 */
@Data
public class RefundAuditReq {

    @NotNull(message = "审核结论不能为空")
    private Boolean approved;

    @Size(max = 200, message = "审核备注最长 200 位")
    private String remark;
}

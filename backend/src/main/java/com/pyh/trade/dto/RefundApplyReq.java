package com.pyh.trade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 买家申请退款入参。
 * <p>由 RefundController#apply 接收，RefundServiceImpl 校验订单归属与状态（1/2/3）后
 * 写 t_refund（待审核）并把订单流转到 5 退款中。
 */
@Data
public class RefundApplyReq {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotBlank(message = "退款原因不能为空")
    @Size(max = 200, message = "退款原因最长 200 位")
    private String reason;
}

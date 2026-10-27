package com.pyh.trade.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 退款申请结果。
 * <p>返回退款单号，前端在「我的退款」里按退款单号展示进度。
 */
@Data
public class RefundNoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String refundNo;

    public RefundNoVO() {
    }

    public RefundNoVO(String refundNo) {
        this.refundNo = refundNo;
    }
}

package com.pyh.trade.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款单出参。
 * <p>买家端（我的退款）与运营端（退款审核）共用；status 0 待审核 / 1 通过 / 2 驳回。
 */
@Data
public class RefundVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String refundNo;

    private String orderNo;

    private Long userId;

    private BigDecimal amount;

    private String reason;

    private Integer status;

    private String auditRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}

package com.pyh.trade.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 秒杀活动列表项出参。
 * <p>买家端只返回 status=1 且未结束的活动；运营端返回全部（含已下线）。
 */
@Data
public class ActivityVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /** 1 上线 / 0 下线 */
    private Integer status;

    /** 活动内 SKU 数量 */
    private Integer skuCount;
}

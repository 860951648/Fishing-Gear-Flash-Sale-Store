package com.pyh.trade.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒杀活动详情出参。
 * <p>在活动信息之外带每个 SKU 的 Redis 实时库存，前端秒杀页直接用这个列表渲染「立即抢购」按钮。
 */
@Data
public class ActivityDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /** 1 上线 / 0 下线 */
    private Integer status;

    /** 活动是否在可抢时间窗内（前端据此置灰按钮） */
    private Boolean inProgress;

    private List<SeckillSkuVO> skus;
}

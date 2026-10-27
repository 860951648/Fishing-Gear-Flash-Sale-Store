package com.pyh.trade.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 运营端创建秒杀活动入参。
 * <p>由 AdminSeckillController#create 接收，AdminSeckillServiceImpl 写 t_seckill_activity + t_seckill_sku；
 * 创建后需再调预热接口把秒杀库存灌进 Redis，买家才能抢。
 */
@Data
public class ActivitySaveReq {

    @NotBlank(message = "活动名称不能为空")
    @Size(max = 60, message = "活动名称最长 60 位")
    private String name;

    @NotNull(message = "开始时间不能为空")
    @JsonDeserialize(using = com.pyh.trade.utils.FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @JsonDeserialize(using = com.pyh.trade.utils.FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime endTime;

    @NotEmpty(message = "至少配置一个秒杀 SKU")
    @Valid
    private List<ActivitySkuReq> skus;

    /**
     * 活动内的秒杀 SKU 配置。
     */
    @Data
    public static class ActivitySkuReq {

        @NotNull(message = "skuId 不能为空")
        private Long skuId;

        @NotNull(message = "秒杀价不能为空")
        @DecimalMin(value = "0.01", message = "秒杀价必须大于 0")
        private BigDecimal seckillPrice;

        @NotNull(message = "秒杀库存不能为空")
        @Min(value = 0, message = "秒杀库存不能为负")
        private Integer stock;
    }
}

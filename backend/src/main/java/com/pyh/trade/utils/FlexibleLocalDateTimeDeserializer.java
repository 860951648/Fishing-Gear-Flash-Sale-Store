package com.pyh.trade.utils;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 宽松的 LocalDateTime 反序列化器（Jackson 3 / tools.jackson）。
 * <p>前端 Element Plus 的日期选择器给的是 {@code yyyy-MM-dd HH:mm:ss}，curl 脚本和 ISO 规范给的是
 * {@code yyyy-MM-ddTHH:mm:ss}，两种都要能进：
 * 业务入参统一用它，避免因为一个空格导致「创建活动 400」这种联调噪音。
 * 由 ActivitySaveReq 的 startTime / endTime 上的 @JsonDeserialize 使用。
 */
public class FlexibleLocalDateTimeDeserializer extends ValueDeserializer<LocalDateTime> {

    private static final DateTimeFormatter SPACE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) {
        String text = parser.getString();
        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.trim();
        try {
            // 先按前端的标准格式解析
            return LocalDateTime.parse(value, SPACE_FORMATTER);
        } catch (RuntimeException ignored) {
            // 再兼容 ISO-8601（带 T）与「精确到分」的写法
        }
        try {
            return LocalDateTime.parse(value);
        } catch (RuntimeException ignored) {
            return LocalDateTime.parse(value, MINUTE_FORMATTER);
        }
    }
}

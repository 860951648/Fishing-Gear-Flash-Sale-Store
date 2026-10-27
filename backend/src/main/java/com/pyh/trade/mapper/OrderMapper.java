package com.pyh.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pyh.trade.entity.Order;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 订单 Mapper。
 * <p>状态机所有流转都用「WHERE status = #{from}」的条件 UPDATE 实现 CAS：
 * 影响 1 行 = 抢到了这次流转；0 行 = 状态已被别的线程/回调改掉，上层返回 409 或按幂等返回成功。
 * 这样支付回调、超时关单、用户取消三条路径并发时不会互相覆盖。
 */
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 支付成功：0 → 1，同时写支付时间。
     *
     * @param orderNo 订单号
     * @param from    期望的当前状态
     * @param payTime 支付时间
     * @return 影响行数
     */
    @Update("UPDATE t_order SET status = 1, pay_time = #{payTime}, update_time = NOW() "
            + "WHERE order_no = #{orderNo} AND status = #{from}")
    int markPaid(@Param("orderNo") String orderNo, @Param("from") Integer from,
                 @Param("payTime") LocalDateTime payTime);

    /**
     * 关单：from → 4，同时写关闭时间。
     *
     * @param orderNo   订单号
     * @param from      期望的当前状态
     * @param closeTime 关闭时间
     * @return 影响行数
     */
    @Update("UPDATE t_order SET status = 4, close_time = #{closeTime}, update_time = NOW() "
            + "WHERE order_no = #{orderNo} AND status = #{from}")
    int markClosed(@Param("orderNo") String orderNo, @Param("from") Integer from,
                   @Param("closeTime") LocalDateTime closeTime);

    /**
     * 通用状态流转（退款申请 1/2/3 → 5、审核通过 5 → 6、驳回 5 → 1）。
     *
     * @param orderNo 订单号
     * @param from    期望的当前状态
     * @param to      目标状态
     * @return 影响行数
     */
    @Update("UPDATE t_order SET status = #{to}, update_time = NOW() "
            + "WHERE order_no = #{orderNo} AND status = #{from}")
    int casStatus(@Param("orderNo") String orderNo, @Param("from") Integer from, @Param("to") Integer to);
}

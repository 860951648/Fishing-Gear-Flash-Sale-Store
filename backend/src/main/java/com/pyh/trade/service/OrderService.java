package com.pyh.trade.service;

import com.pyh.trade.common.PageResult;
import com.pyh.trade.entity.SeckillSku;
import com.pyh.trade.vo.OrderDetailVO;
import com.pyh.trade.vo.OrderVO;

/**
 * 订单服务。
 * <p>买家侧：我的订单分页、详情、取消；运营侧：全量订单分页。
 * 关单逻辑（closeOrder）是幂等的，被取消接口、MQ 消费者、@Scheduled 兜底任务三条路径共用。
 * 由 OrderController / AdminOrderController / OrderCloseTask / OrderCloseConsumer 调用。
 */
public interface OrderService {

    /**
     * 我的订单分页。
     *
     * @param page   页码
     * @param size   每页条数
     * @param status 订单状态，可空
     * @return 分页结果
     */
    PageResult<OrderVO> pageMine(int page, int size, Integer status);

    /**
     * 订单详情（仅本人或运营可看）。
     *
     * @param orderNo 订单号
     * @return 详情
     */
    OrderDetailVO detail(String orderNo);

    /**
     * 买家取消订单（仅待支付可取消）。
     *
     * @param orderNo 订单号
     */
    void cancel(String orderNo);

    /**
     * 幂等关单：CAS 0 → 4，回滚 DB 与 Redis 库存，写库存流水。
     * <p>重复调用（MQ 重复投递 / 已支付 / 已关闭）返回 false，不抛异常。
     *
     * @param orderNo 订单号
     * @return true 表示本次真正完成了关单
     */
    boolean closeOrder(String orderNo);

    /**
     * 运营端订单分页。
     *
     * @param page   页码
     * @param size   每页条数
     * @param status 订单状态，可空
     * @return 分页结果
     */
    PageResult<OrderVO> pageAdmin(int page, int size, Integer status);

    /**
     * 创建秒杀订单（事务边界：条件扣减两个库存 + 写订单/明细/流水）。
     * <p>由 SeckillServiceImpl 在 Redis 预扣成功后调用；抛异常时上层负责补偿 Redis。
     *
     * @param seckillSku 秒杀 SKU（含价格与 skuId）
     * @param activityId 活动 id
     * @param userId     买家 id
     * @return 订单号
     */
    String createSeckillOrder(SeckillSku seckillSku, Long activityId, Long userId);
}

package com.pyh.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pyh.trade.common.BusinessException;
import com.pyh.trade.common.Constants;
import com.pyh.trade.common.PageResult;
import com.pyh.trade.common.ResultCode;
import com.pyh.trade.entity.Order;
import com.pyh.trade.entity.OrderItem;
import com.pyh.trade.entity.Product;
import com.pyh.trade.entity.SeckillSku;
import com.pyh.trade.entity.Sku;
import com.pyh.trade.entity.StockLog;
import com.pyh.trade.mapper.OrderItemMapper;
import com.pyh.trade.mapper.OrderMapper;
import com.pyh.trade.mapper.ProductMapper;
import com.pyh.trade.mapper.SeckillSkuMapper;
import com.pyh.trade.mapper.SkuMapper;
import com.pyh.trade.mapper.StockLogMapper;
import com.pyh.trade.mapper.StockMapper;
import com.pyh.trade.security.LoginUser;
import com.pyh.trade.service.OrderService;
import com.pyh.trade.utils.IdGenerator;
import com.pyh.trade.utils.RedisKeys;
import com.pyh.trade.utils.SecurityUtils;
import com.pyh.trade.vo.OrderDetailVO;
import com.pyh.trade.vo.OrderItemVO;
import com.pyh.trade.vo.OrderVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单服务实现。
 * <p>核心是「状态机 CAS + 幂等关单」：
 * 所有状态流转都用 {@code UPDATE ... WHERE status = #{from}}，影响 0 行说明状态已被别人改掉，
 * 因此支付、取消、超时关单三条并发路径不会互相覆盖。
 * <p>关单回滚涉及三处：t_seckill_sku.stock、t_stock(available/locked)、Redis 秒杀库存，
 * 并用 t_stock_log 的 (order_no, type) 唯一索引兜住重复回滚。
 * 被 OrderController / AdminOrderController / OrderCloseTask / OrderCloseConsumer 调用。
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final SeckillSkuMapper seckillSkuMapper;
    private final SkuMapper skuMapper;
    private final ProductMapper productMapper;
    private final StockMapper stockMapper;
    private final StockLogMapper stockLogMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public OrderServiceImpl(OrderMapper orderMapper, OrderItemMapper orderItemMapper,
                            SeckillSkuMapper seckillSkuMapper, SkuMapper skuMapper, ProductMapper productMapper,
                            StockMapper stockMapper, StockLogMapper stockLogMapper,
                            StringRedisTemplate stringRedisTemplate) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.seckillSkuMapper = seckillSkuMapper;
        this.skuMapper = skuMapper;
        this.productMapper = productMapper;
        this.stockMapper = stockMapper;
        this.stockLogMapper = stockLogMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public PageResult<OrderVO> pageMine(int page, int size, Integer status) {
        Long userId = SecurityUtils.requireUserId();
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getId);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        Page<Order> pageParam = new Page<>(Math.max(page, 1), normalizeSize(size));
        Page<Order> result = orderMapper.selectPage(pageParam, wrapper);
        return toPageResult(result);
    }

    @Override
    public PageResult<OrderVO> pageAdmin(int page, int size, Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>().orderByDesc(Order::getId);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        Page<Order> pageParam = new Page<>(Math.max(page, 1), normalizeSize(size));
        Page<Order> result = orderMapper.selectPage(pageParam, wrapper);
        return toPageResult(result);
    }

    @Override
    public OrderDetailVO detail(String orderNo) {
        Long userId = SecurityUtils.requireUserId();
        Order order = getByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        LoginUser loginUser = SecurityUtils.requireUser();
        if (!order.getUserId().equals(userId) && !loginUser.hasAuthority("order:manage")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能查看自己的订单");
        }
        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setActivityId(order.getActivityId());
        vo.setSeckillSkuId(order.getSeckillSkuId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setStatus(order.getStatus());
        vo.setPayTime(order.getPayTime());
        vo.setCloseTime(order.getCloseTime());
        vo.setCreateTime(order.getCreateTime());
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        vo.setItems(items.stream().map(this::toItemVO).collect(Collectors.toList()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(String orderNo) {
        Order order = getByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        LoginUser loginUser = SecurityUtils.requireUser();
        if (!order.getUserId().equals(loginUser.getUserId()) && !loginUser.hasAuthority("order:manage")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能取消自己的订单");
        }
        if (order.getStatus() == null || order.getStatus() != Constants.ORDER_STATUS_UNPAID) {
            throw new BusinessException(ResultCode.CONFLICT, "只有待支付订单可以取消");
        }
        if (!doClose(orderNo)) {
            throw new BusinessException(ResultCode.CONFLICT, "取消失败，订单状态已变更");
        }
        log.info("用户取消订单 orderNo={} userId={}", orderNo, loginUser.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean closeOrder(String orderNo) {
        return doClose(orderNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createSeckillOrder(SeckillSku seckillSku, Long activityId, Long userId) {
        // ① DB 条件扣减秒杀库存：WHERE stock >= 1，由行锁保证不超卖
        if (seckillSkuMapper.deductStock(seckillSku.getId()) == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "秒杀库存不足");
        }
        // ② 扣减 SKU 库存：可用 -1、锁定 +1
        if (stockMapper.deductOne(seckillSku.getSkuId()) == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "商品库存不足");
        }
        Sku sku = skuMapper.selectById(seckillSku.getSkuId());
        if (sku == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "SKU 不存在");
        }
        Product product = productMapper.selectById(sku.getProductId());
        String productName = product == null ? sku.getName() : product.getName();

        BigDecimal price = seckillSku.getSeckillPrice().setScale(2, RoundingMode.HALF_UP);
        Order order = new Order();
        order.setOrderNo(IdGenerator.orderNo());
        order.setUserId(userId);
        order.setActivityId(activityId);
        order.setSeckillSkuId(seckillSku.getId());
        order.setTotalAmount(price);
        order.setPayAmount(price);
        order.setStatus(Constants.ORDER_STATUS_UNPAID);
        order.setVersion(0);
        // 唯一索引 (user_id, seckill_sku_id) 会在重复下单时抛 DuplicateKeyException，
        // 事务回滚后由 SeckillServiceImpl 补偿 Redis 预扣
        orderMapper.insert(order);

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setSkuId(sku.getId());
        item.setProductName(productName);
        item.setSkuName(sku.getName());
        item.setPrice(price);
        item.setQuantity(1);
        orderItemMapper.insert(item);

        insertStockLog(order.getOrderNo(), sku.getId(), Constants.STOCK_LOG_DEDUCT, 1);
        log.info("秒杀下单成功 orderNo={} userId={} seckillSkuId={}", order.getOrderNo(), userId, seckillSku.getId());
        return order.getOrderNo();
    }

    /**
     * 幂等关单实现：先 CAS 抢「待支付 → 已关闭」，抢到才回滚库存。
     * 这样 MQ 重复投递、支付成功后的迟到消息、定时任务重复扫描都不会重复加库存。
     */
    private boolean doClose(String orderNo) {
        Order order = getByOrderNo(orderNo);
        if (order == null) {
            log.warn("关单跳过：订单不存在 orderNo={}", orderNo);
            return false;
        }
        if (order.getStatus() == null || order.getStatus() != Constants.ORDER_STATUS_UNPAID) {
            // 已支付/已关闭/退款中：关单消息属于迟到消息，直接幂等返回
            log.info("关单跳过：订单状态非待支付 orderNo={} status={}", orderNo, order.getStatus());
            return false;
        }
        int affected = orderMapper.markClosed(orderNo, Constants.ORDER_STATUS_UNPAID, LocalDateTime.now());
        if (affected == 0) {
            log.info("关单跳过：CAS 失败（已被支付或并发关单）orderNo={}", orderNo);
            return false;
        }

        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        for (OrderItem item : items) {
            stockMapper.rollbackOne(item.getSkuId());
            insertStockLog(orderNo, item.getSkuId(), Constants.STOCK_LOG_CLOSE, item.getQuantity());
        }
        // 秒杀订单还要把秒杀库存与 Redis 预扣库存还回去，让活动能继续卖
        if (order.getSeckillSkuId() != null) {
            seckillSkuMapper.restoreStock(order.getSeckillSkuId());
            restoreRedisStock(order.getSeckillSkuId(), order.getUserId());
        }
        log.info("订单已关闭 orderNo={}", orderNo);
        return true;
    }

    /**
     * 回滚 Redis 秒杀库存。
     * 只在 key 还存在时 INCR：key 不存在说明活动已结束/缓存被清，此时补回去会凭空多出库存。
     */
    private void restoreRedisStock(Long seckillSkuId, Long userId) {
        try {
            String key = RedisKeys.seckillStock(seckillSkuId);
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
                stringRedisTemplate.opsForValue().increment(key);
            }
            // 释放该用户的秒杀幂等标记：关单后名额已还回，用户应当能重新抢
            stringRedisTemplate.delete(RedisKeys.seckillUser(seckillSkuId, userId));
        } catch (RedisConnectionFailureException e) {
            // Redis 故障不影响关单主流程（DB 已回滚），下次预热会用 DB 库存覆盖
            log.error("回滚 Redis 秒杀库存失败 seckillSkuId={}", seckillSkuId, e);
        }
    }

    private void insertStockLog(String orderNo, Long skuId, int type, Integer quantity) {
        try {
            StockLog stockLog = new StockLog();
            stockLog.setOrderNo(orderNo);
            stockLog.setSkuId(skuId);
            stockLog.setType(type);
            stockLog.setQuantity(quantity == null ? 1 : quantity);
            stockLog.setCreateTime(LocalDateTime.now());
            stockLogMapper.insert(stockLog);
        } catch (DuplicateKeyException e) {
            // (order_no, type) 唯一索引冲突：同一订单同一动作已经回滚过，忽略即可
            log.info("库存流水已存在，跳过 orderNo={} type={}", orderNo, type);
        }
    }

    private Order getByOrderNo(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            return null;
        }
        return orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
    }

    private PageResult<OrderVO> toPageResult(Page<Order> page) {
        List<OrderVO> records = new ArrayList<>(page.getRecords().size());
        if (page.getRecords().isEmpty()) {
            return new PageResult<>(page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), records);
        }
        // 一次查出本页所有订单的明细，避免 N+1
        List<Long> orderIds = page.getRecords().stream().map(Order::getId).collect(Collectors.toList());
        Map<Long, List<OrderItem>> itemMap = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                        .in(OrderItem::getOrderId, orderIds))
                .stream().collect(Collectors.groupingBy(OrderItem::getOrderId));
        for (Order order : page.getRecords()) {
            OrderVO vo = toVO(order);
            List<OrderItem> items = itemMap.getOrDefault(order.getId(), Collections.emptyList());
            if (!items.isEmpty()) {
                OrderItem first = items.get(0);
                vo.setProductName(first.getProductName());
                vo.setSkuName(first.getSkuName());
                vo.setQuantity(first.getQuantity());
            }
            records.add(vo);
        }
        return new PageResult<>(page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), records);
    }

    private OrderVO toVO(Order order) {
        OrderVO vo = new OrderVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setActivityId(order.getActivityId());
        vo.setSeckillSkuId(order.getSeckillSkuId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setStatus(order.getStatus());
        vo.setPayTime(order.getPayTime());
        vo.setCloseTime(order.getCloseTime());
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }

    private OrderItemVO toItemVO(OrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        vo.setSkuId(item.getSkuId());
        vo.setProductName(item.getProductName());
        vo.setSkuName(item.getSkuName());
        vo.setPrice(item.getPrice());
        vo.setQuantity(item.getQuantity());
        return vo;
    }

    private long normalizeSize(int size) {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, Constants.MAX_PAGE_SIZE);
    }
}

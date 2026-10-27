package com.pyh.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pyh.trade.common.BusinessException;
import com.pyh.trade.common.Constants;
import com.pyh.trade.common.PageResult;
import com.pyh.trade.common.ResultCode;
import com.pyh.trade.dto.RefundApplyReq;
import com.pyh.trade.dto.RefundAuditReq;
import com.pyh.trade.entity.Order;
import com.pyh.trade.entity.OrderItem;
import com.pyh.trade.entity.Refund;
import com.pyh.trade.mapper.OrderItemMapper;
import com.pyh.trade.mapper.OrderMapper;
import com.pyh.trade.mapper.RefundMapper;
import com.pyh.trade.mapper.SeckillSkuMapper;
import com.pyh.trade.mapper.StockLogMapper;
import com.pyh.trade.mapper.StockMapper;
import com.pyh.trade.entity.StockLog;
import com.pyh.trade.security.LoginUser;
import com.pyh.trade.service.RefundService;
import com.pyh.trade.utils.IdGenerator;
import com.pyh.trade.utils.RedisKeys;
import com.pyh.trade.utils.SecurityUtils;
import com.pyh.trade.vo.RefundNoVO;
import com.pyh.trade.vo.RefundVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 退款服务实现。
 * <p>状态机：申请时订单 1/2/3 → 5（退款中）；审核通过 5 → 6（已退款）并回滚库存；
 * 审核驳回 5 → 1（已支付）。
 * <p>为什么驳回固定回 1：A 项目没有发货/收货接口，可退款的订单只可能是「已支付」，
 * 2/3 状态保留在状态机里但当前不可达，因此回滚目标是确定的。
 * 所有订单状态更新都带 {@code WHERE status = 5}，避免与关单/支付并发冲突。
 * 被 RefundController / AdminRefundController 调用。
 */
@Service
public class RefundServiceImpl implements RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundServiceImpl.class);

    private final RefundMapper refundMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final StockMapper stockMapper;
    private final StockLogMapper stockLogMapper;
    private final SeckillSkuMapper seckillSkuMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public RefundServiceImpl(RefundMapper refundMapper, OrderMapper orderMapper, OrderItemMapper orderItemMapper,
                             StockMapper stockMapper, StockLogMapper stockLogMapper,
                             SeckillSkuMapper seckillSkuMapper, StringRedisTemplate stringRedisTemplate) {
        this.refundMapper = refundMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.stockMapper = stockMapper;
        this.stockLogMapper = stockLogMapper;
        this.seckillSkuMapper = seckillSkuMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundNoVO apply(RefundApplyReq req) {
        Long userId = SecurityUtils.requireUserId();
        Order order = getOrder(req.getOrderNo());
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能对自己的订单申请退款");
        }
        Integer status = order.getStatus();
        if (status == null || (status != Constants.ORDER_STATUS_PAID
                && status != Constants.ORDER_STATUS_SHIPPED
                && status != Constants.ORDER_STATUS_FINISHED)) {
            throw new BusinessException(ResultCode.CONFLICT, "当前订单状态不可申请退款");
        }
        Long pending = refundMapper.selectCount(new LambdaQueryWrapper<Refund>()
                .eq(Refund::getOrderNo, req.getOrderNo())
                .eq(Refund::getStatus, Constants.REFUND_STATUS_WAIT));
        if (pending != null && pending > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "该订单已有退款申请在处理中");
        }
        // 订单进入退款中：带 from 条件，防止与支付/关单并发覆盖
        if (orderMapper.casStatus(req.getOrderNo(), status, Constants.ORDER_STATUS_REFUNDING) == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "订单状态已变更，请刷新后重试");
        }
        Refund refund = new Refund();
        refund.setRefundNo(IdGenerator.refundNo());
        refund.setOrderNo(req.getOrderNo());
        refund.setUserId(userId);
        refund.setAmount(order.getPayAmount().setScale(2, RoundingMode.HALF_UP));
        refund.setReason(req.getReason());
        refund.setStatus(Constants.REFUND_STATUS_WAIT);
        refundMapper.insert(refund);
        log.info("退款申请已提交 refundNo={} orderNo={} amount={}", refund.getRefundNo(), req.getOrderNo(), refund.getAmount());
        return new RefundNoVO(refund.getRefundNo());
    }

    @Override
    public PageResult<RefundVO> pageMine(int page, int size) {
        Long userId = SecurityUtils.requireUserId();
        Page<Refund> pageParam = new Page<>(Math.max(page, 1), normalizeSize(size));
        Page<Refund> result = refundMapper.selectPage(pageParam, new LambdaQueryWrapper<Refund>()
                .eq(Refund::getUserId, userId)
                .orderByDesc(Refund::getId));
        return PageResult.of(result, this::toVO);
    }

    @Override
    public PageResult<RefundVO> pageAdmin(int page, int size, Integer status) {
        LambdaQueryWrapper<Refund> wrapper = new LambdaQueryWrapper<Refund>().orderByDesc(Refund::getId);
        if (status != null) {
            wrapper.eq(Refund::getStatus, status);
        }
        Page<Refund> pageParam = new Page<>(Math.max(page, 1), normalizeSize(size));
        return PageResult.of(refundMapper.selectPage(pageParam, wrapper), this::toVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, RefundAuditReq req) {
        Refund refund = refundMapper.selectById(id);
        if (refund == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "退款单不存在");
        }
        if (refund.getStatus() == null || refund.getStatus() != Constants.REFUND_STATUS_WAIT) {
            throw new BusinessException(ResultCode.CONFLICT, "该退款单已审核，请勿重复操作");
        }
        LoginUser loginUser = SecurityUtils.requireUser();
        boolean approved = Boolean.TRUE.equals(req.getApproved());
        int targetStatus = approved ? Constants.REFUND_STATUS_PASS : Constants.REFUND_STATUS_REJECT;
        // 带 status=0 条件更新退款单，两人同时点审核只有一个能成功
        int affected = refundMapper.update(null, new LambdaUpdateWrapper<Refund>()
                .eq(Refund::getId, id)
                .eq(Refund::getStatus, Constants.REFUND_STATUS_WAIT)
                .set(Refund::getStatus, targetStatus)
                .set(Refund::getAuditRemark, req.getRemark())
                .set(Refund::getUpdateTime, LocalDateTime.now()));
        if (affected == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "退款单状态已变更，请刷新后重试");
        }

        if (approved) {
            // 5 → 6 已退款
            if (orderMapper.casStatus(refund.getOrderNo(), Constants.ORDER_STATUS_REFUNDING,
                    Constants.ORDER_STATUS_REFUNDED) == 0) {
                throw new BusinessException(ResultCode.CONFLICT, "订单状态已变更，退款失败");
            }
            rollbackStock(refund.getOrderNo());
            log.info("退款审核通过 refundNo={} orderNo={} auditor={}",
                    refund.getRefundNo(), refund.getOrderNo(), loginUser.getUserId());
        } else {
            // 5 → 1 已支付（钱没退，订单回到可再次申请退款的状态）
            if (orderMapper.casStatus(refund.getOrderNo(), Constants.ORDER_STATUS_REFUNDING,
                    Constants.ORDER_STATUS_PAID) == 0) {
                throw new BusinessException(ResultCode.CONFLICT, "订单状态已变更，驳回失败");
            }
            log.info("退款审核驳回 refundNo={} remark={}", refund.getRefundNo(), req.getRemark());
        }
    }

    /**
     * 退款成功回滚库存：t_stock 可用 +1 锁定 -1，秒杀库存与 Redis 预扣库存也还回去。
     */
    private void rollbackStock(String orderNo) {
        Order order = getOrder(orderNo);
        if (order == null) {
            return;
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        for (OrderItem item : items) {
            stockMapper.rollbackOne(item.getSkuId());
            insertStockLog(orderNo, item.getSkuId(), Constants.STOCK_LOG_REFUND, item.getQuantity());
        }
        if (order.getSeckillSkuId() != null) {
            seckillSkuMapper.restoreStock(order.getSeckillSkuId());
            try {
                String key = RedisKeys.seckillStock(order.getSeckillSkuId());
                if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
                    stringRedisTemplate.opsForValue().increment(key);
                }
            } catch (RedisConnectionFailureException e) {
                log.error("退款回滚 Redis 库存失败 seckillSkuId={}", order.getSeckillSkuId(), e);
            }
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
            // (order_no, type) 唯一：同一订单的退款库存只回滚一次
            log.info("退款库存流水已存在，跳过 orderNo={}", orderNo);
        }
    }

    private Order getOrder(String orderNo) {
        return orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
    }

    private RefundVO toVO(Refund refund) {
        RefundVO vo = new RefundVO();
        vo.setId(refund.getId());
        vo.setRefundNo(refund.getRefundNo());
        vo.setOrderNo(refund.getOrderNo());
        vo.setUserId(refund.getUserId());
        vo.setAmount(refund.getAmount());
        vo.setReason(refund.getReason());
        vo.setStatus(refund.getStatus());
        vo.setAuditRemark(refund.getAuditRemark());
        vo.setCreateTime(refund.getCreateTime());
        vo.setUpdateTime(refund.getUpdateTime());
        return vo;
    }

    private long normalizeSize(int size) {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, Constants.MAX_PAGE_SIZE);
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private List<RefundVO> toVOList(List<Refund> refunds) {
        return refunds.stream().map(this::toVO).collect(Collectors.toList());
    }
}

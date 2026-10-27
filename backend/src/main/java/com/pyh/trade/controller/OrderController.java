package com.pyh.trade.controller;

import com.pyh.trade.common.PageResult;
import com.pyh.trade.common.Result;
import com.pyh.trade.service.OrderService;
import com.pyh.trade.vo.OrderDetailVO;
import com.pyh.trade.vo.OrderVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 买家订单接口（接口契约 §3.3 第 10-12 个）。
 * <p>全部要求登录，数据按当前登录用户过滤；业务在 OrderServiceImpl。
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 我的订单分页 */
    @GetMapping
    public Result<PageResult<OrderVO>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) Integer status) {
        return Result.ok(orderService.pageMine(page, size, status));
    }

    /** 订单详情 */
    @GetMapping("/{orderNo}")
    public Result<OrderDetailVO> detail(@PathVariable String orderNo) {
        return Result.ok(orderService.detail(orderNo));
    }

    /** 取消待支付订单 */
    @PostMapping("/{orderNo}/cancel")
    public Result<Void> cancel(@PathVariable String orderNo) {
        orderService.cancel(orderNo);
        return Result.ok();
    }
}

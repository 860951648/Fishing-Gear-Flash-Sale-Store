package com.pyh.trade.controller;

import com.pyh.trade.common.PageResult;
import com.pyh.trade.common.Result;
import com.pyh.trade.service.OrderService;
import com.pyh.trade.vo.OrderVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运营端订单接口（接口契约 §3.3 第 25 个）。
 * <p>要求 {@code order:manage} 权限；复用 OrderServiceImpl 的分页逻辑，只去掉用户维度过滤。
 */
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 全量订单分页 */
    @GetMapping
    @PreAuthorize("hasAuthority('order:manage')")
    public Result<PageResult<OrderVO>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) Integer status) {
        return Result.ok(orderService.pageAdmin(page, size, status));
    }
}

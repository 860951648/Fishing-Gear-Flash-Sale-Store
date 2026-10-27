package com.pyh.trade.controller;

import com.pyh.trade.common.Result;
import com.pyh.trade.dto.ActivitySaveReq;
import com.pyh.trade.service.AdminSeckillService;
import com.pyh.trade.vo.ActivityVO;
import com.pyh.trade.vo.IdVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 运营端秒杀活动接口（接口契约 §3.3 第 22-24 个）。
 * <p>全部要求 {@code seckill:manage} 权限；业务在 AdminSeckillServiceImpl。
 * 创建活动后必须调一次预热接口，把秒杀库存写进 Redis。
 */
@RestController
@RequestMapping("/api/admin/seckill")
public class AdminSeckillController {

    private final AdminSeckillService adminSeckillService;

    public AdminSeckillController(AdminSeckillService adminSeckillService) {
        this.adminSeckillService = adminSeckillService;
    }

    /** 全部活动列表 */
    @GetMapping("/activities")
    @PreAuthorize("hasAuthority('seckill:manage')")
    public Result<List<ActivityVO>> list() {
        return Result.ok(adminSeckillService.list());
    }

    /** 创建活动（含秒杀 SKU） */
    @PostMapping("/activities")
    @PreAuthorize("hasAuthority('seckill:manage')")
    public Result<IdVO> create(@Valid @RequestBody ActivitySaveReq req) {
        return Result.ok(adminSeckillService.create(req));
    }

    /** 预热：把秒杀库存写入 Redis */
    @PostMapping("/activities/{id}/preheat")
    @PreAuthorize("hasAuthority('seckill:manage')")
    public Result<Void> preheat(@PathVariable Long id) {
        adminSeckillService.preheat(id);
        return Result.ok();
    }
}

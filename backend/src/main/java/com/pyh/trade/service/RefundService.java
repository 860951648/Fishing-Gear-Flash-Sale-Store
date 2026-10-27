package com.pyh.trade.service;

import com.pyh.trade.common.PageResult;
import com.pyh.trade.dto.RefundApplyReq;
import com.pyh.trade.dto.RefundAuditReq;
import com.pyh.trade.vo.RefundNoVO;
import com.pyh.trade.vo.RefundVO;

/**
 * 退款服务。
 * <p>买家申请（订单状态 1/2/3 → 5 退款中）、运营审核（通过 5 → 6 已退款并回滚库存；驳回 5 → 原状态）。
 * 由 RefundController / AdminRefundController 调用。
 */
public interface RefundService {

    /**
     * 买家申请退款。
     *
     * @param req 申请入参
     * @return 退款单号
     */
    RefundNoVO apply(RefundApplyReq req);

    /**
     * 我的退款分页。
     *
     * @param page 页码
     * @param size 每页条数
     * @return 分页结果
     */
    PageResult<RefundVO> pageMine(int page, int size);

    /**
     * 运营端退款分页。
     *
     * @param page   页码
     * @param size   每页条数
     * @param status 退款状态，可空
     * @return 分页结果
     */
    PageResult<RefundVO> pageAdmin(int page, int size, Integer status);

    /**
     * 运营审核退款。
     *
     * @param id  退款单 id
     * @param req 审核入参
     */
    void audit(Long id, RefundAuditReq req);
}

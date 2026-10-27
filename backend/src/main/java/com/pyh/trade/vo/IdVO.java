package com.pyh.trade.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 新建资源返回主键。
 * <p>运营端新增商品/活动后只回 id，前端据此刷新列表或继续预热。
 */
@Data
public class IdVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    public IdVO() {
    }

    public IdVO(Long id) {
        this.id = id;
    }
}

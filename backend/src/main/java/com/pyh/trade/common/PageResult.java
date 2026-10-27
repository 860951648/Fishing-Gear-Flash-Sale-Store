package com.pyh.trade.common;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 分页响应体。
 * <p>对外不暴露 MyBatis-Plus 的 {@code Page} 对象，只暴露前端需要的 total/pages/current/size/records。
 * 由各 Service 的 page 方法构造，Controller 直接塞进 {@code Result}。
 *
 * @param <T> 列表元素类型（通常是 VO）
 */
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总记录数 */
    private long total;
    /** 总页数 */
    private long pages;
    /** 当前页码（从 1 开始） */
    private long current;
    /** 每页条数 */
    private long size;
    /** 当前页数据 */
    private List<T> records;

    public PageResult() {
    }

    public PageResult(long total, long pages, long current, long size, List<T> records) {
        this.total = total;
        this.pages = pages;
        this.current = current;
        this.size = size;
        this.records = records == null ? Collections.emptyList() : records;
    }

    /**
     * 直接用 MP 的 IPage 数据构造（元素类型一致时）。
     */
    public static <T> PageResult<T> of(com.baomidou.mybatisplus.core.metadata.IPage<T> page) {
        return new PageResult<>(page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    /**
     * 把 MP 的分页结果按 mapper 函数转换成 VO 分页，避免在 Controller 里做转换。
     */
    public static <E, T> PageResult<T> of(com.baomidou.mybatisplus.core.metadata.IPage<E> page, Function<E, T> mapper) {
        List<T> list = page.getRecords().stream().map(mapper).toList();
        return new PageResult<>(page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), list);
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getPages() {
        return pages;
    }

    public void setPages(long pages) {
        this.pages = pages;
    }

    public long getCurrent() {
        return current;
    }

    public void setCurrent(long current) {
        this.current = current;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }
}

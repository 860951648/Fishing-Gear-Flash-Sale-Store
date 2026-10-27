package com.pyh.trade.service;

import java.util.List;

/**
 * 权限服务。
 * <p>A 项目只有 t_user 一张用户表，没有独立的角色/权限表，因此权限按「运营账号」规则推导：
 * {@code admin} 账号拥有 5 个管理权限码，其余账号只有 ROLE_USER（只能访问登录态接口）。
 * 权限列表缓存到 Redis {@code trade:perm:{userId}}（5 分钟），改权限时直接删缓存即可生效。
 * 由 JwtAuthenticationFilter（每请求加载）与 AuthServiceImpl（登录返回权限）调用。
 */
public interface PermissionService {

    /**
     * 加载用户权限码列表（带 Redis 缓存）。
     *
     * @param userId 用户 id
     * @return 权限码列表；用户不存在/被禁用返回空列表
     */
    List<String> loadAuthorities(Long userId);

    /**
     * 删除用户权限缓存，权限变更后调用。
     *
     * @param userId 用户 id
     */
    void evictPermissions(Long userId);

    /**
     * 读取当前 token 版本号（Redis 不存在时初始化为 0）。
     *
     * @param userId 用户 id
     * @return 版本号
     */
    long currentVersion(Long userId);

    /**
     * 递增 token 版本号，使该用户已签发的 token 全部失效（改密/禁用场景）。
     *
     * @param userId 用户 id
     */
    void bumpVersion(Long userId);
}

package com.pyh.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pyh.trade.common.BusinessException;
import com.pyh.trade.common.ResultCode;
import com.pyh.trade.dto.LoginReq;
import com.pyh.trade.dto.RegisterReq;
import com.pyh.trade.entity.User;
import com.pyh.trade.mapper.UserMapper;
import com.pyh.trade.security.JwtUtils;
import com.pyh.trade.security.LoginUser;
import com.pyh.trade.service.AuthService;
import com.pyh.trade.service.PermissionService;
import com.pyh.trade.utils.IdGenerator;
import com.pyh.trade.utils.RedisKeys;
import com.pyh.trade.utils.SecurityUtils;
import com.pyh.trade.vo.LoginVO;
import com.pyh.trade.vo.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/**
 * 认证服务实现。
 * <p>登录链路：查用户 → BCrypt 校验 → 查当前 token 版本号 → 签发 JWT（userId/jti/ver）
 * → 写 Redis 白名单（TTL 与 token 一致）→ 返回权限码。
 * 登出链路：按登录上下文里的 jti 删除白名单，token 立即失效。
 * 被 AuthController 调用。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate stringRedisTemplate;
    private final PermissionService permissionService;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtils jwtUtils,
                           StringRedisTemplate stringRedisTemplate, PermissionService permissionService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.stringRedisTemplate = stringRedisTemplate;
        this.permissionService = permissionService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterReq req) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "用户名已存在");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        // 只存 BCrypt 摘要，明文密码不落库
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname() == null || req.getNickname().isBlank()
                ? req.getUsername() : req.getNickname());
        user.setPhone(req.getPhone());
        user.setStatus(1);
        userMapper.insert(user);
        log.info("新用户注册成功 userId={} username={}", user.getId(), user.getUsername());
    }

    @Override
    public LoginVO login(LoginReq req) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername()));
        // 用户名不存在与密码错误返回同一提示，避免被用来枚举账号
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }

        long version = permissionService.currentVersion(user.getId());
        String jti = IdGenerator.uuid();
        String token = jwtUtils.generate(user.getId(), jti, version);
        // 白名单：token 本身无法撤销，靠这个 key 让「登出/改密」立即生效
        stringRedisTemplate.opsForValue().set(RedisKeys.authWhitelist(jti),
                String.valueOf(user.getId()), Duration.ofSeconds(jwtUtils.getExpireSeconds()));
        List<String> permissions = permissionService.loadAuthorities(user.getId());
        log.info("用户登录成功 userId={} jti={}", user.getId(), jti);
        return new LoginVO(token, jwtUtils.getExpireSeconds(), toVO(user), permissions);
    }

    @Override
    public void logout() {
        LoginUser loginUser = SecurityUtils.currentUser();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        stringRedisTemplate.delete(RedisKeys.authWhitelist(loginUser.getJti()));
        log.info("用户登出 userId={} jti={}", loginUser.getUserId(), loginUser.getJti());
    }

    @Override
    public UserVO me() {
        User user = userMapper.selectById(SecurityUtils.requireUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return toVO(user);
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}

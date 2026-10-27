package com.pyh.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pyh.trade.entity.User;

/**
 * 用户表 Mapper。
 * <p>继承 MyBatis-Plus BaseMapper，注册与登录按 username 查一条由 AuthService 调 selectOne 完成。
 */
public interface UserMapper extends BaseMapper<User> {
}

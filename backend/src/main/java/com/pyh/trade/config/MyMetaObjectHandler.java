package com.pyh.trade.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.pyh.trade.security.LoginUser;
import com.pyh.trade.utils.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 公共字段自动填充。
 * <p>insert 填 createTime/updateTime，update 填 updateTime；createBy 从当前登录用户取，
 * 拿不到（例如定时任务、MQ 消费线程）就留空，不抛异常。
 * 由 MyBatis-Plus 在 insert/update 时回调，业务代码不用手写时间字段。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        if (metaObject.hasSetter("createBy")) {
            LoginUser loginUser = SecurityUtils.currentUser();
            strictInsertFill(metaObject, "createBy", Long.class, loginUser == null ? null : loginUser.getUserId());
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}

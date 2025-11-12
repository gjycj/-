package com.house.deed.pavilion.config.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.house.deed.pavilion.common.util.TenantContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// 自定义填充处理器
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        // 填充租户ID（所有含tenant_id字段的实体都会自动注入）
        this.strictInsertFill(metaObject, "tenantId", Long.class, TenantContext.getTenantId());
        // 填充创建时间
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 填充更新时间
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
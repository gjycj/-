package com.house.deed.pavilion.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审计字段自动填充处理器
 * <p>
 * 核心功能：基于MyBatis-Plus的MetaObjectHandler接口，实现实体类中审计相关字段的自动填充，
 * 无需手动设置创建时间、更新时间等字段，简化开发并确保时间字段的一致性。
 * <p>
 * 支持的自动填充场景：
 * 1. 新增操作（INSERT）：自动填充创建时间（createTime）
 * 2. 新增/更新操作（INSERT_UPDATE）：自动填充更新时间（updateTime）
 * <p>
 * 字段匹配规则：
 * 仅对实体类中声明了@TableField(fill = ...)注解的字段生效，且字段名需为以下约定名称：
 * - createTime：创建时间（LocalDateTime类型）
 * - updateTime：更新时间（LocalDateTime类型）
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Component
public class AuditFieldAutoFillHandler implements MetaObjectHandler {

    /**
     * 新增操作时自动填充字段
     * <p>
     * 触发时机：执行INSERT语句时，MyBatis-Plus会调用此方法，对标记为FieldFill.INSERT的字段进行填充。
     * 此处主要填充创建时间（createTime），使用当前系统时间（LocalDateTime.now()），
     * 且仅在字段值为null时填充（避免覆盖已手动设置的值）。
     *
     * @param metaObject 元对象，封装了当前实体的属性信息
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        // 填充创建时间：判断实体是否有createTime字段且值为null，是则填充当前时间
        this.strictInsertFill(
                metaObject,
                "createTime",  // 实体类中的属性名（需与字段名一致）
                LocalDateTime.class,  // 字段类型（使用LocalDateTime确保线程安全和时间精度）
                LocalDateTime.now()  // 填充值：当前系统时间
        );

        // 同时填充更新时间：新增时updateTime与createTime保持一致
        this.strictInsertFill(
                metaObject,
                "updateTime",
                LocalDateTime.class,
                LocalDateTime.now()
        );
    }

    /**
     * 更新操作时自动填充字段
     * <p>
     * 触发时机：执行UPDATE语句时，MyBatis-Plus会调用此方法，对标记为FieldFill.UPDATE或FieldFill.INSERT_UPDATE的字段进行填充。
     * 此处主要更新更新时间（updateTime），使用当前系统时间，同样仅在字段值为null时填充。
     *
     * @param metaObject 元对象，封装了当前实体的属性信息
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        // 填充更新时间：判断实体是否有updateTime字段且值为null，是则填充当前时间
        this.strictUpdateFill(
                metaObject,
                "updateTime",
                LocalDateTime.class,
                LocalDateTime.now()
        );
    }
}
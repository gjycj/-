package com.house.deed.pavilion.common.aspect.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AgentDataPermission {
    // 操作类型枚举（内置注解中，统一管理）
    enum OperationType { QUERY, CREATE, UPDATE, DELETE }

    /** 操作类型（必选） */
    OperationType operation();

    /** 关联实体类（必选，用于提取租户/创建人字段） */
    Class<?> entityClass();

    /** 创建人字段名（可选，默认createAgentId） */
    String creatorField() default "createAgentId";

    /** 业务ID参数名（可选，默认id，非查询操作时提取该参数值） */
    String dataIdParam() default "id";

    // 新增：关联实体权限校验（如维修工单关联房源）
    Class<?> relatedEntityClass() default void.class; // 关联实体类（如House.class）
    String relatedIdField() default ""; // 当前实体中关联ID的字段名（如"houseId"）
    String relatedCreatorField() default "createAgentId"; // 关联实体的创建人字段

    // 新增：支持多实体校验（如带看记录同时校验客户和房源）
    Class<?>[] multiEntityClasses() default {}; // 多实体类数组
    String[] multiIdParams() default {}; // 对应实体的ID参数名数组
}
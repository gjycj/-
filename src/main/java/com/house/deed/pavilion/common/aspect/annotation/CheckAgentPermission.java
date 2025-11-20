package com.house.deed.pavilion.common.aspect.annotation;

import java.lang.annotation.*;

/**
 * 经纪人操作权限校验注解
 * 用于标记需要校验"是否为资源创建人或管理员"的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CheckAgentPermission {

    /**
     * 资源实体类（如House、Customer等）
     */
    Class<?> entityClass();

    /**
     * 资源ID参数的名称（方法参数中表示资源ID的变量名）
     */
    String resourceIdParam() default "id";

    /**
     * 实体类中创建人字段名（默认createAgentId）
     */
    String creatorField() default "createAgentId";
}
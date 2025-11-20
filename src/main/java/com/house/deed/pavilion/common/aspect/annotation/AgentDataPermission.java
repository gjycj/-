package com.house.deed.pavilion.common.aspect.annotation;

import java.lang.annotation.*;

/**
 * 经纪人数据权限注解：标记需要进行"仅操作自己创建数据"控制的方法
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AgentDataPermission {

    /**
     * 实体类的创建人字段名（如house表的createAgentId，visit_record表的agentId）
     */
    String creatorField()  default "";

    /**
     * 操作类型：查询/新增/更新/删除（默认查询）
     */
    OperationType operation();

    // 新增实体类Class属性
    Class<?> entityClass();

    /**
     * 操作类型枚举
     */
    enum OperationType {
        QUERY,  // 查询操作：自动添加创建人过滤条件
        CREATE, // 新增操作：校验关联实体的创建人归属
        UPDATE, // 更新操作：校验当前实体的创建人归属
        DELETE  // 删除操作：校验当前实体的创建人归属
    }
}
package com.house.deed.pavilion.common.aspect.interceptor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.house.deed.pavilion.common.aspect.annotation.AgentDataPermission;
import com.house.deed.pavilion.common.aspect.annotation.CheckAgentPermission;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.AgentContext;
import com.house.deed.pavilion.common.util.RoleUtil;
import com.house.deed.pavilion.common.util.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 经纪人数据权限切面（支持多实体字段适配）
 */
@Aspect
@Component
@Slf4j
public class AgentDataPermissionAspect {

    @Autowired
    private ApplicationContext applicationContext; // 用于获取Service Bean

    @Before("@annotation(checkPermission)")
    public void checkPermission(JoinPoint joinPoint, CheckAgentPermission checkPermission) throws Exception {
        // 1. 获取注解参数
        Class<?> entityClass = checkPermission.entityClass();
        String resourceIdParam = checkPermission.resourceIdParam();
        String creatorField = checkPermission.creatorField();

        // 2. 从方法参数中获取资源ID（如房源ID）
        Long resourceId = getResourceIdFromParams(joinPoint, resourceIdParam);
        if (resourceId == null) {
            throw new BusinessException(400, "资源ID参数不存在");
        }

        // 3. 查询资源实体（通过对应Service的getById方法）
        Object entity = getEntityById(entityClass, resourceId);
        if (entity == null) {
            throw new BusinessException(404, entityClass.getSimpleName() + "不存在");
        }

        // 4. 校验租户归属
        checkTenantPermission(entity);

        // 5. 校验操作权限（管理员/店长跳过，否则必须是创建人）
        checkCreatorPermission(entity, creatorField);
    }

    // 从方法参数中提取资源ID
    private Long getResourceIdFromParams(JoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            if (paramName.equals(paramNames[i]) && args[i] instanceof Long) {
                return (Long) args[i];
            }
        }
        return null;
    }

    // 通过实体类对应的Service查询实体
    private Object getEntityById(Class<?> entityClass, Long id) throws Exception {
        // 假设Service命名规则为XXXService（如House -> HouseService）
        String serviceName = Character.toLowerCase(entityClass.getSimpleName().charAt(0))
                + entityClass.getSimpleName().substring(1) + "Service";
        Object service = applicationContext.getBean(serviceName);

        // 调用Service的getById方法
        Method getByIdMethod = service.getClass().getMethod("getById", Long.class);
        return getByIdMethod.invoke(service, id);
    }

    // 校验租户归属（实体的tenantId必须与当前租户一致）
    private void checkTenantPermission(Object entity) throws Exception {
        Long tenantId = TenantContext.getTenantId();
        Field tenantField = entity.getClass().getDeclaredField("tenantId");
        tenantField.setAccessible(true);
        Long entityTenantId = (Long) tenantField.get(entity);

        if (!tenantId.equals(entityTenantId)) {
            throw new BusinessException(403, "无权访问其他租户的资源");
        }
    }

    // 校验创建人权限（非管理员必须是资源创建人）
    private void checkCreatorPermission(Object entity, String creatorField) throws Exception {
        // 管理员/店长直接通过
        if (RoleUtil.isAdmin() || RoleUtil.isStoreManager()) {
            return;
        }

        // 获取当前经纪人ID
        Long currentAgentId = AgentContext.getAgentId();

        // 获取实体的创建人ID
        Field creatorFieldObj = entity.getClass().getDeclaredField(creatorField);
        creatorFieldObj.setAccessible(true);
        Long entityCreatorId = (Long) creatorFieldObj.get(entity);

        if (!currentAgentId.equals(entityCreatorId)) {
            throw new BusinessException(403, "无权操作他人创建的资源");
        }
    }

    @Before("@annotation(agentDataPermission)")
    public void before(JoinPoint joinPoint, AgentDataPermission agentDataPermission) throws Exception {
        Long currentAgentId = AgentContext.getAgentId();
        Long currentTenantId = TenantContext.getTenantId();

        if (currentTenantId == null) {
            throw new BusinessException(403, "经纪人或租户上下文信息缺失，无法应用数据权限");
        }

        // 获取注解参数：实体类和创建人字段
        Class<?> entityClass = agentDataPermission.entityClass();
        String creatorField = agentDataPermission.creatorField();

        // 遍历方法参数，处理LambdaQueryWrapper
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof LambdaQueryWrapper<?> queryWrapper) {
                // 增强查询条件：租户隔离 + 经纪人权限
                addTenantFilter(queryWrapper, entityClass, currentTenantId);
                addCreatorFilter(queryWrapper, entityClass, creatorField, currentAgentId);
            }
        }
    }

    /**
     * 添加租户过滤条件（通用逻辑：适配所有含tenantId字段的实体）
     */
    private void addTenantFilter(LambdaQueryWrapper<?> queryWrapper, Class<?> entityClass, Long tenantId) throws Exception {
        if (!hasField(entityClass, "tenantId")) {
            log.warn("实体{}无tenantId字段，跳过租户过滤", entityClass.getSimpleName());
            return;
        }
        // 调用LambdaQueryWrapper的eq方法，动态添加tenantId条件
        Method eqMethod = LambdaQueryWrapper.class.getMethod("eq", String.class, Object.class);
        eqMethod.invoke(queryWrapper, "tenantId", tenantId);
        log.debug("实体{}添加租户过滤：tenantId = {}", entityClass.getSimpleName(), tenantId);
    }

    /**
     * 添加创建人过滤条件（核心适配逻辑：支持多字段名）
     */
    private void addCreatorFilter(LambdaQueryWrapper<?> queryWrapper, Class<?> entityClass,
                                  String creatorField, Long agentId) throws Exception {
        // 步骤1：确定最终使用的创建人字段名
        String finalCreatorField = determineCreatorField(entityClass, creatorField);
        if (finalCreatorField == null) {
            throw new BusinessException(500, "实体" + entityClass.getSimpleName() + "未配置有效的创建人字段");
        }

        // 步骤2：特殊实体字段类型处理（如非Long类型）
        Object filteredValue = handleSpecialFieldType(entityClass, finalCreatorField, agentId);

        // 步骤3：添加创建人条件
        Method eqMethod = LambdaQueryWrapper.class.getMethod("eq", String.class, Object.class);
        eqMethod.invoke(queryWrapper, finalCreatorField, filteredValue);
        log.debug("实体{}添加权限过滤：{} = {}", entityClass.getSimpleName(), finalCreatorField, filteredValue);
    }

    /**
     * 确定实体类的创建人字段（注解指定优先，否则自动匹配）
     */
    private String determineCreatorField(Class<?> entityClass, String annotationField) {
        // 1. 优先使用注解指定的字段
        if (annotationField != null && !annotationField.isEmpty()) {
            if (hasField(entityClass, annotationField)) {
                return annotationField;
            } else {
                log.error("实体{}不存在字段{}", entityClass.getSimpleName(), annotationField);
                return null;
            }
        }

        // 2. 自动匹配常见创建人字段（按优先级）
        String[] candidateFields = {"createAgentId", "agentId", "creatorId"};
        for (String field : candidateFields) {
            if (hasField(entityClass, field)) {
                return field;
            }
        }
        return null;
    }

    /**
     * 处理特殊字段类型（如字段类型非Long时的转换）
     */
    private Object handleSpecialFieldType(Class<?> entityClass, String fieldName, Long agentId) throws Exception {
        Field field = entityClass.getDeclaredField(fieldName);
        Class<?> fieldType = field.getType();

        // 若字段类型为String，转换agentId为字符串
        if (fieldType == String.class) {
            return String.valueOf(agentId);
        }
        // 其他类型（如Integer）可在此扩展
        return agentId;
    }

    /**
     * 检查实体类是否包含指定字段（含父类字段）
     */
    private boolean hasField(Class<?> clazz, String fieldName) {
        // 递归检查当前类及父类
        while (clazz != null) {
            try {
                clazz.getDeclaredField(fieldName);
                return true;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass(); // 检查父类
            }
        }
        return false;
    }
}
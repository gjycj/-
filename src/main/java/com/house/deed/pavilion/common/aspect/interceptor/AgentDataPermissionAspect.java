package com.house.deed.pavilion.common.aspect.interceptor;

import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.common.aspect.annotation.AgentDataPermission;
import com.house.deed.pavilion.common.aspect.annotation.CheckAgentPermission;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.AgentContext;
import com.house.deed.pavilion.common.util.RoleUtil;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.common.util.ValidateUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 经纪人数据权限切面（支持多实体字段适配）
 * 修正说明：统一拦截逻辑、修复编译错误、去重冗余代码、规范风格
 */
@Aspect
@Component
@Slf4j
public class AgentDataPermissionAspect {

    @Autowired
    private ApplicationContext applicationContext;

    // ==================== 拦截 @CheckAgentPermission 注解（增删改操作：资源归属校验）====================
    @Before("@annotation(checkPermission)")
    public void checkResourcePermission(JoinPoint joinPoint, CheckAgentPermission checkPermission) throws Exception {
        // 1. 提取注解参数
        Class<?> entityClass = checkPermission.entityClass();
        String resourceIdParamName = checkPermission.resourceIdParam();
        String creatorField = checkPermission.creatorField();

        // 2. 提取资源ID（复用通用参数提取方法）
        Long resourceId = getParamValueByParamName(joinPoint, resourceIdParamName, Long.class);
        ValidateUtil.notNull(resourceId, "资源ID参数不存在或格式错误");

        // 3. 查询实体（复用通用查询方法）
        Object entity = getEntityByClassAndId(entityClass, resourceId);

        // 4. 权限校验（复用通用校验方法）
        checkEntityTenantPermission(entity);
        checkEntityCreatorPermission(entity, creatorField);
    }

    // ==================== 拦截 @AgentDataPermission 注解（所有操作：查询条件增强+权限校验）====================
    @Around("@annotation(dataPermission)")
    public Object handleDataPermission(ProceedingJoinPoint joinPoint, AgentDataPermission dataPermission) throws Throwable {
        // 1. 基础上下文校验
        Long currentAgentId = AgentContext.getAgentId();
        Long currentTenantId = TenantContext.getTenantId();
        ValidateUtil.notNull(currentAgentId, "经纪人上下文信息缺失，无法应用数据权限");
        ValidateUtil.notNull(currentTenantId, "租户上下文信息缺失，无法应用数据权限");

        // 2. 提取注解参数
        Class<?> entityClass = dataPermission.entityClass();
        String creatorField = dataPermission.creatorField();
        AgentDataPermission.OperationType operation = dataPermission.operation();

        // 3. 非查询操作：校验资源归属（复用@CheckAgentPermission的校验逻辑）
        if (operation != AgentDataPermission.OperationType.QUERY) {
            String dataIdParamName = dataPermission.dataIdParam();
            Long dataId = getParamValueByParamName(joinPoint, dataIdParamName, Long.class);
            ValidateUtil.notNull(dataId, "业务ID参数不存在或格式错误");

            Object entity = getEntityByClassAndId(entityClass, dataId);
            checkEntityTenantPermission(entity);
            checkEntityCreatorPermission(entity, creatorField);
        }

        // 4. 查询操作：增强LambdaQueryWrapper（反射调用eq，避免编译错误）
        if (operation == AgentDataPermission.OperationType.QUERY) {
            enhanceLambdaQueryWrapper(joinPoint.getArgs(), entityClass, currentTenantId, currentAgentId, creatorField);
        }

        // 5. 执行原业务方法
        return joinPoint.proceed();
    }

    // ==================== 核心工具方法（统一风格、去重冗余）====================

    /**
     * 从方法参数中提取指定名称+类型的参数值（通用复用）
     */
    private <T> T getParamValueByParamName(JoinPoint joinPoint, String paramName, Class<T> paramType) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // Spring Boot 3推荐：通过-parameters编译选项获取参数名
        StandardReflectionParameterNameDiscoverer discoverer = new StandardReflectionParameterNameDiscoverer();
        String[] paramNames = discoverer.getParameterNames(method);
        Object[] args = joinPoint.getArgs();

        if (paramNames == null || args == null || paramNames.length != args.length) {
            log.error("方法参数解析失败，方法名：{}", method.getName());
            return null;
        }

        // 匹配参数名并转换类型
        for (int i = 0; i < paramNames.length; i++) {
            if (paramName.equals(paramNames[i]) && paramType.isInstance(args[i])) {
                return paramType.cast(args[i]);
            }
        }
        return null;
    }

    /**
     * 根据实体类+ID查询实体（通用复用，依赖Service命名规范：XXXService）
     */
    private Object getEntityByClassAndId(Class<?> entityClass, Long id) throws Exception {
        String serviceBeanName = lowerFirstChar(entityClass.getSimpleName()) + "Service";
        IService<?> service = (IService<?>) applicationContext.getBean(serviceBeanName);

        Object entity = service.getById(id);
        if (entity == null) {
            throw new BusinessException(404, String.format("%s不存在（ID：%d）", entityClass.getSimpleName(), id));
        }
        return entity;
    }

    /**
     * 校验实体租户归属（通用复用）
     */
    private void checkEntityTenantPermission(Object entity) {
        Long currentTenantId = TenantContext.getTenantId();
        Long entityTenantId = (Long) ReflectUtil.getFieldValue(entity, "tenantId");

        if (!Objects.equals(currentTenantId, entityTenantId)) {
            throw new BusinessException(403, "无权访问其他租户的资源");
        }
    }

    /**
     * 校验实体创建人权限（通用复用，管理员/店长豁免）
     */
    private void checkEntityCreatorPermission(Object entity, String creatorField) {
        if (RoleUtil.isAdmin() || RoleUtil.isStoreManager()) {
            return;
        }

        Long currentAgentId = AgentContext.getAgentId();
        Long entityCreatorId = (Long) ReflectUtil.getFieldValue(entity, creatorField);

        if (!Objects.equals(currentAgentId, entityCreatorId)) {
            throw new BusinessException(403, "无权操作他人创建的资源");
        }
    }

    /**
     * 增强LambdaQueryWrapper：反射调用eq添加租户+创建人过滤（修复编译错误）
     */
    private void enhanceLambdaQueryWrapper(Object[] args, Class<?> entityClass,
                                           Long tenantId, Long agentId, String creatorField) throws Exception {
        // 1. 获取LambdaQueryWrapper的eq方法（精确匹配：String column, Object val）
        Method eqMethod = getLambdaQueryWrapperEqMethod();
        if (eqMethod == null) {
            throw new BusinessException(500, "获取查询条件eq方法失败，无法应用数据权限");
        }

        // 2. 确定有效创建人字段
        String finalCreatorField = determineValidCreatorField(entityClass, creatorField);

        // 3. 遍历参数，给LambdaQueryWrapper添加过滤条件
        for (Object arg : args) {
            if (arg instanceof LambdaQueryWrapper<?>) {
                LambdaQueryWrapper<?> queryWrapper = (LambdaQueryWrapper<?>) arg;

                // 反射添加租户过滤
                eqMethod.invoke(queryWrapper, "tenantId", tenantId);
                // 反射添加创建人过滤（处理特殊字段类型）
                Object filteredValue = handleSpecialFieldType(entityClass, finalCreatorField, agentId);
                eqMethod.invoke(queryWrapper, finalCreatorField, filteredValue);

                log.debug("查询条件增强：实体={}, 租户过滤（tenantId={}）, 创建人过滤（{}={}）",
                        entityClass.getSimpleName(), tenantId, finalCreatorField, filteredValue);
            }
        }
    }

    /**
     * 获取LambdaQueryWrapper的eq方法（String column, Object val），解决编译冲突
     */
    private Method getLambdaQueryWrapperEqMethod() {
        try {
            return LambdaQueryWrapper.class.getMethod("eq", String.class, Object.class);
        } catch (NoSuchMethodException e) {
            log.error("未找到LambdaQueryWrapper.eq(String, Object)方法，请检查MyBatis-Plus版本", e);
            return null;
        }
    }

    /**
     * 确定实体有效创建人字段（注解指定优先，否则自动匹配常见字段）
     */
    private String determineValidCreatorField(Class<?> entityClass, String annotationField) {
        // 1. 优先使用注解指定字段
        if (annotationField != null && !annotationField.isEmpty() && ReflectUtil.hasField(entityClass, annotationField)) {
            return annotationField;
        }

        // 2. 自动匹配常见创建人字段（按优先级）
        String[] candidateFields = {"createAgentId", "agentId", "creatorId"};
        for (String field : candidateFields) {
            if (ReflectUtil.hasField(entityClass, field)) {
                return field;
            }
        }

        throw new BusinessException(500, String.format("实体%s未配置有效的创建人字段", entityClass.getSimpleName()));
    }

    /**
     * 处理特殊字段类型（如创建人字段为String时，转换agentId类型）
     */
    private Object handleSpecialFieldType(Class<?> entityClass, String fieldName, Long agentId) {
        Class<?> fieldType = ReflectUtil.getField(entityClass, fieldName).getType();
        // 字段类型为String时，转换Long为String
        if (fieldType == String.class) {
            return String.valueOf(agentId);
        }
        // 其他类型（如Integer）可在此扩展转换逻辑
        return agentId;
    }

    /**
     * 首字母小写（用于Service Bean名称转换：如House -> houseService）
     */
    private String lowerFirstChar(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }
}
package com.house.deed.pavilion.common.aspect.interceptor;

import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.common.aspect.annotation.AgentDataPermission;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.AgentContext;
import com.house.deed.pavilion.common.util.RoleUtil;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.common.util.ValidateUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 经纪人数据权限切面（支持关联实体校验和多实体校验）
 */
@Aspect
@Component
@Slf4j
public class AgentDataPermissionAspect {

    @Autowired
    private ApplicationContext applicationContext;

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

        // 3. 非查询操作：校验资源归属（含新增的关联实体和多实体校验）
        if (operation != AgentDataPermission.OperationType.QUERY) {
            String dataIdParamName = dataPermission.dataIdParam();
            Long dataId = getParamValueByParamName(joinPoint, dataIdParamName, Long.class);
            ValidateUtil.notNull(dataId, "业务ID参数不存在或格式错误");

            // 3.1 校验主实体权限（原有逻辑）
            Object mainEntity = getEntityByClassAndId(entityClass, dataId);
            checkEntityTenantPermission(mainEntity);
            checkEntityCreatorPermission(mainEntity, creatorField);

            // 3.2 新增：校验关联实体权限（如维修工单关联的房源）
            checkRelatedEntityPermission(mainEntity, dataPermission);

            // 3.3 新增：校验多实体权限（如带看记录关联的客户和房源）
            checkMultiEntityPermission(joinPoint, dataPermission);
        }

        // 4. 查询操作：增强LambdaQueryWrapper（保持原有逻辑）
        if (operation == AgentDataPermission.OperationType.QUERY) {
            enhanceLambdaQueryWrapper(joinPoint.getArgs(), entityClass, currentTenantId, currentAgentId, creatorField);
        }

        // 5. 执行原业务方法
        return joinPoint.proceed();
    }

    // ==================== 新增：关联实体权限校验（如维修工单关联房源）====================
    private void checkRelatedEntityPermission(Object mainEntity, AgentDataPermission dataPermission) throws Exception {
        Class<?> relatedEntityClass = dataPermission.relatedEntityClass();
        String relatedIdField = dataPermission.relatedIdField();
        String relatedCreatorField = dataPermission.relatedCreatorField();

        // 若未配置关联实体，直接返回
        if (relatedEntityClass == void.class || relatedIdField.isEmpty()) {
            return;
        }

        // 1. 从主实体中获取关联ID（如从MaintenanceOrder中获取houseId）
        Object relatedId = ReflectUtil.getFieldValue(mainEntity, relatedIdField);
        ValidateUtil.notNull(relatedId, String.format("主实体缺少关联字段：%s", relatedIdField));
        if (!(relatedId instanceof Long)) {
            throw new BusinessException(400, String.format("关联字段%s类型必须为Long", relatedIdField));
        }

        // 2. 查询关联实体（如House）
        Object relatedEntity = getEntityByClassAndId(relatedEntityClass, (Long) relatedId);

        // 3. 校验关联实体的租户和创建人权限
        checkEntityTenantPermission(relatedEntity);
        checkEntityCreatorPermission(relatedEntity, relatedCreatorField);

        log.debug("关联实体权限校验通过：主实体={}，关联实体={}，关联ID={}",
                mainEntity.getClass().getSimpleName(),
                relatedEntityClass.getSimpleName(),
                relatedId);
    }

    // ==================== 新增：多实体权限校验（如带看记录同时校验客户和房源）====================
    private void checkMultiEntityPermission(ProceedingJoinPoint joinPoint, AgentDataPermission dataPermission) throws Exception {
        Class<?>[] multiEntityClasses = dataPermission.multiEntityClasses();
        String[] multiIdParams = dataPermission.multiIdParams();

        // 若未配置多实体，或参数长度不匹配，直接返回
        if (multiEntityClasses.length == 0 || multiIdParams.length == 0 || multiEntityClasses.length != multiIdParams.length) {
            return;
        }

        // 遍历校验每个实体
        for (int i = 0; i < multiEntityClasses.length; i++) {
            Class<?> entityClass = multiEntityClasses[i];
            String idParamName = multiIdParams[i];

            // 1. 提取当前实体的ID参数值（如客户ID、房源ID）
            Long entityId = getParamValueByParamName(joinPoint, idParamName, Long.class);
            ValidateUtil.notNull(entityId, String.format("多实体参数缺失：%s", idParamName));

            // 2. 查询实体并校验权限（复用主实体的校验方法）
            Object entity = getEntityByClassAndId(entityClass, entityId);
            checkEntityTenantPermission(entity);
            // 多实体默认使用自身的creatorField（如需自定义可扩展注解参数）
            String creatorField = determineValidCreatorField(entityClass, "createAgentId");
            checkEntityCreatorPermission(entity, creatorField);

            log.debug("多实体权限校验通过：实体={}，ID={}", entityClass.getSimpleName(), entityId);
        }
    }

    // ==================== 原有工具方法（保持不变）====================

    private <T> T getParamValueByParamName(ProceedingJoinPoint joinPoint, String paramName, Class<T> paramType) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        StandardReflectionParameterNameDiscoverer discoverer = new StandardReflectionParameterNameDiscoverer();
        String[] paramNames = discoverer.getParameterNames(method);
        Object[] args = joinPoint.getArgs();

        if (paramNames == null || args == null || paramNames.length != args.length) {
            log.error("方法参数解析失败，方法名：{}", method.getName());
            return null;
        }

        for (int i = 0; i < paramNames.length; i++) {
            if (paramName.equals(paramNames[i]) && paramType.isInstance(args[i])) {
                return paramType.cast(args[i]);
            }
        }
        return null;
    }

    private Object getEntityByClassAndId(Class<?> entityClass, Long id) throws Exception {
        String serviceBeanName = lowerFirstChar(entityClass.getSimpleName()) + "Service";
        IService<?> service = (IService<?>) applicationContext.getBean(serviceBeanName);

        Object entity = service.getById(id);
        if (entity == null) {
            throw new BusinessException(404, String.format("%s不存在（ID：%d）", entityClass.getSimpleName(), id));
        }
        return entity;
    }

    private void checkEntityTenantPermission(Object entity) {
        Long currentTenantId = TenantContext.getTenantId();
        Long entityTenantId = (Long) ReflectUtil.getFieldValue(entity, "tenantId");

        if (!Objects.equals(currentTenantId, entityTenantId)) {
            throw new BusinessException(403, "无权访问其他租户的资源");
        }
    }

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

    private void enhanceLambdaQueryWrapper(Object[] args, Class<?> entityClass,
                                           Long tenantId, Long agentId, String creatorField) throws Exception {
        Method eqMethod = getLambdaQueryWrapperEqMethod();
        if (eqMethod == null) {
            throw new BusinessException(500, "获取查询条件eq方法失败，无法应用数据权限");
        }

        String finalCreatorField = determineValidCreatorField(entityClass, creatorField);

        for (Object arg : args) {
            if (arg instanceof LambdaQueryWrapper<?>) {
                LambdaQueryWrapper<?> queryWrapper = (LambdaQueryWrapper<?>) arg;

                eqMethod.invoke(queryWrapper, "tenantId", tenantId);
                Object filteredValue = handleSpecialFieldType(entityClass, finalCreatorField, agentId);
                eqMethod.invoke(queryWrapper, finalCreatorField, filteredValue);

                log.debug("查询条件增强：实体={}, 租户过滤（tenantId={}）, 创建人过滤（{}={}）",
                        entityClass.getSimpleName(), tenantId, finalCreatorField, filteredValue);
            }
        }
    }

    private Method getLambdaQueryWrapperEqMethod() {
        try {
            return LambdaQueryWrapper.class.getMethod("eq", String.class, Object.class);
        } catch (NoSuchMethodException e) {
            log.error("未找到LambdaQueryWrapper.eq(String, Object)方法，请检查MyBatis-Plus版本", e);
            return null;
        }
    }

    private String determineValidCreatorField(Class<?> entityClass, String annotationField) {
        if (annotationField != null && !annotationField.isEmpty() && ReflectUtil.hasField(entityClass, annotationField)) {
            return annotationField;
        }

        String[] candidateFields = {"createAgentId", "agentId", "creatorId"};
        for (String field : candidateFields) {
            if (ReflectUtil.hasField(entityClass, field)) {
                return field;
            }
        }

        throw new BusinessException(500, String.format("实体%s未配置有效的创建人字段", entityClass.getSimpleName()));
    }

    private Object handleSpecialFieldType(Class<?> entityClass, String fieldName, Long agentId) {
        Class<?> fieldType = ReflectUtil.getField(entityClass, fieldName).getType();
        if (fieldType == String.class) {
            return String.valueOf(agentId);
        }
        return agentId;
    }

    private String lowerFirstChar(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }
}
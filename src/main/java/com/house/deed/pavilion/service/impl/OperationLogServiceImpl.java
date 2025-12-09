package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.OperationLog;
import com.house.deed.pavilion.mapper.OperationLogMapper;
import com.house.deed.pavilion.service.OperationLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 系统操作日志服务实现类
 *
 * <p>实现操作日志的增删改查及批量操作，所有方法均包含严格的租户级数据隔离和审计特性</p>
 * <p>业务特点：</p>
 * <ul>
 *   <li>租户ID=0为系统级日志，仅管理员可操作</li>
 *   <li>普通租户仅能操作自身租户的日志</li>
 *   <li>核心审计字段（模块、操作类型、操作内容、IP地址）禁止修改</li>
 *   <li>删除限制：系统级日志禁止删除，仅允许删除指定时间前的非系统级日志</li>
 * </ul>
 * <p>技术实现：</p>
 * <ul>
 *   <li>所有枚举值校验完全匹配实体类allowableValues配置</li>
 *   <li>自动填充操作时间和创建时间（通过MyBatis Plus字段填充器）</li>
 *   <li>使用QueryWrapper而非LambdaQueryWrapper以适应动态查询需求</li>
 *   <li>使用Spring Assert进行参数校验</li>
 * </ul>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    // ==================== 实体类枚举常量（完全匹配allowableValues） ====================

    /**
     * 合法操作模块集合
     * <p>与实体类allowableValues配置保持一致，包括：</p>
     * <ul>
     *   <li>HOUSE_MANAGE - 房源管理</li>
     *   <li>CUSTOMER_MANAGE - 客户管理</li>
     *   <li>CONTRACT_MANAGE - 合同管理</li>
     *   <li>LANDLORD_MANAGE - 房东管理</li>
     *   <li>USER_MANAGE - 用户管理</li>
     *   <li>SYSTEM_CONFIG - 系统配置</li>
     *   <li>OTHER - 其他</li>
     * </ul>
     */
    private static final Set<String> VALID_MODULES = Set.of(
            "HOUSE_MANAGE", "CUSTOMER_MANAGE", "CONTRACT_MANAGE",
            "LANDLORD_MANAGE", "USER_MANAGE", "SYSTEM_CONFIG", "OTHER"
    );

    /**
     * 合法操作类型集合
     * <p>与实体类allowableValues配置保持一致，包括：</p>
     * <ul>
     *   <li>ADD - 新增</li>
     *   <li>UPDATE - 更新</li>
     *   <li>DELETE - 删除</li>
     *   <li>QUERY - 查询</li>
     *   <li>IMPORT - 导入</li>
     *   <li>EXPORT - 导出</li>
     *   <li>CONFIG - 配置</li>
     *   <li>OTHER - 其他</li>
     * </ul>
     */
    private static final Set<String> VALID_OPERATION_TYPES = Set.of(
            "ADD", "UPDATE", "DELETE", "QUERY", "IMPORT", "EXPORT", "CONFIG", "OTHER"
    );

    /**
     * IP地址正则表达式
     * <p>匹配实体类Pattern注解配置，支持格式：</p>
     * <ul>
     *   <li>IPv4地址（如：192.168.1.1）</li>
     *   <li>IPv6地址（如：2001:0db8:85a3:0000:0000:8a2e:0370:7334）</li>
     * </ul>
     */
    private static final Pattern IP_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$|" +
                    "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$"
    );

    // ==================== 基础CRUD实现 ====================

    /**
     * 新增操作日志
     *
     * <p>业务校验：</p>
     * <ul>
     *   <li>基础字段非空校验（模块、操作类型、操作内容等）</li>
     *   <li>枚举值合法性校验（操作模块、操作类型）</li>
     *   <li>格式校验（IP地址格式、字段长度等）</li>
     * </ul>
     * <p>默认值设置：</p>
     * <ul>
     *   <li>租户ID未传时默认设为0（系统级日志）</li>
     *   <li>操作时间自动填充（通过MyBatis Plus字段填充器）</li>
     * </ul>
     *
     * @param log 操作日志实体对象，需包含必填信息
     * @return 新增成功返回true，否则返回false
     * @throws IllegalArgumentException 当违反任何业务规则时抛出
     */
    @Override
    public boolean saveOperationLog(OperationLog log) {
        // 1. 基础字段+枚举+格式校验（完全匹配实体类注解）
        validateEntityBaseConstraints(log);

        // 2. 租户ID默认值：未传则设为0（系统级）
        if (log.getTenantId() == null) {
            log.setTenantId(0L);
        }

        // 3. 执行新增操作（createTime自动填充）
        return baseMapper.insert(log) > 0;
    }

    /**
     * 根据ID更新操作日志
     *
     * <p>业务限制：仅允许修改操作人姓名，其他核心审计字段禁止修改</p>
     * <p>业务校验：</p>
     * <ul>
     *   <li>数据必须存在且属于当前租户（系统级日志需管理员权限）</li>
     *   <li>禁止修改核心审计字段（模块、操作类型、操作内容、IP地址等）</li>
     *   <li>操作人姓名长度校验（不超过50字符）</li>
     * </ul>
     *
     * @param log 操作日志实体对象，需包含主键ID、租户ID和需要更新的字段
     * @return 更新成功返回true，否则返回false
     * @throws IllegalArgumentException 当违反任何业务规则时抛出
     */
    @Override
    public boolean updateOperationLogById(OperationLog log) {
        // 1. 基础参数校验
        Assert.notNull(log.getId(), "日志ID不能为空");
        Assert.notNull(log.getTenantId(), "租户ID不能为空");

        // 2. 校验日志存在且归属当前租户（系统级日志需管理员权限，此处简化校验）
        OperationLog existLog = getOperationLogById(log.getId(), log.getTenantId());
        Assert.notNull(existLog, "操作日志不存在或无权限操作");

        // 3. 禁止修改核心审计字段（仅允许修改操作人姓名）
        validateImmutableFields(log, existLog);

        // 4. 操作人姓名长度校验（匹配实体类@Size）
        if (StringUtils.hasText(log.getOperatorName())) {
            Assert.isTrue(log.getOperatorName().length() <= 50, "操作人姓名长度不能超过50字符");
        } else {
            throw new IllegalArgumentException("仅允许修改操作人姓名，且姓名不能为空");
        }

        // 5. 执行更新（仅更新operator_name字段）
        return baseMapper.update(
                new OperationLog() {{
                    setId(log.getId());
                    setOperatorName(log.getOperatorName());
                }},
                new QueryWrapper<OperationLog>()
                        .eq("id", log.getId())
                        .eq("tenant_id", log.getTenantId())
        ) > 0;
    }

    /**
     * 根据ID物理删除操作日志
     *
     * <p>业务限制：</p>
     * <ul>
     *   <li>系统级日志（tenantId=0）禁止删除</li>
     *   <li>仅允许删除7天前的日志（可配置，防止误删最新审计日志）</li>
     * </ul>
     *
     * @param id 操作日志主键ID
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 删除成功返回true，否则返回false
     * @throws IllegalArgumentException 当违反删除限制时抛出
     */
    @Override
    public boolean removeOperationLogById(Long id, Long tenantId) {
        // 1. 基础参数校验
        Assert.notNull(id, "日志ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 2. 校验日志存在且归属当前租户
        OperationLog existLog = getOperationLogById(id, tenantId);
        Assert.notNull(existLog, "操作日志不存在或无权限操作");

        // 3. 系统级日志（tenantId=0）禁止删除
        if (existLog.getTenantId() != null && existLog.getTenantId() == 0) {
            throw new IllegalArgumentException("系统级操作日志禁止删除");
        }

        // 4. 仅允许删除7天前的日志（可配置，防止误删最新审计日志）
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        if (existLog.getCreateTime() == null || existLog.getCreateTime().isAfter(sevenDaysAgo)) {
            throw new IllegalArgumentException("仅允许删除7天前的操作日志，禁止删除近期日志");
        }

        return baseMapper.deleteById(id) > 0;
    }

    /**
     * 根据ID查询操作日志详细信息（租户隔离）
     *
     * <p>租户隔离规则：</p>
     * <ul>
     *   <li>普通租户仅能查询自身租户的日志</li>
     *   <li>系统级日志（tenantId=0）仅管理员可查询</li>
     * </ul>
     *
     * @param id 操作日志主键ID
     * @param tenantId 租户ID，用于数据隔离
     * @return 符合条件的操作日志实体对象，未找到返回null
     */
    @Override
    public OperationLog getOperationLogById(Long id, Long tenantId) {
        Assert.notNull(id, "日志ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        QueryWrapper<OperationLog> wrapper = new QueryWrapper<OperationLog>()
                .eq("id", id);

        // 租户隔离：普通租户仅查自身日志，系统级日志（tenantId=0）需管理员权限（此处简化）
        if (tenantId != 0) {
            wrapper.eq("tenant_id", tenantId);
        } else {
            wrapper.eq("tenant_id", 0);
        }

        return baseMapper.selectOne(wrapper);
    }

    // ==================== 多条件查询实现 ====================

    /**
     * 多条件分页查询操作日志
     *
     * <p>支持以下查询条件：</p>
     * <ul>
     *   <li>操作模块精确查询</li>
     *   <li>操作类型精确查询</li>
     *   <li>操作人ID精确查询</li>
     *   <li>操作人姓名模糊查询</li>
     *   <li>IP地址精确查询</li>
     *   <li>操作时间范围查询</li>
     * </ul>
     * <p>排序规则：按创建时间倒序（最新日志在前）</p>
     *
     * @param page 分页参数对象，包含页码和每页大小
     * @param queryParams 查询参数映射表，key为字段名，value为查询值
     * @param tenantId 租户ID，用于数据隔离
     * @return 分页查询结果，包含操作日志列表和分页信息
     */
    @Override
    public IPage<OperationLog> pageQuery(Page<OperationLog> page, Map<String, Object> queryParams, Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");
        QueryWrapper<OperationLog> wrapper = buildQueryWrapper(queryParams, tenantId);

        // 默认排序：操作时间倒序（最新日志优先）
        wrapper.orderByDesc("create_time");

        return baseMapper.selectPage(page, wrapper);
    }

    /**
     * 多条件查询操作日志列表（不分页）
     *
     * <p>查询条件与分页查询方法保持一致，但不进行分页处理</p>
     *
     * @param queryParams 查询参数映射表，key为字段名，value为查询值
     * @param tenantId 租户ID，用于数据隔离
     * @return 符合条件的操作日志实体对象列表
     */
    @Override
    public List<OperationLog> listByConditions(Map<String, Object> queryParams, Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");
        return baseMapper.selectList(buildQueryWrapper(queryParams, tenantId));
    }

    /**
     * 根据操作模块和类型查询日志列表
     *
     * <p>按创建时间倒序排列，最新的日志在前</p>
     *
     * @param module 操作模块，必须在VALID_MODULES中
     * @param operationType 操作类型，必须在VALID_OPERATION_TYPES中
     * @param tenantId 租户ID，用于数据隔离
     * @return 符合条件的操作日志实体对象列表
     * @throws IllegalArgumentException 当模块或类型无效时抛出
     */
    @Override
    public List<OperationLog> listByModuleAndType(String module, String operationType, Long tenantId) {
        Assert.hasLength(module, "操作模块不能为空");
        Assert.hasLength(operationType, "操作类型不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.isTrue(VALID_MODULES.contains(module), "无效操作模块：" + module);
        Assert.isTrue(VALID_OPERATION_TYPES.contains(operationType), "无效操作类型：" + operationType);

        QueryWrapper<OperationLog> wrapper = new QueryWrapper<OperationLog>()
                .eq("module", module)
                .eq("operation_type", operationType)
                .orderByDesc("create_time");

        // 租户隔离
        if (tenantId != 0) {
            wrapper.eq("tenant_id", tenantId);
        } else {
            wrapper.eq("tenant_id", 0);
        }

        return baseMapper.selectList(wrapper);
    }

    /**
     * 根据操作人ID查询日志列表
     *
     * <p>按创建时间倒序排列，最新的日志在前</p>
     *
     * @param operatorId 操作人ID
     * @param tenantId 租户ID，用于数据隔离
     * @return 符合条件的操作日志实体对象列表
     */
    @Override
    public List<OperationLog> listByOperatorId(Long operatorId, Long tenantId) {
        Assert.notNull(operatorId, "操作人ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        QueryWrapper<OperationLog> wrapper = new QueryWrapper<OperationLog>()
                .eq("operator_id", operatorId)
                .orderByDesc("create_time");

        // 租户隔离
        if (tenantId != 0) {
            wrapper.eq("tenant_id", tenantId);
        } else {
            wrapper.eq("tenant_id", 0);
        }

        return baseMapper.selectList(wrapper);
    }

    // ==================== 批量操作实现 ====================

    /**
     * 批量新增操作日志（事务保证）
     *
     * <p>在单个事务中执行批量新增，任一记录校验失败或保存失败将导致整个操作回滚</p>
     * <p>批量校验：</p>
     * <ul>
     *   <li>租户一致性校验（所有日志必须属于同一租户/系统级）</li>
     *   <li>实体约束校验（复用单条新增的校验逻辑）</li>
     * </ul>
     *
     * @param logList 操作日志实体对象列表
     * @return 批量新增成功返回true，否则返回false
     * @throws IllegalArgumentException 当违反任何业务规则时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveOperationLogs(List<OperationLog> logList) {
        if (CollectionUtils.isEmpty(logList)) {
            return false;
        }

        // 1. 校验租户一致性（批量日志需属于同一租户/系统级）
        Long tenantId = logList.get(0).getTenantId() == null ? 0 : logList.get(0).getTenantId();
        boolean hasInvalidTenant = logList.stream()
                .anyMatch(log -> {
                    Long logTenantId = log.getTenantId() == null ? 0 : log.getTenantId();
                    return !Objects.equals(logTenantId, tenantId);
                });
        Assert.isTrue(!hasInvalidTenant, "批量新增的日志必须属于同一租户/系统级");

        // 2. 逐条校验实体约束
        for (OperationLog log : logList) {
            validateEntityBaseConstraints(log);
            // 统一租户ID（避免单条日志租户ID不一致）
            log.setTenantId(tenantId);
        }

        // 执行批量保存（事务保证）
        return saveBatch(logList);
    }

    /**
     * 批量删除操作日志（事务保证）
     *
     * <p>在单个事务中执行批量删除，任一记录校验失败或删除失败将导致整个操作回滚</p>
     * <p>业务限制：</p>
     * <ul>
     *   <li>系统级日志禁止删除</li>
     *   <li>仅允许删除指定时间前的日志</li>
     *   <li>所有日志必须属于当前租户</li>
     * </ul>
     *
     * @param ids 待删除的日志ID列表
     * @param tenantId 租户ID，用于数据隔离验证
     * @param beforeTime 删除时间阈值，仅删除此时间之前的日志
     * @return 批量删除成功返回true，否则返回false
     * @throws IllegalArgumentException 当违反删除限制时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemoveOperationLogs(List<Long> ids, Long tenantId, LocalDateTime beforeTime) {
        // 1. 基础参数校验
        Assert.notEmpty(ids, "日志ID列表不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.notNull(beforeTime, "删除时间阈值不能为空");

        // 2. 校验所有日志归属当前租户
        validateLogIdsBelongToTenant(tenantId, ids);

        // 3. 校验所有日志均为非系统级+指定时间前
        QueryWrapper<OperationLog> queryWrapper = new QueryWrapper<OperationLog>()
                .select("id", "tenant_id", "create_time")
                .in("id", ids);
        List<OperationLog> logs = baseMapper.selectList(queryWrapper);

        List<Long> invalidIds = new ArrayList<>();
        for (OperationLog log : logs) {
            // 系统级日志禁止删除
            if (log.getTenantId() != null && log.getTenantId() == 0) {
                invalidIds.add(log.getId());
            }
            // 未到删除时间阈值
            if (log.getCreateTime() == null || log.getCreateTime().isAfter(beforeTime)) {
                invalidIds.add(log.getId());
            }
        }
        Assert.isTrue(invalidIds.isEmpty(),
                "以下日志不允许删除（系统级/未到删除时间）：" + invalidIds);

        // 4. 执行批量删除（事务保证）
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 验证日志ID列表是否全部属于当前租户
     *
     * <p>校验逻辑：</p>
     * <ul>
     *   <li>检查ID是否存在（是否存在未查询到的ID）</li>
     *   <li>检查存在的ID是否属于当前租户</li>
     * </ul>
     * <p>权限规则：普通租户仅能操作自身日志，系统级需tenantId=0</p>
     *
     * @param tenantId 租户ID
     * @param logIds 待验证的日志ID列表
     * @throws IllegalArgumentException 当存在不存在的ID或不属于当前租户的ID时抛出
     */
    @Override
    public void validateLogIdsBelongToTenant(Long tenantId, List<Long> logIds) {
        if (CollectionUtils.isEmpty(logIds)) {
            return;
        }

        // 1. 查询存在的日志ID及租户ID（提取到方法中）
        List<OperationLog> logs = findLogsByIds(logIds);

        // 2. 检查不存在的ID
        Set<Long> existingIds = logs.stream().map(OperationLog::getId).collect(Collectors.toSet());
        List<Long> nonExistentIds = logIds.stream()
                .filter(id -> !existingIds.contains(id))
                .toList();
        Assert.isTrue(nonExistentIds.isEmpty(), "以下日志ID不存在：" + nonExistentIds);

        // 3. 检查租户权限（普通租户仅能操作自身日志，系统级需tenantId=0）
        List<Long> invalidIds = new ArrayList<>();
        for (OperationLog log : logs) {
            Long logTenantId = log.getTenantId() == null ? 0 : log.getTenantId();
            if (tenantId != 0 && !Objects.equals(logTenantId, tenantId)) {
                invalidIds.add(log.getId());
            }
        }
        Assert.isTrue(invalidIds.isEmpty(), "无权限操作以下日志ID：" + invalidIds);
    }

    /**
     * 根据ID列表查询操作日志（提取方法，便于测试）
     *
     * <p>查询指定ID的操作日志，仅返回ID和租户ID字段</p>
     *
     * @param logIds 日志ID列表
     * @return 符合条件的操作日志实体对象列表（仅含id和tenant_id字段）
     */
    public List<OperationLog> findLogsByIds(List<Long> logIds) {
        if (CollectionUtils.isEmpty(logIds)) {
            return Collections.emptyList();
        }

        QueryWrapper<OperationLog> queryWrapper = new QueryWrapper<OperationLog>()
                .select("id", "tenant_id")
                .in("id", logIds);
        return baseMapper.selectList(queryWrapper);
    }

    // ==================== 私有工具方法（完全匹配实体类约束） ====================

    /**
     * 构建查询条件（严格匹配实体类字段）
     *
     * <p>根据查询参数动态构建查询条件，支持以下参数：</p>
     * <ul>
     *   <li>module - 操作模块精确查询</li>
     *   <li>operationType - 操作类型精确查询</li>
     *   <li>operatorId - 操作人ID精确查询</li>
     *   <li>operatorName - 操作人姓名模糊查询</li>
     *   <li>ipAddress - IP地址精确查询</li>
     *   <li>startCreateTime/endCreateTime - 创建时间范围查询</li>
     * </ul>
     * <p>所有查询均自动添加租户隔离条件</p>
     *
     * @param queryParams 查询参数映射表
     * @param tenantId 租户ID，用于数据隔离
     * @return 构建完成的QueryWrapper对象
     */
    private QueryWrapper<OperationLog> buildQueryWrapper(Map<String, Object> queryParams, Long tenantId) {
        QueryWrapper<OperationLog> wrapper = new QueryWrapper<>();

        // 租户隔离
        if (tenantId != 0) {
            wrapper.eq("tenant_id", tenantId);
        } else {
            wrapper.eq("tenant_id", 0);
        }

        if (ObjectUtils.isEmpty(queryParams)) {
            return wrapper;
        }

        // 操作模块精确查询
        if (queryParams.containsKey("module") && StringUtils.hasText(queryParams.get("module").toString())) {
            String module = queryParams.get("module").toString();
            Assert.isTrue(VALID_MODULES.contains(module), "无效操作模块：" + module);
            wrapper.eq("module", module);
        }

        // 操作类型精确查询
        if (queryParams.containsKey("operationType") && StringUtils.hasText(queryParams.get("operationType").toString())) {
            String operationType = queryParams.get("operationType").toString();
            Assert.isTrue(VALID_OPERATION_TYPES.contains(operationType), "无效操作类型：" + operationType);
            wrapper.eq("operation_type", operationType);
        }

        // 操作人ID精确查询
        if (queryParams.containsKey("operatorId") && queryParams.get("operatorId") != null) {
            wrapper.eq("operator_id", queryParams.get("operatorId"));
        }

        // 操作人姓名模糊查询
        if (queryParams.containsKey("operatorName") && StringUtils.hasText(queryParams.get("operatorName").toString())) {
            wrapper.like("operator_name", queryParams.get("operatorName"));
        }

        // IP地址精确查询
        if (queryParams.containsKey("ipAddress") && StringUtils.hasText(queryParams.get("ipAddress").toString())) {
            wrapper.eq("ip_address", queryParams.get("ipAddress"));
        }

        // 操作时间范围查询
        if (queryParams.containsKey("startCreateTime") && queryParams.get("startCreateTime") != null) {
            wrapper.ge("create_time", queryParams.get("startCreateTime"));
        }
        if (queryParams.containsKey("endCreateTime") && queryParams.get("endCreateTime") != null) {
            wrapper.le("create_time", queryParams.get("endCreateTime"));
        }

        return wrapper;
    }

    /**
     * 校验实体类基础约束（NotBlank/Pattern/Size/枚举等）
     *
     * <p>完全匹配实体类注解配置，确保数据完整性</p>
     * <p>校验内容包括：</p>
     * <ul>
     *   <li>非空字段校验（模块、操作类型、操作内容、操作人姓名、IP地址）</li>
     *   <li>字段长度校验（操作内容、操作人姓名、IP地址）</li>
     *   <li>枚举值合法性校验（操作模块、操作类型）</li>
     *   <li>格式校验（IP地址格式）</li>
     * </ul>
     *
     * @param log 待校验的操作日志实体对象
     * @throws IllegalArgumentException 当任何校验失败时抛出，包含具体的错误信息
     */
    private void validateEntityBaseConstraints(OperationLog log) {
        // 1. 非空字段校验（匹配实体类@NotBlank）
        Assert.hasLength(log.getModule(), "操作模块不能为空");
        Assert.hasLength(log.getOperationType(), "操作类型不能为空");
        Assert.hasLength(log.getOperationContent(), "操作内容不能为空");
        Assert.hasLength(log.getOperatorName(), "操作人姓名不能为空");
        Assert.hasLength(log.getIpAddress(), "操作IP地址不能为空");

        // 2. 长度校验（匹配实体类@Size）
        Assert.isTrue(log.getOperationContent().length() <= 500, "操作内容长度不能超过500字符");
        Assert.isTrue(log.getOperatorName().length() <= 50, "操作人姓名长度不能超过50字符");
        Assert.isTrue(log.getIpAddress().length() <= 50, "IP地址长度不能超过50字符");

        // 3. 枚举值校验（匹配实体类allowableValues）
        Assert.isTrue(VALID_MODULES.contains(log.getModule()),
                "无效操作模块：" + log.getModule() + "，允许值：" + VALID_MODULES);
        Assert.isTrue(VALID_OPERATION_TYPES.contains(log.getOperationType()),
                "无效操作类型：" + log.getOperationType() + "，允许值：" + VALID_OPERATION_TYPES);

        // 4. IP格式校验（匹配实体类@Pattern）
        Assert.isTrue(IP_PATTERN.matcher(log.getIpAddress()).matches(),
                "IP地址格式错误（支持IPv4/IPv6），当前值：" + log.getIpAddress());
    }

    /**
     * 校验不可修改的核心审计字段
     *
     * <p>审计日志的核心字段在创建后不允许修改，包括：</p>
     * <ul>
     *   <li>操作模块（module）</li>
     *   <li>操作类型（operationType）</li>
     *   <li>操作内容（operationContent）</li>
     *   <li>操作人ID（operatorId）</li>
     *   <li>IP地址（ipAddress）</li>
     *   <li>租户ID（tenantId）</li>
     *   <li>操作时间（createTime）</li>
     * </ul>
     *
     * @param updateLog 待更新的日志对象
     * @param existingLog 数据库中已存在的日志对象
     * @throws IllegalArgumentException 当尝试修改不可变字段时抛出
     */
    private void validateImmutableFields(OperationLog updateLog, OperationLog existingLog) {
        // 比较字段值是否被修改，而不是是否为null

        // 如果 updateLog 中设置了 module 且与 existingLog 不同，则抛出异常
        if (updateLog.getModule() != null && !updateLog.getModule().equals(existingLog.getModule())) {
            throw new IllegalArgumentException("操作模块禁止修改");
        }

        // 如果 updateLog 中设置了 operationType 且与 existingLog 不同
        if (updateLog.getOperationType() != null && !updateLog.getOperationType().equals(existingLog.getOperationType())) {
            throw new IllegalArgumentException("操作类型禁止修改");
        }

        // 如果 updateLog 中设置了 operationContent 且与 existingLog 不同
        if (updateLog.getOperationContent() != null && !updateLog.getOperationContent().equals(existingLog.getOperationContent())) {
            throw new IllegalArgumentException("操作内容禁止修改");
        }

        // 如果 updateLog 中设置了 operatorId 且与 existingLog 不同
        if (updateLog.getOperatorId() != null && !updateLog.getOperatorId().equals(existingLog.getOperatorId())) {
            throw new IllegalArgumentException("操作人ID禁止修改");
        }

        // 如果 updateLog 中设置了 ipAddress 且与 existingLog 不同
        if (updateLog.getIpAddress() != null && !updateLog.getIpAddress().equals(existingLog.getIpAddress())) {
            throw new IllegalArgumentException("操作IP地址禁止修改");
        }

        // 关键：如果 updateLog 中设置了 tenantId 且与 existingLog 不同
        if (updateLog.getTenantId() != null && !updateLog.getTenantId().equals(existingLog.getTenantId())) {
            throw new IllegalArgumentException("租户ID禁止修改");
        }

        // 如果 updateLog 中设置了 createTime 且与 existingLog 不同
        if (updateLog.getCreateTime() != null && !updateLog.getCreateTime().equals(existingLog.getCreateTime())) {
            throw new IllegalArgumentException("操作时间禁止修改");
        }
    }
}
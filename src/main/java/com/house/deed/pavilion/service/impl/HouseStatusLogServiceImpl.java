package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.HouseStatusLog;
import com.house.deed.pavilion.mapper.HouseStatusLogMapper;
import com.house.deed.pavilion.service.HouseStatusLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * 房源状态变更日志表（租户级数据） 服务实现类
 * </p>
 * <p>
 * 负责房源状态变更日志的全生命周期管理，包括状态变更记录的创建、查询、更新、删除等核心操作。
 * 房源状态变更日志是房源生命周期管理的关键组件，记录房源状态流转的完整轨迹，支持状态变更的审计追溯和分析。
 * 所有操作均强制进行租户数据隔离校验，确保数据安全性和业务完整性。
 * </p>
 * <p>
 * 核心特性：
 * 1. 租户数据隔离：所有操作必须验证租户ID，确保跨租户数据不可见
 * 2. 审计追溯保障：状态变更记录核心字段不可篡改，确保状态历史的真实性
 * 3. 业务规则校验：严格校验状态变更的合理性和有效性
 * 4. 批量操作优化：提供批量增删功能，支持事务一致性保障
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class HouseStatusLogServiceImpl extends ServiceImpl<HouseStatusLogMapper, HouseStatusLog> implements HouseStatusLogService {

    /**
     * 房源状态有效值常量定义（与实体类枚举值保持一致）
     */
    private static final String STATUS_ON_SALE = "ON_SALE";
    private static final String STATUS_RESERVED = "RESERVED";
    private static final String STATUS_SOLD = "SOLD";
    private static final String STATUS_OFF_SHELF = "OFF_SHELF";

    /**
     * 变更原因最大长度限制（与实体类@Size注解匹配）
     */
    private static final int CHANGE_REASON_MAX_LENGTH = 200;

    /**
     * 操作人姓名最大长度限制（与实体类@Size注解匹配）
     */
    private static final int OPERATOR_NAME_MAX_LENGTH = 50;

    // ==================== 基础CRUD实现 ====================

    /**
     * 新增房源状态变更日志记录
     *
     * @param log 状态变更日志实体对象，包含状态变更前后信息、原因、操作人等
     * @return boolean 新增成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败或业务规则不满足时抛出
     *
     * 执行流程：
     * 1. 实体类非空字段校验（租户ID、房源ID、变更前后状态等必填字段）
     * 2. 业务规则校验（变更前后状态不能相同）
     * 3. 状态值合法性校验（必须为预定义的合法状态值）
     * 4. 字段长度校验（变更原因、操作人姓名等字符串字段长度限制）
     * 5. 调用MyBatis-Plus保存方法持久化数据
     *
     * 业务约束：
     * 1. 变更前后状态必须不同，确保状态变更有意义
     * 2. 状态值必须为预定义的有效值（ON_SALE/RESERVED/SOLD/OFF_SHELF）
     * 3. 变更原因和操作人姓名长度需符合数据库字段定义
     * 4. 操作人信息必须完整，确保责任可追溯
     */
    @Override
    public boolean saveStatusLog(HouseStatusLog log) {
        // 1. 实体类非空字段校验（与@NotNull/@NotBlank注解匹配）
        Assert.notNull(log.getTenantId(), "租户ID不能为空");
        Assert.notNull(log.getHouseId(), "房源ID不能为空");
        Assert.hasText(log.getStatusBefore(), "变更前状态不能为空");
        Assert.hasText(log.getStatusAfter(), "变更后状态不能为空");
        Assert.hasText(log.getChangeReason(), "变更原因不能为空");
        Assert.notNull(log.getOperatorId(), "操作人ID不能为空");
        Assert.hasText(log.getOperatorName(), "操作人姓名不能为空");

        // 2. 业务规则：变更前后状态必须不同（符合实体类"状态变更记录"的核心含义）
        Assert.isTrue(!log.getStatusBefore().equals(log.getStatusAfter()), "变更前后状态不能相同");

        // 3. 状态合法性校验（与实体类枚举值匹配）
        Assert.isTrue(isValidStatus(log.getStatusBefore()),
                "变更前状态不合法（允许值：" + getValidStatusList() + "）");
        Assert.isTrue(isValidStatus(log.getStatusAfter()),
                "变更后状态不合法（允许值：" + getValidStatusList() + "）");

        // 4. 变更原因长度校验（与实体类@Size注解匹配）
        Assert.isTrue(log.getChangeReason().length() <= CHANGE_REASON_MAX_LENGTH,
                "变更原因长度不能超过" + CHANGE_REASON_MAX_LENGTH + "字符");
        Assert.isTrue(log.getOperatorName().length() <= OPERATOR_NAME_MAX_LENGTH,
                "操作人姓名长度不能超过" + OPERATOR_NAME_MAX_LENGTH + "字符");

        // 5. 保存数据
        return save(log);
    }

    /**
     * 更新房源状态变更日志记录
     *
     * @param log 更新后的状态变更日志实体对象，必须包含ID和租户ID
     * @return boolean 更新成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败时抛出
     * @throws IllegalStateException 当记录不存在或权限不足时抛出
     *
     * 执行流程：
     * 1. 基础参数校验（日志ID和租户ID不能为空）
     * 2. 记录存在性及租户归属校验
     * 3. 保护核心审计字段不被篡改，确保状态历史的不可篡改性
     * 4. 执行数据库更新操作
     *
     * 更新限制：
     * 1. 核心审计字段（变更前后状态、房源ID、操作人ID、创建时间）不允许修改
     * 2. 仅允许更新非核心信息，如变更原因的补充说明等
     * 3. 租户ID不可变更，确保数据归属一致性
     *
     * 审计原则：状态变更记录一旦创建，核心审计信息不可更改
     */
    @Override
    public boolean updateStatusLogById(HouseStatusLog log) {
        // 1. 基础参数校验
        Assert.notNull(log.getId(), "日志ID不能为空");
        Assert.notNull(log.getTenantId(), "租户ID不能为空");

        // 2. 校验记录存在且归属当前租户
        HouseStatusLog exist = getById(log.getId());
        Assert.notNull(exist, "状态变更记录不存在");
        Assert.isTrue(Objects.equals(exist.getTenantId(), log.getTenantId()),
                "无权限操作其他租户的记录");

        // 3. 禁止修改核心审计字段（与实体类"状态追溯"设计目标一致）
        log.setStatusBefore(exist.getStatusBefore());   // 变更前状态不可更改
        log.setStatusAfter(exist.getStatusAfter());     // 变更后状态不可更改
        log.setHouseId(exist.getHouseId());             // 关联房源不可更改
        log.setOperatorId(exist.getOperatorId());       // 操作人ID不可更改
        log.setCreateTime(exist.getCreateTime());       // 创建时间不可更改（自动填充字段）
        log.setTenantId(exist.getTenantId());           // 租户ID不可变更

        return updateById(log);
    }

    /**
     * 删除房源状态变更日志记录
     *
     * @param id 状态变更日志记录的唯一标识
     * @param tenantId 租户ID，用于权限校验
     * @return boolean 删除成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数为空时抛出
     * @throws IllegalStateException 当记录不存在或权限不足时抛出
     *
     * 执行流程：
     * 1. 参数非空校验
     * 2. 记录存在性及租户归属校验
     * 3. 执行物理删除操作
     *
     * 注意事项：
     * 1. 状态变更日志通常用于审计目的，建议谨慎删除
     * 2. 删除操作不可逆，建议先确认业务影响
     * 3. 建议记录删除操作人和删除原因用于审计
     */
    @Override
    public boolean removeStatusLogById(Long id, Long tenantId) {
        // 1. 参数校验
        Assert.notNull(id, "日志ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 2. 校验租户归属
        HouseStatusLog exist = getById(id);
        Assert.notNull(exist, "状态变更记录不存在");
        Assert.isTrue(Objects.equals(exist.getTenantId(), tenantId),
                "无权限操作其他租户的记录");

        // 3. 执行删除操作
        return removeById(id);
    }

    /**
     * 按ID查询状态变更日志记录（租户隔离）
     *
     * @param id 状态变更日志记录的唯一标识
     * @param tenantId 租户ID，用于数据隔离
     * @return HouseStatusLog 状态变更日志实体对象，不存在时返回null
     * @throws IllegalArgumentException 当参数为空时抛出
     *
     * 说明：
     * 1. 强制添加租户ID查询条件，确保租户数据隔离
     * 2. 返回包含记录所有字段的完整信息
     * 3. 主要用于状态变更详情查看和审计追溯
     */
    @Override
    public HouseStatusLog getStatusLogById(Long id, Long tenantId) {
        // 参数校验
        Assert.notNull(id, "日志ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        return getOne(new LambdaQueryWrapper<HouseStatusLog>()
                .eq(HouseStatusLog::getId, id)
                .eq(HouseStatusLog::getTenantId, tenantId));
    }

    // ==================== 多条件查询实现 ====================

    /**
     * 分页查询状态变更日志记录
     *
     * @param page 分页参数对象，包含页码、每页大小等信息
     * @param queryParams 查询参数Map，支持动态条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return IPage<HouseStatusLog> 分页结果对象，包含数据列表和分页信息
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 支持的查询条件：
     * 1. houseId: 房源ID（精确匹配，对应实体类house_id字段）
     * 2. operatorId: 操作人ID（精确匹配，对应实体类operator_id字段）
     * 3. statusBefore: 变更前状态（精确匹配，对应实体类status_before字段）
     * 4. statusAfter: 变更后状态（精确匹配，对应实体类status_after字段）
     * 5. startTime/endTime: 变更时间范围查询（对应实体类create_time字段）
     * 6. changeReason: 变更原因模糊查询（对应实体类change_reason字段）
     *
     * 默认排序：按变更时间倒序排列（最新变更记录在前）
     */
    @Override
    public IPage<HouseStatusLog> pageQuery(Page<HouseStatusLog> page, Map<String, Object> queryParams, Long tenantId) {
        // 租户ID必填校验
        Assert.notNull(tenantId, "租户ID不能为空");

        // 构建查询条件
        LambdaQueryWrapper<HouseStatusLog> wrapper = buildQueryWrapper(queryParams, tenantId);
        return baseMapper.selectPage(page, wrapper);
    }

    /**
     * 多条件查询状态变更日志列表（租户隔离）
     *
     * @param queryParams 查询参数Map，支持动态条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return List<HouseStatusLog> 符合条件的状态变更日志列表
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 说明：
     * 1. 此方法与分页查询使用相同的查询逻辑，但不进行分页处理
     * 2. 适用于需要获取所有匹配记录的场景
     * 3. 按变更时间倒序排列，最新记录在前
     */
    @Override
    public List<HouseStatusLog> listByConditions(Map<String, Object> queryParams, Long tenantId) {
        // 租户ID必填校验
        Assert.notNull(tenantId, "租户ID不能为空");

        // 构建查询条件
        LambdaQueryWrapper<HouseStatusLog> wrapper = buildQueryWrapper(queryParams, tenantId);
        return baseMapper.selectList(wrapper);
    }

    /**
     * 按房源ID查询状态变更日志历史
     *
     * @param houseId 房源ID
     * @param tenantId 租户ID，用于数据隔离
     * @return List<HouseStatusLog> 该房源的所有状态变更日志列表，按变更时间倒序排列
     * @throws IllegalArgumentException 当房源ID或租户ID为空时抛出
     *
     * 业务用途：
     * 1. 查看房源的状态变更完整历史
     * 2. 分析房源的状态流转路径和规律
     * 3. 状态变更的审计追溯和问题排查
     *
     * 返回说明：
     * 1. 返回列表包含该房源的所有状态变更记录
     * 2. 按变更时间倒序排列，最新变更在前
     * 3. 每条记录包含完整的变更信息和操作人信息
     */
    @Override
    public List<HouseStatusLog> listByHouseId(Long houseId, Long tenantId) {
        // 参数校验
        Assert.notNull(houseId, "房源ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        return baseMapper.selectList(new LambdaQueryWrapper<HouseStatusLog>()
                .eq(HouseStatusLog::getHouseId, houseId)
                .eq(HouseStatusLog::getTenantId, tenantId)
                .orderByDesc(HouseStatusLog::getCreateTime)); // 最新变更在前
    }

    // ==================== 批量操作实现 ====================

    /**
     * 批量保存状态变更日志记录
     *
     * @param logList 状态变更日志记录列表
     * @return boolean 批量新增成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数为空或记录格式无效时抛出
     *
     * 执行流程：
     * 1. 列表非空校验
     * 2. 逐条记录基础参数校验
     * 3. 逐条记录业务规则校验
     * 4. 逐条记录状态值合法性校验
     * 5. 逐条记录字段长度校验
     * 6. 批量保存到数据库
     *
     * 使用场景：
     * 1. 批量导入历史状态变更数据
     * 2. 批量处理多个房源的状态变更记录
     * 3. 数据迁移时的批量创建
     */
    @Override
    public boolean batchSaveStatusLogs(List<HouseStatusLog> logList) {
        // 列表非空校验
        Assert.notEmpty(logList, "批量保存的记录列表不能为空");

        // 逐条校验（与实体类约束一致）
        for (HouseStatusLog log : logList) {
            // 基础参数校验
            Assert.notNull(log.getTenantId(), "租户ID不能为空");
            Assert.notNull(log.getHouseId(), "房源ID不能为空");
            Assert.hasText(log.getStatusBefore(), "变更前状态不能为空");
            Assert.hasText(log.getStatusAfter(), "变更后状态不能为空");
            Assert.hasText(log.getChangeReason(), "变更原因不能为空");
            Assert.notNull(log.getOperatorId(), "操作人ID不能为空");
            Assert.hasText(log.getOperatorName(), "操作人姓名不能为空");

            // 业务规则校验
            Assert.isTrue(!log.getStatusBefore().equals(log.getStatusAfter()), "变更前后状态不能相同");

            // 状态值合法性校验
            Assert.isTrue(isValidStatus(log.getStatusBefore()) && isValidStatus(log.getStatusAfter()),
                    "状态值不合法（允许值：" + getValidStatusList() + "）");

            // 字段长度校验
            Assert.isTrue(log.getChangeReason().length() <= CHANGE_REASON_MAX_LENGTH,
                    "变更原因长度超限（最大" + CHANGE_REASON_MAX_LENGTH + "字符）");
            Assert.isTrue(log.getOperatorName().length() <= OPERATOR_NAME_MAX_LENGTH,
                    "操作人姓名长度超限（最大" + OPERATOR_NAME_MAX_LENGTH + "字符）");
        }

        return saveBatch(logList);
    }

    /**
     * 批量删除状态变更日志记录
     *
     * @param ids 状态变更日志记录ID列表
     * @param tenantId 租户ID，用于权限校验
     * @return boolean 批量删除成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数为空或存在跨租户记录时抛出
     *
     * 执行流程：
     * 1. 参数非空校验
     * 2. 跨租户记录校验（防止越权删除）
     * 3. 执行批量删除操作
     *
     * 安全机制：
     * 1. 强制租户ID校验，确保只能删除自己租户的数据
     * 2. 批量操作前验证所有记录归属，防止部分成功部分失败
     */
    @Override
    public boolean batchRemoveStatusLogs(List<Long> ids, Long tenantId) {
        // 参数校验
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.notEmpty(ids, "批量删除的ID列表不能为空");

        // 校验所有记录的租户归属
        long invalidCount = baseMapper.selectCount(new LambdaQueryWrapper<HouseStatusLog>()
                .in(HouseStatusLog::getId, ids)
                .ne(HouseStatusLog::getTenantId, tenantId));
        Assert.isTrue(invalidCount == 0, "存在不属于当前租户的记录，无法批量删除");

        return removeByIds(ids);
    }

    // ==================== 内部工具方法 ====================

    /**
     * 构建多条件查询封装器
     *
     * @param queryParams 查询参数Map，支持动态条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return LambdaQueryWrapper<HouseStatusLog> 查询条件封装器
     *
     * 支持的查询条件：
     * 1. houseId: 房源ID（精确匹配，对应实体类house_id字段）
     * 2. operatorId: 操作人ID（精确匹配，对应实体类operator_id字段）
     * 3. statusBefore: 变更前状态（精确匹配，对应实体类status_before字段）
     * 4. statusAfter: 变更后状态（精确匹配，对应实体类status_after字段）
     * 5. startTime/endTime: 变更时间范围查询（对应实体类create_time字段）
     * 6. changeReason: 变更原因模糊查询（对应实体类change_reason字段）
     *
     * 默认排序：按变更时间倒序排列（最新记录在前）
     *
     * 说明：查询条件与实体类字段一一对应，确保查询的准确性和一致性
     */
    private LambdaQueryWrapper<HouseStatusLog> buildQueryWrapper(Map<String, Object> queryParams, Long tenantId) {
        // 强制添加租户隔离条件
        LambdaQueryWrapper<HouseStatusLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HouseStatusLog::getTenantId, tenantId);

        // 如果查询参数为空，直接返回基本查询条件
        if (ObjectUtils.isEmpty(queryParams)) {
            wrapper.orderByDesc(HouseStatusLog::getCreateTime);
            return wrapper;
        }

        // 房源ID查询（匹配实体类house_id）
        if (queryParams.containsKey("houseId") && queryParams.get("houseId") != null) {
            wrapper.eq(HouseStatusLog::getHouseId, queryParams.get("houseId"));
        }

        // 操作人ID查询（匹配实体类operator_id）
        if (queryParams.containsKey("operatorId") && queryParams.get("operatorId") != null) {
            wrapper.eq(HouseStatusLog::getOperatorId, queryParams.get("operatorId"));
        }

        // 状态条件查询（匹配实体类status_before/status_after）
        if (queryParams.containsKey("statusBefore") && StringUtils.hasText(queryParams.get("statusBefore").toString())) {
            wrapper.eq(HouseStatusLog::getStatusBefore, queryParams.get("statusBefore"));
        }
        if (queryParams.containsKey("statusAfter") && StringUtils.hasText(queryParams.get("statusAfter").toString())) {
            wrapper.eq(HouseStatusLog::getStatusAfter, queryParams.get("statusAfter"));
        }

        // 时间范围查询（匹配实体类create_time）
        if (queryParams.containsKey("startTime") && queryParams.get("startTime") != null) {
            wrapper.ge(HouseStatusLog::getCreateTime, queryParams.get("startTime"));
        }
        if (queryParams.containsKey("endTime") && queryParams.get("endTime") != null) {
            wrapper.le(HouseStatusLog::getCreateTime, queryParams.get("endTime"));
        }

        // 变更原因模糊查询（匹配实体类change_reason）
        if (queryParams.containsKey("changeReason") && StringUtils.hasText(queryParams.get("changeReason").toString())) {
            wrapper.like(HouseStatusLog::getChangeReason, queryParams.get("changeReason"));
        }

        // 按变更时间倒序（最新记录在前）
        wrapper.orderByDesc(HouseStatusLog::getCreateTime);

        return wrapper;
    }

    /**
     * 校验状态值合法性（与实体类枚举值匹配）
     *
     * @param status 待校验的状态值
     * @return boolean 状态值合法返回true，否则返回false
     *
     * 说明：此方法确保状态值符合预定义的业务规则，防止非法状态值进入系统
     */
    private boolean isValidStatus(String status) {
        return STATUS_ON_SALE.equals(status)
                || STATUS_RESERVED.equals(status)
                || STATUS_SOLD.equals(status)
                || STATUS_OFF_SHELF.equals(status);
    }

    /**
     * 获取有效的状态值列表（用于错误提示信息）
     *
     * @return String 逗号分隔的有效状态值列表
     */
    private String getValidStatusList() {
        return String.join(", ", STATUS_ON_SALE, STATUS_RESERVED, STATUS_SOLD, STATUS_OFF_SHELF);
    }
}
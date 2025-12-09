package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.HousePriceLog;
import com.house.deed.pavilion.mapper.HousePriceLogMapper;
import com.house.deed.pavilion.service.HousePriceLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * 房源价格变动记录表（租户级数据） 服务实现类
 * </p>
 * <p>
 * 负责房源价格变动记录的全生命周期管理，包括价格变动记录的创建、查询、更新、删除等核心操作。
 * 房源价格变动记录是房源价格审计和追溯的关键数据，记录每次价格调整的详细信息，确保价格变动的透明度和可追溯性。
 * 所有操作均强制进行租户数据隔离校验，确保数据安全性和业务完整性。
 * </p>
 * <p>
 * 核心特性：
 * 1. 租户数据隔离：所有操作必须验证租户ID，确保跨租户数据不可见
 * 2. 审计追溯保障：价格变动记录不可篡改，确保价格调整历史的真实性
 * 3. 业务规则校验：严格校验价格调整的合理性和有效性
 * 4. 批量操作优化：提供批量增删功能，支持事务一致性保障
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class HousePriceLogServiceImpl extends ServiceImpl<HousePriceLogMapper, HousePriceLog> implements HousePriceLogService {

    // ==================== 基础CRUD实现 ====================

    /**
     * 新增房源价格变动记录
     *
     * @param log 价格变动记录实体对象，包含价格调整前后信息、原因、操作人等
     * @param tenantId 租户ID，用于数据隔离
     * @return boolean 新增成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败或业务规则不满足时抛出
     *
     * 执行流程：
     * 1. 基础参数校验（租户ID、房源ID、价格、原因、操作人等必填字段）
     * 2. 业务规则校验（调整前后价格不能相同，价格不能为负数）
     * 3. 强制绑定租户ID，确保数据归属正确
     * 4. 调用MyBatis-Plus保存方法持久化数据
     *
     * 业务约束：
     * 1. 调整前后价格必须不同，确保价格变动有意义
     * 2. 价格不能为负数，符合业务逻辑
     * 3. 调价原因必须明确，便于后续追溯
     * 4. 操作人信息必须完整，确保责任可追溯
     */
    @Override
    public boolean savePriceLog(HousePriceLog log, Long tenantId) {
        // 1. 参数校验（与实体类约束一致）
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.notNull(log.getHouseId(), "房源ID不能为空");
        Assert.notNull(log.getPriceBefore(), "调整前价格不能为空");
        Assert.notNull(log.getPriceAfter(), "调整后价格不能为空");
        Assert.hasText(log.getChangeReason(), "调价原因不能为空");
        Assert.notNull(log.getOperatorId(), "操作人ID不能为空");
        Assert.hasText(log.getOperatorName(), "操作人姓名不能为空");

        // 2. 业务规则校验
        // 调整前后价格必须不同（实体类要求价格变动有意义）
        Assert.isTrue(!log.getPriceBefore().equals(log.getPriceAfter()), "调整前后价格不能相同");
        // 价格非负校验（补充实体类注解校验，服务层双重保障）
        Assert.isTrue(log.getPriceBefore().compareTo(BigDecimal.ZERO) >= 0, "调整前价格不能为负数");
        Assert.isTrue(log.getPriceAfter().compareTo(BigDecimal.ZERO) >= 0, "调整后价格不能为负数");

        // 3. 强制绑定租户ID（实体类tenant_id为核心隔离字段）
        log.setTenantId(tenantId);

        // 4. 保存数据
        return save(log);
    }

    /**
     * 更新价格变动记录信息
     *
     * @param log 更新后的价格变动记录实体对象，必须包含ID和租户ID
     * @param tenantId 租户ID，用于权限校验
     * @return boolean 更新成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败时抛出
     * @throws IllegalStateException 当记录不存在或权限不足时抛出
     *
     * 执行流程：
     * 1. 基础参数校验（记录ID和租户ID不能为空）
     * 2. 记录存在性及租户归属校验
     * 3. 禁止修改核心审计字段，确保价格历史的不可篡改性
     * 4. 执行数据库更新操作
     *
     * 更新限制：
     * 1. 核心审计字段（调整前后价格、房源ID、操作人ID、创建时间）不允许修改
     * 2. 仅允许修改非核心字段，如备注信息
     * 3. 不支持跨租户迁移价格变动记录
     *
     * 审计原则：价格变动记录一旦创建，核心审计信息不可更改
     */
    @Override
    public boolean updatePriceLogById(HousePriceLog log, Long tenantId) {
        // 1. 基础参数校验
        Assert.notNull(log.getId(), "记录ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 2. 校验记录存在且归属当前租户
        HousePriceLog exist = getById(log.getId());
        Assert.notNull(exist, "价格变动记录不存在");
        Assert.isTrue(Objects.equals(exist.getTenantId(), tenantId), "无权限操作其他租户的记录");

        // 3. 禁止修改核心审计字段（价格追溯依据，不允许篡改）
        log.setPriceBefore(exist.getPriceBefore());   // 调整前价格不可更改
        log.setPriceAfter(exist.getPriceAfter());     // 调整后价格不可更改
        log.setHouseId(exist.getHouseId());           // 关联房源不可更改
        log.setOperatorId(exist.getOperatorId());     // 操作人ID不可更改
        log.setCreateTime(exist.getCreateTime());     // 创建时间不可更改（自动填充字段）
        log.setTenantId(tenantId);                    // 保持租户ID一致

        return updateById(log);
    }

    /**
     * 删除价格变动记录
     *
     * @param id 价格变动记录的唯一标识
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
     * 1. 价格变动记录通常用于审计目的，建议谨慎删除
     * 2. 删除操作不可逆，建议先确认业务影响
     * 3. 建议记录删除操作人和删除原因用于审计
     */
    @Override
    public boolean removePriceLogById(Long id, Long tenantId) {
        // 1. 参数校验
        Assert.notNull(id, "记录ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 2. 校验租户归属
        HousePriceLog exist = getById(id);
        Assert.notNull(exist, "价格变动记录不存在");
        Assert.isTrue(Objects.equals(exist.getTenantId(), tenantId), "无权限操作其他租户的记录");

        // 3. 执行删除操作
        return removeById(id);
    }

    /**
     * 按ID查询价格变动记录（租户隔离）
     *
     * @param id 价格变动记录的唯一标识
     * @param tenantId 租户ID，用于数据隔离
     * @return HousePriceLog 价格变动记录实体对象，不存在时返回null
     * @throws IllegalArgumentException 当参数为空时抛出
     *
     * 说明：
     * 1. 强制添加租户ID查询条件，确保租户数据隔离
     * 2. 返回包含记录所有字段的完整信息
     * 3. 主要用于记录详情查看和审计追溯
     */
    @Override
    public HousePriceLog getPriceLogById(Long id, Long tenantId) {
        // 参数校验
        Assert.notNull(id, "记录ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 租户隔离查询（匹配实体类tenant_id字段）
        return getOne(new LambdaQueryWrapper<HousePriceLog>()
                .eq(HousePriceLog::getId, id)
                .eq(HousePriceLog::getTenantId, tenantId));
    }

    // ==================== 多条件查询实现 ====================

    /**
     * 分页查询价格变动记录
     *
     * @param page 分页参数对象，包含页码、每页大小等信息
     * @param queryParams 查询参数Map，支持动态条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return IPage<HousePriceLog> 分页结果对象，包含数据列表和分页信息
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 支持的查询条件：
     * 1. houseId: 房源ID（精确匹配）
     * 2. operatorId: 操作人ID（精确匹配）
     * 3. minPriceAfter/maxPriceAfter: 调整后价格范围查询
     * 4. startTime/endTime: 调价时间范围查询
     * 5. changeReason: 调价原因（模糊匹配）
     *
     * 默认排序：按调价时间倒序排列（最新调价记录在前）
     */
    @Override
    public IPage<HousePriceLog> pageQuery(Page<HousePriceLog> page, Map<String, Object> queryParams, Long tenantId) {
        // 租户ID必填校验
        Assert.notNull(tenantId, "租户ID不能为空");

        // 构建查询条件
        LambdaQueryWrapper<HousePriceLog> wrapper = buildQueryWrapper(queryParams, tenantId);
        return baseMapper.selectPage(page, wrapper);
    }

    /**
     * 多条件查询价格变动记录列表（租户隔离）
     *
     * @param queryParams 查询参数Map，支持动态条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return List<HousePriceLog> 符合条件的价格变动记录列表
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 说明：
     * 1. 此方法与分页查询使用相同的查询逻辑，但不进行分页处理
     * 2. 适用于需要获取所有匹配记录的场景
     * 3. 按调价时间倒序排列，最新记录在前
     */
    @Override
    public List<HousePriceLog> listByConditions(Map<String, Object> queryParams, Long tenantId) {
        // 租户ID必填校验
        Assert.notNull(tenantId, "租户ID不能为空");

        // 构建查询条件
        LambdaQueryWrapper<HousePriceLog> wrapper = buildQueryWrapper(queryParams, tenantId);
        return baseMapper.selectList(wrapper);
    }

    /**
     * 按房源ID查询价格变动记录历史
     *
     * @param houseId 房源ID
     * @param tenantId 租户ID，用于数据隔离
     * @return List<HousePriceLog> 该房源的所有价格变动记录列表，按调价时间倒序排列
     * @throws IllegalArgumentException 当房源ID或租户ID为空时抛出
     *
     * 业务用途：
     * 1. 查看房源的价格变动历史
     * 2. 分析房源的价格趋势
     * 3. 价格调整的审计和追溯
     *
     * 返回说明：
     * 1. 返回列表包含该房源的所有价格变动记录
     * 2. 按调价时间倒序排列，最新调价在前
     * 3. 每条记录包含完整的调价信息和操作人信息
     */
    @Override
    public List<HousePriceLog> listByHouseId(Long houseId, Long tenantId) {
        // 参数校验
        Assert.notNull(houseId, "房源ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 按房源ID查询（匹配实体类house_id字段）
        return baseMapper.selectList(new LambdaQueryWrapper<HousePriceLog>()
                .eq(HousePriceLog::getHouseId, houseId)
                .eq(HousePriceLog::getTenantId, tenantId)
                .orderByDesc(HousePriceLog::getCreateTime)); // 按调价时间倒序（实体类createTime为自动填充时间）
    }

    // ==================== 批量操作实现 ====================

    /**
     * 批量新增价格变动记录
     *
     * @param logList 价格变动记录列表
     * @param tenantId 租户ID，用于数据隔离
     * @return boolean 批量新增成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数为空或记录格式无效时抛出
     *
     * 执行流程：
     * 1. 参数非空校验
     * 2. 逐条记录基础参数校验
     * 3. 逐条记录业务规则校验
     * 4. 强制绑定租户ID，确保数据归属正确
     * 5. 批量保存到数据库
     *
     * 使用场景：
     * 1. 批量导入历史价格变动数据
     * 2. 批量处理多个房源的价格调整记录
     * 3. 数据迁移时的批量创建
     */
    @Override
    public boolean batchSavePriceLogs(List<HousePriceLog> logList, Long tenantId) {
        // 参数校验
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.notEmpty(logList, "批量保存的记录列表不能为空");

        // 批量校验（与实体类字段一一对应）
        for (HousePriceLog log : logList) {
            // 基础参数校验
            Assert.notNull(log.getHouseId(), "房源ID不能为空");
            Assert.notNull(log.getPriceBefore(), "调整前价格不能为空");
            Assert.notNull(log.getPriceAfter(), "调整后价格不能为空");
            Assert.hasText(log.getChangeReason(), "调价原因不能为空");
            Assert.notNull(log.getOperatorId(), "操作人ID不能为空");
            Assert.hasText(log.getOperatorName(), "操作人姓名不能为空");

            // 业务规则校验
            Assert.isTrue(!log.getPriceBefore().equals(log.getPriceAfter()), "调整前后价格不能相同");
            Assert.isTrue(log.getPriceBefore().compareTo(BigDecimal.ZERO) >= 0, "调整前价格不能为负数");
            Assert.isTrue(log.getPriceAfter().compareTo(BigDecimal.ZERO) >= 0, "调整后价格不能为负数");

            // 强制绑定租户ID
            log.setTenantId(tenantId);
        }

        return saveBatch(logList);
    }

    /**
     * 批量删除价格变动记录
     *
     * @param ids 价格变动记录ID列表
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
    public boolean batchRemovePriceLogs(List<Long> ids, Long tenantId) {
        // 参数校验
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.notEmpty(ids, "批量删除的ID列表不能为空");

        // 校验所有记录的租户归属
        long count = baseMapper.selectCount(new LambdaQueryWrapper<HousePriceLog>()
                .in(HousePriceLog::getId, ids)
                .ne(HousePriceLog::getTenantId, tenantId));
        Assert.isTrue(count == 0, "存在不属于当前租户的记录，无法批量删除");

        return removeByIds(ids);
    }

    // ==================== 内部工具方法 ====================

    /**
     * 构建多条件查询封装器
     *
     * @param queryParams 查询参数Map，支持动态条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return LambdaQueryWrapper<HousePriceLog> 查询条件封装器
     *
     * 支持的查询条件：
     * 1. houseId: 房源ID（精确匹配，对应实体类house_id字段）
     * 2. operatorId: 操作人ID（精确匹配，对应实体类operator_id字段）
     * 3. minPriceAfter/maxPriceAfter: 调整后价格范围查询（对应实体类price_after字段）
     * 4. startTime/endTime: 调价时间范围查询（对应实体类create_time字段）
     * 5. changeReason: 调价原因模糊查询（对应实体类change_reason字段）
     *
     * 默认排序：按调价时间倒序排列（最新记录在前）
     *
     * 说明：查询条件与实体类字段一一对应，确保查询的准确性和一致性
     */
    private LambdaQueryWrapper<HousePriceLog> buildQueryWrapper(Map<String, Object> queryParams, Long tenantId) {
        // 强制添加租户隔离条件
        LambdaQueryWrapper<HousePriceLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HousePriceLog::getTenantId, tenantId);

        // 如果查询参数为空，直接返回基本查询条件
        if (ObjectUtils.isEmpty(queryParams)) {
            wrapper.orderByDesc(HousePriceLog::getCreateTime);
            return wrapper;
        }

        // 房源ID查询（匹配实体类house_id）
        if (queryParams.containsKey("houseId") && queryParams.get("houseId") != null) {
            wrapper.eq(HousePriceLog::getHouseId, queryParams.get("houseId"));
        }

        // 操作人ID查询（匹配实体类operator_id）
        if (queryParams.containsKey("operatorId") && queryParams.get("operatorId") != null) {
            wrapper.eq(HousePriceLog::getOperatorId, queryParams.get("operatorId"));
        }

        // 价格范围查询（匹配实体类price_after）
        if (queryParams.containsKey("minPriceAfter") && queryParams.get("minPriceAfter") != null) {
            wrapper.ge(HousePriceLog::getPriceAfter, new BigDecimal(queryParams.get("minPriceAfter").toString()));
        }
        if (queryParams.containsKey("maxPriceAfter") && queryParams.get("maxPriceAfter") != null) {
            wrapper.le(HousePriceLog::getPriceAfter, new BigDecimal(queryParams.get("maxPriceAfter").toString()));
        }

        // 调价时间范围查询（匹配实体类create_time）
        if (queryParams.containsKey("startTime") && queryParams.get("startTime") != null) {
            wrapper.ge(HousePriceLog::getCreateTime, queryParams.get("startTime"));
        }
        if (queryParams.containsKey("endTime") && queryParams.get("endTime") != null) {
            wrapper.le(HousePriceLog::getCreateTime, queryParams.get("endTime"));
        }

        // 调价原因模糊查询（匹配实体类change_reason）
        if (queryParams.containsKey("changeReason") && StringUtils.hasText(queryParams.get("changeReason").toString())) {
            wrapper.like(HousePriceLog::getChangeReason, queryParams.get("changeReason"));
        }

        // 按调价时间倒序（最新记录在前）
        wrapper.orderByDesc(HousePriceLog::getCreateTime);

        return wrapper;
    }
}
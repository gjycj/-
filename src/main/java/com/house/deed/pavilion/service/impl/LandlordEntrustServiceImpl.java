package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.LandlordEntrust;
import com.house.deed.pavilion.mapper.LandlordEntrustMapper;
import com.house.deed.pavilion.service.LandlordEntrustService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * 房东委托信息表（租户级数据） 服务实现类
 * </p>
 * <p>
 * 负责房东委托信息的全生命周期管理，包括委托关系的创建、查询、更新、删除等核心操作。
 * 房东委托信息是房东与房屋管理服务之间的核心约定，记录委托授权、服务范围、佣金标准等关键信息。
 * 所有操作均强制进行租户数据隔离校验，确保数据安全性和业务完整性。
 * </p>
 * <p>
 * 核心特性：
 * 1. 租户数据隔离：所有操作必须验证租户ID，确保跨租户数据不可见
 * 2. 委托冲突校验：同一房源在同一时间只能存在一条有效委托，防止委托冲突
 * 3. 多维度查询：支持按房东、房屋、委托类型、状态等多维度查询委托信息
 * 4. 批量操作优化：提供批量增删改功能，支持事务一致性保障
 * 5. 状态管理：支持委托状态（有效/过期取消）的流转和批量更新
 * </p>
 * <p>
 * 业务规则：
 * 1. 同一房源在同一时间段内不能存在多条有效委托记录
 * 2. 委托开始时间不能晚于结束时间
 * 3. 委托状态只能为0（过期/取消）或1（有效）
 * 4. 到期提醒标志只能为0（不提醒）或1（提醒）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class LandlordEntrustServiceImpl extends ServiceImpl<LandlordEntrustMapper, LandlordEntrust> implements LandlordEntrustService {

    /**
     * 委托状态常量定义
     */
    private static final byte STATUS_INACTIVE = 0; // 过期/取消状态
    private static final byte STATUS_ACTIVE = 1;   // 有效状态

    /**
     * 到期提醒常量定义
     */
    private static final byte RENEW_REMIND_NO = 0;  // 不提醒
    private static final byte RENEW_REMIND_YES = 1; // 提醒

    @Resource
    private LandlordEntrustMapper landlordEntrustMapper;

    // ==================== 基础CRUD方法 ====================

    /**
     * 新增房东委托记录
     *
     * @param entity 委托实体对象，包含委托的所有相关信息
     * @return boolean 新增成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败或业务规则不满足时抛出
     *
     * 执行流程：
     * 1. 委托实体基础参数校验（必填字段、时间逻辑等）
     * 2. 校验同一房源是否存在时间重叠的有效委托（委托冲突校验）
     * 3. 调用MyBatis-Plus保存方法持久化数据
     *
     * 业务约束：
     * 1. 同一房源在同一时间段内不能存在多条有效委托记录
     * 2. 委托开始时间不能晚于结束时间
     * 3. 到期提醒标志必须为0或1
     *
     * 事务保障：使用@Transactional确保数据一致性，异常时回滚
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveEntrust(LandlordEntrust entity) {
        // 1. 基础参数校验（实体类注解校验+业务校验）
        validateEntrustEntity(entity, true);

        // 2. 校验同一房源是否存在有效委托（时间重叠校验）
        validateHouseEntrustConflict(entity);

        // 3. 保存数据
        return save(entity);
    }

    /**
     * 根据ID查询委托记录（租户隔离）
     *
     * @param id 委托记录的唯一标识
     * @param tenantId 租户ID，用于数据隔离
     * @return LandlordEntrust 委托实体对象，不存在时返回null
     * @throws IllegalArgumentException 当ID或租户ID为空时抛出
     *
     * 说明：
     * 1. 强制添加租户ID查询条件，确保租户数据隔离
     * 2. 返回包含委托记录所有字段的完整信息
     * 3. 用于委托详情查看、合同打印和修改前数据加载
     */
    @Override
    public LandlordEntrust getById(Long id, Long tenantId) {
        // 参数校验
        Assert.notNull(id, "委托ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 构建查询条件（ID + 租户ID双重验证）
        QueryWrapper<LandlordEntrust> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id)
                .eq("tenant_id", tenantId);
        return getOne(wrapper);
    }

    /**
     * 更新委托记录（租户隔离校验）
     *
     * @param entity 更新后的委托实体对象，必须包含ID和租户ID
     * @return boolean 更新成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败时抛出
     * @throws IllegalStateException 当记录不存在或权限不足时抛出
     *
     * 执行流程：
     * 1. 基础参数校验（委托ID和租户ID不能为空）
     * 2. 记录存在性及租户归属校验
     * 3. 业务参数校验（保持数据完整性）
     * 4. 若修改房源或时间范围，重新校验委托冲突
     * 5. 执行数据库更新操作
     *
     * 更新限制：
     * 1. 已生效的委托记录核心信息修改需谨慎
     * 2. 修改委托时间或房源时需重新校验冲突
     * 3. 不支持跨租户迁移委托记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateEntrust(LandlordEntrust entity) {
        // 1. 基础参数校验
        Assert.notNull(entity.getId(), "委托ID不能为空");
        Assert.notNull(entity.getTenantId(), "租户ID不能为空");

        // 2. 校验数据归属
        LandlordEntrust exist = getById(entity.getId(), entity.getTenantId());
        Assert.notNull(exist, "委托记录不存在或不属于当前租户");

        // 3. 业务参数校验
        validateEntrustEntity(entity, false);

        // 4. 若修改房源或时间范围，需再次校验冲突
        if (!Objects.equals(exist.getHouseId(), entity.getHouseId())
                || !Objects.equals(exist.getEntrustStartTime(), entity.getEntrustStartTime())
                || !Objects.equals(exist.getEntrustEndTime(), entity.getEntrustEndTime())) {
            validateHouseEntrustConflict(entity);
        }

        return updateById(entity);
    }

    /**
     * 删除委托记录（租户隔离）
     *
     * @param id 委托记录的唯一标识
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
     * 删除策略：
     * 1. 已生效的委托记录不建议直接删除，建议使用状态更新为过期/取消
     * 2. 删除前需确认无关联业务数据
     * 3. 建议记录删除操作人和删除原因用于审计
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeEntrust(Long id, Long tenantId) {
        // 1. 参数校验
        Assert.notNull(id, "委托ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 2. 校验数据归属
        LandlordEntrust exist = getById(id, tenantId);
        Assert.notNull(exist, "委托记录不存在或不属于当前租户");

        // 3. 执行删除操作
        return removeById(id);
    }

    // ==================== 多条件查询方法 ====================

    /**
     * 多条件分页查询委托记录
     *
     * @param page 分页参数对象，包含页码、每页大小等信息
     * @param query 查询条件实体对象，支持实体字段条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return IPage<LandlordEntrust> 分页结果对象，包含数据列表和分页信息
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 支持的查询条件（基于实体对象）：
     * 1. houseId: 房源ID（精确匹配）
     * 2. landlordId: 房东ID（精确匹配）
     * 3. entrustType: 委托类型（精确匹配）
     * 4. status: 委托状态（精确匹配：0=过期/取消，1=有效）
     * 5. entrustStartTime/entrustEndTime: 委托时间范围查询
     *
     * 默认排序：按委托开始时间倒序排列（最新委托在前）
     */
    @Override
    public IPage<LandlordEntrust> pageQuery(Page<LandlordEntrust> page, LandlordEntrust query, Long tenantId) {
        // 租户ID必填校验
        Assert.notNull(tenantId, "租户ID不能为空");

        // 构建查询条件
        QueryWrapper<LandlordEntrust> wrapper = buildQueryWrapper(query, tenantId);
        return landlordEntrustMapper.selectPage(page, wrapper);
    }

    /**
     * 多条件查询委托记录列表
     *
     * @param queryParams 查询参数Map，支持灵活的条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return List<LandlordEntrust> 符合条件的委托记录列表
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 支持的查询参数（基于Map）：
     * 1. houseId: 房源ID（精确匹配）
     * 2. landlordId: 房东ID（精确匹配）
     * 3. entrustType: 委托类型（精确匹配）
     * 4. status: 委托状态（精确匹配）
     * 5. startTime/endTime: 委托时间范围查询
     *
     * 默认排序：按委托开始时间倒序排列（最新委托在前）
     */
    @Override
    public List<LandlordEntrust> listByConditions(Map<String, Object> queryParams, Long tenantId) {
        // 租户ID必填校验
        Assert.notNull(tenantId, "租户ID不能为空");

        // 构建查询条件
        QueryWrapper<LandlordEntrust> wrapper = buildQueryWrapper(queryParams, tenantId);
        return list(wrapper);
    }

    /**
     * 根据房源ID查询委托记录
     *
     * @param houseId 房源ID
     * @param tenantId 租户ID，用于数据隔离
     * @return List<LandlordEntrust> 该房源的所有委托记录列表，按委托开始时间倒序排列
     * @throws IllegalArgumentException 当房源ID或租户ID为空时抛出
     *
     * 业务用途：
     * 1. 查看房屋的历史委托记录
     * 2. 分析房屋的委托管理情况
     * 3. 为房屋续签委托提供历史参考
     *
     * 返回说明：
     * 1. 返回列表包含该房源的所有委托记录
     * 2. 按委托开始时间倒序排列，最新委托在前
     * 3. 包含各种状态的记录（有效、过期/取消）
     */
    @Override
    public List<LandlordEntrust> listByHouseId(Long houseId, Long tenantId) {
        // 参数校验
        Assert.notNull(houseId, "房源ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 构建查询条件（房源ID + 租户ID）
        QueryWrapper<LandlordEntrust> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", tenantId)
                .eq("house_id", houseId)
                .orderByDesc("entrust_start_time"); // 按委托开始时间倒序
        return list(wrapper);
    }

    /**
     * 根据房东ID查询委托记录
     *
     * @param landlordId 房东ID
     * @param tenantId 租户ID，用于数据隔离
     * @return List<LandlordEntrust> 该房东的所有委托记录列表，按委托开始时间倒序排列
     * @throws IllegalArgumentException 当房东ID或租户ID为空时抛出
     *
     * 业务用途：
     * 1. 查看房东的所有委托房屋
     * 2. 分析房东的委托合作情况
     * 3. 房东委托服务的统计和管理
     *
     * 返回说明：
     * 1. 返回列表包含该房东的所有委托记录
     * 2. 按委托开始时间倒序排列，最新委托在前
     * 3. 可用于房东委托服务的综合管理
     */
    @Override
    public List<LandlordEntrust> listByLandlordId(Long landlordId, Long tenantId) {
        // 参数校验
        Assert.notNull(landlordId, "房东ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 构建查询条件（房东ID + 租户ID）
        QueryWrapper<LandlordEntrust> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", tenantId)
                .eq("landlord_id", landlordId)
                .orderByDesc("entrust_start_time"); // 按委托开始时间倒序
        return list(wrapper);
    }

    // ==================== 批量操作方法 ====================

    /**
     * 批量创建委托记录
     *
     * @param entrustList 委托记录列表
     * @return boolean 批量创建成功返回true，失败返回false（事务回滚）
     * @throws IllegalArgumentException 当参数为空或记录格式无效时抛出
     *
     * 执行流程：
     * 1. 列表非空校验
     * 2. 租户一致性校验（批量记录必须属于同一租户）
     * 3. 逐条记录基础参数校验
     * 4. 逐条记录委托冲突校验
     * 5. 批量保存到数据库（事务保障）
     *
     * 使用场景：
     * 1. 批量导入历史委托数据
     * 2. 批量创建相似房东的委托记录
     * 3. 数据迁移时的批量创建
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchCreate(List<LandlordEntrust> entrustList) {
        // 列表非空校验
        Assert.notEmpty(entrustList, "委托记录列表不能为空");

        // 1. 校验租户ID一致性
        Long tenantId = entrustList.get(0).getTenantId();
        Assert.notNull(tenantId, "租户ID不能为空");
        boolean hasInvalidTenant = entrustList.stream()
                .anyMatch(entrust -> !Objects.equals(entrust.getTenantId(), tenantId));
        Assert.isTrue(!hasInvalidTenant, "批量创建的记录必须属于同一租户");

        // 2. 校验每个实体并检查房源委托冲突
        for (LandlordEntrust entrust : entrustList) {
            validateEntrustEntity(entrust, true);
            validateHouseEntrustConflict(entrust);
        }

        // 3. 执行批量保存
        return saveBatch(entrustList);
    }

    /**
     * 批量删除委托记录
     *
     * @param ids 委托记录ID列表
     * @param tenantId 租户ID，用于权限校验
     * @return boolean 批量删除成功返回true，失败返回false（事务回滚）
     * @throws IllegalArgumentException 当参数为空或存在跨租户记录时抛出
     *
     * 执行流程：
     * 1. 参数非空校验
     * 2. 跨租户记录校验（防止越权删除）
     * 3. 执行批量删除操作（事务保障）
     *
     * 安全机制：
     * 1. 强制租户ID校验，确保只能删除自己租户的数据
     * 2. 批量操作前验证所有记录归属，防止部分成功部分失败
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemove(List<Long> ids, Long tenantId) {
        // 参数非空校验
        Assert.notEmpty(ids, "记录ID列表不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 1. 校验批量数据归属
        validateIdsBelongToTenant(ids, tenantId);

        // 2. 执行批量删除
        return removeByIds(ids);
    }

    /**
     * 批量更新委托状态
     *
     * @param ids 委托记录ID列表
     * @param status 目标状态值（0=过期/取消，1=有效）
     * @param tenantId 租户ID，用于权限校验
     * @return boolean 批量更新成功返回true，失败返回false（事务回滚）
     * @throws IllegalArgumentException 当参数为空或状态值无效时抛出
     *
     * 执行流程：
     * 1. 参数非空校验及状态值有效性校验
     * 2. 跨租户记录校验（防止越权更新）
     * 3. 执行批量更新操作（事务保障）
     *
     * 状态流转规则：
     * 1. 有效 → 过期/取消：委托到期或提前终止
     * 2. 过期/取消 → 有效：恢复已取消的委托（需特殊权限）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateStatus(List<Long> ids, Byte status, Long tenantId) {
        // 1. 参数非空校验
        Assert.notEmpty(ids, "记录ID列表不能为空");
        Assert.notNull(status, "目标状态不能为空");
        Assert.isTrue(status == STATUS_INACTIVE || status == STATUS_ACTIVE,
                "状态只能是" + STATUS_INACTIVE + "（过期/取消）或" + STATUS_ACTIVE + "（有效）");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 2. 校验数据归属
        validateIdsBelongToTenant(ids, tenantId);

        // 3. 批量更新状态
        LandlordEntrust updateEntity = new LandlordEntrust();
        updateEntity.setStatus(status);
        QueryWrapper<LandlordEntrust> wrapper = new QueryWrapper<>();
        wrapper.in("id", ids)
                .eq("tenant_id", tenantId);
        return update(updateEntity, wrapper);
    }

    // ==================== 内部工具方法 ====================

    /**
     * 构建基于实体对象的查询条件封装器
     *
     * @param query 查询条件实体对象
     * @param tenantId 租户ID，用于数据隔离
     * @return QueryWrapper<LandlordEntrust> 查询条件封装器
     *
     * 支持的查询条件：
     * 1. houseId: 房源ID（精确匹配，对应数据库house_id字段）
     * 2. landlordId: 房东ID（精确匹配，对应数据库landlord_id字段）
     * 3. entrustType: 委托类型（精确匹配，对应数据库entrust_type字段）
     * 4. status: 委托状态（精确匹配，对应数据库status字段）
     * 5. entrustStartTime/entrustEndTime: 委托时间范围查询
     *
     * 说明：此方法使用实体对象封装查询条件，类型安全且易于扩展
     */
    private QueryWrapper<LandlordEntrust> buildQueryWrapper(LandlordEntrust query, Long tenantId) {
        // 强制添加租户隔离条件
        QueryWrapper<LandlordEntrust> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", tenantId);

        // 房源ID精确查询
        if (query.getHouseId() != null) {
            wrapper.eq("house_id", query.getHouseId());
        }
        // 房东ID精确查询
        if (query.getLandlordId() != null) {
            wrapper.eq("landlord_id", query.getLandlordId());
        }
        // 委托类型查询
        if (StringUtils.hasText(query.getEntrustType())) {
            wrapper.eq("entrust_type", query.getEntrustType());
        }
        // 状态查询
        if (query.getStatus() != null) {
            wrapper.eq("status", query.getStatus());
        }
        // 委托时间范围查询
        if (query.getEntrustStartTime() != null) {
            wrapper.ge("entrust_start_time", query.getEntrustStartTime());
        }
        if (query.getEntrustEndTime() != null) {
            wrapper.le("entrust_end_time", query.getEntrustEndTime());
        }

        return wrapper;
    }

    /**
     * 构建基于Map参数的查询条件封装器
     *
     * @param queryParams 查询参数Map，支持动态条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return QueryWrapper<LandlordEntrust> 查询条件封装器
     *
     * 支持的查询参数：
     * 1. houseId: 房源ID（精确匹配）
     * 2. landlordId: 房东ID（精确匹配）
     * 3. entrustType: 委托类型（精确匹配）
     * 4. status: 委托状态（精确匹配）
     * 5. startTime/endTime: 委托时间范围查询
     *
     * 默认排序：按委托开始时间倒序排列（最新委托在前）
     */
    private QueryWrapper<LandlordEntrust> buildQueryWrapper(Map<String, Object> queryParams, Long tenantId) {
        // 强制添加租户隔离条件
        QueryWrapper<LandlordEntrust> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", tenantId);

        // 如果查询参数为空，直接返回基本查询条件
        if (!ObjectUtils.isEmpty(queryParams)) {
            // 房源ID
            if (queryParams.containsKey("houseId") && queryParams.get("houseId") != null) {
                wrapper.eq("house_id", queryParams.get("houseId"));
            }
            // 房东ID
            if (queryParams.containsKey("landlordId") && queryParams.get("landlordId") != null) {
                wrapper.eq("landlord_id", queryParams.get("landlordId"));
            }
            // 委托类型
            if (queryParams.containsKey("entrustType") && StringUtils.hasText(queryParams.get("entrustType").toString())) {
                wrapper.eq("entrust_type", queryParams.get("entrustType"));
            }
            // 状态
            if (queryParams.containsKey("status") && queryParams.get("status") != null) {
                wrapper.eq("status", queryParams.get("status"));
            }
            // 委托时间范围
            if (queryParams.containsKey("startTime") && queryParams.get("startTime") != null) {
                wrapper.ge("entrust_start_time", queryParams.get("startTime"));
            }
            if (queryParams.containsKey("endTime") && queryParams.get("endTime") != null) {
                wrapper.le("entrust_end_time", queryParams.get("endTime"));
            }
        }

        // 默认按委托开始时间倒序（最新委托在前）
        wrapper.orderByDesc("entrust_start_time");
        return wrapper;
    }

    /**
     * 校验委托实体必填项及业务规则
     *
     * @param entity 委托实体对象
     * @param isNew 是否为新增操作（新增时校验更多必填项）
     * @throws IllegalArgumentException 当参数为空或业务规则不满足时抛出
     *
     * 校验规则：
     * 1. 租户ID、房东ID、房源ID、委托类型不能为空
     * 2. 委托开始时间和结束时间不能为空，且结束时间不能早于开始时间
     * 3. 是否到期提醒标志不能为空且必须为0或1
     * 4. 新增时状态不能为空且必须为0或1
     *
     * 业务意义：
     * 1. 确保委托记录的基本信息完整性
     * 2. 防止不合法的委托时间设置
     * 3. 保证状态值的合法性
     */
    private void validateEntrustEntity(LandlordEntrust entity, boolean isNew) {
        // 共用校验（新增和更新都需要）
        Assert.notNull(entity.getTenantId(), "租户ID不能为空");
        Assert.notNull(entity.getLandlordId(), "房东ID不能为空");
        Assert.notNull(entity.getHouseId(), "房源ID不能为空");
        Assert.hasText(entity.getEntrustType(), "委托类型不能为空");
        Assert.notNull(entity.getEntrustStartTime(), "委托开始时间不能为空");
        Assert.notNull(entity.getEntrustEndTime(), "委托结束时间不能为空");
        Assert.notNull(entity.getRenewRemind(), "是否到期提醒不能为空");
        Assert.isTrue(entity.getRenewRemind() == RENEW_REMIND_NO || entity.getRenewRemind() == RENEW_REMIND_YES,
                "是否到期提醒只能是" + RENEW_REMIND_NO + "（不提醒）或" + RENEW_REMIND_YES + "（提醒）");

        // 时间逻辑校验
        Assert.isTrue(!entity.getEntrustStartTime().isAfter(entity.getEntrustEndTime()),
                "委托结束时间不能早于开始时间");

        // 新增时额外校验
        if (isNew) {
            Assert.notNull(entity.getStatus(), "委托状态不能为空");
            Assert.isTrue(entity.getStatus() == STATUS_INACTIVE || entity.getStatus() == STATUS_ACTIVE,
                    "委托状态只能是" + STATUS_INACTIVE + "（过期/取消）或" + STATUS_ACTIVE + "（有效）");
        }
    }

    /**
     * 校验房源是否存在重叠的有效委托
     *
     * @param entity 委托实体对象
     * @throws IllegalArgumentException 当存在委托冲突时抛出
     *
     * 校验逻辑：
     * 1. 查询同一租户下同一房源的有效委托记录（状态为1）
     * 2. 排除自身记录（更新时使用）
     * 3. 检查时间重叠：委托开始时间在区间内、结束时间在区间内、或完全包含区间
     *
     * 业务意义：
     * 1. 防止同一房源在同一时间段存在多条有效委托
     * 2. 确保委托排他性，避免业务冲突
     * 3. 提供明确的冲突提示信息，便于问题定位
     */
    private void validateHouseEntrustConflict(LandlordEntrust entity) {
        // 构建冲突查询条件
        QueryWrapper<LandlordEntrust> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", entity.getTenantId())
                .eq("house_id", entity.getHouseId())
                .eq("status", STATUS_ACTIVE) // 仅校验有效状态的委托
                .ne(entity.getId() != null, "id", entity.getId()); // 排除自身（更新场景）

        // 时间重叠条件：开始时间在区间内 或 结束时间在区间内 或 完全包含区间
        wrapper.and(wq -> wq
                .ge("entrust_start_time", entity.getEntrustStartTime())
                .le("entrust_start_time", entity.getEntrustEndTime())
                .or()
                .ge("entrust_end_time", entity.getEntrustStartTime())
                .le("entrust_end_time", entity.getEntrustEndTime())
                .or()
                .le("entrust_start_time", entity.getEntrustStartTime())
                .ge("entrust_end_time", entity.getEntrustEndTime())
        );

        // 执行查询计数
        long conflictCount = count(wrapper);
        Assert.isTrue(conflictCount == 0,
                "该房源在[" + entity.getEntrustStartTime() + "至" + entity.getEntrustEndTime() + "]已存在有效委托");
    }

    /**
     * 验证ID列表是否属于当前租户
     *
     * @param ids 委托记录ID列表
     * @param tenantId 租户ID
     * @throws IllegalArgumentException 当存在无效记录或跨租户记录时抛出
     *
     * 校验逻辑：
     * 1. 查询指定ID列表且租户ID匹配的记录数量
     * 2. 比较查询到的记录数量与输入ID列表数量是否一致
     *
     * 安全机制：
     * 1. 防止批量操作时误操作其他租户的数据
     * 2. 确保所有操作记录都属于当前租户
     * 3. 提供明确的错误提示，便于问题排查
     */
    private void validateIdsBelongToTenant(List<Long> ids, Long tenantId) {
        QueryWrapper<LandlordEntrust> wrapper = new QueryWrapper<>();
        wrapper.select("id")
                .in("id", ids)
                .eq("tenant_id", tenantId);
        long validCount = count(wrapper);
        Assert.isTrue(validCount == ids.size(), "存在无效的记录ID或不属于当前租户的记录");
    }
}
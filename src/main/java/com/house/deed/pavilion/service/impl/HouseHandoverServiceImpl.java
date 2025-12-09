package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.HouseHandover;
import com.house.deed.pavilion.mapper.HouseHandoverMapper;
import com.house.deed.pavilion.service.HouseHandoverService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * 房屋交接记录表（租户级数据） 服务实现类
 * </p>
 * 实现说明：
 * 1. 强制租户级数据隔离，所有操作均校验tenantId
 * 2. 支持房屋交接记录的全生命周期管理（创建、查询、更新、删除）
 * 3. 适配交接记录特有的业务查询场景（如按房源ID、交接状态、时间范围等）
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class HouseHandoverServiceImpl extends ServiceImpl<HouseHandoverMapper, HouseHandover> implements HouseHandoverService {

    @Resource
    private HouseHandoverMapper houseHandoverMapper;

    // ==================== 基础CRUD方法 ====================

    /**
     * 新增房屋交接记录
     * @param entity 交接记录实体
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveHandover(HouseHandover entity) {
        Assert.notNull(entity.getTenantId(), "租户ID不能为空");
        Assert.notNull(entity.getHouseId(), "房源ID不能为空");
        Assert.hasText(entity.getHandoverPerson(), "交接人不能为空");
        return save(entity);
    }

    /**
     * 根据ID查询交接记录（租户隔离）
     * @param id 记录ID
     * @param tenantId 租户ID
     * @return 交接记录实体
     */
    @Override
    public HouseHandover getById(Long id, Long tenantId) {
        Assert.notNull(id, "记录ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        QueryWrapper<HouseHandover> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id)
                .eq("tenant_id", tenantId);
        return getOne(wrapper);
    }

    /**
     * 更新交接记录（租户隔离校验）
     * @param entity 交接记录实体
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateHandover(HouseHandover entity) {
        Assert.notNull(entity.getId(), "记录ID不能为空");
        Assert.notNull(entity.getTenantId(), "租户ID不能为空");

        // 校验数据归属
        HouseHandover exist = getById(entity.getId(), entity.getTenantId());
        Assert.notNull(exist, "交接记录不存在或不属于当前租户");
        return updateById(entity);
    }

    /**
     * 删除交接记录（租户隔离）
     * @param id 记录ID
     * @param tenantId 租户ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeHandover(Long id, Long tenantId) {
        Assert.notNull(id, "记录ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 校验数据归属
        HouseHandover exist = getById(id, tenantId);
        Assert.notNull(exist, "交接记录不存在或不属于当前租户");
        return removeById(id);
    }


    // ==================== 多条件查询 ====================

    /**
     * 多条件分页查询交接记录
     * @param page 分页参数
     * @param query 查询条件实体
     * @param tenantId 租户ID
     * @return 分页结果
     */
    @Override
    public IPage<HouseHandover> pageQuery(Page<HouseHandover> page, HouseHandover query, Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");

        QueryWrapper<HouseHandover> wrapper = buildQueryWrapper(query, tenantId);
        return houseHandoverMapper.selectPage(page, wrapper);
    }

    /**
     * 多条件查询交接记录列表
     * @param queryParams 查询参数
     * @param tenantId 租户ID
     * @return 交接记录列表
     */
    @Override
    public List<HouseHandover> listByConditions(Map<String, Object> queryParams, Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");

        QueryWrapper<HouseHandover> wrapper = buildQueryWrapper(queryParams, tenantId);
        return list(wrapper);
    }

    /**
     * 根据房源ID查询交接记录
     * @param houseId 房源ID
     * @param tenantId 租户ID
     * @return 交接记录列表
     */
    @Override
    public List<HouseHandover> listByHouseId(Long houseId, Long tenantId) {
        Assert.notNull(houseId, "房源ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        QueryWrapper<HouseHandover> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", tenantId)
                .eq("house_id", houseId)
                .orderByDesc("handover_time"); // 按交接时间倒序
        return list(wrapper);
    }

    /**
     * 构建查询条件封装（实体参数）
     */
    private QueryWrapper<HouseHandover> buildQueryWrapper(HouseHandover query, Long tenantId) {
        QueryWrapper<HouseHandover> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", tenantId);

        // 房源ID精确查询
        if (query.getHouseId() != null) {
            wrapper.eq("house_id", query.getHouseId());
        }
        // 交接状态查询（如：1-已交接，0-未交接）
        if (query.getStatus() != null) {
            wrapper.eq("status", query.getStatus());
        }
        // 交接人模糊查询
        if (StringUtils.hasText(query.getHandoverPerson())) {
            wrapper.like("handover_person", query.getHandoverPerson());
        }
        // 交接时间范围查询
        if (query.getHandoverTime() != null) {
            wrapper.ge("handover_time", query.getHandoverTime());
        }
        // 接收人模糊查询
        if (StringUtils.hasText(query.getReceiver())) {
            wrapper.like("receiver", query.getReceiver());
        }

        return wrapper;
    }

    /**
     * 构建查询条件封装（Map参数）
     */
    private QueryWrapper<HouseHandover> buildQueryWrapper(Map<String, Object> queryParams, Long tenantId) {
        QueryWrapper<HouseHandover> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", tenantId);

        if (!ObjectUtils.isEmpty(queryParams)) {
            // 房源ID
            if (queryParams.containsKey("houseId") && queryParams.get("houseId") != null) {
                wrapper.eq("house_id", queryParams.get("houseId"));
            }
            // 交接状态
            if (queryParams.containsKey("status") && queryParams.get("status") != null) {
                wrapper.eq("status", queryParams.get("status"));
            }
            // 交接时间范围
            if (queryParams.containsKey("startTime") && queryParams.get("startTime") != null) {
                wrapper.ge("handover_time", queryParams.get("startTime"));
            }
            if (queryParams.containsKey("endTime") && queryParams.get("endTime") != null) {
                wrapper.le("handover_time", queryParams.get("endTime"));
            }
            // 交接人
            if (queryParams.containsKey("handoverPerson") && StringUtils.hasText(queryParams.get("handoverPerson").toString())) {
                wrapper.like("handover_person", queryParams.get("handoverPerson"));
            }
        }

        // 默认按交接时间倒序
        wrapper.orderByDesc("handover_time");
        return wrapper;
    }


    // ==================== 批量操作 ====================

    /**
     * 批量创建交接记录
     * @param handoverList 交接记录列表
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchCreate(List<HouseHandover> handoverList) {
        Assert.notEmpty(handoverList, "交接记录列表不能为空");

        // 校验租户ID一致性
        Long tenantId = handoverList.get(0).getTenantId();
        Assert.notNull(tenantId, "租户ID不能为空");
        boolean hasInvalidTenant = handoverList.stream()
                .anyMatch(handover -> !Objects.equals(handover.getTenantId(), tenantId));
        Assert.isTrue(!hasInvalidTenant, "批量创建的记录必须属于同一租户");

        // 校验必填字段
        for (HouseHandover handover : handoverList) {
            Assert.notNull(handover.getHouseId(), "房源ID不能为空");
            Assert.hasText(handover.getHandoverPerson(), "交接人不能为空");
        }

        return saveBatch(handoverList);
    }

    /**
     * 批量删除交接记录
     * @param ids 记录ID列表
     * @param tenantId 租户ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemove(List<Long> ids, Long tenantId) {
        Assert.notEmpty(ids, "记录ID列表不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 校验批量数据归属
        QueryWrapper<HouseHandover> wrapper = new QueryWrapper<>();
        wrapper.in("id", ids)
                .ne("tenant_id", tenantId);
        long invalidCount = count(wrapper);
        Assert.isTrue(invalidCount == 0, "存在不属于当前租户的记录，无法删除");

        return removeByIds(ids);
    }

    /**
     * 批量更新交接状态
     * @param ids 记录ID列表
     * @param status 目标状态
     * @param tenantId 租户ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateStatus(List<Long> ids, String status, Long tenantId) {
        Assert.notEmpty(ids, "记录ID列表不能为空");
        Assert.notNull(status, "目标状态不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 校验数据归属
        validateIdsBelongToTenant(ids, tenantId);

        // 批量更新状态
        HouseHandover updateEntity = new HouseHandover();
        updateEntity.setStatus(status);
        QueryWrapper<HouseHandover> wrapper = new QueryWrapper<>();
        wrapper.in("id", ids)
                .eq("tenant_id", tenantId);
        return update(updateEntity, wrapper);
    }

    /**
     * 验证ID列表是否属于当前租户
     */
    private void validateIdsBelongToTenant(List<Long> ids, Long tenantId) {
        QueryWrapper<HouseHandover> wrapper = new QueryWrapper<>();
        wrapper.select("id")
                .in("id", ids)
                .eq("tenant_id", tenantId);
        long validCount = count(wrapper);
        Assert.isTrue(validCount == ids.size(), "存在无效的记录ID或不属于当前租户的记录");
    }
}
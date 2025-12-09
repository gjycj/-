package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.CustomerFollowUp;
import com.house.deed.pavilion.mapper.CustomerFollowUpMapper;
import com.house.deed.pavilion.service.CustomerFollowUpService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 客户跟进记录表（租户级数据） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class CustomerFollowUpServiceImpl extends ServiceImpl<CustomerFollowUpMapper, CustomerFollowUp> implements CustomerFollowUpService {

    /**
     * 新增跟进记录（校验租户ID非空）
     */
    @Override
    public boolean saveFollowUp(CustomerFollowUp followUp) {
        Assert.notNull(followUp.getTenantId(), "租户ID不能为空");
        Assert.notNull(followUp.getCustomerId(), "客户ID不能为空");
        Assert.notNull(followUp.getAgentId(), "跟进经纪人ID不能为空");
        return save(followUp);
    }

    /**
     * 更新跟进记录（校验租户归属）
     */
    @Override
    public boolean updateFollowUpById(CustomerFollowUp followUp) {
        Assert.notNull(followUp.getId(), "记录ID不能为空");
        Assert.notNull(followUp.getTenantId(), "租户ID不能为空");

        // 校验记录存在且属于当前租户
        CustomerFollowUp exist = getById(followUp.getId());
        Assert.notNull(exist, "跟进记录不存在");
        Assert.isTrue(Objects.equals(exist.getTenantId(), followUp.getTenantId()), "无权限操作此记录");

        return updateById(followUp);
    }

    /**
     * 删除跟进记录（校验租户归属）
     */
    @Override
    public boolean removeFollowUpById(Long id, Long tenantId) {
        Assert.notNull(id, "记录ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        // 校验记录存在且属于当前租户
        CustomerFollowUp exist = getById(id);
        Assert.notNull(exist, "跟进记录不存在");
        Assert.isTrue(Objects.equals(exist.getTenantId(), tenantId), "无权限操作此记录");

        return removeById(id);
    }

    /**
     * 按ID查询（租户隔离）
     */
    @Override
    public CustomerFollowUp getFollowUpById(Long id, Long tenantId) {
        Assert.notNull(id, "记录ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        return getOne(new LambdaQueryWrapper<CustomerFollowUp>()
                .eq(CustomerFollowUp::getId, id)
                .eq(CustomerFollowUp::getTenantId, tenantId));
    }

    /**
     * 多条件分页查询
     */
    @Override
    public IPage<CustomerFollowUp> pageQuery(Page<CustomerFollowUp> page, CustomerFollowUp query) {
        Assert.notNull(query.getTenantId(), "租户ID不能为空");

        LambdaQueryWrapper<CustomerFollowUp> wrapper = buildQueryWrapper(query);
        return baseMapper.selectPage(page, wrapper);
    }

    /**
     * 多条件列表查询
     */
    @Override
    public List<CustomerFollowUp> listByConditions(CustomerFollowUp query) {
        Assert.notNull(query.getTenantId(), "租户ID不能为空");

        LambdaQueryWrapper<CustomerFollowUp> wrapper = buildQueryWrapper(query);
        return baseMapper.selectList(wrapper);
    }

    /**
     * 按客户ID查询跟进记录
     */
    @Override
    public List<CustomerFollowUp> listByCustomerId(Long customerId, Long tenantId) {
        Assert.notNull(customerId, "客户ID不能为空");
        Assert.notNull(tenantId, "租户ID不能为空");

        CustomerFollowUp query = new CustomerFollowUp();
        query.setTenantId(tenantId);
        query.setCustomerId(customerId);
        return listByConditions(query);
    }

    /**
     * 批量新增跟进记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveFollowUps(List<CustomerFollowUp> followUpList) {
        if (CollectionUtils.isEmpty(followUpList)) {
            return true;
        }

        // 校验租户ID一致性
        Long tenantId = followUpList.get(0).getTenantId();
        Assert.notNull(tenantId, "租户ID不能为空");
        boolean hasInvalidTenant = followUpList.stream()
                .anyMatch(follow -> !Objects.equals(follow.getTenantId(), tenantId));
        Assert.isTrue(!hasInvalidTenant, "批量记录必须属于同一租户");

        // 校验必填字段
        followUpList.forEach(follow -> {
            Assert.notNull(follow.getCustomerId(), "客户ID不能为空");
            Assert.notNull(follow.getAgentId(), "经纪人ID不能为空");
            Assert.notNull(follow.getFollowTime(), "跟进时间不能为空");
        });

        return saveBatch(followUpList);
    }

    /**
     * 批量删除跟进记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchRemoveFollowUps(List<Long> ids, Long tenantId) {
        Assert.notNull(tenantId, "租户ID不能为空");
        Assert.notEmpty(ids, "记录ID列表不能为空");

        // 校验所有ID属于当前租户
        long count = baseMapper.selectCount(new LambdaQueryWrapper<CustomerFollowUp>()
                .in(CustomerFollowUp::getId, ids)
                .ne(CustomerFollowUp::getTenantId, tenantId));
        Assert.isTrue(count == 0, "存在跨租户记录，无法删除");

        return removeByIds(ids);
    }

    /**
     * 构建查询条件（复用逻辑）
     */
    public LambdaQueryWrapper<CustomerFollowUp> buildQueryWrapper(CustomerFollowUp query) {
        LambdaQueryWrapper<CustomerFollowUp> wrapper = new LambdaQueryWrapper<>();
        // 强制租户隔离
        wrapper.eq(CustomerFollowUp::getTenantId, query.getTenantId());

        // 动态条件
        if (query.getCustomerId() != null) {
            wrapper.eq(CustomerFollowUp::getCustomerId, query.getCustomerId());
        }
        if (query.getAgentId() != null) {
            wrapper.eq(CustomerFollowUp::getAgentId, query.getAgentId());
        }
        if (query.getHouseId() != null) {
            wrapper.eq(CustomerFollowUp::getHouseId, query.getHouseId());
        }
        if (query.getFollowTime() != null) {
            wrapper.ge(CustomerFollowUp::getFollowTime, query.getFollowTime()); // 大于等于指定时间
        }
        if (StringUtils.hasText(query.getContent())) {
            wrapper.like(CustomerFollowUp::getContent, query.getContent()); // 跟进内容模糊查询
        }
        if (query.getContractId() != null) {
            wrapper.eq(CustomerFollowUp::getContractId, query.getContractId());
        }

        // 按跟进时间倒序（最新跟进在前）
        wrapper.orderByDesc(CustomerFollowUp::getFollowTime);
        return wrapper;
    }
}
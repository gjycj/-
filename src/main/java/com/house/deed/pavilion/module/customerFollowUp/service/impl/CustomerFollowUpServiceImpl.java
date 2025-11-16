package com.house.deed.pavilion.module.customerFollowUp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.ValidateUtil;
import com.house.deed.pavilion.module.customerFollowUp.entity.CustomerFollowUp;
import com.house.deed.pavilion.module.customerFollowUp.mapper.CustomerFollowUpMapper;
import com.house.deed.pavilion.module.customerFollowUp.service.ICustomerFollowUpService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 客户跟进记录表（租户级数据） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class CustomerFollowUpServiceImpl extends ServiceImpl<CustomerFollowUpMapper, CustomerFollowUp> implements ICustomerFollowUpService {

    @Override
    public List<CustomerFollowUp> getByContractId(Long contractId, Long tenantId) {
        return lambdaQuery()
                .eq(CustomerFollowUp::getTenantId, tenantId)
                .eq(CustomerFollowUp::getContractId, contractId)
                .list();
    }

    // 补充：分页查询客户的跟进记录
    @Override
    public Page<CustomerFollowUp> getByCustomerId(Page<CustomerFollowUp> page, Long customerId, Long tenantId) {
        // 构建查询条件：租户隔离 + 客户ID匹配 + 按跟进时间倒序
        LambdaQueryWrapper<CustomerFollowUp> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CustomerFollowUp::getTenantId, tenantId)  // 多租户隔离
                .eq(CustomerFollowUp::getCustomerId, customerId)  // 匹配目标客户
                .orderByDesc(CustomerFollowUp::getFollowTime);  // 最新跟进记录在前

        // 执行分页查询
        return baseMapper.selectPage(page, queryWrapper);
    }

    /**
     * 保存跟进记录（增强：时间时序校验）
     */
    @Transactional
    public boolean saveWithTimeCheck(CustomerFollowUp followUp) {
        Long customerId = followUp.getCustomerId();
        Long tenantId = followUp.getTenantId();
        LocalDateTime currentFollowTime = followUp.getFollowTime();

        // 1. 校验当前跟进时间不为空
        ValidateUtil.notNull(currentFollowTime, "跟进时间不能为空");

        // 2. 查询该客户上一次的跟进时间
        LocalDateTime lastFollowTime = baseMapper.selectLastFollowTime(customerId, tenantId);

        // 3. 若存在上一次跟进，校验当前时间是否更晚
        if (lastFollowTime != null && currentFollowTime.isBefore(lastFollowTime)) {
            throw new BusinessException(400, "跟进时间不能早于上一次跟进时间（上一次跟进时间：" + lastFollowTime + "）");
        }

        // 4. 保存记录
        return save(followUp);
    }

}

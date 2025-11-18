package com.house.deed.pavilion.module.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.contract.entity.Contract;
import com.house.deed.pavilion.module.contract.mapper.ContractMapper;
import com.house.deed.pavilion.module.contract.service.IContractService;
import com.house.deed.pavilion.module.customer.entity.Customer;
import com.house.deed.pavilion.module.customer.mapper.CustomerMapper;
import com.house.deed.pavilion.module.customer.service.ICustomerService;
import com.house.deed.pavilion.module.customer.vo.CustomerFullFlowVO;
import com.house.deed.pavilion.module.customerFollowUp.entity.CustomerFollowUp;
import com.house.deed.pavilion.module.customerFollowUp.service.ICustomerFollowUpService;
import com.house.deed.pavilion.module.visitRecord.entity.VisitRecord;
import com.house.deed.pavilion.module.visitRecord.service.IVisitRecordService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 客户信息表（租户级数据隔离） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements ICustomerService {

    @Resource
    private IVisitRecordService visitRecordService;

    @Resource
    private ContractMapper contractMapper;

    @Resource
    private ICustomerFollowUpService followUpService;

    // 在CustomerServiceImpl中新增该方法
    @Override
    public CustomerFullFlowVO getFullFlowByCustomerId(Long customerId, Long tenantId) {
        // 1. 校验客户存在性及租户归属
        Customer customer = getById(customerId);
        if (customer == null || !customer.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "客户不存在或无权访问");
        }

        // 2. 查询关联数据
        // 2.1 查询带看记录（需依赖IVisitRecordService的getByCustomerId方法）
        List<VisitRecord> visitRecords = visitRecordService.getByCustomerId(customerId, tenantId);

        // 2.2 查询合同记录（需依赖IContractService的getByCustomerId方法）
        List<Contract> contracts = contractMapper.selectList(
                Wrappers.<Contract>lambdaQuery().eq(Contract::getCustomerId, customerId).eq(Contract::getTenantId, tenantId)
        );

        // 2.3 查询跟进记录（此处即为用户提到的代码）
        Page<CustomerFollowUp> followUpPage = followUpService.getByCustomerId(
                new Page<>(1, Integer.MAX_VALUE),  // 第一页，最大条数获取全量
                customerId,
                tenantId
        );
        List<CustomerFollowUp> followUps = followUpPage.getRecords();

        // 3. 封装VO对象
        CustomerFullFlowVO vo = new CustomerFullFlowVO();
        BeanUtils.copyProperties(customer, vo); // 复制客户基本信息
        vo.setVisitRecords(visitRecords);
        vo.setContracts(contracts);
        vo.setFollowUps(followUps);

        return vo;
    }

    @Override
    public boolean existsById(Long id) {
        return this.exists(Wrappers.<Customer>lambdaQuery().eq(Customer::getId, id));
    }

    @Override
    public Page<Customer> getCustomerPageByCondition(
            Page<Customer> page,
            Long intendedRegionId,
            BigDecimal priceMin,
            BigDecimal priceMax,
            String customerType,
            String status) {

        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<Customer> queryWrapper = Wrappers.lambdaQuery();

        // 多租户隔离（必加条件）
        queryWrapper.eq(Customer::getTenantId, tenantId);

        // 条件筛选（仅当参数不为空时添加）
        queryWrapper.eq(intendedRegionId != null, Customer::getIntendedRegionId, intendedRegionId)
                .ge(priceMin != null, Customer::getIntendedPriceMin, priceMin)
                .le(priceMax != null, Customer::getIntendedPriceMax, priceMax)
                .eq(customerType != null, Customer::getCustomerType, customerType)
                .eq(status != null, Customer::getStatus, status)
                .orderByDesc(Customer::getCreateTime); // 默认按创建时间倒序

        return baseMapper.selectPage(page, queryWrapper);
    }

    @Override
    public boolean updateStatus(Long customerId, String targetStatus, Long operatorId) {
        Long tenantId = TenantContext.getTenantId();

        // 1. 校验客户存在性及租户归属
        Customer customer = baseMapper.selectById(customerId);
        if (customer == null || !customer.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "客户不存在或无权访问");
        }

        // 2. 校验目标状态合法性
        if (!("ACTIVE".equals(targetStatus) || "DEALED".equals(targetStatus) || "DORMANT".equals(targetStatus))) {
            throw new BusinessException(400, "目标状态只能是ACTIVE/DEALED/DORMANT");
        }

        // 3. 校验状态流转合法性
        String currentStatus = customer.getStatus();
        validateStatusTransition(currentStatus, targetStatus);

        // 4. 更新状态
        customer.setStatus(targetStatus);
        customer.setUpdateTime(LocalDateTime.now());
        return baseMapper.updateById(customer) > 0;
    }

    /**
     * 校验状态流转是否合法
     * 允许的流转规则：
     * ACTIVE → DEALED（成交）、ACTIVE → DORMANT（休眠）
     * DORMANT → ACTIVE（激活）
     * DEALED 不可流转（一旦成交状态固定）
     */
    private void validateStatusTransition(String currentStatus, String targetStatus) {
        if ("DEALED".equals(currentStatus)) {
            throw new BusinessException(400, "已成交客户不可变更状态");
        }
        if ("DORMANT".equals(currentStatus) && "DEALED".equals(targetStatus)) {
            throw new BusinessException(400, "休眠客户不可直接标记为成交");
        }
    }
}

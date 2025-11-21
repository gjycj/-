package com.house.deed.pavilion.module.customer.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.aspect.annotation.AgentDataPermission;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.AgentContext;
import com.house.deed.pavilion.common.util.RoleUtil;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.contract.entity.Contract;
import com.house.deed.pavilion.module.contract.mapper.ContractMapper;
import com.house.deed.pavilion.module.customer.dto.CustomerQueryDTO;
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

    /**
     * 查询权限优化点：
     * 1. 移除手动动租户归属校验，通过注解自动校验
     * 2. 移除创建者权限判断，由注解切面控制
     */
    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = Customer.class,
            dataIdParam = "customerId" // 从参数获取客户ID进行权限校验
    )
    public CustomerFullFlowVO getFullFlowByCustomerId(Long customerId, Long tenantId) {
        // 1. 注解已自动完成：客户存在性校验、租户归属校验、创建者权限校验
        Customer customer = getById(customerId);

        // 2. 查询关联数据（注：带看记录/合同的权限由各自服务的注解控制）
        List<VisitRecord> visitRecords = visitRecordService.getByCustomerId(customerId);
        List<Contract> contracts = contractMapper.selectList(
                Wrappers.<Contract>lambdaQuery()
                        .eq(Contract::getCustomerId, customerId)
                        .eq(Contract::getTenantId, tenantId)
        );
        Page<CustomerFollowUp> followUpPage = followUpService.getByCustomerId(
                new Page<>(1, Integer.MAX_VALUE),
                customerId,
                tenantId
        );

        // 3. 封装VO
        CustomerFullFlowVO vo = new CustomerFullFlowVO();
        BeanUtils.copyProperties(customer, vo);
        vo.setVisitRecords(visitRecords);
        vo.setContracts(contracts);
        vo.setFollowUps(followUpPage.getRecords());
        return vo;
    }

    /**
     * 权限优化点：
     * 1. 移除手动拼接的createAgentId条件，由注解自动添加
     * 2. 保留租户隔离（注解通常会自动处理，此处冗余保留确保安全）
     */
    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = Customer.class,
            creatorField = "createAgentId" // 过滤条件：create_agent_id = 当前经纪人ID
    )
    public Page<Customer> getCustomerPage(Page<Customer> page, CustomerQueryDTO query) {
        Long tenantId = TenantContext.getTenantId();

        return baseMapper.selectPage(page,
                new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getTenantId, tenantId) // 租户隔离（注解通常已包含，冗余保留）
                        // 注解自动添加：eq(Customer::getCreateAgentId, currentAgentId)
                        .like(StrUtil.isNotBlank(query.getName()), Customer::getName, query.getName())
                        .eq(StrUtil.isNotBlank(query.getStatus()), Customer::getStatus, query.getStatus())
                        .eq(StrUtil.isNotBlank(query.getType()), Customer::getCustomerType, query.getType())
        );
    }

    @Override
    public boolean existsById(Long id) {
        return this.exists(Wrappers.<Customer>lambdaQuery().eq(Customer::getId, id));
    }

    /**
     * 权限优化点：
     * 1. 移除手动动的isPrivileged判断和createAgentId过滤，注解注解自动处理
     * 2. 注解会根据角色自动动适配：管理员/店长查全部，普通经纪人纪人查自己的
     */
    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = Customer.class,
            creatorField = "createAgentId"
    )
    public Page<Customer> getCustomerPageByCondition(
            Page<Customer> page,
            Long intendedRegionId,
            BigDecimal priceMin,
            BigDecimal priceMax,
            String customerType,
            String status,
            Long currentAgentId) {

        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<Customer> queryWrapper = Wrappers.lambdaQuery();

        // 1. 租户隔离
        queryWrapper.eq(Customer::getTenantId, tenantId);

        // 2. 注解自动添加：非管理员时拼接加eq(Customer::getCreateAgentId, currentAgentId)

        // 3. 业务条件筛选
        queryWrapper.eq(intendedRegionId != null, Customer::getIntendedRegionId, intendedRegionId)
                .ge(priceMin != null, Customer::getIntendedPriceMin, priceMin)
                .le(priceMax != null, Customer::getIntendedPriceMax, priceMax)
                .eq(customerType != null, Customer::getCustomerType, customerType)
                .eq(status != null, Customer::getStatus, status)
                .orderByDesc(Customer::getCreateTime);

        return baseMapper.selectPage(page, queryWrapper);
    }

    /**
     * 权限优化点：
     * 1. 移除手动的租户归属和创建者权限校验
     * 2. 通过注解解切面自动校验：仅创建者/管理员可操作
     */
    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.UPDATE,
            entityClass = Customer.class,
            dataIdParam = "customerId" // 从参数获取客户ID
    )
    public boolean updateStatus(Long customerId, String targetStatus, Long operatorId) {
        // 1. 注解解已自动完成：客户存在性、租户归属、创建者权限校验
        Customer customer = baseMapper.selectById(customerId);

        // 2. 状态合法性校验（业务逻辑保留）
        if (!("ACTIVE".equals(targetStatus) || "DEALED".equals(targetStatus) || "DORMANT".equals(targetStatus))) {
            throw new BusinessException(400, "目标状态只能是ACTIVE/DEALED/DORMANT");
        }

        // 3. 状态流转校验
        validateStatusTransition(customer.getStatus(), targetStatus);

        // 4. 更新状态
        customer.setStatus(targetStatus);
        customer.setUpdateTime(LocalDateTime.now());
        return baseMapper.updateById(customer) > 0;
    }

    /**
     * 校验状态流转合法性
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
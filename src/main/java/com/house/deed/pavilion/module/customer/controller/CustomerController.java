package com.house.deed.pavilion.module.customer.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.AgentContext;
import com.house.deed.pavilion.common.util.RoleUtil;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.customer.dto.CustomerQueryDTO;
import com.house.deed.pavilion.module.customer.entity.Customer;
import com.house.deed.pavilion.module.customer.service.ICustomerService;
import com.house.deed.pavilion.module.customer.vo.CustomerFullFlowVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 客户信息控制器
 */
@RestController
@RequestMapping("/module/customer")
public class CustomerController {

    @Resource
    private ICustomerService customerService;

    @GetMapping("/{customerId}/full-flow")
    public ResultDTO<CustomerFullFlowVO> getCustomerFullFlow(@PathVariable Long customerId) {
        Long tenantId = TenantContext.getTenantId();
        Long currentAgentId = AgentContext.getAgentId();

        // 1. 校验客户存在性及租户归属（基于customer.tenant_id）
        Customer customer = customerService.getById(customerId);
        if (customer == null || !customer.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "客户不存在或无权访问");
        }

        // 2. 校验操作人权限（基于customer.create_agent_id，管理员/店长豁免）
        if (!RoleUtil.isAdmin() && !RoleUtil.isStoreManager()
                && !customer.getCreateAgentId().equals(currentAgentId)) {
            throw new BusinessException(403, "无权访问非本人创建的客户全流程信息");
        }

        // 3. 查询全流程信息
        CustomerFullFlowVO result = customerService.getFullFlowByCustomerId(customerId, tenantId);
        return ResultDTO.success(result);
    }

    /**
     * 新增客户
     */
    @PostMapping
    public ResultDTO<Boolean> addCustomer(@RequestBody Customer customer) {
        // 校验必填字段（name/phone对应customer表非空约束）
        if (customer.getName() == null || customer.getPhone() == null) {
            throw new BusinessException(400, "客户姓名和电话不能为空");
        }

        // 自动填充租户ID和创建者ID（关联customer.tenant_id和create_agent_id）
        Long tenantId = TenantContext.getTenantId();
        Long currentAgentId = AgentContext.getAgentId();
        customer.setTenantId(tenantId);
        customer.setCreateAgentId(currentAgentId); // 绑定创建者，为后续权限判断奠基

        boolean success = customerService.save(customer);
        return ResultDTO.success(success);
    }

    /**
     * 更新客户状态
     */
    @PatchMapping("/{id}/status")
    public ResultDTO<Boolean> updateCustomerStatus(
            @PathVariable Long id,
            @RequestParam String targetStatus,
            @RequestParam Long operatorId) {

        Long tenantId = TenantContext.getTenantId();
        Long currentAgentId = AgentContext.getAgentId();

        // 1. 校验客户存在性及租户归属（customer.tenant_id匹配）
        Customer customer = customerService.getById(id);
        if (customer == null || !customer.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "客户不存在或无权访问");
        }

        // 2. 校验操作人权限（仅创建者/管理员/店长可操作）
        if (!RoleUtil.isAdmin() && !RoleUtil.isStoreManager()
                && !customer.getCreateAgentId().equals(currentAgentId)) {
            throw new BusinessException(403, "无权修改非本人创建的客户状态");
        }

        // 3. 执行状态更新（更新customer.status字段）
        boolean success = customerService.updateStatus(id, targetStatus, operatorId);
        return ResultDTO.success(success);
    }

    /**
     * 查询单个客户
     */
    @GetMapping("/{id}")
    public ResultDTO<Customer> getCustomerById(@PathVariable Long id) {
        Long tenantId = TenantContext.getTenantId();
        Long currentAgentId = AgentContext.getAgentId();

        // 1. 查询客户并校验租户归属
        Customer customer = customerService.getById(id);
        if (customer == null || !customer.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "客户不存在或无权访问");
        }

        // 2. 校验当前经纪人是否为创建者（管理员/店长豁免）
        if (!RoleUtil.isAdmin() && !RoleUtil.isStoreManager()
                && !customer.getCreateAgentId().equals(currentAgentId)) {
            throw new BusinessException(403, "无权访问非本人创建的客户");
        }

        return ResultDTO.success(customer);
    }

    // 分页查询客户列表（仅返回当前经纪人创建的客户）
    @GetMapping("/page")
    public ResultDTO<Page<Customer>> getCustomerPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestBody CustomerQueryDTO query) {
        Page<Customer> page = new Page<>(pageNum, pageSize);
        Page<Customer> resultPage = customerService.getCustomerPage(page, query);
        return ResultDTO.success(resultPage);
    }

    /**
     * 更新客户
     */
    @PutMapping("/{id}")
    public ResultDTO<Boolean> updateCustomer(@PathVariable Long id, @RequestBody Customer customer) {
        if (!id.equals(customer.getId())) {
            throw new BusinessException(400, "ID不匹配");
        }

        Long tenantId = TenantContext.getTenantId();
        Long currentAgentId = AgentContext.getAgentId(); // 当前经纪人ID

        // 1. 校验客户存在性及租户归属
        Customer existingCustomer = customerService.getById(id);
        if (existingCustomer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        if (!existingCustomer.getTenantId().equals(tenantId)) {
            throw new BusinessException(403, "无权访问该客户");
        }

        // 2. 新增：校验当前经纪人是否为客户创建人
        if (!existingCustomer.getCreateAgentId().equals(currentAgentId)) {
            throw new BusinessException(403, "无权修改他人创建的客户");
        }

        // 3. 禁止修改租户ID和创建人ID（防篡改）
        customer.setTenantId(tenantId);
        customer.setCreateAgentId(currentAgentId);

        boolean success = customerService.updateById(customer);
        return ResultDTO.success(success);
    }

    /**
     * 删除客户
     */
    @DeleteMapping("/{id}")
    public ResultDTO<Boolean> deleteCustomer(@PathVariable Long id) {
        Long tenantId = TenantContext.getTenantId();
        Long currentAgentId = AgentContext.getAgentId();

        // 1. 校验客户存在性及租户归属
        Customer customer = customerService.getById(id);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        if (!customer.getTenantId().equals(tenantId)) {
            throw new BusinessException(403, "无权访问该客户");
        }

        // 2. 新增：校验当前经纪人是否为客户创建人
        if (!customer.getCreateAgentId().equals(currentAgentId)) {
            throw new BusinessException(403, "无权删除他人创建的客户");
        }

        // 3. 执行删除（如果有业务关联校验，可在此处添加）
        boolean success = customerService.removeById(id);
        return ResultDTO.success(success);
    }

    /**
     * 带条件的客户分页查询
     */
    @GetMapping("/page/condition")
    public ResultDTO<Page<Customer>> getCustomerPageByCondition(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long intendedRegionId,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(required = false) String customerType,
            @RequestParam(required = false) String status) {

        // 参数合法性校验（匹配customer表的customer_type和status字段枚举值）
        if (status != null && !("ACTIVE".equals(status) || "DEALED".equals(status) || "DORMANT".equals(status))) {
            throw new BusinessException(400, "状态只能是ACTIVE/DEALED/DORMANT");
        }
        if (customerType != null && !("ORDINARY".equals(customerType) || "VIP".equals(customerType) || "INVEST".equals(customerType))) {
            throw new BusinessException(400, "客户类型只能是ORDINARY/VIP/INVEST");
        }

        Long tenantId = TenantContext.getTenantId();
        Long currentAgentId = AgentContext.getAgentId();
        Page<Customer> page = new Page<>(pageNum, pageSize);

        // 带条件查询，同时过滤租户和创建者（非管理员仅查自己的客户）
        Page<Customer> resultPage = customerService.getCustomerPageByCondition(
                page,
                intendedRegionId,
                priceMin,
                priceMax,
                customerType,
                status,
                currentAgentId
        );
        return ResultDTO.success(resultPage);
    }
}
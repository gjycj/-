package com.house.deed.pavilion.module.customer.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.module.customer.entity.Customer;
import com.house.deed.pavilion.module.customer.service.ICustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 客户信息控制器
 */
@RestController
@RequestMapping("/module/customer")
public class CustomerController {

    @Autowired
    private ICustomerService customerService;

    /**
     * 新增客户
     */
    @PostMapping
    public ResultDTO<Boolean> addCustomer(@RequestBody Customer customer) {
        // 校验必填字段
        if (customer.getName() == null || customer.getPhone() == null) {
            throw new BusinessException(400, "客户姓名和电话不能为空");
        }
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

        boolean success = customerService.updateStatus(id, targetStatus, operatorId);
        return ResultDTO.success(success);
    }

    /**
     * 查询单个客户
     */
    @GetMapping("/{id}")
    public ResultDTO<Customer> getCustomerById(@PathVariable Long id) {
        Customer customer = customerService.getById(id);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        return ResultDTO.success(customer);
    }

    /**
     * 分页查询客户
     */
    @GetMapping("/page")
    public ResultDTO<Page<Customer>> getCustomerPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Customer> page = new Page<>(pageNum, pageSize);
        Page<Customer> resultPage = customerService.page(page);
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
        if (!customerService.existsById(id)) {
            throw new BusinessException(404, "客户不存在");
        }
        boolean success = customerService.updateById(customer);
        return ResultDTO.success(success);
    }

    /**
     * 删除客户
     */
    @DeleteMapping("/{id}")
    public ResultDTO<Boolean> deleteCustomer(@PathVariable Long id) {
        if (!customerService.existsById(id)) {
            throw new BusinessException(404, "客户不存在");
        }
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

        // 校验状态参数合法性
        if (status != null && !("ACTIVE".equals(status) || "DEALED".equals(status) || "DORMANT".equals(status))) {
            throw new BusinessException(400, "状态只能是ACTIVE/DEALED/DORMANT");
        }

        // 校验客户类型参数合法性
        if (customerType != null && !("ORDINARY".equals(customerType) || "VIP".equals(customerType) || "INVEST".equals(customerType))) {
            throw new BusinessException(400, "客户类型只能是ORDINARY/VIP/INVEST");
        }

        Page<Customer> page = new Page<>(pageNum, pageSize);
        Page<Customer> resultPage = customerService.getCustomerPageByCondition(
                page, intendedRegionId, priceMin, priceMax, customerType, status);
        return ResultDTO.success(resultPage);
    }
}
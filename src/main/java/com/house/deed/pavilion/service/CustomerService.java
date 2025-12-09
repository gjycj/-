package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.Customer;
import java.util.List;

/**
 * <p>
 * 客户信息表（租户级数据隔离） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface CustomerService extends IService<Customer> {

    // ==================== 单条CRUD（增强租户校验） ====================
    /**
     * 新增客户（带租户校验）
     * @param customer 客户实体
     * @return 是否新增成功
     */
    boolean saveCustomer(Customer customer);

    /**
     * 更新客户信息（带租户校验）
     * @param customer 客户实体
     * @return 是否更新成功
     */
    boolean updateCustomerById(Customer customer);

    /**
     * 删除客户（带租户校验）
     * @param id 客户ID
     * @param tenantId 租户ID
     * @return 是否删除成功
     */
    boolean removeCustomerById(Long id, Long tenantId);

    /**
     * 按ID查询客户（带租户隔离）
     * @param id 客户ID
     * @param tenantId 租户ID
     * @return 客户实体
     */
    Customer getCustomerById(Long id, Long tenantId);

    // ==================== 多条件查询 ====================
    /**
     * 分页查询客户（多条件+租户隔离）
     * @param page 分页参数
     * @param query 查询条件（含租户ID）
     * @return 分页结果
     */
    IPage<Customer> pageQuery(Page<Customer> page, Customer query);

    /**
     * 多条件查询客户列表（租户隔离）
     * @param query 查询条件（含租户ID）
     * @return 客户列表
     */
    List<Customer> listByConditions(Customer query);

    /**
     * 按客户姓名模糊查询（租户隔离）
     * @param name 客户姓名
     * @param tenantId 租户ID
     * @return 客户列表
     */
    List<Customer> listByNameLike(String name, Long tenantId);

    // ==================== 批量操作 ====================
    /**
     * 批量新增客户（同一租户）
     * @param customerList 客户列表
     * @return 是否批量新增成功
     */
    boolean batchSaveCustomers(List<Customer> customerList);

    /**
     * 批量更新客户状态（租户隔离）
     * @param ids 客户ID列表
     * @param status 目标状态
     * @param tenantId 租户ID
     * @return 是否批量更新成功
     */
    boolean batchUpdateStatus(List<Long> ids, String status, Long tenantId);

    /**
     * 批量删除客户（租户隔离）
     * @param ids 客户ID列表
     * @param tenantId 租户ID
     * @return 是否批量删除成功
     */
    boolean batchRemoveCustomers(List<Long> ids, Long tenantId);
}
package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.Tenant;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 多租户核心信息表（租户隔离根表） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface TenantService extends IService<Tenant> {

    /**
     * 新增租户
     * @param tenant 租户实体
     * @return 是否成功
     */
    boolean saveTenant(Tenant tenant);

    /**
     * 根据ID查询租户
     * @param id 租户主键ID
     * @return 租户实体
     */
    Tenant getTenantById(Long id);

    /**
     * 根据租户编码编码查询租户
     * @param tenantCode 租户编码
     * @return 租户实体
     */
    Tenant getTenantByCode(String tenantCode);

    /**
     * 更新租户信息
     * @param tenant 租户实体
     * @return 是否成功
     */
    boolean updateTenant(Tenant tenant);

    /**
     * 根据ID删除租户
     * @param id 租户主键ID
     * @return 是否成功
     */
    boolean removeTenantById(Long id);

    /**
     * 多条件分页查询租户
     * @param page 分页参数
     * @param tenantName 租户名称
     * @param status 租户状态
     * @param expireTime 过期时间
     * @param createTimeStart 创建时间起始
     * @param createTimeEnd 创建时间结束
     * @return 分页结果
     */
    IPage<Tenant> pageTenants(Page<Tenant> page, String tenantName, Byte status,
                              LocalDateTime expireTime, LocalDateTime createTimeStart, LocalDateTime createTimeEnd);

    /**
     * 批量新增租户
     * @param tenants 租户列表
     * @return 是否成功
     */
    boolean batchSaveTenants(List<Tenant> tenants);

    /**
     * 批量更新租户状态
     * @param ids 租户ID列表
     * @param status 目标状态
     * @return 是否成功
     */
    boolean batchUpdateStatus(List<Long> ids, Byte status);

    /**
     * 批量删除租户
     * @param ids 租户ID列表
     * @return 是否成功
     */
    boolean batchRemoveTenants(List<Long> ids);
}
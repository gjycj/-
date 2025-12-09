package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.MaintenanceOrder;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 房源维修工单表（租户级数据） 服务类
 * </p>
 * 核心业务：贴合实体类设计的维修工单全生命周期管理（报修→派单→维修→验收→结算）
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface MaintenanceOrderService extends IService<MaintenanceOrder> {

    // ==================== 基础CRUD（租户隔离+实体约束校验） ====================
    /**
     * 新增维修工单（严格校验实体类约束：租户内工单编号唯一、场景化关联字段、枚举值等）
     * @param order 维修工单实体
     * @return 是否新增成功
     */
    boolean saveMaintenanceOrder(MaintenanceOrder order);

    /**
     * 根据ID更新维修工单（遵循状态流转规则，禁止修改核心关联字段）
     * @param order 维修工单实体
     * @return 是否更新成功
     */
    boolean updateMaintenanceOrderById(MaintenanceOrder order);

    /**
     * 根据ID删除维修工单（租户隔离+状态校验：仅已提交/已派单状态可删除）
     * @param id 工单ID
     * @param tenantId 租户ID
     * @return 是否删除成功
     */
    boolean removeMaintenanceOrderById(Long id, Long tenantId);

    /**
     * 根据ID查询维修工单（租户隔离）
     * @param id 工单ID
     * @param tenantId 租户ID
     * @return 维修工单实体
     */
    MaintenanceOrder getMaintenanceOrderById(Long id, Long tenantId);

    // ==================== 多条件查询（贴合实体类字段） ====================
    /**
     * 分页查询维修工单（多条件+租户隔离）
     * @param page 分页参数
     * @param queryParams 查询条件（houseId/status/maintenanceType/reporterType/时间范围等）
     * @param tenantId 租户ID
     * @return 分页结果
     */
    IPage<MaintenanceOrder> pageQuery(Page<MaintenanceOrder> page, Map<String, Object> queryParams, Long tenantId);

    /**
     * 多条件查询工单列表（租户隔离）
     * @param queryParams 查询条件
     * @param tenantId 租户ID
     * @return 工单列表
     */
    List<MaintenanceOrder> listByConditions(Map<String, Object> queryParams, Long tenantId);

    /**
     * 按房源ID查询维修工单（租户隔离，按创建时间倒序）
     * @param houseId 房源ID
     * @param tenantId 租户ID
     * @return 工单列表
     */
    List<MaintenanceOrder> listByHouseId(Long houseId, Long tenantId);

    /**
     * 按报修人ID+类型查询工单（租户隔离）
     * @param reporterId 报修人ID
     * @param reporterType 报修人类型（TENANT/LANDLORD/AGENT/OTHER）
     * @param tenantId 租户ID
     * @return 工单列表
     */
    List<MaintenanceOrder> listByReporter(Long reporterId, String reporterType, Long tenantId);

    // ==================== 批量操作（事务保证+实体约束） ====================
    /**
     * 批量新增维修工单（同一租户，校验工单编号唯一性+实体约束）
     * @param orderList 工单列表
     * @return 是否批量新增成功
     */
    boolean batchSaveMaintenanceOrders(List<MaintenanceOrder> orderList);

    /**
     * 批量更新工单状态（遵循流转规则，自动填充状态联动字段）
     * @param ids 工单ID列表
     * @param targetStatus 目标状态（SUBMITTED/ASSIGNED/REPAIRING/COMPLETED/CANCELED）
     * @param tenantId 租户ID
     * @return 是否批量更新成功
     */
    boolean batchUpdateOrderStatus(List<Long> ids, String targetStatus, Long tenantId);

    /**
     * 批量删除维修工单（仅已提交/已派单状态，租户隔离）
     * @param ids 工单ID列表
     * @param tenantId 租户ID
     * @return 是否批量删除成功
     */
    boolean batchRemoveMaintenanceOrders(List<Long> ids, Long tenantId);

    /**
     * 校验工单ID列表是否属于当前租户
     * @param tenantId 租户ID
     * @param orderIds 工单ID列表
     */
    void validateOrderIdsBelongToTenant(Long tenantId, List<Long> orderIds);
}
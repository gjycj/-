package com.house.deed.pavilion.module.maintenanceOrder.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.module.maintenanceOrder.entity.MaintenanceOrder;

/**
 * <p>
 * 房源维修工单表（租户级数据） 服务类
 * 负责维修工单的核心业务逻辑（创建、状态流转、权限校验等）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
public interface IMaintenanceOrderService extends IService<MaintenanceOrder> {

    /**
     * 创建维修工单
     * @param order 工单信息（含房源ID、报修人信息等）
     * @return 工单ID
     */
    Long createOrder(MaintenanceOrder order);

    /**
     * 查询工单详情（带租户权限校验）
     * @param id 工单ID
     * @return 工单信息（null表示无权限或不存在）
     */
    MaintenanceOrder getOrderById(Long id);

    /**
     * 更新工单状态及关联信息
     * @param updateInfo 包含ID和待更新字段的对象
     * @return 是否更新成功
     */
    boolean updateOrderStatus(MaintenanceOrder updateInfo);
}
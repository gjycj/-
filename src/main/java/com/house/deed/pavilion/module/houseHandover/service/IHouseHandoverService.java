package com.house.deed.pavilion.module.houseHandover.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.module.houseHandover.entity.HouseHandover;
import com.house.deed.pavilion.module.houseHandover.repository.HouseHandoverDTO;
import com.house.deed.pavilion.module.maintenanceOrder.entity.MaintenanceOrder;

import java.util.List;

/**
 * <p>
 * 房屋交接记录表（租户级数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
public interface IHouseHandoverService extends IService<HouseHandover> {

    /**
     * 查询交接记录关联的维修工单
     * @param handoverId 交接记录ID
     * @return 维修工单列表
     */
    List<MaintenanceOrder> getRelatedMaintenanceOrders(Long handoverId);

    /**
     * 创建房屋交接记录
     * @param dto 交接信息DTO
     * @return 交接记录ID
     */
    Long createHandover(HouseHandoverDTO dto);

    /**
     * 分页查询房源的交接记录
     * @param page 分页参数
     * @param houseId 房源ID
     * @return 分页结果
     */
    Page<HouseHandover> getHandoverPageByHouse(Page<HouseHandover> page, Long houseId);

    // 新增：按ID查询单个交接记录
    HouseHandover getById(Long id);

    // 新增：更新交接记录
    boolean updateHandover(Long id, HouseHandoverDTO dto);

    // 新增：删除交接记录
    boolean deleteHandover(Long id);

    // 新增：按合同ID查询交接记录
    List<HouseHandover> getByContractId(Long contractId);

    // 新增：校验交接记录是否存在且属于当前租户
    boolean existsByIdAndTenant(Long id);
}

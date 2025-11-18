package com.house.deed.pavilion.module.visitRecord.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.module.visitRecord.entity.VisitRecord;

import java.util.List;

/**
 * <p>
 * 带看记录表（租户级数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
public interface IVisitRecordService extends IService<VisitRecord> {
    // 新增：通过合同ID查询带看记录
    List<VisitRecord> getByContractId(Long contractId, Long tenantId);

    /**
     * 通过房源ID查询带看记录（带租户隔离）
     * @param houseId 房源ID
     * @param tenantId 租户ID
     * @return 带看记录列表
     */
    List<VisitRecord> getByHouseId(Long houseId, Long tenantId);

    List<VisitRecord> getByCustomerId(Long customerId, Long tenantId);

}

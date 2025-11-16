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
}

package com.house.deed.pavilion.module.agentPerformance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.module.agentPerformance.entity.AgentPerformance;
import com.house.deed.pavilion.module.visitRecord.entity.VisitRecord;

import java.math.BigDecimal;

/**
 * <p>
 * 经纪人业绩记录表（租户级数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
public interface IAgentPerformanceService extends IService<AgentPerformance> {

    /**
     * 从带看记录关联的合同生成业绩（带看转化签约时触发）
     * @param visitRecord 带看记录
     * @param contractId 关联的成交合同ID
     * @param dealAmount 合同成交金额（万元）
     * @param commissionAmount 佣金金额（元）
     */
    void createPerformanceFromVisit(VisitRecord visitRecord, Long contractId,
                                    BigDecimal dealAmount, BigDecimal commissionAmount);

    /**
     * 基于带看记录生成基础业绩（如有效带看奖励）
     * @param visitRecord 带看记录
     */
    void createBasePerformanceFromVisit(VisitRecord visitRecord);

}

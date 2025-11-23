package com.house.deed.pavilion.module.agentPerformance.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.module.agentPerformance.entity.AgentPerformance;
import com.house.deed.pavilion.module.agentPerformance.mapper.AgentPerformanceMapper;
import com.house.deed.pavilion.module.agentPerformance.service.IAgentPerformanceService;
import com.house.deed.pavilion.module.visitRecord.entity.VisitRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * <p>
 * 经纪人业绩记录表（租户级数据） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class AgentPerformanceServiceImpl extends ServiceImpl<AgentPerformanceMapper, AgentPerformance> implements IAgentPerformanceService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPerformanceFromVisit(VisitRecord visitRecord, Long contractId,
                                           BigDecimal dealAmount, BigDecimal commissionAmount) {
        // 1. 生成业绩月份（格式：yyyyMM，如202311）
        String performanceMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));

        // 2. 构建业绩记录（使用实际表结构字段）
        AgentPerformance performance = new AgentPerformance();
        performance.setTenantId(visitRecord.getTenantId()); // 租户ID
        performance.setAgentId(visitRecord.getAgentId()); // 带看经纪人ID
        performance.setContractId(contractId); // 关联成交合同ID
        performance.setPerformanceMonth(performanceMonth); // 业绩月份
        performance.setDealAmount(dealAmount); // 成交金额（万元）
        performance.setCommissionAmount(commissionAmount); // 佣金金额（元）
        performance.setPerformanceStatus("UNSETTLED"); // 初始状态：未结算
        performance.setCreateTime(LocalDateTime.now()); // 创建时间

        // 3. 保存业绩记录（校验唯一索引：同一经纪人+同一合同仅生成一次业绩）
        boolean saved = this.save(performance);
        if (!saved) {
            throw new RuntimeException("业绩记录创建失败，可能已存在该合同的业绩");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBasePerformanceFromVisit(VisitRecord visitRecord) {
        // 1. 生成业绩月份（格式：yyyyMM）
        String performanceMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));

        // 2. 构建基础业绩记录（合同ID可为空，需明确业绩状态为“带看基础业绩”）
        AgentPerformance performance = new AgentPerformance();
        performance.setTenantId(visitRecord.getTenantId()); // 租户ID
        performance.setAgentId(visitRecord.getAgentId()); // 带看经纪人ID
        performance.setContractId(null); // 带看时无合同，暂为空
        performance.setPerformanceMonth(performanceMonth); // 业绩月份
        performance.setDealAmount(BigDecimal.ZERO); // 带看无成交金额，设为0
        // 假设基础带看业绩佣金固定为100元（需根据业务调整）
        performance.setCommissionAmount(new BigDecimal("100"));
        performance.setPerformanceStatus("BASE_VISIT"); // 自定义状态：带看基础业绩
        performance.setCreateTime(LocalDateTime.now());

        // 3. 保存业绩记录（可添加唯一索引：同一带看记录不重复计业绩）
        boolean saved = this.save(performance);
        if (!saved) {
            throw new RuntimeException("带看基础业绩创建失败，可能已存在");
        }
    }



}

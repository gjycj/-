package com.house.deed.pavilion.mapper;

import com.house.deed.pavilion.entity.AgentPerformance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 经纪人业绩记录表（租户级数据） Mapper 接口
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface AgentPerformanceMapper extends BaseMapper<AgentPerformance> {

    /**
     * 按周期统计经纪人业绩总和
     * @param cycleType 周期类型（如MONTH/QUARTER/YEAR）
     * @param statisticDate 统计基准日期
     * @param tenantId 租户ID（数据隔离）
     * @return 包含agent_id和total_amount的Map列表
     */
    List<Map<String, Object>> sumPerformanceByCycle(String cycleType, LocalDate statisticDate, Long tenantId);
}
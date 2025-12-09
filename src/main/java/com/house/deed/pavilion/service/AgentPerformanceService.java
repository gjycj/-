package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.AgentPerformance;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 经纪人业绩记录表（租户级数据） 服务类
 * </p>
 * 核心业务说明：
 * 1. 业绩统计：记录经纪人在房产交易（买卖/租赁）中的业绩数据，支撑佣金结算、绩效考核；
 * 2. 租户隔离：所有操作强制关联tenantId，确保租户间业绩数据独立核算；
 * 3. 核心关联：
 *    - agent_id 关联经纪人表（同租户下的经纪人），标识业绩归属主体；
 *    - contract_id 关联合同表（同租户下的交易合同），业绩数据与交易合同强绑定；
 * 4. 统计维度：支持按经纪人、时间周期（日/月/季/年）、业绩类型（买卖/租赁）等多维度查询。
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface AgentPerformanceService extends IService<AgentPerformance> {

    /**
     * 多条件分页查询经纪人业绩记录（租户隔离）
     * @param page 分页参数（包含页码、每页条数）
     * @param queryParams 动态查询参数（支持agentId/performanceType/startDate/endDate/contractId等）
     * @param tenantId 租户ID（强制隔离字段，不可为空）
     * @return 分页结果集（包含当前页业绩数据及分页信息）
     */
    IPage<AgentPerformance> pageQuery(Page<AgentPerformance> page, Map<String, Object> queryParams, Long tenantId);

    /**
     * 根据经纪人ID查询业绩记录列表（租户隔离）
     * @param agentId 经纪人ID（关联agent表主键）
     * @param startTime 统计开始时间（含，格式：yyyy-MM-dd）
     * @param endTime 统计结束时间（含，格式：yyyy-MM-dd）
     * @param tenantId 租户ID（强制隔离字段，不可为空）
     * @return 该经纪人在指定时间范围内的业绩记录列表
     */
    List<AgentPerformance> listByAgentId(Long agentId, LocalDate startTime, LocalDate endTime, Long tenantId);

    /**
     * 按时间周期统计经纪人业绩总和（租户隔离）
     * @param cycleType 周期类型（DAY-日，MONTH-月，QUARTER-季，YEAR-年）
     * @param statisticDate 统计基准日期（格式：yyyy-MM-dd，如统计2025年11月则传2025-11-01）
     * @param tenantId 租户ID（强制隔离字段，不可为空）
     * @return 周期内所有经纪人的业绩总和（key：agentId，value：业绩总金额）
     */
    Map<Long, BigDecimal> sumByCycle(String cycleType, LocalDate statisticDate, Long tenantId);

    /**
     * 根据业绩类型查询记录（租户隔离）
     * @param performanceType 业绩类型（SALE-买卖业绩，RENT-租赁业绩）
     * @param tenantId 租户ID（强制隔离字段，不可为空）
     * @return 指定类型的业绩记录列表
     */
    List<AgentPerformance> listByType(String performanceType, Long tenantId);

    /**
     * 根据合同ID查询关联的业绩记录（租户隔离）
     * @param contractId 合同ID（关联contract表主键）
     * @param tenantId 租户ID（强制隔离字段，不可为空）
     * @return 合同对应的业绩记录（一个合同可能对应一条或多条业绩记录）
     */
    List<AgentPerformance> listByContractId(Long contractId, Long tenantId);

    /**
     * 添加单条经纪人业绩记录（租户隔离）
     * @param performance 业绩记录实体（需包含租户ID、经纪人ID、合同ID等核心字段）
     * @param tenantId 租户ID（强制隔离，需与实体中tenantId一致）
     * @return 是否添加成功
     */
    boolean saveAgentPerformance(AgentPerformance performance, Long tenantId);

    /**
     * 批量添加经纪人业绩记录（租户隔离）
     * @param performances 业绩记录列表（所有记录需属于同一租户）
     * @param tenantId 租户ID（强制隔离，需与所有实体中tenantId一致）
     * @return 是否批量添加成功（事务保证，全成功或全失败）
     */
    boolean batchSaveAgentPerformances(List<AgentPerformance> performances, Long tenantId);

    /**
     * 批量删除业绩记录（物理删除，租户隔离）
     * @param ids 业绩记录ID列表
     * @param tenantId 租户ID（强制校验，防止跨租户操作）
     * @return 是否删除成功
     */
    boolean batchRemove(List<Long> ids, Long tenantId);

    /**
     * 批量更新业绩状态（如批量标记为已结算）
     * @param ids 业绩记录ID列表
     * @param status 目标状态（UNSETTLED/SETTLED/CANCELED）
     * @param settleTime 结算时间（状态为SETTLED时必填）
     * @param tenantId 租户ID（强制校验）
     * @return 是否更新成功
     */
    boolean batchUpdateStatus(List<Long> ids, String status, LocalDateTime settleTime, Long tenantId);

}
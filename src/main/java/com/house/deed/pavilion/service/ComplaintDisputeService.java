package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.ComplaintDispute;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 投诉与纠纷记录表（租户级数据） 服务类
 * 核心业务：投诉纠纷的全生命周期管理，包括新增、查询、状态更新、批量操作及流程追溯
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface ComplaintDisputeService extends IService<ComplaintDispute> {

    /**
     * 新增投诉纠纷记录
     * 业务说明：生成唯一纠纷编号，校验租户内编号唯一性，自动填充创建时间
     *
     * @param dispute 投诉纠纷实体（包含租户ID、投诉人信息等核心字段）
     * @return 新增是否成功
     */
    boolean saveComplaintDispute(ComplaintDispute dispute);

    /**
     * 根据ID更新投诉纠纷记录
     * 业务说明：支持部分字段更新（如状态、处理人），校验租户权限，禁止修改创建人及编号
     *
     * @param dispute 投诉纠纷实体（必须包含ID和租户ID）
     * @return 更新是否成功
     */
    boolean updateComplaintDisputeById(ComplaintDispute dispute);

    /**
     * 根据ID删除投诉纠纷记录
     * 业务说明：物理删除，需校验租户权限，已处理的纠纷不允许删除
     *
     * @param id       记录ID
     * @param tenantId 租户ID（数据隔离校验）
     * @return 删除是否成功
     */
    boolean removeComplaintDisputeById(Long id, Long tenantId);

    /**
     * 根据ID查询投诉纠纷详情
     * 业务说明：仅返回当前租户的记录，包含完整处理轨迹关联信息
     *
     * @param id       记录ID
     * @param tenantId 租户ID（数据隔离）
     * @return 投诉纠纷实体（null表示不存在或无权限）
     */
    ComplaintDispute getComplaintDisputeById(Long id, Long tenantId);

    /**
     * 多条件分页查询投诉纠纷
     * 业务说明：支持按纠纷类型、状态、投诉人类型、时间范围等筛选，仅查询当前租户数据
     *
     * @param page     分页参数（页码、每页条数）
     * @param queryMap 查询条件（键值对，支持：disputeType、status、complainantType、startTime、endTime等）
     * @param tenantId 租户ID（强制筛选）
     * @return 分页结果（含总条数、当前页数据）
     */
    IPage<ComplaintDispute> pageQuery(Page<ComplaintDispute> page, Map<String, Object> queryMap, Long tenantId);

    /**
     * 多条件查询投诉纠纷列表
     * 业务说明：支持按关联合同ID、处理人等条件批量筛选，用于导出或关联查询
     *
     * @param queryMap 查询条件（键值对）
     * @param tenantId 租户ID（数据隔离）
     * @return 符合条件的记录列表
     */
    List<ComplaintDispute> listByConditions(Map<String, Object> queryMap, Long tenantId);

    /**
     * 批量更新纠纷状态
     * 业务说明：用于批量处理状态流转（如批量标记为已解决），需校验租户权限及状态合法性
     *
     * @param ids      记录ID列表
     * @param status   目标状态（ACCEPTED/PROCESSING/RESOLVED/CANCELED）
     * @param handlerId 处理人ID（操作人）
     * @param tenantId 租户ID（数据隔离）
     * @return 批量更新是否成功
     */
    boolean batchUpdateStatus(List<Long> ids, String status, Long handlerId, Long tenantId);

    /**
     * 批量删除投诉纠纷记录
     * 业务说明：物理删除，需校验租户权限，仅允许删除未处理的纠纷（状态为ACCEPTED）
     *
     * @param ids      记录ID列表
     * @param tenantId 租户ID（数据隔离）
     * @return 批量删除是否成功
     */
    boolean batchRemoveComplaintDisputes(List<Long> ids, Long tenantId);

    /**
     * 校验纠纷ID列表是否均属于当前租户
     * @param tenantId 当前租户ID
     * @param disputeIds 纠纷ID列表
     * @throws IllegalArgumentException 当存在不属于当前租户的ID时抛出
     */
    void validateDisputeIdsBelongToTenant(Long tenantId, List<Long> disputeIds);
}
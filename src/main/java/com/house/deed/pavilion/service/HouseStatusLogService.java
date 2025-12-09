package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.HouseStatusLog;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 房源状态变更日志表（租户级数据） 服务接口
 * </p>
 * <p>
 * 负责房源状态变更日志的全生命周期管理，包括状态变更记录的创建、查询、更新、删除等功能。
 * 房源状态变更日志是房源生命周期管理的关键组件，记录房源状态流转的完整轨迹，支持状态变更的审计追溯和分析。
 * 所有操作均强制进行租户数据隔离校验，确保数据安全性和业务完整性。
 * </p>
 * <p>
 * 核心业务功能：
 * 1. 状态变更记录管理：记录房源状态变更的完整信息，包括变更前后状态、变更原因、操作人等
 * 2. 多维度查询分析：支持按房源、状态、时间等多维度查询状态变更历史
 * 3. 批量操作支持：提供批量创建、删除等操作，提升管理效率
 * 4. 审计追溯保障：确保状态变更记录的完整性和不可篡改性
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface HouseStatusLogService extends IService<HouseStatusLog> {

    // ==================== 基础CRUD操作 ====================

    /**
     * 创建房源状态变更日志记录
     *
     * @param log 状态变更日志实体对象，包含状态变更前后信息、原因、操作人等
     * @return boolean 创建成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败时抛出
     *
     * 核心字段要求：
     * 1. 租户ID、房源ID、变更前状态、变更后状态不能为空
     * 2. 状态变更原因必须明确，便于后续追溯
     * 3. 操作人信息必须完整，确保责任可追溯
     *
     * 业务约束：
     * 1. 变更前后状态不能相同，确保状态变更有意义
     * 2. 状态变更必须符合预定义的状态流转规则
     * 3. 状态变更时间应准确反映实际变更发生时间
     *
     * 使用场景：
     * 1. 房源状态变更时的自动记录
     * 2. 手动添加状态变更历史记录
     * 3. 数据迁移或系统初始化时的状态变更记录创建
     */
    boolean saveStatusLog(HouseStatusLog log);

    /**
     * 更新房源状态变更日志记录
     *
     * @param log 更新后的状态变更日志实体对象
     * @return boolean 更新成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败时抛出
     * @throws IllegalStateException 当记录不存在或状态不允许修改时抛出
     *
     * 更新规则：
     * 1. 核心审计字段（变更前后状态、房源ID、操作人、变更时间）通常不允许修改
     * 2. 仅允许补充或修正非核心信息，如变更原因的详细说明
     * 3. 状态变更记录一旦创建，核心审计信息应保持不可篡改性
     *
     * 注意事项：
     * 1. 状态变更记录具有审计性质，核心字段修改需谨慎
     * 2. 建议记录更新操作人和更新原因用于二次审计
     * 3. 更新操作应保留历史版本或记录修改痕迹
     */
    boolean updateStatusLogById(HouseStatusLog log);

    /**
     * 删除房源状态变更日志记录
     *
     * @param id 状态变更日志记录的唯一标识
     * @param tenantId 租户ID，用于权限校验
     * @return boolean 删除成功返回true，失败返回false
     * @throws IllegalArgumentException 当ID或租户ID为空时抛出
     * @throws IllegalStateException 当记录不存在或权限不足时抛出
     *
     * 删除策略：
     * 1. 状态变更日志通常用于审计目的，建议谨慎删除
     * 2. 删除前需确认日志记录无业务关联或合规要求
     * 3. 建议记录删除操作人和删除原因用于审计
     *
     * 安全机制：
     * 1. 强制租户ID校验，防止跨租户删除
     * 2. 删除前验证记录归属，确保权限控制
     */
    boolean removeStatusLogById(Long id, Long tenantId);

    /**
     * 根据ID查询状态变更日志记录（租户隔离）
     *
     * @param id 状态变更日志记录的唯一标识
     * @param tenantId 租户ID，用于数据隔离
     * @return HouseStatusLog 状态变更日志实体对象，不存在时返回null
     * @throws IllegalArgumentException 当ID或租户ID为空时抛出
     *
     * 说明：
     * 1. 强制添加租户ID条件，确保租户只能访问自己的数据
     * 2. 返回的记录包含完整的状态变更信息和关联数据
     * 3. 用于状态变更详情查看和审计追溯
     */
    HouseStatusLog getStatusLogById(Long id, Long tenantId);

    // ==================== 多条件查询操作 ====================

    /**
     * 多条件分页查询状态变更日志记录
     *
     * @param page 分页参数对象，包含页码、每页大小等信息
     * @param queryParams 查询参数Map，支持灵活的条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return IPage<HouseStatusLog> 分页结果对象，包含数据列表和分页信息
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 支持的查询条件：
     * 1. houseId: 房源ID（精确匹配）
     * 2. statusBefore: 变更前状态（精确匹配）
     * 3. statusAfter: 变更后状态（精确匹配）
     * 4. operatorId: 操作人ID（精确匹配）
     * 5. operatorName: 操作人姓名（模糊匹配）
     * 6. changeReason: 变更原因（模糊匹配）
     * 7. startTime/endTime: 变更时间范围查询
     *
     * 默认排序：按变更时间倒序排列（最新变更记录在前）
     */
    IPage<HouseStatusLog> pageQuery(Page<HouseStatusLog> page, Map<String, Object> queryParams, Long tenantId);

    /**
     * 多条件列表查询状态变更日志记录
     *
     * @param queryParams 查询条件Map，支持灵活的条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return List<HouseStatusLog> 符合条件的状态变更日志列表
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 常用查询参数：
     * - houseId: 房源ID（精确匹配）
     * - statusBefore: 变更前状态（精确匹配）
     * - statusAfter: 变更后状态（精确匹配）
     * - operatorId: 操作人ID（精确匹配）
     * - operatorName: 操作人姓名（模糊匹配）
     * - changeReason: 变更原因（模糊匹配）
     * - startTime/endTime: 变更时间范围
     *
     * 使用场景：
     * 1. 导出状态变更日志报表
     * 2. 统计分析状态变更数据
     * 3. 批量处理状态变更记录
     */
    List<HouseStatusLog> listByConditions(Map<String, Object> queryParams, Long tenantId);

    /**
     * 根据房源ID查询状态变更日志历史
     *
     * @param houseId 房源ID
     * @param tenantId 租户ID，用于数据隔离
     * @return List<HouseStatusLog> 该房源的所有状态变更日志列表，按变更时间倒序排列
     * @throws IllegalArgumentException 当房源ID或租户ID为空时抛出
     *
     * 业务用途：
     * 1. 查看房源的状态变更完整历史
     * 2. 分析房源的状态流转路径和规律
     * 3. 状态变更的审计追溯和问题排查
     *
     * 返回说明：
     * 1. 返回列表包含该房源的所有状态变更记录
     * 2. 按变更时间倒序排列，最新变更在前
     * 3. 每条记录包含完整的变更信息和操作人信息
     */
    List<HouseStatusLog> listByHouseId(Long houseId, Long tenantId);

    // ==================== 批量操作接口 ====================

    /**
     * 批量创建状态变更日志记录
     *
     * @param logList 状态变更日志记录列表
     * @return boolean 批量创建成功返回true，失败返回false（事务回滚）
     * @throws IllegalArgumentException 当参数为空或记录格式无效时抛出
     *
     * 使用场景：
     * 1. 批量导入历史状态变更数据
     * 2. 批量处理多个房源的状态变更记录
     * 3. 数据迁移时的批量创建
     *
     * 约束条件：
     * 1. 批量记录需属于同一租户（租户ID一致）
     * 2. 每个记录必须包含必填字段
     * 3. 房源ID必须在当前租户下存在
     * 4. 操作人ID必须为有效用户ID
     *
     * 事务保障：批量操作使用事务，确保全部成功或全部回滚
     */
    boolean batchSaveStatusLogs(List<HouseStatusLog> logList);

    /**
     * 批量删除状态变更日志记录
     *
     * @param ids 状态变更日志记录ID列表
     * @param tenantId 租户ID，用于权限校验
     * @return boolean 批量删除成功返回true，失败返回false（事务回滚）
     * @throws IllegalArgumentException 当参数为空或存在跨租户记录时抛出
     *
     * 安全机制：
     * 1. 强制租户ID校验，防止跨租户删除
     * 2. 批量删除前验证所有记录属于当前租户
     * 3. 删除操作需考虑审计要求和合规性
     *
     * 注意事项：
     * 1. 批量删除前建议先备份数据
     * 2. 状态变更日志具有审计价值，建议保留必要历史记录
     * 3. 记录删除操作日志用于审计
     */
    boolean batchRemoveStatusLogs(List<Long> ids, Long tenantId);
}
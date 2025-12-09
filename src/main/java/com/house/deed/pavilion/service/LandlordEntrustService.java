package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.LandlordEntrust;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 房东委托信息表（租户级数据） 服务接口
 * </p>
 * <p>
 * 负责房东委托信息的全生命周期管理，包括委托关系的创建、查询、更新、删除等功能。
 * 房东委托信息是房东与房屋管理服务之间的核心约定，记录委托授权、服务范围、佣金标准等关键信息。
 * 所有操作均强制进行租户数据隔离校验，确保数据安全性和业务完整性。
 * </p>
 * <p>
 * 核心业务功能：
 * 1. 委托关系管理：房东委托房屋管理服务的授权记录管理
 * 2. 多维度查询分析：支持按房东、房屋、状态等多维度查询委托信息
 * 3. 批量操作支持：提供批量创建、更新、删除等操作，提升管理效率
 * 4. 状态流转控制：支持委托状态（待确认、生效中、已终止等）的流转管理
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface LandlordEntrustService extends IService<LandlordEntrust> {

    // ==================== 基础CRUD操作 ====================

    /**
     * 创建房东委托信息记录
     *
     * @param entity 委托信息实体对象，包含委托双方、委托内容、期限、佣金等信息
     * @return boolean 创建成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败时抛出
     *
     * 核心字段要求：
     * 1. 租户ID、房东ID、房屋ID、委托类型不能为空
     * 2. 委托开始时间和结束时间必须合理且形成有效期间
     * 3. 佣金比例或金额必须明确且符合业务规则
     *
     * 业务约束：
     * 1. 同一房东对同一房屋在同一时间段的委托不能重叠
     * 2. 委托期限不能超过法律规定的最大期限
     * 3. 佣金标准必须在合理范围内
     *
     * 使用场景：
     * 1. 房东新签订房屋委托管理协议
     * 2. 续签或变更已有的委托协议
     * 3. 系统自动生成标准委托记录
     */
    boolean saveEntrust(LandlordEntrust entity);

    /**
     * 根据ID查询委托信息记录（租户隔离）
     *
     * @param id 委托记录的唯一标识
     * @param tenantId 租户ID，用于数据隔离
     * @return LandlordEntrust 委托信息实体对象，不存在时返回null
     * @throws IllegalArgumentException 当ID或租户ID为空时抛出
     *
     * 说明：
     * 1. 强制添加租户ID条件，确保租户只能访问自己的数据
     * 2. 返回的记录包含完整的委托信息和关联数据
     * 3. 用于委托详情查看、合同打印和修改前数据加载
     */
    LandlordEntrust getById(Long id, Long tenantId);

    /**
     * 更新委托信息记录
     *
     * @param entity 更新后的委托信息实体对象
     * @return boolean 更新成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败时抛出
     * @throws IllegalStateException 当记录不存在或状态不允许修改时抛出
     *
     * 更新规则：
     * 1. 已生效的委托记录核心信息（委托期限、佣金等）修改需重新审批
     * 2. 已终止的委托记录不允许修改
     * 3. 核心关联字段（房东ID、房屋ID）通常不允许变更
     *
     * 使用场景：
     * 1. 修正委托记录中的错误信息
     * 2. 更新委托状态和生效信息
     * 3. 补充委托执行过程中的相关信息
     */
    boolean updateEntrust(LandlordEntrust entity);

    /**
     * 删除委托信息记录
     *
     * @param id 委托记录的唯一标识
     * @param tenantId 租户ID，用于权限校验
     * @return boolean 删除成功返回true，失败返回false
     * @throws IllegalArgumentException 当ID或租户ID为空时抛出
     * @throws IllegalStateException 当记录不存在或权限不足时抛出
     *
     * 删除策略：
     * 1. 已生效的委托记录不允许直接删除，建议使用终止操作
     * 2. 删除前需校验关联的业务单据状态
     * 3. 建议记录删除操作人和删除原因用于审计
     */
    boolean removeEntrust(Long id, Long tenantId);

    // ==================== 多条件查询操作 ====================

    /**
     * 多条件分页查询委托信息记录
     *
     * @param page 分页参数对象，包含页码、每页大小等信息
     * @param query 查询条件实体对象，支持实体字段条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return IPage<LandlordEntrust> 分页结果对象，包含数据列表和分页信息
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 支持查询条件（基于实体字段）：
     * 1. landlordId: 房东ID（精确匹配）
     * 2. houseId: 房屋ID（精确匹配）
     * 3. entrustType: 委托类型（精确匹配：独家委托/非独家委托）
     * 4. entrustStatus: 委托状态（精确匹配：待确认/生效中/已终止）
     * 5. startTime/endTime: 委托时间范围
     * 6. commissionRate: 佣金比例范围
     *
     * 默认排序：按创建时间倒序排列（最新委托记录在前）
     */
    IPage<LandlordEntrust> pageQuery(Page<LandlordEntrust> page, LandlordEntrust query, Long tenantId);

    /**
     * 多条件列表查询委托信息记录
     *
     * @param queryParams 查询条件Map，支持灵活的条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return List<LandlordEntrust> 符合条件的委托信息列表
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 常用查询参数：
     * - landlordId: 房东ID（精确匹配）
     * - houseId: 房屋ID（精确匹配）
     * - entrustType: 委托类型（精确匹配）
     * - entrustStatus: 委托状态（精确匹配）
     * - minStartTime/maxStartTime: 委托开始时间范围
     * - minEndTime/maxEndTime: 委托结束时间范围
     * - minCommissionRate/maxCommissionRate: 佣金比例范围
     *
     * 使用场景：
     * 1. 导出委托信息报表
     * 2. 统计分析委托数据
     * 3. 批量处理委托记录
     */
    List<LandlordEntrust> listByConditions(Map<String, Object> queryParams, Long tenantId);

    /**
     * 根据房屋ID查询委托信息记录
     *
     * @param houseId 房屋ID
     * @param tenantId 租户ID，用于数据隔离
     * @return List<LandlordEntrust> 该房屋的所有委托记录列表，按创建时间倒序排列
     * @throws IllegalArgumentException 当房屋ID或租户ID为空时抛出
     *
     * 业务用途：
     * 1. 查看房屋的历史委托记录
     * 2. 分析房屋的委托管理情况
     * 3. 为房屋续签委托提供历史参考
     *
     * 返回说明：
     * 1. 返回列表包含该房屋的所有委托记录（包括历史记录）
     * 2. 按创建时间倒序排列，最新委托在前
     * 3. 包含各种状态的记录（待确认、生效中、已终止）
     */
    List<LandlordEntrust> listByHouseId(Long houseId, Long tenantId);

    /**
     * 根据房东ID查询委托信息记录
     *
     * @param landlordId 房东ID
     * @param tenantId 租户ID，用于数据隔离
     * @return List<LandlordEntrust> 该房东的所有委托记录列表，按创建时间倒序排列
     * @throws IllegalArgumentException 当房东ID或租户ID为空时抛出
     *
     * 业务用途：
     * 1. 查看房东的所有委托房屋
     * 2. 分析房东的委托合作情况
     * 3. 房东委托服务的统计和管理
     *
     * 返回说明：
     * 1. 返回列表包含该房东的所有委托记录
     * 2. 按创建时间倒序排列，最新委托在前
     * 3. 可用于房东委托服务的综合管理
     */
    List<LandlordEntrust> listByLandlordId(Long landlordId, Long tenantId);

    // ==================== 批量操作接口 ====================

    /**
     * 批量创建委托信息记录
     *
     * @param entrustList 委托信息记录列表
     * @return boolean 批量创建成功返回true，失败返回false（事务回滚）
     * @throws IllegalArgumentException 当参数为空或记录格式无效时抛出
     *
     * 使用场景：
     * 1. 批量导入历史委托数据
     * 2. 批量创建相似房东的委托记录
     * 3. 数据迁移时的批量创建
     *
     * 约束条件：
     * 1. 批量记录需属于同一租户
     * 2. 每个记录必须包含必填字段
     * 3. 房东ID和房屋ID必须在当前租户下存在有效数据
     * 4. 委托期限必须合理且无冲突
     *
     * 事务保障：批量操作使用事务，确保全部成功或全部回滚
     */
    boolean batchCreate(List<LandlordEntrust> entrustList);

    /**
     * 批量删除委托信息记录
     *
     * @param ids 委托信息记录ID列表
     * @param tenantId 租户ID，用于权限校验
     * @return boolean 批量删除成功返回true，失败返回false（事务回滚）
     * @throws IllegalArgumentException 当参数为空或存在跨租户记录时抛出
     *
     * 安全机制：
     * 1. 强制租户ID校验，防止跨租户删除
     * 2. 批量删除前验证所有记录属于当前租户
     * 3. 仅允许删除特定状态的记录（如待确认状态）
     *
     * 注意事项：
     * 1. 批量删除前建议先备份数据
     * 2. 已生效的委托记录不建议批量删除
     * 3. 记录删除操作日志用于审计
     */
    boolean batchRemove(List<Long> ids, Long tenantId);

    /**
     * 批量更新委托信息状态
     *
     * @param ids 委托信息记录ID列表
     * @param status 目标状态值（Byte类型，与数据库字段类型匹配）
     * @param tenantId 租户ID，用于权限校验
     * @return boolean 批量更新成功返回true，失败返回false（事务回滚）
     * @throws IllegalArgumentException 当参数为空或状态值无效时抛出
     *
     * 状态流转规则：
     * 1. 待确认 → 生效中：委托协议确认生效
     * 2. 生效中 → 已终止：委托提前终止或到期终止
     * 3. 待确认 → 已终止：取消待确认的委托
     * 4. 已终止 → 生效中：恢复已终止的委托（需特殊权限）
     *
     * 使用场景：
     * 1. 批量确认多个委托记录生效
     * 2. 批量终止到期的委托记录
     * 3. 批量作废无效的委托记录
     */
    boolean batchUpdateStatus(List<Long> ids, Byte status, Long tenantId);
}
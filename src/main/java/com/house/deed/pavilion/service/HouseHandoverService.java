package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.HouseHandover;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 房屋交接记录表（租户级数据） 服务类
 * </p>
 * <p>
 * 负责房屋交接过程的全生命周期管理，包括交接记录的创建、查询、更新和删除等功能。
 * 房屋交接是租赁或买卖过程中的关键环节，记录包括房屋状况检查、物品清点、钥匙交接等信息。
 * 所有操作均强制进行租户数据隔离校验，确保数据安全性和业务完整性。
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
public interface HouseHandoverService extends IService<HouseHandover> {

    // ==================== 基础CRUD操作 ====================

    /**
     * 创建房屋交接记录
     *
     * @param entity 房屋交接实体对象，包含交接相关的所有信息
     * @return boolean 创建成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败时抛出
     *
     * 核心字段要求：
     * 1. 租户ID、房屋ID、交接类型不能为空
     * 2. 交接日期不能为空且不能晚于当前日期
     * 3. 交接人（甲方、乙方）信息必须完整
     *
     * 业务场景：
     * 1. 租赁合同到期时的房屋交还
     * 2. 买卖交易完成时的房屋交付
     * 3. 物业管理权移交
     */
    boolean saveHandover(HouseHandover entity);

    /**
     * 根据ID查询交接记录（租户隔离）
     *
     * @param id 交接记录的唯一标识
     * @param tenantId 租户ID，用于数据隔离
     * @return HouseHandover 房屋交接实体对象，不存在时返回null
     * @throws IllegalArgumentException 当ID或租户ID为空时抛出
     *
     * 说明：
     * 1. 强制添加租户ID条件，确保租户只能访问自己的数据
     * 2. 返回的记录包含完整的交接信息和附件列表
     * 3. 用于交接详情查看和修改前的数据加载
     */
    HouseHandover getById(Long id, Long tenantId);

    /**
     * 更新房屋交接记录
     *
     * @param entity 更新后的房屋交接实体对象
     * @return boolean 更新成功返回true，失败返回false
     * @throws IllegalArgumentException 当参数校验失败时抛出
     * @throws IllegalStateException 当记录不存在或状态不允许修改时抛出
     *
     * 更新限制：
     * 1. 已确认完成的交接记录不允许修改
     * 2. 核心信息（房屋ID、交接类型）通常不允许变更
     * 3. 附件信息可以追加，但历史附件需要保留
     *
     * 使用场景：
     * 1. 修正交接记录中的错误信息
     * 2. 补充交接过程中的遗漏信息
     * 3. 更新交接状态和确认信息
     */
    boolean updateHandover(HouseHandover entity);

    /**
     * 删除交接记录
     *
     * @param id 交接记录ID
     * @param tenantId 租户ID，用于权限校验
     * @return boolean 删除成功返回true，失败返回false
     * @throws IllegalArgumentException 当ID或租户ID为空时抛出
     * @throws IllegalStateException 当记录不存在或权限不足时抛出
     *
     * 删除策略：
     * 1. 已确认的交接记录通常不允许删除，建议使用作废操作
     * 2. 删除前需校验关联的业务单据状态
     * 3. 建议记录删除操作人和删除原因用于审计
     */
    boolean removeHandover(Long id, Long tenantId);

    // ==================== 多条件查询操作 ====================

    /**
     * 多条件分页查询交接记录
     *
     * @param page 分页参数对象，包含页码、每页大小等信息
     * @param query 查询条件实体对象
     * @param tenantId 租户ID，用于数据隔离
     * @return IPage<HouseHandover> 分页结果对象，包含数据列表和分页信息
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 支持查询条件：
     * 1. 房屋ID：精确匹配指定房屋的交接记录
     * 2. 交接类型：精确匹配（租赁交接/买卖交接等）
     * 3. 交接状态：精确匹配（待确认/已完成/已作废）
     * 4. 交接时间范围：按交接日期区间查询
     * 5. 交接人信息：支持甲方、乙方姓名模糊查询
     *
     * 默认排序：按交接日期倒序排列（最新交接在前）
     */
    IPage<HouseHandover> pageQuery(Page<HouseHandover> page, HouseHandover query, Long tenantId);

    /**
     * 多条件列表查询交接记录
     *
     * @param queryParams 查询条件Map，支持灵活的条件组合
     * @param tenantId 租户ID，用于数据隔离
     * @return List<HouseHandover> 符合条件的交接记录列表
     * @throws IllegalArgumentException 当租户ID为空时抛出
     *
     * 常用查询参数：
     * - houseId: 房屋ID（精确匹配）
     * - handoverType: 交接类型（精确匹配）
     * - handoverStatus: 交接状态（精确匹配）
     * - startDate/endDate: 交接日期范围
     * - partyAName/partyBName: 甲方/乙方姓名（模糊匹配）
     *
     * 使用场景：
     * 1. 导出交接记录报表
     * 2. 统计分析交接数据
     * 3. 批量处理交接记录
     */
    List<HouseHandover> listByConditions(Map<String, Object> queryParams, Long tenantId);

    /**
     * 根据房屋ID查询交接记录历史
     *
     * @param houseId 房屋ID
     * @param tenantId 租户ID，用于数据隔离
     * @return List<HouseHandover> 该房屋的所有交接记录列表，按交接日期倒序排列
     * @throws IllegalArgumentException 当房屋ID或租户ID为空时抛出
     *
     * 业务用途：
     * 1. 查看房屋的历史交接记录
     * 2. 分析房屋的流转情况
     * 3. 为新交接提供历史参考
     *
     * 返回说明：
     * 1. 返回列表包含该房屋的所有交接记录
     * 2. 按交接日期倒序排列，最新交接在前
     * 3. 包含各种状态的记录（待确认、已完成、已作废）
     */
    List<HouseHandover> listByHouseId(Long houseId, Long tenantId);

    // ==================== 批量操作接口 ====================

    /**
     * 批量创建交接记录
     *
     * @param handoverList 交接记录列表
     * @return boolean 批量创建成功返回true，失败返回false（事务回滚）
     * @throws IllegalArgumentException 当参数为空或记录格式无效时抛出
     *
     * 使用场景：
     * 1. 批量导入历史交接数据
     * 2. 批量处理多套房屋的交接
     * 3. 数据迁移时的批量创建
     *
     * 约束条件：
     * 1. 批量记录需属于同一租户
     * 2. 每个记录必须包含必填字段
     * 3. 房屋ID必须在当前租户下存在
     *
     * 事务保障：批量操作使用事务，确保全部成功或全部回滚
     */
    boolean batchCreate(List<HouseHandover> handoverList);

    /**
     * 批量删除交接记录
     *
     * @param ids 交接记录ID列表
     * @param tenantId 租户ID，用于权限校验
     * @return boolean 批量删除成功返回true，失败返回false（事务回滚）
     * @throws IllegalArgumentException 当参数为空或存在跨租户记录时抛出
     *
     * 安全机制：
     * 1. 强制租户ID校验，防止跨租户删除
     * 2. 批量删除前验证所有记录属于当前租户
     * 3. 仅允许删除特定状态的记录（如草稿状态）
     *
     * 注意事项：
     * 1. 批量删除前建议先备份数据
     * 2. 已确认的交接记录通常不允许删除
     * 3. 记录删除操作日志用于审计
     */
    boolean batchRemove(List<Long> ids, Long tenantId);

    /**
     * 批量更新交接记录状态
     *
     * @param ids 交接记录ID列表
     * @param status 目标状态值
     * @param tenantId 租户ID，用于权限校验
     * @return boolean 批量更新成功返回true，失败返回false（事务回滚）
     * @throws IllegalArgumentException 当参数为空或状态值无效时抛出
     *
     * 状态流转规则：
     * 1. 待确认 → 已完成：确认交接完成
     * 2. 待确认 → 已作废：取消交接操作
     * 3. 已完成 → 已作废：特殊情况下撤销确认（需权限）
     * 4. 已作废 → 待确认：恢复作废的交接记录
     *
     * 使用场景：
     * 1. 批量确认多个交接记录完成
     * 2. 批量作废无效的交接记录
     * 3. 批量恢复误操作的记录
     */
    boolean batchUpdateStatus(List<Long> ids, String status, Long tenantId);
}
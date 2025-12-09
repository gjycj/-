package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.VisitRecord;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 带看记录表（租户级数据）服务接口
 *
 * <p>管理带看记录的核心业务操作，为房产经纪业务提供带看记录的全生命周期管理。
 * 所有操作均遵循租户数据隔离原则，确保不同租户间数据完全隔离。</p>
 *
 * <p><b>核心业务特性：</b></p>
 * <ul>
 *   <li><b>租户数据隔离</b>：所有操作均需指定租户ID，确保数据安全</li>
 *   <li><b>多维度查询</b>：支持房源、客户、经纪人等多维度筛选</li>
 *   <li><b>时间范围查询</b>：支持带看时间范围筛选，便于统计分析</li>
 *   <li><b>批量操作支持</b>：提供批量增删改功能，提升处理效率</li>
 *   <li><b>分页查询优化</b>：支持大数据量下的分页查询</li>
 * </ul>
 *
 * <p><b>业务实体关系：</b></p>
 * <ul>
 *   <li>关联房源（house_id）</li>
 *   <li>关联客户（customer_id）</li>
 *   <li>关联经纪人（agent_id）</li>
 *   <li>所属租户（tenant_id）</li>
 * </ul>
 *
 * @author yuquanxi
 * @since 2025-11-26
 * @version 1.0.0
 */
public interface VisitRecordService extends IService<VisitRecord> {

    /**
     * 新增带看记录
     * <p>创建新的带看记录，操作前会自动校验租户权限和关联实体存在性</p>
     *
     * @param visitRecord 带看记录实体对象，必须包含租户ID、房源ID、客户ID等必填字段
     * @return boolean 操作结果，true-成功 false-失败
     * @throws IllegalArgumentException 当必填字段缺失或关联实体不存在时抛出异常
     *
     * <p><b>必填字段校验：</b></p>
     * <ul>
     *   <li>tenantId（租户ID）- 数据隔离依据</li>
     *   <li>houseId（房源ID）- 带看目标房源</li>
     *   <li>customerId（客户ID）- 带看客户</li>
     *   <li>agentId（经纪人ID）- 负责经纪人</li>
     *   <li>visitTime（带看时间）- 带看发生时间</li>
     * </ul>
     */
    boolean saveVisitRecord(VisitRecord visitRecord);

    /**
     * 根据ID查询带看记录（租户隔离）
     * <p>在指定租户下根据主键ID精确查询带看记录，确保数据隔离安全</p>
     *
     * @param id 带看记录主键ID，不能为null
     * @param tenantId 租户ID，用于数据隔离校验，不能为null
     * @return VisitRecord 带看记录实体，未找到或租户不匹配时返回null
     *
     * <p><b>安全机制：</b></p>
     * <ul>
     *   <li>双重校验：ID存在性 + 租户匹配性</li>
     *   <li>数据隔离：仅返回属于指定租户的记录</li>
     *   <li>权限控制：防止跨租户数据访问</li>
     * </ul>
     */
    VisitRecord getByIdWithTenant(Long id, Long tenantId);

    /**
     * 更新带看记录
     * <p>更新现有带看记录信息，操作前会校验租户权限和记录存在性</p>
     *
     * @param visitRecord 待更新的带看记录实体，必须包含ID和租户ID
     * @return boolean 操作结果，true-成功 false-失败
     * @throws IllegalArgumentException 当记录不存在或租户不匹配时抛出异常
     *
     * <p><b>更新限制：</b></p>
     * <table border="1">
     *   <tr><th>字段</th><th>是否可更新</th><th>说明</th></tr>
     *   <tr><td>id</td><td>❌ 不可更新</td><td>记录唯一标识</td></tr>
     *   <tr><td>tenantId</td><td>❌ 不可更新</td><td>租户隔离标识</td></tr>
     *   <tr><td>visitNotes</td><td>✅ 可更新</td><td>带看备注</td></tr>
     *   <tr><td>intentionLevel</td><td>✅ 可更新</td><td>意向等级</td></tr>
     *   <tr><td>visitResult</td><td>✅ 可更新</td><td>带看结果</td></tr>
     *   <tr><td>updateTime</td><td>⏱️ 自动更新</td><td>最后更新时间</td></tr>
     * </table>
     */
    boolean updateVisitRecord(VisitRecord visitRecord);

    /**
     * 根据ID删除带看记录（租户隔离）
     * <p>在指定租户下删除指定带看记录，确保操作权限和数据安全</p>
     *
     * @param id 带看记录主键ID，不能为null
     * @param tenantId 租户ID，用于权限校验，不能为null
     * @return boolean 操作结果，true-成功 false-失败
     * @throws IllegalArgumentException 当记录不存在或租户不匹配时抛出异常
     *
     * <p><b>删除策略：</b></p>
     * <ul>
     *   <li>物理删除：直接从数据库移除记录</li>
     *   <li>权限校验：确保操作者只能删除本租户数据</li>
     *   <li>事务保障：操作失败时自动回滚</li>
     * </ul>
     */
    boolean removeByIdWithTenant(Long id, Long tenantId);

    /**
     * 多条件分页查询带看记录
     * <p>支持多维度组合条件查询，返回分页结果，适用于管理后台列表展示和统计分析</p>
     *
     * @param page 分页参数对象，包含页码、每页大小等分页信息
     * @param tenantId 租户ID，必须指定，用于数据隔离
     * @param houseId 房源ID，精确匹配，null表示不限制
     * @param customerId 客户ID，精确匹配，null表示不限制
     * @param agentId 经纪人ID，精确匹配，null表示不限制
     * @param startTime 带看时间起始范围，null表示无开始时间限制
     * @param endTime 带看时间结束范围，null表示无结束时间限制
     * @param visitType 带看类型，精确匹配，null表示不限制
     * @param intentionLevel 意向等级，精确匹配，null表示不限制
     * @return IPage<VisitRecord> 分页查询结果，包含数据列表和分页统计信息
     *
     * <p><b>查询条件组合示例：</b></p>
     * <pre>
     * // 查询指定经纪人某时间段内的带看记录
     * pageQuery(page, tenantId, null, null, agentId, startTime, endTime, null, null);
     *
     * // 查询指定房源的高意向带看记录
     * pageQuery(page, tenantId, houseId, null, null, null, null, null, 1);
     * </pre>
     *
     * <p><b>排序规则：</b>默认按带看时间倒序排列（最新带看在前）</p>
     */
    IPage<VisitRecord> pageQuery(Page<VisitRecord> page,
                                 Long tenantId,
                                 Long houseId,
                                 Long customerId,
                                 Long agentId,
                                 LocalDateTime startTime,
                                 LocalDateTime endTime,
                                 String visitType,
                                 Byte intentionLevel);

    /**
     * 批量新增带看记录
     * <p>批量创建带看记录，适用于数据导入或批量操作场景</p>
     *
     * @param visitRecordList 带看记录实体集合，不能为null或空
     * @return boolean 批量操作结果，true-全部成功 false-任一失败
     * @throws IllegalArgumentException 当参数非法或必填字段缺失时抛出异常
     *
     * <p><b>事务特性：</b></p>
     * <ul>
     *   <li>原子操作：任一记录失败则全部回滚</li>
     *   <li>性能优化：使用批量插入提高数据库操作效率</li>
     *   <li>数据校验：批量校验必填字段和业务规则</li>
     * </ul>
     */
    boolean saveBatchVisitRecord(List<VisitRecord> visitRecordList);

    /**
     * 批量更新带看记录
     * <p>批量更新现有带看记录，适用于批量修改备注、结果等场景</p>
     *
     * @param visitRecordList 待更新的带看记录集合，每个实体必须包含ID
     * @return boolean 批量操作结果，true-全部成功 false-任一失败
     * @throws IllegalArgumentException 当参数非法或记录不存在时抛出异常
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>批量更新带看结果</li>
     *   <li>批量修改意向等级</li>
     *   <li>批量添加备注信息</li>
     * </ul>
     */
    boolean updateBatchVisitRecord(List<VisitRecord> visitRecordList);

    /**
     * 批量删除带看记录（租户隔离）
     * <p>在指定租户下批量删除带看记录，确保数据安全和操作权限</p>
     *
     * @param ids 待删除的记录ID集合，不能为null或空
     * @param tenantId 租户ID，用于权限校验，不能为null
     * @return boolean 批量操作结果，true-全部成功 false-任一失败
     * @throws IllegalArgumentException 当参数非法或记录不存在时抛出异常
     *
     * <p><b>安全机制：</b></p>
     * <ul>
     *   <li>租户校验：确保所有待删除记录都属于指定租户</li>
     *   <li>存在性校验：确保所有ID对应的记录都存在</li>
     *   <li>权限控制：防止越权删除其他租户数据</li>
     * </ul>
     */
    boolean removeBatchByIdsWithTenant(List<Long> ids, Long tenantId);
}
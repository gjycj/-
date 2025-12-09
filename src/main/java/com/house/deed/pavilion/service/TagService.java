package com.house.deed.pavilion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.entity.Tag;
import java.util.List;
import java.util.Map;

/**
 * 标签表业务服务接口
 * <p>
 * 负责标签表（租户级数据隔离）的所有业务操作，包括增删改查及批量处理。
 * 注意：所有操作均需指定租户ID以保证数据隔离。
 * </p>
 *
 * @author yuquanxi
 * @version 1.0.0
 * @since 2025-11-26
 */
public interface TagService extends IService<Tag> {

    // ==================== 基础CRUD操作 ====================

    /**
     * 创建标签记录
     * <p>
     * 保存一个新的标签到数据库，执行租户数据隔离校验。
     * </p>
     *
     * @param tag 标签实体对象，包含标签名称、类型等属性
     * @return 操作是否成功
     * @throws IllegalArgumentException 当标签参数为空或缺少必要字段时抛出
     * @throws RuntimeException 当数据库操作失败或违反唯一约束时抛出
     */
    boolean saveTag(Tag tag);

    /**
     * 更新标签信息
     * <p>
     * 根据标签ID更新标签信息，更新前会验证租户权限。
     * </p>
     *
     * @param tag 标签实体对象，必须包含有效的ID
     * @return 操作是否成功
     * @throws IllegalArgumentException 当标签ID为空或标签对象无效时抛出
     * @throws org.springframework.dao.DataAccessException 当数据访问失败时抛出
     */
    boolean updateTagById(Tag tag);

    /**
     * 删除标签记录（逻辑/物理删除）
     * <p>
     * 根据标签ID和租户ID删除指定标签，确保租户数据隔离。
     * 具体删除方式（逻辑删除或物理删除）由实现类决定。
     * </p>
     *
     * @param id 标签主键ID，不能为空
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 操作是否成功
     * @throws IllegalArgumentException 当ID为空或租户ID无效时抛出
     * @throws org.springframework.dao.DataAccessException 当删除失败时抛出
     */
    boolean removeTagById(Long id, Long tenantId);

    /**
     * 根据ID查询标签详情
     * <p>
     * 根据标签ID和租户ID查询标签详细信息，确保只返回当前租户的数据。
     * </p>
     *
     * @param id 标签主键ID，不能为空
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 标签实体对象，如果不存在则返回null
     * @throws IllegalArgumentException 当ID为空或租户ID无效时抛出
     */
    Tag getTagById(Long id, Long tenantId);

    // ==================== 多条件查询操作 ====================

    /**
     * 分页查询标签列表
     * <p>
     * 根据查询条件分页获取标签列表，支持多条件筛选和排序。
     * 查询参数支持：
     * - tagName: 标签名称（模糊查询）
     * - tagType: 标签类型（精确匹配）
     * - status: 标签状态
     * - createTime: 创建时间范围
     * </p>
     *
     * @param page 分页参数对象，包含页码、每页数量等信息
     * @param queryParams 查询条件Map，键为字段名，值为查询条件
     * @param tenantId 租户ID，用于数据隔离
     * @return 分页结果对象，包含数据列表和分页信息
     * @throws IllegalArgumentException 当分页参数无效或租户ID为空时抛出
     */
    IPage<Tag> pageQuery(Page<Tag> page, Map<String, Object> queryParams, Long tenantId);

    /**
     * 条件查询标签列表（不分页）
     * <p>
     * 根据查询条件获取所有符合条件的标签列表，适用于数据导出或下拉选择。
     * 注意：如果数据量较大，建议使用分页查询。
     * </p>
     *
     * @param queryParams 查询条件Map，键为字段名，值为查询条件
     * @param tenantId 租户ID，用于数据隔离
     * @return 标签实体列表，如果没有符合条件的记录则返回空列表
     * @throws IllegalArgumentException 当租户ID为空时抛出
     */
    List<Tag> listByConditions(Map<String, Object> queryParams, Long tenantId);

    /**
     * 根据标签类型查询标签列表
     * <p>
     * 获取指定类型的所有标签，常用于标签分类展示。
     * </p>
     *
     * @param tagType 标签类型，如："系统标签"、"用户标签"等
     * @param tenantId 租户ID，用于数据隔离
     * @return 指定类型的标签列表，如果没有则返回空列表
     * @throws IllegalArgumentException 当标签类型为空或租户ID无效时抛出
     */
    List<Tag> listByTagType(String tagType, Long tenantId);

    // ==================== 批量操作 ====================

    /**
     * 批量保存标签
     * <p>
     * 一次性保存多个标签记录，采用事务保证数据一致性。
     * 当任意一条记录保存失败时，所有操作将回滚。
     * </p>
     *
     * @param tagList 标签实体列表，不能为空或包含null元素
     * @return 批量操作是否全部成功
     * @throws IllegalArgumentException 当标签列表为空或包含无效数据时抛出
     * @throws org.springframework.dao.DataAccessException 当批量插入失败时抛出
     */
    boolean batchSaveTags(List<Tag> tagList);

    /**
     * 批量删除标签
     * <p>
     * 根据ID列表批量删除标签，执行租户权限验证。
     * 删除方式与单个删除保持一致。
     * </p>
     *
     * @param ids 标签ID列表，不能为空
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 批量操作是否全部成功
     * @throws IllegalArgumentException 当ID列表为空或租户ID无效时抛出
     * @throws org.springframework.dao.DataAccessException 当批量删除失败时抛出
     */
    boolean batchRemoveTags(List<Long> ids, Long tenantId);
}
package com.house.deed.pavilion.service;

import com.house.deed.pavilion.entity.TenantConfig;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * 租户个性化配置表服务接口
 * <p>
 * 负责管理租户级个性化配置数据，支持系统级默认配置和租户级自定义配置的隔离与覆盖。
 * 核心功能：
 * 1. 租户配置隔离：每个租户拥有独立的配置空间，配置互不干扰
 * 2. 配置层次化：支持系统默认配置（is_system=1）和租户自定义配置（is_system=0）
 * 3. 批量操作：支持配置的批量增删改，提升管理效率
 * 4. 配置获取：支持按租户和配置键快速获取配置值
 * </p>
 * <p>
 * 适用场景：
 * - 租户个性化设置：如页面主题、业务规则参数等
 * - 系统参数配置：提供系统级默认值，租户可选择性覆盖
 * - 功能开关管理：控制租户级别的功能可用性
 * </p>
 * <p>
 * 数据模型说明：
 * - 系统级配置（is_system=1）：平台预设的默认配置，所有租户共享
 * - 租户级配置（is_system=0）：租户自定义的个性化配置，覆盖系统默认值
 * - 配置键唯一性：同一租户下配置键必须唯一（tenant_id + config_key + is_system）
 * </p>
 *
 * @author yuquanxi
 * @version 1.0.0
 * @since 2025-11-26
 */
public interface TenantConfigService extends IService<TenantConfig> {

    /**
     * 保存租户配置（新增）
     * <p>
     * 创建新的租户配置记录，支持系统级配置和租户级配置。
     * 执行前会校验配置键的唯一性，防止重复配置。
     * </p>
     *
     * @param tenantConfig 租户配置实体对象，必须包含配置键、配置值、租户ID等必要字段
     * @return true-保存成功，false-保存失败
     * @throws IllegalArgumentException 当参数校验失败（如配置键已存在、必填字段缺失等）
     * @throws org.springframework.dao.DataAccessException 当数据库操作异常
     *
     * @note 注意：系统级配置的tenant_id通常为0或特定标识，表示全局配置
     */
    boolean saveTenantConfig(TenantConfig tenantConfig);

    /**
     * 更新租户配置
     * <p>
     * 更新已存在的租户配置信息，主要更新配置值和描述信息。
     * 配置键、租户ID、系统标识等核心字段通常不允许修改。
     * </p>
     *
     * @param tenantConfig 租户配置实体对象，必须包含有效的ID和需要更新的字段
     * @return true-更新成功，false-更新失败
     * @throws IllegalArgumentException 当配置不存在或参数无效时抛出
     * @throws org.springframework.dao.DataAccessException 当数据库操作异常
     *
     * @note 建议使用乐观锁机制防止并发更新冲突
     */
    boolean updateTenantConfig(TenantConfig tenantConfig);

    /**
     * 删除指定配置
     * <p>
     * 根据配置ID删除租户配置记录。
     * 系统级配置可能需要特殊权限才能删除。
     * </p>
     *
     * @param id 配置主键ID，不能为空
     * @return true-删除成功，false-删除失败
     * @throws IllegalArgumentException 当ID为空或配置不存在时抛出
     * @throws org.springframework.dao.DataAccessException 当数据库操作异常
     *
     * @warning 删除操作不可逆，请谨慎操作，特别是系统级配置
     */
    boolean removeTenantConfig(Long id);

    /**
     * 批量删除配置
     * <p>
     * 根据ID列表批量删除配置记录，支持事务保证数据一致性。
     * 当任意一条记录删除失败时，所有操作将回滚。
     * </p>
     *
     * @param ids 配置ID列表，不能为空
     * @return 实际删除的记录数
     * @throws IllegalArgumentException 当ID列表为空或包含无效ID时抛出
     * @throws org.springframework.dao.DataAccessException 当数据库操作异常
     *
     * @performance 批量删除性能优于逐条删除，建议一次性批量操作不超过1000条
     */
    int batchRemove(List<Long> ids);

    /**
     * 多条件查询配置列表
     * <p>
     * 根据租户ID、配置键、系统标识等条件组合查询配置列表。
     * 支持精确匹配和模糊查询（根据实现）。
     * </p>
     * <p>
     * 典型使用场景：
     * 1. 获取租户所有自定义配置：tenantId != null, isSystem = 0
     * 2. 获取系统默认配置：tenantId = null, isSystem = 1
     * 3. 按配置键搜索：configKey != null
     * </p>
     *
     * @param tenantId 租户ID，可为空（查询系统配置或所有租户配置）
     * @param configKey 配置键，支持模糊查询（根据实现决定），可为空
     * @param isSystem 系统标识：0-租户配置，1-系统配置，可为空（查询所有类型）
     * @return 符合条件的配置列表，如果没有匹配项则返回空列表
     *
     * @example
     * <pre>
     * // 查询租户1001的所有页面主题配置
     * List<TenantConfig> themes = queryByConditions(1001L, "page.theme.%", null);
     * // 查询所有系统级邮件配置
     * List<TenantConfig> emailConfigs = queryByConditions(null, "email.%", (byte)1);
     * </pre>
     */
    List<TenantConfig> queryByConditions(Long tenantId, String configKey, Byte isSystem);

    /**
     * 批量保存配置
     * <p>
     * 一次性保存多个配置记录，采用事务保证数据一致性。
     * 执行前会校验每个配置的合法性和唯一性。
     * </p>
     *
     * @param configs 配置实体列表，不能为空或包含null元素
     * @return true-批量保存成功，false-批量保存失败
     * @throws IllegalArgumentException 当参数校验失败时抛出
     * @throws org.springframework.dao.DataAccessException 当数据库操作异常
     *
     * @note 注意：批量保存时需确保所有配置属于同一租户（或系统配置）
     */
    boolean batchSave(List<TenantConfig> configs);

    /**
     * 批量更新配置
     * <p>
     * 一次性更新多个配置记录，采用事务保证数据一致性。
     * 每个配置必须包含有效的ID。
     * </p>
     *
     * @param configs 配置实体列表，每个配置必须包含有效的ID
     * @return true-批量更新成功，false-批量更新失败
     * @throws IllegalArgumentException 当参数校验失败时抛出
     * @throws org.springframework.dao.DataAccessException 当数据库操作异常
     *
     * @performance 批量更新性能优于逐条更新，建议配合乐观锁使用
     */
    boolean batchUpdate(List<TenantConfig> configs);

    /**
     * 根据租户和配置键获取配置
     * <p>
     * 获取指定租户的配置值，遵循配置获取优先级：
     * 1. 优先返回租户自定义配置（is_system=0）
     * 2. 如果没有租户自定义配置，返回系统默认配置（is_system=1）
     * 3. 如果都不存在，返回null
     * </p>
     * <p>
     * 这是最常用的配置获取方法，用于业务系统中获取配置值。
     * </p>
     *
     * @param tenantId 租户ID，不能为空
     * @param configKey 配置键，不能为空
     * @return 配置实体对象，如果不存在则返回null
     * @throws IllegalArgumentException 当参数为空时抛出
     *
     * @example
     * <pre>
     * // 获取租户1001的页面主题配置
     * TenantConfig themeConfig = getByTenantAndKey(1001L, "page.theme.color");
     * String themeColor = themeConfig != null ? themeConfig.getConfigValue() : "default";
     * </pre>
     */
    TenantConfig getByTenantAndKey(Long tenantId, String configKey);
}
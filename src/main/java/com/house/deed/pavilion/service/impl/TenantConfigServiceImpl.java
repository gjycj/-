package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.TenantConfig;
import com.house.deed.pavilion.mapper.TenantConfigMapper;
import com.house.deed.pavilion.service.TenantConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 租户个性化配置表服务实现类
 *
 * <p>提供租户配置的增删改查等核心业务逻辑实现，包含数据唯一性校验、
 * 系统内置配置保护、批量操作等业务规则</p>
 *
 * <p><b>核心业务规则：</b></p>
 * <ul>
 *   <li>租户ID + 配置键 组合需保持唯一</li>
 *   <li>系统内置配置（is_system=1）不允许删除</li>
 *   <li>批量操作支持事务回滚确保数据一致性</li>
 * </ul>
 *
 * @author yuquanxi
 * @since 2025-11-26
 * @version 1.0.0
 */
@Service
public class TenantConfigServiceImpl extends ServiceImpl<TenantConfigMapper, TenantConfig> implements TenantConfigService {

    /**
     * 租户配置数据访问层接口
     * <p>用于执行特定数据库操作，如批量删除等</p>
     */
    @Autowired
    private TenantConfigMapper tenantConfigMapper;

    /**
     * 新增租户个性化配置项
     * <p>执行唯一性校验确保同一租户下配置键不重复，校验失败将抛出业务异常</p>
     *
     * @param tenantConfig 租户配置实体对象，必须包含tenantId、configKey等必填字段
     * @return boolean 新增操作结果，true-成功 false-失败
     * @throws RuntimeException 当配置键已存在时抛出"配置项已存在"异常
     *
     * <p><b>业务逻辑流程：</b></p>
     * <ol>
     *   <li>校验租户ID+配置键唯一性约束</li>
     *   <li>唯一性校验通过则执行数据持久化</li>
     *   <li>整个操作在事务中执行，失败时自动回滚</li>
     * </ol>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveTenantConfig(TenantConfig tenantConfig) {
        // 唯一性校验：确保同一租户下配置键不重复
        if (checkConfigKeyExists(tenantConfig.getTenantId(), tenantConfig.getConfigKey(), null)) {
            throw new RuntimeException("配置项已存在：租户ID=" + tenantConfig.getTenantId() + ", 配置键=" + tenantConfig.getConfigKey());
        }
        return save(tenantConfig);
    }

    /**
     * 更新租户个性化配置项
     * <p>更新前校验数据存在性、唯一性约束及系统内置配置保护</p>
     *
     * @param tenantConfig 待更新的配置实体，必须包含id主键
     * @return boolean 更新操作结果，true-成功 false-失败
     * @throws RuntimeException 当配置不存在或违反唯一性约束时抛出业务异常
     *
     * <p><b>特殊校验规则：</b></p>
     * <ul>
     *   <li>更新时需排除当前记录自身进行唯一性校验</li>
     *   <li>系统内置配置允许更新配置值和描述信息</li>
     * </ul>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTenantConfig(TenantConfig tenantConfig) {
        // 校验数据存在性
        TenantConfig oldConfig = getById(tenantConfig.getId());
        if (oldConfig == null) {
            throw new RuntimeException("配置项不存在：ID=" + tenantConfig.getId());
        }

        // 唯一性校验：排除当前记录进行租户ID+配置键唯一性检查
        if (checkConfigKeyExists(tenantConfig.getTenantId(), tenantConfig.getConfigKey(), tenantConfig.getId())) {
            throw new RuntimeException("配置项已存在：租户ID=" + tenantConfig.getTenantId() + ", 配置键=" + tenantConfig.getConfigKey());
        }

        return updateById(tenantConfig);
    }

    /**
     * 删除指定配置项
     * <p>系统内置配置（is_system=1）受保护，不允许删除操作</p>
     *
     * @param id 配置记录主键ID
     * @return boolean 删除操作结果，true-成功 false-失败
     * @throws RuntimeException 当配置不存在或为系统内置配置时抛出业务异常
     *
     * <p><b>删除保护机制：</b></p>
     * <ul>
     *   <li>前置校验：确认配置记录存在</li>
     *   <li>权限校验：系统内置配置禁止删除</li>
     *   <li>事务保障：操作失败时数据回滚</li>
     * </ul>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeTenantConfig(Long id) {
        TenantConfig config = getById(id);
        if (config == null) {
            throw new RuntimeException("配置项不存在：ID=" + id);
        }
        // 系统内置配置保护校验
        if (config.getIsSystem() == 1) {
            throw new RuntimeException("系统内置配置不可删除：ID=" + id);
        }
        return removeById(id);
    }

    /**
     * 批量删除配置项
     * <p>批量删除前会检查列表中是否包含系统内置配置，如有则终止整个操作</p>
     *
     * @param ids 配置ID集合，支持批量操作
     * @return int 实际删除的记录数量
     * @throws RuntimeException 当包含系统内置配置时抛出异常，阻止批量删除
     *
     * <p><b>批量处理逻辑：</b></p>
     * <ol>
     *   <li>空集合检查：直接返回0</li>
     *   <li>系统配置检查：批量校验是否包含受保护配置</li>
     *   <li>批量删除：使用MyBatis Plus的deleteBatchIds方法</li>
     * </ol>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchRemove(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return 0;
        }

        // 批量检查系统内置配置保护
        List<TenantConfig> configs = listByIds(ids);
        for (TenantConfig config : configs) {
            if (config.getIsSystem() == 1) {
                throw new RuntimeException("包含系统内置配置，无法批量删除：ID=" + config.getId());
            }
        }

        // 执行批量物理删除
        return tenantConfigMapper.deleteBatchIds(ids);
    }

    /**
     * 多条件组合查询配置项
     * <p>支持租户ID精确匹配、配置键模糊匹配、系统配置标识过滤</p>
     *
     * @param tenantId  租户ID，可为null表示不限定租户
     * @param configKey 配置键，支持模糊查询，可为null或空字符串
     * @param isSystem  系统配置标识：1-系统内置 0-用户自定义，null表示不限
     * @return List<TenantConfig> 满足条件的配置项集合，按租户ID和配置键升序排序
     *
     * <p><b>查询条件说明：</b></p>
     * <table border="1">
     *   <tr><th>参数</th><th>查询条件</th><th>为空处理</th></tr>
     *   <tr><td>tenantId</td><td>精确匹配</td><td>跳过该条件</td></tr>
     *   <tr><td>configKey</td><td>LIKE模糊匹配</td><td>跳过该条件</td></tr>
     *   <tr><td>isSystem</td><td>精确匹配</td><td>跳过该条件</td></tr>
     * </table>
     */
    @Override
    public List<TenantConfig> queryByConditions(Long tenantId, String configKey, Byte isSystem) {
        LambdaQueryWrapper<TenantConfig> queryWrapper = new LambdaQueryWrapper<>();
        // 租户ID精确匹配条件
        if (tenantId != null) {
            queryWrapper.eq(TenantConfig::getTenantId, tenantId);
        }
        // 配置键模糊匹配条件（非空校验）
        if (configKey != null && !configKey.isEmpty()) {
            queryWrapper.like(TenantConfig::getConfigKey, configKey);
        }
        // 系统配置标识过滤条件
        if (isSystem != null) {
            queryWrapper.eq(TenantConfig::getIsSystem, isSystem);
        }
        // 结果排序规则：先按租户ID升序，再按配置键升序
        queryWrapper.orderByAsc(TenantConfig::getTenantId).orderByAsc(TenantConfig::getConfigKey);
        return list(queryWrapper);
    }

    /**
     * 批量新增配置项
     * <p>批量插入前会对所有记录执行唯一性校验，任一记录重复则整个操作失败</p>
     *
     * @param configs 租户配置实体集合，不能为空
     * @return boolean 批量新增结果，true-全部成功 false-失败
     * @throws RuntimeException 当集合中任一配置违反唯一性约束时抛出异常
     *
     * <p><b>事务一致性保证：</b></p>
     * <ul>
     *   <li>前置校验：逐条校验唯一性，任一失败则整个事务回滚</li>
     *   <li>批量插入：使用MyBatis Plus的saveBatch方法</li>
     *   <li>原子操作：所有记录要么全部成功，要么全部失败</li>
     * </ul>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSave(List<TenantConfig> configs) {
        if (CollectionUtils.isEmpty(configs)) {
            return false;
        }

        // 批量唯一性校验：任一记录重复则抛出异常终止操作
        for (TenantConfig config : configs) {
            if (checkConfigKeyExists(config.getTenantId(), config.getConfigKey(), null)) {
                throw new RuntimeException("批量新增失败，配置项已存在：租户ID=" + config.getTenantId() + ", 配置键=" + config.getConfigKey());
            }
        }

        // 执行批量插入操作
        return saveBatch(configs);
    }

    /**
     * 批量更新配置项
     * <p>仅允许更新配置值和描述信息，其他字段不允许通过此接口修改</p>
     *
     * @param configs 配置实体集合，每个实体必须包含id主键
     * @return boolean 批量更新结果，true-成功 false-失败
     * @throws RuntimeException 当配置ID为空或更新失败时抛出异常
     *
     * <p><b>更新字段限制：</b></p>
     * <ul>
     *   <li>可更新字段：configValue（配置值）、configDesc（配置描述）</li>
     *   <li>禁止更新：tenantId、configKey、isSystem等核心字段</li>
     *   <li>更新策略：逐条更新，任一失败不影响其他记录</li>
     * </ul>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdate(List<TenantConfig> configs) {
        if (CollectionUtils.isEmpty(configs)) {
            return false;
        }

        // 逐条更新配置项（仅更新值和描述）
        for (TenantConfig config : configs) {
            if (config.getId() == null) {
                throw new RuntimeException("批量更新失败，配置ID不能为空");
            }
            // 创建更新对象，限制只更新允许的字段
            TenantConfig updateEntity = new TenantConfig();
            updateEntity.setId(config.getId());
            updateEntity.setConfigValue(config.getConfigValue());
            updateEntity.setConfigDesc(config.getConfigDesc());
            updateById(updateEntity);
        }
        return true;
    }

    /**
     * 根据租户ID和配置键查询唯一配置
     * <p>精确匹配查询，用于获取特定租户的特定配置项</p>
     *
     * @param tenantId  租户ID，不能为null
     * @param configKey 配置键，不能为null
     * @return TenantConfig 匹配的配置实体，未找到时返回null
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>获取租户特定配置值</li>
     *   <li>配置项存在性验证</li>
     *   <li>配置值读取</li>
     * </ul>
     */
    @Override
    public TenantConfig getByTenantAndKey(Long tenantId, String configKey) {
        LambdaQueryWrapper<TenantConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TenantConfig::getTenantId, tenantId)
                .eq(TenantConfig::getConfigKey, configKey);
        return getOne(queryWrapper);
    }

    /**
     * 内部校验方法：检查配置键唯一性约束
     * <p>验证指定租户下配置键是否已存在，支持更新时排除自身记录</p>
     *
     * @param tenantId  租户ID，不能为null
     * @param configKey 配置键，不能为null
     * @param excludeId 需要排除的记录ID（用于更新操作），可为null
     * @return boolean true-已存在 false-不存在
     *
     * <p><b>查询逻辑说明：</b></p>
     * <pre>
     * SELECT COUNT(*) FROM tenant_config
     * WHERE tenant_id = #{tenantId}
     *   AND config_key = #{configKey}
     *   AND (excludeId IS NULL OR id != #{excludeId})
     * </pre>
     */
    private boolean checkConfigKeyExists(Long tenantId, String configKey, Long excludeId) {
        LambdaQueryWrapper<TenantConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TenantConfig::getTenantId, tenantId)
                .eq(TenantConfig::getConfigKey, configKey);
        // 更新操作时排除当前记录自身
        if (excludeId != null) {
            queryWrapper.ne(TenantConfig::getId, excludeId);
        }
        return count(queryWrapper) > 0;
    }
}
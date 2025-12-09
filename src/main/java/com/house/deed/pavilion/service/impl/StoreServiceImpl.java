package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.Store;
import com.house.deed.pavilion.mapper.StoreMapper;
import com.house.deed.pavilion.service.StoreService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 门店信息服务实现类
 *
 * <p>实现门店信息的增删改查及批量操作，所有方法均包含租户级数据隔离和严格的业务校验</p>
 * <p>业务特点：</p>
 * <ul>
 *   <li>严格租户隔离：所有操作均强制校验tenantId，确保数据仅归属租户可见可控</li>
 *   <li>门店编码唯一性：同一租户内门店编码必须唯一，支持经纪人归属和业务分配</li>
 *   <li>关联实体管理：关联Region（区域）、Agent（经纪人/店长），需保证关联数据一致性</li>
 * </ul>
 * <p>技术实现：</p>
 * <ul>
 *   <li>使用QueryWrapper构建动态查询条件</li>
 *   <li>批量操作包含事务保证，确保数据一致性</li>
 *   <li>支持多条件组合查询和范围查询</li>
 * </ul>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class StoreServiceImpl extends ServiceImpl<StoreMapper, Store> implements StoreService {

    /**
     * 新增门店信息
     *
     * <p>业务校验：</p>
     * <ul>
     *   <li>租户ID非空校验</li>
     *   <li>同一租户内门店编码唯一性校验</li>
     * </ul>
     * <p>技术实现：自动填充创建时间和更新时间（通过MyBatis Plus字段填充器）</p>
     *
     * @param store 门店实体对象，需包含租户ID、门店编码等必填信息
     * @return 新增成功返回true，否则返回false
     * @throws IllegalArgumentException 当租户ID为空或门店编码重复时抛出
     */
    @Override
    public boolean saveStore(Store store) {
        // 校验租户ID存在
        if (store.getTenantId() == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }

        // 校验同一租户内门店编码唯一
        QueryWrapper<Store> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tenant_id", store.getTenantId())
                .eq("store_code", store.getStoreCode());
        long count = baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new IllegalArgumentException("当前租户下已存在相同门店编码：" + store.getStoreCode());
        }

        // 执行新增操作
        return baseMapper.insert(store) > 0;
    }

    /**
     * 根据ID更新门店信息
     *
     * <p>业务校验：</p>
     * <ul>
     *   <li>租户ID和主键ID非空校验</li>
     *   <li>数据必须存在且属于当前租户</li>
     *   <li>若更新门店编码，需校验租户内唯一性（排除自身）</li>
     * </ul>
     * <p>技术实现：自动填充更新时间（通过MyBatis Plus字段填充器）</p>
     *
     * @param store 门店实体对象，需包含主键ID、租户ID及需要更新的字段
     * @return 更新成功返回true，否则返回false
     * @throws IllegalArgumentException 当参数异常、数据不存在或无权限操作时抛出
     */
    @Override
    public boolean updateStoreById(Store store) {
        // 校验租户ID和主键存在
        if (store.getTenantId() == null || store.getId() == null) {
            throw new IllegalArgumentException("租户ID和门店ID不能为空");
        }

        // 校验数据归属当前租户
        Store existStore = baseMapper.selectById(store.getId());
        if (existStore == null || !existStore.getTenantId().equals(store.getTenantId())) {
            throw new IllegalArgumentException("门店不存在或无权限操作");
        }

        // 若更新门店编码，需再次校验唯一性（排除自身）
        if (store.getStoreCode() != null) {
            QueryWrapper<Store> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("tenant_id", store.getTenantId())
                    .eq("store_code", store.getStoreCode())
                    .ne("id", store.getId());
            long count = baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new IllegalArgumentException("当前租户下已存在相同门店编码：" + store.getStoreCode());
            }
        }

        // 执行更新操作
        return baseMapper.updateById(store) > 0;
    }

    /**
     * 根据ID物理删除门店信息
     *
     * <p>业务校验：</p>
     * <ul>
     *   <li>门店ID和租户ID非空校验</li>
     *   <li>数据必须存在且属于当前租户</li>
     * </ul>
     * <p>扩展建议：实际业务中可增加校验门店下是否有经纪人/房源，防止误删</p>
     *
     * @param id 门店主键ID
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 删除成功返回true，否则返回false
     * @throws IllegalArgumentException 当参数异常或数据不存在或无权限操作时抛出
     */
    @Override
    public boolean removeStoreById(Long id, Long tenantId) {
        if (id == null || tenantId == null) {
            throw new IllegalArgumentException("门店ID和租户ID不能为空");
        }

        // 校验数据归属当前租户
        Store existStore = baseMapper.selectById(id);
        if (existStore == null || !existStore.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("门店不存在或无权限操作");
        }

        // 执行删除操作
        return baseMapper.deleteById(id) > 0;
    }

    /**
     * 根据ID查询门店详细信息（租户隔离）
     *
     * <p>查询时自动应用租户隔离条件，确保只能查询到当前租户的数据</p>
     *
     * @param id 门店主键ID
     * @param tenantId 租户ID，用于数据隔离
     * @return 符合条件的门店实体对象，未找到返回null
     */
    @Override
    public Store getStoreById(Long id, Long tenantId) {
        if (id == null || tenantId == null) {
            return null;
        }

        QueryWrapper<Store> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id)
                .eq("tenant_id", tenantId);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 多条件分页查询门店信息
     *
     * <p>支持以下查询条件：</p>
     * <ul>
     *   <li>区域ID精确查询</li>
     *   <li>运营状态精确查询（0=停业，1=营业）</li>
     *   <li>门店名称模糊查询</li>
     *   <li>店长ID精确查询</li>
     *   <li>门店编码模糊查询</li>
     * </ul>
     * <p>排序规则：按创建时间降序（最新创建的门店在前）</p>
     *
     * @param page 分页参数对象，包含页码和每页大小
     * @param queryParams 查询参数映射表，key为字段名，value为查询值
     * @param tenantId 租户ID，用于数据隔离
     * @return 分页查询结果，包含门店列表和分页信息
     */
    @Override
    public IPage<Store> pageQuery(Page<Store> page, Map<String, Object> queryParams, Long tenantId) {
        QueryWrapper<Store> queryWrapper = buildQueryWrapper(queryParams, tenantId);
        return baseMapper.selectPage(page, queryWrapper);
    }

    /**
     * 多条件查询门店列表（不分页）
     *
     * <p>查询条件与分页查询方法保持一致，但不进行分页处理</p>
     *
     * @param queryParams 查询参数映射表，key为字段名，value为查询值
     * @param tenantId 租户ID，用于数据隔离
     * @return 符合条件的门店实体对象列表
     */
    @Override
    public List<Store> listByConditions(Map<String, Object> queryParams, Long tenantId) {
        QueryWrapper<Store> queryWrapper = buildQueryWrapper(queryParams, tenantId);
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 根据区域ID查询门店列表
     *
     * <p>按门店名称升序排列，便于查看和管理</p>
     *
     * @param regionId 区域ID
     * @param tenantId 租户ID，用于数据隔离
     * @return 区域下的门店实体对象列表，按门店名称升序排列
     */
    @Override
    public List<Store> listByRegionId(Long regionId, Long tenantId) {
        if (regionId == null || tenantId == null) {
            return List.of();
        }

        QueryWrapper<Store> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tenant_id", tenantId)
                .eq("region_id", regionId)
                .orderByAsc("store_name");
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 批量新增门店信息（事务保证）
     *
     * <p>在单个事务中执行批量新增，任一记录校验失败或保存失败将导致整个操作回滚</p>
     * <p>批量校验：</p>
     * <ul>
     *   <li>租户一致性校验（所有门店必须属于同一租户）</li>
     *   <li>门店编码唯一性校验（批量内部去重+租户内已存在检查）</li>
     * </ul>
     *
     * @param storeList 门店实体对象列表
     * @return 批量新增成功返回true，否则返回false
     * @throws IllegalArgumentException 当参数异常、租户不一致或编码重复时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveStores(List<Store> storeList) {
        if (storeList == null || storeList.isEmpty()) {
            throw new IllegalArgumentException("批量新增的门店列表不能为空");
        }

        // 校验所有门店租户ID一致
        Long tenantId = storeList.get(0).getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }
        boolean allSameTenant = storeList.stream()
                .allMatch(store -> tenantId.equals(store.getTenantId()));
        if (!allSameTenant) {
            throw new IllegalArgumentException("批量新增的门店必须属于同一租户");
        }

        // 批量校验编码唯一性
        List<String> storeCodes = storeList.stream()
                .map(Store::getStoreCode)
                .toList();
        QueryWrapper<Store> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tenant_id", tenantId)
                .in("store_code", storeCodes);
        long count = baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new IllegalArgumentException("批量新增的门店中存在重复编码");
        }

        // 执行批量保存（事务保证）
        return saveBatch(storeList);
    }

    /**
     * 批量更新门店状态（事务保证）
     *
     * <p>在单个事务中执行批量状态更新，任一记录校验失败或更新失败将导致整个操作回滚</p>
     * <p>业务校验：</p>
     * <ul>
     *   <li>所有门店必须属于当前租户</li>
     *   <li>状态值必须在合法范围内（0=停业，1=营业）</li>
     * </ul>
     *
     * @param ids 待更新的门店ID列表
     * @param status 目标状态值（0=停业，1=营业）
     * @param tenantId 租户ID，用于数据隔离验证
     * @return 批量更新成功返回true，否则返回false
     * @throws IllegalArgumentException 当参数异常、状态值无效或无权限操作时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateStatus(List<Long> ids, Byte status, Long tenantId) {
        if (ids == null || ids.isEmpty() || status == null || tenantId == null) {
            throw new IllegalArgumentException("门店ID列表、状态和租户ID不能为空");
        }
        if (status < 0 || status > 1) {
            throw new IllegalArgumentException("状态值只能是0（停业）或1（营业）");
        }

        // 校验所有门店归属当前租户
        QueryWrapper<Store> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tenant_id", tenantId)
                .in("id", ids);
        long count = baseMapper.selectCount(queryWrapper);
        if (count != ids.size()) {
            throw new IllegalArgumentException("部分门店不存在或无权限操作");
        }

        // 执行批量更新（事务保证）
        Store updateEntity = new Store();
        updateEntity.setStatus(status);
        QueryWrapper<Store> updateWrapper = new QueryWrapper<>();
        updateWrapper.in("id", ids)
                .eq("tenant_id", tenantId);
        return baseMapper.update(updateEntity, updateWrapper) > 0;
    }

    /**
     * 构建查询条件（严格匹配实体类字段）
     *
     * <p>根据查询参数动态构建查询条件，支持以下参数：</p>
     * <ul>
     *   <li>regionId - 区域ID精确查询</li>
     *   <li>status - 运营状态精确查询（0=停业，1=营业）</li>
     *   <li>storeName - 门店名称模糊查询</li>
     *   <li>managerId - 店长ID精确查询</li>
     *   <li>storeCode - 门店编码模糊查询</li>
     * </ul>
     * <p>所有查询均自动添加租户隔离条件</p>
     *
     * @param queryParams 查询参数映射表
     * @param tenantId 租户ID，用于数据隔离
     * @return 构建完成的QueryWrapper对象
     */
    private QueryWrapper<Store> buildQueryWrapper(Map<String, Object> queryParams, Long tenantId) {
        QueryWrapper<Store> queryWrapper = new QueryWrapper<>();
        // 强制租户隔离
        queryWrapper.eq("tenant_id", tenantId);

        if (queryParams == null) {
            return queryWrapper;
        }

        // 动态拼接查询条件
        if (queryParams.containsKey("regionId") && queryParams.get("regionId") != null) {
            queryWrapper.eq("region_id", queryParams.get("regionId"));
        }
        if (queryParams.containsKey("status") && queryParams.get("status") != null) {
            queryWrapper.eq("status", queryParams.get("status"));
        }
        if (queryParams.containsKey("storeName") && queryParams.get("storeName") != null) {
            queryWrapper.like("store_name", queryParams.get("storeName"));
        }
        if (queryParams.containsKey("managerId") && queryParams.get("managerId") != null) {
            queryWrapper.eq("manager_id", queryParams.get("managerId"));
        }
        if (queryParams.containsKey("storeCode") && queryParams.get("storeCode") != null) {
            queryWrapper.like("store_code", queryParams.get("storeCode"));
        }

        // 默认按创建时间降序
        queryWrapper.orderByDesc("create_time");
        return queryWrapper;
    }
}
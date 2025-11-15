package com.house.deed.pavilion.module.store.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.module.agent.entity.Agent;
import com.house.deed.pavilion.module.agent.service.IAgentService;
import com.house.deed.pavilion.module.region.service.IRegionService;
import com.house.deed.pavilion.module.store.entity.Store;
import com.house.deed.pavilion.module.store.mapper.StoreMapper;
import com.house.deed.pavilion.module.store.service.IStoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;

/**
 * <p>
 * 门店信息表（租户级数据）服务实现类
 * 负责门店的CRUD、编码唯一性校验、店长关联校验等核心业务逻辑
 * 涉及关联表：经纪人表（agent）、区域表（region）
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
@Slf4j
public class StoreServiceImpl extends ServiceImpl<StoreMapper, Store> implements IStoreService {

    @Resource
    private IRegionService regionService;

    @Resource
    private IAgentService agentService;

    /**
     * 校验门店编码在租户内的唯一性
     * <p>
     * 用于门店新增和编辑场景，通过数据库查询判断编码是否已存在
     * 新增时excludeId为null，编辑时传入当前门店ID以排除自身
     * </p>
     *
     * @param tenantId   租户ID（数据隔离标识）
     * @param storeCode  待校验的门店编码
     * @param excludeId  需排除的门店ID（编辑时使用）
     * @return boolean  true-编码唯一，false-编码已存在
     */
    @Override
    public boolean checkStoreCodeUnique(Long tenantId, String storeCode, Long excludeId) {
        log.debug("校验门店编码唯一性：租户ID={}, 编码={}, 排除ID={}", tenantId, storeCode, excludeId);

        // 构建查询条件：租户ID匹配 + 编码匹配 + 排除自身（如需）
        int count = Math.toIntExact(count(Wrappers.<Store>lambdaQuery()
                .eq(Store::getTenantId, tenantId)
                .eq(Store::getStoreCode, storeCode)
                .ne(excludeId != null, Store::getId, excludeId)));
        return !(count == 0);
    }


    /**
     * 校验店长是否为当前租户的在职经纪人
     * <p>
     * 业务规则：
     * 1. 店长ID为null时允许（表示未分配店长）
     * 2. 非空时需校验：经纪人存在 + 属于当前租户 + 状态为在职（status=1）
     * </p>
     *
     * @param tenantId  租户ID（数据隔离标识）
     * @param managerId 店长ID（关联agent表的主键）
     * @throws BusinessException 当校验失败时抛出，包含具体错误信息
     */
    @Override
    public void validateManager(Long tenantId, Long managerId) {
        if (managerId == null) {
            log.debug("店长ID为null，无需校验");
            return;
        }

        log.debug("校验店长合法性：租户ID={}, 店长ID={}", tenantId, managerId);
        Agent agent = agentService.getById(managerId);

        // 校验经纪人是否存在
        if (agent == null) {
            throw new BusinessException(400, "店长不存在（经纪人ID不存在）");
        }

        // 校验租户归属
        if (!tenantId.equals(agent.getTenantId())) {
            throw new BusinessException(400, "店长不属于当前租户");
        }

        // 校验在职状态（1-在职）
        if (agent.getStatus() != 1) {
            throw new BusinessException(400, "店长已离职（状态异常）");
        }

        log.debug("店长校验通过：经纪人ID={}, 姓名={}", managerId, agent.getName());
    }

    /**
     * 校验区域合法性
     * 委托区域服务实现：区域必须存在且属于当前租户或系统默认区域
     */
    @Override
    public void validateRegion(Long tenantId, Long regionId) {
        regionService.validateRegion(tenantId, regionId);
    }

}
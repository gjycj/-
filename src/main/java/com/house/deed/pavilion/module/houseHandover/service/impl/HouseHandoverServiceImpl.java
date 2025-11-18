package com.house.deed.pavilion.module.houseHandover.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.BeanConvertUtil;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.contract.service.IContractService;
import com.house.deed.pavilion.module.house.service.IHouseService;
import com.house.deed.pavilion.module.houseHandover.entity.HouseHandover;
import com.house.deed.pavilion.module.houseHandover.mapper.HouseHandoverMapper;
import com.house.deed.pavilion.module.houseHandover.dto.HouseHandoverDTO;
import com.house.deed.pavilion.module.houseHandover.service.IHouseHandoverService;
import com.house.deed.pavilion.module.maintenanceOrder.entity.MaintenanceOrder;
import com.house.deed.pavilion.module.maintenanceOrder.service.IMaintenanceOrderService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * 房屋交接记录表（租户级数据） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class HouseHandoverServiceImpl extends ServiceImpl<HouseHandoverMapper, HouseHandover> implements IHouseHandoverService {

    @Resource
    private IHouseService houseService;

    @Resource
    private IMaintenanceOrderService maintenanceOrderService;

    @Autowired
    @Lazy
    private IContractService contractService;

    @Override
    public List<MaintenanceOrder> getRelatedMaintenanceOrders(Long handoverId) {
        Long tenantId = TenantContext.getTenantId();
        // 先校验交接记录是否属于当前租户
        HouseHandover handover = getById(handoverId);
        if (handover == null || !handover.getTenantId().equals(tenantId)) {
            return Collections.emptyList(); // 或抛出无权限异常
        }
        // 查询关联的维修工单
        return maintenanceOrderService.getByHouseHandoverId(handoverId, tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHandover(HouseHandoverDTO dto) {
        Long tenantId = TenantContext.getTenantId();

        // 校验房源存在性（当前租户）
        if (!houseService.existsById(dto.getHouseId())) {
            throw new BusinessException(400, "房源不存在或无权访问");
        }

        // DTO转实体
        HouseHandover handover = BeanConvertUtil.convert(dto, HouseHandover.class);
        handover.setTenantId(tenantId);
        handover.setCreateTime(LocalDateTime.now());

        // 保存交接记录
        this.save(handover);
        // 新增：若为退租交接，自动更新合同状态为COMPLETED
        if ("CHECK_OUT".equals(dto.getHandoverType())) {
            // 调用getLatestCheckOut确认当前记录是最新的退租记录
            HouseHandover latest = this.getLatestCheckOut(
                    dto.getHouseId(), dto.getContractId(), tenantId
            );
            if (latest != null && latest.getId().equals(handover.getId())) {
                // 调用合同服务更新状态
                contractService.updateContractStatus(dto.getContractId(), "COMPLETED");
            }
        }
        return handover.getId();
    }

    @Override
    public HouseHandover getLatestCheckOutByHouseAndContract(Long houseId, Long contractId) {
        Long tenantId = TenantContext.getTenantId();
        return this.getOne(new LambdaQueryWrapper<HouseHandover>()
                .eq(HouseHandover::getTenantId, tenantId)
                .eq(HouseHandover::getHouseId, houseId)
                .eq(HouseHandover::getContractId, contractId)
                .eq(HouseHandover::getHandoverType, "CHECK_OUT")
                .orderByDesc(HouseHandover::getHandoverTime)
                .last("LIMIT 1"));
    }

    @Override
    public HouseHandover getLatestCheckOut(Long houseId, Long contractId, Long tenantId) {
        LambdaQueryWrapper<HouseHandover> queryWrapper = new LambdaQueryWrapper<HouseHandover>()
                .eq(HouseHandover::getTenantId, tenantId)
                .eq(HouseHandover::getHouseId, houseId)
                .eq(HouseHandover::getContractId, contractId)
                .eq(HouseHandover::getHandoverType, "CHECK_OUT")
                .orderByDesc(HouseHandover::getHandoverTime)
                .last("LIMIT 1");
        // 第二个参数 false：多记录时取第一条，不抛异常
        return this.getOne(queryWrapper, false);
    }

    @Override
    public Page<HouseHandover> getHandoverPageByHouse(Page<HouseHandover> page, Long houseId) {
        Long tenantId = TenantContext.getTenantId();
        QueryWrapper<HouseHandover> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tenant_id", tenantId)
                .eq("house_id", houseId)
                .orderByDesc("handover_time");
        return baseMapper.selectPage(page, queryWrapper);
    }

    // 新增：按ID查询单个交接记录（带租户校验）
    @Override
    public HouseHandover getById(Long id) {
        Long tenantId = TenantContext.getTenantId();
        HouseHandover handover = baseMapper.selectById(id);
        if (handover == null || !handover.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "交接记录不存在或无权访问");
        }
        return handover;
    }

    // 新增：更新交接记录
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateHandover(Long id, HouseHandoverDTO dto) {
        // 校验记录存在性及租户归属
        if (existsByIdAndTenant(id)) {
            throw new BusinessException(404, "交接记录不存在或无权访问");
        }

        // 校验房源一致性（不允许跨房源更新）
        if (!dto.getHouseId().equals(baseMapper.selectById(id).getHouseId())) {
            throw new BusinessException(400, "不允许修改房源ID");
        }

        // DTO转实体并更新
        HouseHandover handover = BeanConvertUtil.convert(dto, HouseHandover.class);
        handover.setId(id);
        handover.setTenantId(TenantContext.getTenantId()); // 强制绑定当前租户
        return baseMapper.updateById(handover) > 0;
    }

    // 新增：删除交接记录
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteHandover(Long id) {
        if (existsByIdAndTenant(id)) {
            throw new BusinessException(404, "交接记录不存在或无权访问");
        }
        return baseMapper.deleteById(id) > 0;
    }

    // 新增：按合同ID查询交接记录
    @Override
    public List<HouseHandover> getByContractId(Long contractId) {
        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<HouseHandover> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HouseHandover::getTenantId, tenantId)
                .eq(HouseHandover::getContractId, contractId)
                .orderByDesc(HouseHandover::getHandoverTime);
        return baseMapper.selectList(queryWrapper);
    }

    // 新增：校验交接记录是否存在且属于当前租户
    @Override
    public boolean existsByIdAndTenant(Long id) {
        Long tenantId = TenantContext.getTenantId();
        int count = Math.toIntExact(baseMapper.selectCount(new LambdaQueryWrapper<HouseHandover>()
                .eq(HouseHandover::getId, id)
                .eq(HouseHandover::getTenantId, tenantId)));
        return !(count > 0);
    }
}

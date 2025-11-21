package com.house.deed.pavilion.module.houseHandover.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.aspect.annotation.AgentDataPermission;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.AgentContext;
import com.house.deed.pavilion.common.util.BeanConvertUtil;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.common.util.ValidateUtil;
import com.house.deed.pavilion.module.contract.service.IContractService;
import com.house.deed.pavilion.module.house.entity.House;
import com.house.deed.pavilion.module.house.service.IHouseService;
import com.house.deed.pavilion.module.houseHandover.entity.HouseHandover;
import com.house.deed.pavilion.module.houseHandover.mapper.HouseHandoverMapper;
import com.house.deed.pavilion.module.houseHandover.dto.HouseHandoverDTO;
import com.house.deed.pavilion.module.houseHandover.service.IHouseHandoverService;
import com.house.deed.pavilion.module.maintenanceOrder.entity.MaintenanceOrder;
import com.house.deed.pavilion.module.maintenanceOrder.service.IMaintenanceOrderService;
import io.micrometer.common.util.StringUtils;
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
        Long currentAgentId = AgentContext.getAgentId();
        // 1. 校验房源存在性及归属（当前租户+当前经纪人创建）
        House house = houseService.getById(dto.getHouseId());
        if (house == null || !house.getTenantId().equals(tenantId)) {
            throw new BusinessException(400, "房源不存在或无权访问");
        }
        if (!house.getCreateAgentId().equals(currentAgentId)) {
            throw new BusinessException(403, "无权为该房源创建交接记录：仅房源创建人可操作");
        }

        // 2. 草稿状态无需校验交接人、接收人等必填字段
        if ("CONFIRMED".equals(dto.getStatus())) {
            validateRequiredFields(dto); // 抽取必填字段校验逻辑
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
    // 查询指定房源的交接记录：关联房源创建人权限
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = House.class, // 关联核心实体：房源
            creatorField = "createAgentId", // 房源的创建人字段
            dataIdParam = "houseId" // 方法中房源ID的参数名（与方法参数Long houseId对应）
    )
    public List<HouseHandover> getByHouseId(Long houseId) {
        ValidateUtil.notNull(houseId, "房源ID不能为空");
        Long tenantId = TenantContext.getTenantId();
        // 基础查询逻辑（切面自动添加租户隔离和房源创建人过滤）
        return lambdaQuery()
                .eq(HouseHandover::getHouseId, houseId)
                .eq(HouseHandover::getTenantId, tenantId)
                .list();
    }

    /**
     * 校验已确认状态下的必填字段
     */
    private void validateRequiredFields(HouseHandoverDTO dto) {
        // 交接人、接收人校验（已存在）
        ValidateUtil.notNull(dto.getHandoverPerson(), "交接人不能为空");
        ValidateUtil.notNull(dto.getReceiver(), "接收人不能为空");

        // 核心关联字段校验（必须关联房源和合同）
        ValidateUtil.notNull(dto.getHouseId(), "房源ID不能为空");
        ValidateUtil.notNull(dto.getContractId(), "合同ID不能为空");

        // 交接时间校验（确认时必须明确交接时间）
        ValidateUtil.notNull(dto.getHandoverTime(), "交接时间不能为空");

        // 交接类型校验（退租交接必须明确类型）
        ValidateUtil.notNull(dto.getHandoverType(), "交接类型不能为空");
        if (!"CHECK_OUT".equals(dto.getHandoverType())) {
            throw new BusinessException(400, "退租交接记录类型必须为CHECK_OUT");
        }

        // 关键交接内容校验（如物品状态、费用结算等，根据业务最小必要原则补充）
        ValidateUtil.notNull(dto.getSettlementStatus(), "费用结算状态不能为空（如已结算/未结算）");
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
        Long currentAgentId = AgentContext.getAgentId();

        // 1. 校验房源是否为当前经纪人创建
        House house = houseService.getById(houseId);
        if (house == null || !house.getTenantId().equals(tenantId)
                || !house.getCreateAgentId().equals(currentAgentId)) {
            throw new BusinessException(403, "无权访问该房源的交接记录");
        }

        // 2. 查询该房源的交接记录
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
    // 更新交接记录：关联房源创建人权限
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.UPDATE,
            entityClass = House.class,
            creatorField = "createAgentId",
            dataIdParam = "dto.houseId" // 从DTO中获取房源ID（参数为HouseHandoverDTO dto）
    )
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

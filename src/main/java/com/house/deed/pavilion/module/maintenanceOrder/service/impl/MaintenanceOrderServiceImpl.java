package com.house.deed.pavilion.module.maintenanceOrder.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.aspect.annotation.AgentDataPermission;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.AgentContext;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.common.util.ValidateUtil;
import com.house.deed.pavilion.module.contract.entity.Contract;
import com.house.deed.pavilion.module.contract.service.IContractService;
import com.house.deed.pavilion.module.house.entity.House;
import com.house.deed.pavilion.module.house.service.IHouseService;
import com.house.deed.pavilion.module.houseHandover.entity.HouseHandover;
import com.house.deed.pavilion.module.houseHandover.mapper.HouseHandoverMapper;
import com.house.deed.pavilion.module.houseHandover.service.IHouseHandoverService;
import com.house.deed.pavilion.module.maintenanceOrder.entity.MaintenanceOrder;
import com.house.deed.pavilion.module.maintenanceOrder.mapper.MaintenanceOrderMapper;
import com.house.deed.pavilion.module.maintenanceOrder.service.IMaintenanceOrderService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * 房源维修工单表（租户级数据） 服务实现类
 * 包含工单创建、状态更新、权限校验等核心业务逻辑
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class MaintenanceOrderServiceImpl extends ServiceImpl<MaintenanceOrderMapper, MaintenanceOrder> implements IMaintenanceOrderService {

    @Resource
    private IHouseService houseService;

    // 工单编号生成计数器（实际生产环境建议用Redis自增）
    private final AtomicInteger orderNoCounter = new AtomicInteger(1);

    @Resource
    private IContractService contractService;

    @Resource
    private HouseHandoverMapper houseHandoverMapper;

    @Autowired
    @Lazy
    private IHouseHandoverService houseHandoverService;

    /**
     * 按房屋交接ID查询工单
     * 权限逻辑：自动校验交接记录归属 + 关联房源权限
     */
    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = HouseHandover.class, // 补充缺失的主实体类（必填项）
            multiEntityClasses = {House.class}, // 仅需补充关联的其他实体
            multiIdParams = {"handoverId", "houseId"}, // 与实体类顺序对应：交接ID参数 + 房源ID参数
            creatorField = "createAgentId" // 房源的创建人字段（最终权限校验依据）
    )
    public List<MaintenanceOrder> getByHouseHandoverId(Long handoverId, Long tenantId) {
        // 注解自动处理：
        // 1. 租户隔离（自动添加tenant_id条件）
        // 2. 交接记录归属校验（通过handoverId查询HouseHandover的tenant_id是否匹配）
        // 3. 关联房源权限校验（通过houseId查询House的create_agent_id是否为当前经纪人）
        return lambdaQuery()
                .eq(MaintenanceOrder::getHouseHandoverId, handoverId)
                .orderByDesc(MaintenanceOrder::getCreateTime)
                .list();
    }

    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = House.class,
            creatorField = "createAgentId",
            dataIdParam = "houseId"
    )
    public List<MaintenanceOrder> getByHouseId(Long houseId) {
        ValidateUtil.notNull(houseId, "房源ID不能为空");
        // 注解自动添加租户隔离 + 房源创建人权限过滤
        return baseMapper.selectList(new LambdaQueryWrapper<MaintenanceOrder>()
                .eq(MaintenanceOrder::getHouseId, houseId)
        );
    }

    /**
     * 创建维修工单
     * 权限逻辑：通过注解关联House+Contract双重校验，保留核心报修人权限校验
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.CREATE,
            entityClass = House.class, // 补充必填的主实体类（房源作为核心实体）
            multiEntityClasses = {Contract.class}, // 关联合同（主实体已指定House，这里补充其他实体）
            multiIdParams = {"order.houseId", "order.contractId"}, // 与实体类顺序对应：房源ID、合同ID
            creatorField = "createAgentId" // 房源创建人字段；合同通过切面扩展校验签约经纪人
    )
    public Long createOrder(MaintenanceOrder order) {
        Long tenantId = TenantContext.getTenantId();
        ValidateUtil.notNull(tenantId, "租户上下文获取失败");

        // 1. 基础参数校验
        validateOrderParams(order);

        // 2. 房屋交接记录业务校验
        if (order.getHouseHandoverId() != null) {
            HouseHandover handover = houseHandoverMapper.selectById(order.getHouseHandoverId());
            if (handover == null || !handover.getTenantId().equals(tenantId)) {
                throw new BusinessException(400, "关联的房屋交接记录不存在或无权访问");
            }
            if (!"CHECK_OUT".equals(handover.getHandoverType())) {
                throw new BusinessException(400, "仅退租交接记录可关联维修工单");
            }
            if (!handover.getHouseId().equals(order.getHouseId())) {
                throw new BusinessException(400, "维修工单房源与关联的交接记录房源不一致");
            }
        }

        // 3. 注解已完成：
        // - 房源属于当前租户且创建人匹配当前经纪人
        // - 合同属于当前租户且有效

        // 4. 补充报修人权限校验（确保方法存在且参数正确）
        validateReporterHasPermission(order, order.getHouseId(), tenantId);

        // 5. 生成订单号 + 填充默认值
        String orderNo = generateOrderNo(tenantId);
        order.setOrderNo(orderNo);
        order.setTenantId(tenantId);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        if (order.getUrgencyLevel() == null) {
            order.setUrgencyLevel((byte) 2);
        }
        if (StrUtil.isBlank(order.getStatus())) {
            order.setStatus("SUBMITTED");
        }

        // 6. 保存工单
        boolean saveSuccess = this.save(order);
        if (!saveSuccess) {
            throw new BusinessException(500, "工单创建失败");
        }
        return order.getId();
    }

    /**
     * 校验报修人是否有权限创建该房源的维修单
     * 规则：仅合同关联人（租户/房东/签约经纪人）可创建
     */
    private void validateReporterHasPermission(MaintenanceOrder order, Long houseId, Long tenantId) {
        String reporterType = order.getReporterType();
        Long reporterId = order.getReporterId();

        // 查询该房源的有效合同（租赁：执行中；买卖：已完成但可能有保修期）
        List<Contract> validContracts = contractService.lambdaQuery()
                .eq(Contract::getTenantId, tenantId)
                .eq(Contract::getHouseId, houseId)
                .in(Contract::getStatus, Arrays.asList("SIGNED", "EXECUTING", "COMPLETED")) // 有效合同状态
                .list();

        if (validContracts.isEmpty()) {
            throw new BusinessException(403, "该房源无有效合同，无法创建维修单");
        }

        // 校验报修人是否为合同关联方
        boolean hasPermission = validContracts.stream().anyMatch(contract -> {
            return switch (reporterType) {
                case "TENANT" -> // 租户：必须是合同中的客户
                        contract.getCustomerId().equals(reporterId);
                case "LANDLORD" -> // 房东：必须是合同中的房东
                        contract.getLandlordId().equals(reporterId);
                case "AGENT" -> // 经纪人：必须是合同的签约经纪人
                        contract.getAgentId().equals(reporterId);
                default -> false;
            };
        });

        if (!hasPermission) {
            throw new BusinessException(403, "报修人不是该房源的合同关联方，无权创建维修单");
        }
    }

    /**
     * 查询工单详情
     * 权限逻辑：校验工单归属 + 关联房源/合同权限
     */
    @Override
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.QUERY,
            entityClass = MaintenanceOrder.class,
            dataIdParam = "id",
            multiEntityClasses = {House.class, Contract.class}, // 关联房源 + 合同
            multiIdParams = {"houseId", "contractId"} // 从工单查询房源ID、合同ID
    )
    public MaintenanceOrder getOrderById(Long id) {
        ValidateUtil.notNull(id, "工单ID不能为空");
        // 注解自动处理：1. 租户隔离 2. 工单存在性 3. 关联房源/合同权限校验
        return this.getById(id);
    }

    /**
     * 更新工单状态
     * 权限逻辑：校验工单归属 + 关联房源/合同权限
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AgentDataPermission(
            operation = AgentDataPermission.OperationType.UPDATE,
            entityClass = MaintenanceOrder.class,
            dataIdParam = "updateInfo.id",
            multiEntityClasses = {House.class, Contract.class},
            multiIdParams = {"houseId", "contractId"}
    )
    public boolean updateOrderStatus(MaintenanceOrder updateInfo) {
        Long id = updateInfo.getId();
        ValidateUtil.notNull(id, "工单ID不能为空");

        // 1. 工单存在性由注解校验，直接获取
        MaintenanceOrder existOrder = this.getById(id);
        String oldStatus = existOrder.getStatus();
        String newStatus = updateInfo.getStatus();

        // 2. 状态流转合法性校验（业务逻辑保留）
        validateStatusTransition(oldStatus, newStatus);

        // 3. 完成状态特殊逻辑（业务逻辑保留）
        if ("COMPLETED".equals(newStatus) && !"COMPLETED".equals(oldStatus)) {
            updateInfo.setCompleteTime(LocalDateTime.now());
            syncToHouseHandover(existOrder);
        }

        // 4. 仅更新允许修改的字段（业务逻辑保留）
        MaintenanceOrder updateWrapper = new MaintenanceOrder();
        updateWrapper.setId(id);
        updateWrapper.setStatus(updateInfo.getStatus());
        updateWrapper.setRepairmanId(updateInfo.getRepairmanId());
        updateWrapper.setAppointmentTime(updateInfo.getAppointmentTime());
        updateWrapper.setCompleteTime(updateInfo.getCompleteTime());
        updateWrapper.setCostAmount(updateInfo.getCostAmount());
        updateWrapper.setCostBearer(updateInfo.getCostBearer());
        updateWrapper.setRemark(updateInfo.getRemark());
        updateWrapper.setUpdateTime(LocalDateTime.now());
        return this.updateById(updateWrapper);
    }

    // 以下工具方法保持不变
    private void syncToHouseHandover(MaintenanceOrder completedOrder) {
        Long houseId = completedOrder.getHouseId();
        Long contractId = completedOrder.getContractId();
        Long tenantId = completedOrder.getTenantId();

        HouseHandover latestCheckOut = houseHandoverService.getOne(new LambdaQueryWrapper<HouseHandover>()
                .eq(HouseHandover::getTenantId, tenantId)
                .eq(HouseHandover::getHouseId, houseId)
                .eq(HouseHandover::getContractId, contractId)
                .eq(HouseHandover::getHandoverType, "CHECK_OUT")
                .orderByDesc(HouseHandover::getHandoverTime)
                .last("LIMIT 1"));

        if (latestCheckOut != null) {
            latestCheckOut.setMaintenanceRemark(completedOrder.getRemark());
            latestCheckOut.setMaintenanceCost(completedOrder.getCostAmount());
            latestCheckOut.setMaintenanceBearer(completedOrder.getCostBearer());
            latestCheckOut.setLastMaintenanceId(completedOrder.getId());
            houseHandoverService.updateById(latestCheckOut);
        }
    }

    private String generateOrderNo(Long tenantId) {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = orderNoCounter.getAndIncrement() % 1000;
        return String.format("TENANT%d_MAINT%s%03d", tenantId, dateStr, seq);
    }

    private void validateOrderParams(MaintenanceOrder order) {
        ValidateUtil.notNull(order.getHouseId(), "房源ID不能为空");
        ValidateUtil.notNull(order.getReporterType(), "报修人类型不能为空");
        ValidateUtil.notNull(order.getReporterId(), "报修人ID不能为空");
        ValidateUtil.notNull(order.getReporterPhone(), "报修人电话不能为空");
        ValidateUtil.notNull(order.getMaintenanceType(), "维修类型不能为空");
        ValidateUtil.notNull(order.getDescription(), "故障描述不能为空");
    }

    private void validateStatusTransition(String oldStatus, String newStatus) {
        if ("SUBMITTED".equals(oldStatus)) {
            if (!"ASSIGNED".equals(newStatus)) {
                throw new BusinessException(400, "已提交状态仅能转为已分配");
            }
        } else if ("ASSIGNED".equals(oldStatus)) {
            if (!"COMPLETED".equals(newStatus)) {
                throw new BusinessException(400, "已分配状态仅能转为已完成");
            }
        } else if ("COMPLETED".equals(oldStatus)) {
            throw new BusinessException(400, "已完成工单不允许修改状态");
        }
    }
}
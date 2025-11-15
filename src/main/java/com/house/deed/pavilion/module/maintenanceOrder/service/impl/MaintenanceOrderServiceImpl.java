package com.house.deed.pavilion.module.maintenanceOrder.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.common.util.ValidateUtil;
import com.house.deed.pavilion.module.house.entity.House;
import com.house.deed.pavilion.module.house.service.IHouseService;
import com.house.deed.pavilion.module.maintenanceOrder.entity.MaintenanceOrder;
import com.house.deed.pavilion.module.maintenanceOrder.mapper.MaintenanceOrderMapper;
import com.house.deed.pavilion.module.maintenanceOrder.service.IMaintenanceOrderService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    /**
     * 创建维修工单（事务保证原子性）
     * 1. 校验必填参数 2. 校验房源归属 3. 生成唯一订单号 4. 填充租户ID和时间 5. 保存数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(MaintenanceOrder order) {
        Long tenantId = TenantContext.getTenantId();
        ValidateUtil.notNull(tenantId, "租户上下文获取失败");

        // 1. 基础参数校验
        validateOrderParams(order);

        // 2. 校验房源是否属于当前租户
        House house = houseService.getById(order.getHouseId());
        if (house == null || !house.getTenantId().equals(tenantId)) {
            throw new BusinessException(400, "房源不存在或不属于当前租户");
        }

        // 3. 生成租户内唯一订单号（格式：TENANT{租户ID}_MAINT{yyyyMMdd}{3位序号}）
        String orderNo = generateOrderNo(tenantId);
        order.setOrderNo(orderNo);

        // 4. 填充默认值
        order.setTenantId(tenantId);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        if (order.getUrgencyLevel() == null) {
            order.setUrgencyLevel((byte) 2); // 默认中等紧急
        }
        if (StrUtil.isBlank(order.getStatus())) {
            order.setStatus("SUBMITTED"); // 默认已提交状态
        }

        // 5. 保存工单
        boolean saveSuccess = this.save(order);
        if (!saveSuccess) {
            throw new BusinessException(500, "工单创建失败");
        }
        return order.getId();
    }

    /**
     * 查询工单详情（带租户权限过滤）
     */
    @Override
    public MaintenanceOrder getOrderById(Long id) {
        ValidateUtil.notNull(id, "工单ID不能为空");
        Long tenantId = TenantContext.getTenantId();

        return this.getOne(new LambdaQueryWrapper<MaintenanceOrder>()
                .eq(MaintenanceOrder::getId, id)
                .eq(MaintenanceOrder::getTenantId, tenantId));
    }

    /**
     * 更新工单状态（仅允许更新状态、维修师傅、费用等字段）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateOrderStatus(MaintenanceOrder updateInfo) {
        Long id = updateInfo.getId();
        Long tenantId = TenantContext.getTenantId();

        // 1. 校验工单是否存在且属于当前租户
        MaintenanceOrder existOrder = this.getOne(new LambdaQueryWrapper<MaintenanceOrder>()
                .eq(MaintenanceOrder::getId, id)
                .eq(MaintenanceOrder::getTenantId, tenantId));
        if (existOrder == null) {
            throw new BusinessException(404, "工单不存在或无权访问");
        }

        // 2. 校验状态流转合法性（示例：已提交→已分配→已完成）
        validateStatusTransition(existOrder.getStatus(), updateInfo.getStatus());

        // 3. 仅更新允许修改的字段
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

    /**
     * 生成租户内唯一工单编号
     */
    private String generateOrderNo(Long tenantId) {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = orderNoCounter.getAndIncrement() % 1000; // 取模防止溢出
        return String.format("TENANT%d_MAINT%s%03d", tenantId, dateStr, seq);
    }

    /**
     * 工单参数校验
     */
    private void validateOrderParams(MaintenanceOrder order) {
        ValidateUtil.notNull(order.getHouseId(), "房源ID不能为空");
        ValidateUtil.notNull(order.getReporterType(), "报修人类型不能为空");
        ValidateUtil.notNull(order.getReporterId(), "报修人ID不能为空");
        ValidateUtil.notNull(order.getReporterPhone(), "报修人电话不能为空");
        ValidateUtil.notNull(order.getMaintenanceType(), "维修类型不能为空");
        ValidateUtil.notNull(order.getDescription(), "故障描述不能为空");
    }

    /**
     * 校验状态流转合法性
     */
    private void validateStatusTransition(String oldStatus, String newStatus) {
        // 示例状态流转规则：SUBMITTED→ASSIGNED→COMPLETED
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
package com.house.deed.pavilion;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.entity.MaintenanceOrder;
import com.house.deed.pavilion.mapper.MaintenanceOrderMapper;
import com.house.deed.pavilion.service.impl.MaintenanceOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("维修工单服务单元测试")
@SpringBootTest
class MaintenanceOrderServiceImplTest {

    @Mock
    private MaintenanceOrderMapper maintenanceOrderMapper;

    @InjectMocks
    private MaintenanceOrderServiceImpl maintenanceOrderService;

    @Captor
    private ArgumentCaptor<LambdaQueryWrapper<MaintenanceOrder>> queryWrapperCaptor;

    @Captor
    private ArgumentCaptor<MaintenanceOrder> maintenanceOrderCaptor;

    private MaintenanceOrder testOrder;
    private static final Long TENANT_ID = 1001L;
    private static final Long ORDER_ID = 1L;
    private static final Long HOUSE_ID = 2001L;
    private static final Long CONTRACT_ID = 3001L;
    private static final Long HOUSE_HANDOVER_ID = 4001L;
    private static final Long REPORTER_ID = 5001L;
    private static final Long REPAIRMAN_ID = 6001L;
    private static final String ORDER_NO = "MO20241126001";
    private static final String REPORTER_TYPE = "TENANT";
    private static final String REPORTER_PHONE = "13800138000";
    private static final String MAINTENANCE_TYPE = "APPLIANCE";
    private static final String DESCRIPTION = "空调不制冷";
    private static final Byte URGENCY_LEVEL = 2;
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_ASSIGNED = "ASSIGNED";
    private static final String STATUS_REPAIRING = "REPAIRING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELED = "CANCELED";
    private static final LocalDateTime CREATE_TIME = LocalDateTime.of(2024, 11, 26, 10, 30);
    private static final LocalDateTime APPOINTMENT_TIME = LocalDateTime.of(2024, 11, 27, 14, 0);
    private static final LocalDateTime COMPLETE_TIME = LocalDateTime.of(2024, 11, 27, 16, 30);
    private static final BigDecimal COST_AMOUNT = new BigDecimal("150.00");
    private static final String COST_BEARER = "LANDLORD";
    private static final String REMARK = "更换压缩机，问题解决";

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testOrder = new MaintenanceOrder();
        testOrder.setId(ORDER_ID);
        testOrder.setTenantId(TENANT_ID);
        testOrder.setHouseId(HOUSE_ID);
        testOrder.setContractId(CONTRACT_ID);
        testOrder.setHouseHandoverId(null);
        testOrder.setOrderNo(ORDER_NO);
        testOrder.setReporterType(REPORTER_TYPE);
        testOrder.setReporterId(REPORTER_ID);
        testOrder.setReporterPhone(REPORTER_PHONE);
        testOrder.setMaintenanceType(MAINTENANCE_TYPE);
        testOrder.setDescription(DESCRIPTION);
        testOrder.setUrgencyLevel(URGENCY_LEVEL);
        testOrder.setStatus(STATUS_SUBMITTED);
        testOrder.setCreateTime(CREATE_TIME);
        testOrder.setAppointmentTime(null);
        testOrder.setRepairmanId(null);
        testOrder.setCompleteTime(null);
        testOrder.setCostAmount(null);
        testOrder.setCostBearer(null);
        testOrder.setRemark(null);

        // 设置baseMapper
        ReflectionTestUtils.setField(maintenanceOrderService, "baseMapper", maintenanceOrderMapper);
    }

    // ==================== saveMaintenanceOrder 测试 ====================

    @Test
    @DisplayName("新增维修工单 - 成功")
    void saveMaintenanceOrder_Success() {
        // 准备
        MaintenanceOrder newOrder = createOrder(null, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                ORDER_NO, REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE, MAINTENANCE_TYPE,
                DESCRIPTION, URGENCY_LEVEL, STATUS_SUBMITTED, CREATE_TIME, null, null,
                null, null, null, null);

        // 使用安全的Mock方式
        doReturn(1).when(maintenanceOrderMapper).insert(any(MaintenanceOrder.class));
        doReturn(0L).when(maintenanceOrderMapper).selectCount(any());

        // 执行
        boolean result = maintenanceOrderService.saveMaintenanceOrder(newOrder);

        // 验证
        assertTrue(result);
        verify(maintenanceOrderMapper).insert(maintenanceOrderCaptor.capture());
        MaintenanceOrder captured = maintenanceOrderCaptor.getValue();
        assertEquals(TENANT_ID, captured.getTenantId());
        assertEquals(HOUSE_ID, captured.getHouseId());
        assertEquals(ORDER_NO, captured.getOrderNo());
        assertEquals(STATUS_SUBMITTED, captured.getStatus());
    }

    @Test
    @DisplayName("新增维修工单 - 租户ID为空")
    void saveMaintenanceOrder_TenantIdNull() {
        // 准备
        MaintenanceOrder order = createOrder(null, null, HOUSE_ID, CONTRACT_ID, null,
                ORDER_NO, REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE, MAINTENANCE_TYPE,
                DESCRIPTION, URGENCY_LEVEL, STATUS_SUBMITTED, null, null, null,
                null, null, null, null);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> maintenanceOrderService.saveMaintenanceOrder(order));
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("新增维修工单 - 房源ID为空")
    void saveMaintenanceOrder_HouseIdNull() {
        // 准备
        MaintenanceOrder order = createOrder(null, TENANT_ID, null, CONTRACT_ID, null,
                ORDER_NO, REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE, MAINTENANCE_TYPE,
                DESCRIPTION, URGENCY_LEVEL, STATUS_SUBMITTED, null, null, null,
                null, null, null, null);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> maintenanceOrderService.saveMaintenanceOrder(order));
        assertEquals("房源ID不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("新增维修工单 - 工单编号为空")
    void saveMaintenanceOrder_OrderNoNull() {
        // 准备
        MaintenanceOrder order = createOrder(null, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                null, REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE, MAINTENANCE_TYPE,
                DESCRIPTION, URGENCY_LEVEL, STATUS_SUBMITTED, null, null, null,
                null, null, null, null);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> maintenanceOrderService.saveMaintenanceOrder(order));
        assertEquals("工单编号不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("新增维修工单 - 无效报修人类型")
    void saveMaintenanceOrder_InvalidReporterType() {
        // 准备
        MaintenanceOrder order = createOrder(null, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                ORDER_NO, "INVALID_TYPE", REPORTER_ID, REPORTER_PHONE, MAINTENANCE_TYPE,
                DESCRIPTION, URGENCY_LEVEL, STATUS_SUBMITTED, null, null, null,
                null, null, null, null);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> maintenanceOrderService.saveMaintenanceOrder(order));
        assertTrue(exception.getMessage().contains("无效报修人类型"));
    }

    @Test
    @DisplayName("新增维修工单 - 租户报修但合同ID为空")
    void saveMaintenanceOrder_TenantReporterWithoutContract() {
        // 准备：报修人类型为租户，但没有合同ID
        MaintenanceOrder order = createOrder(null, TENANT_ID, HOUSE_ID, null, null,
                ORDER_NO, REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE, MAINTENANCE_TYPE,
                DESCRIPTION, URGENCY_LEVEL, STATUS_SUBMITTED, null, null, null,
                null, null, null, null);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> maintenanceOrderService.saveMaintenanceOrder(order));
        assertEquals("租户报修（租赁场景）必须填写关联合同ID", exception.getMessage());
    }

    @Test
    @DisplayName("新增维修工单 - 初始状态非SUBMITTED")
    void saveMaintenanceOrder_InvalidInitialStatus() {
        // 准备
        MaintenanceOrder order = createOrder(null, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                ORDER_NO, REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE, MAINTENANCE_TYPE,
                DESCRIPTION, URGENCY_LEVEL, STATUS_ASSIGNED, null, null, null,
                null, null, null, null);

        // 使用安全的Mock方式
        doReturn(0L).when(maintenanceOrderMapper).selectCount(any());

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> maintenanceOrderService.saveMaintenanceOrder(order));
        assertEquals("新增工单仅允许初始状态为已提交（SUBMITTED）", exception.getMessage());
    }

    @Test
    @DisplayName("新增维修工单 - 工单编号重复")
    void saveMaintenanceOrder_DuplicateOrderNo() {
        // 准备
        MaintenanceOrder order = createOrder(null, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                ORDER_NO, REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE, MAINTENANCE_TYPE,
                DESCRIPTION, URGENCY_LEVEL, STATUS_SUBMITTED, null, null, null,
                null, null, null, null);

        // 使用安全的Mock方式
        doReturn(1L).when(maintenanceOrderMapper).selectCount(any());

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> maintenanceOrderService.saveMaintenanceOrder(order));
        assertTrue(exception.getMessage().contains("当前租户下工单编号已存在"));
    }

    @Test
    @DisplayName("新增维修工单 - 无效电话格式")
    void saveMaintenanceOrder_InvalidPhoneFormat() {
        // 准备
        MaintenanceOrder order = createOrder(null, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                ORDER_NO, REPORTER_TYPE, REPORTER_ID, "123456", MAINTENANCE_TYPE,
                DESCRIPTION, URGENCY_LEVEL, STATUS_SUBMITTED, null, null, null,
                null, null, null, null);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> maintenanceOrderService.saveMaintenanceOrder(order));
        assertTrue(exception.getMessage().contains("报修人电话格式错误"));
    }

    // ==================== updateMaintenanceOrderById 测试 ====================

    @Test
    @DisplayName("更新维修工单 - 成功（修改描述）")
    void updateMaintenanceOrderById_Success() {
        // 准备
        MaintenanceOrder updateRequest = new MaintenanceOrder();
        updateRequest.setId(ORDER_ID);
        updateRequest.setTenantId(TENANT_ID);
        updateRequest.setDescription("空调完全不制冷，需要紧急处理");

        // 使用安全的Mock方式
        doReturn(testOrder).when(maintenanceOrderMapper).selectOne(any());
        doReturn(1).when(maintenanceOrderMapper).updateById(any(MaintenanceOrder.class));

        // 执行
        boolean result = maintenanceOrderService.updateMaintenanceOrderById(updateRequest);

        // 验证
        assertTrue(result);
        verify(maintenanceOrderMapper).updateById(maintenanceOrderCaptor.capture());
        assertEquals("空调完全不制冷，需要紧急处理", maintenanceOrderCaptor.getValue().getDescription());
    }

    @Test
    @DisplayName("更新维修工单 - 工单不存在")
    void updateMaintenanceOrderById_NotFound() {
        // 准备
        MaintenanceOrder updateRequest = new MaintenanceOrder();
        updateRequest.setId(ORDER_ID);
        updateRequest.setTenantId(TENANT_ID);

        // 使用安全的Mock方式
        doReturn(null).when(maintenanceOrderMapper).selectOne(any());

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> maintenanceOrderService.updateMaintenanceOrderById(updateRequest));
        assertEquals("维修工单不存在或无权限操作", exception.getMessage());
    }

    @Test
    @DisplayName("更新维修工单 - 修改房源ID")
    void updateMaintenanceOrderById_ChangeHouseId() {
        // 准备
        MaintenanceOrder updateRequest = new MaintenanceOrder();
        updateRequest.setId(ORDER_ID);
        updateRequest.setTenantId(TENANT_ID);
        updateRequest.setHouseId(9999L);

        // 使用安全的Mock方式
        doReturn(testOrder).when(maintenanceOrderMapper).selectOne(any());

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> maintenanceOrderService.updateMaintenanceOrderById(updateRequest));
        assertEquals("房源ID不允许修改", exception.getMessage());
    }

    @Test
    @DisplayName("更新维修工单 - 修改报修人类型")
    void updateMaintenanceOrderById_ChangeReporterType() {
        // 准备
        MaintenanceOrder updateRequest = new MaintenanceOrder();
        updateRequest.setId(ORDER_ID);
        updateRequest.setTenantId(TENANT_ID);
        updateRequest.setReporterType("LANDLORD");

        // 使用安全的Mock方式
        doReturn(testOrder).when(maintenanceOrderMapper).selectOne(any());

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> maintenanceOrderService.updateMaintenanceOrderById(updateRequest));
        assertEquals("报修人类型不允许修改", exception.getMessage());
    }

    @Test
    @DisplayName("更新维修工单 - 工单编号重复")
    void updateMaintenanceOrderById_DuplicateOrderNo() {
        // 准备
        MaintenanceOrder updateRequest = new MaintenanceOrder();
        updateRequest.setId(ORDER_ID);
        updateRequest.setTenantId(TENANT_ID);
        updateRequest.setOrderNo("MO20241126002");

        // 使用安全的Mock方式
        doReturn(testOrder).when(maintenanceOrderMapper).selectOne(any());
        doReturn(1L).when(maintenanceOrderMapper).selectCount(any());

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> maintenanceOrderService.updateMaintenanceOrderById(updateRequest));
        assertTrue(exception.getMessage().contains("当前租户下工单编号已存在"));
    }

    @Test
    @DisplayName("更新维修工单 - 状态流转成功")
    void updateMaintenanceOrderById_StatusFlowSuccess() {
        // 准备：从SUBMITTED变更为ASSIGNED
        MaintenanceOrder updateRequest = new MaintenanceOrder();
        updateRequest.setId(ORDER_ID);
        updateRequest.setTenantId(TENANT_ID);
        updateRequest.setStatus(STATUS_ASSIGNED);
        updateRequest.setRepairmanId(REPAIRMAN_ID);
        updateRequest.setAppointmentTime(APPOINTMENT_TIME);

        // 使用安全的Mock方式
        doReturn(testOrder).when(maintenanceOrderMapper).selectOne(any());
        doReturn(1).when(maintenanceOrderMapper).updateById(any(MaintenanceOrder.class));

        // 执行
        boolean result = maintenanceOrderService.updateMaintenanceOrderById(updateRequest);

        // 验证
        assertTrue(result);
        verify(maintenanceOrderMapper).updateById(maintenanceOrderCaptor.capture());
        MaintenanceOrder updated = maintenanceOrderCaptor.getValue();
        assertEquals(STATUS_ASSIGNED, updated.getStatus());
        assertEquals(REPAIRMAN_ID, updated.getRepairmanId());
        assertEquals(APPOINTMENT_TIME, updated.getAppointmentTime());
    }

    @Test
    @DisplayName("更新维修工单 - 无效状态流转")
    void updateMaintenanceOrderById_InvalidStatusFlow() {
        // 准备：从SUBMITTED直接变更为COMPLETED（不允许）
        MaintenanceOrder updateRequest = new MaintenanceOrder();
        updateRequest.setId(ORDER_ID);
        updateRequest.setTenantId(TENANT_ID);
        updateRequest.setStatus(STATUS_COMPLETED);

        // 使用安全的Mock方式
        doReturn(testOrder).when(maintenanceOrderMapper).selectOne(any());

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> maintenanceOrderService.updateMaintenanceOrderById(updateRequest));
        assertTrue(exception.getMessage().contains("工单状态不允许从SUBMITTED变更为COMPLETED"));
    }

    @Test
    public void updateMaintenanceOrderById_CompletedStatusMissingFields() {
        // Arrange
        Long orderId = 1L;
        Long tenantId = 1001L;

        // 模拟数据库中已存在的工单（状态为REPAIRING）
        MaintenanceOrder existingOrder = createValidRepairingOrder(orderId, tenantId);

        when(maintenanceOrderMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(existingOrder);

        // 创建缺少字段的更新请求
        MaintenanceOrder updateToCompleted = new MaintenanceOrder();
        updateToCompleted.setId(orderId);
        updateToCompleted.setTenantId(tenantId);
        updateToCompleted.setStatus("COMPLETED");
        // 故意不设置完成时间、费用、承担方、备注等字段

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            maintenanceOrderService.updateMaintenanceOrderById(updateToCompleted);
        });

        // 验证异常消息 - 由于completeTime会被自动填充，所以第一个错误是维修费用
        assertEquals("已完成状态必须填写维修费用", exception.getMessage());

        verify(maintenanceOrderMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
        verify(maintenanceOrderMapper, never()).updateById(any(MaintenanceOrder.class));
    }

    private MaintenanceOrder createValidRepairingOrder(Long id, Long tenantId) {
        MaintenanceOrder order = new MaintenanceOrder();
        order.setId(id);
        order.setTenantId(tenantId);
        order.setHouseId(101L);
        order.setOrderNo("MO20250101001");
        order.setReporterType("TENANT");
        order.setReporterId(2001L);
        order.setReporterPhone("13800138000");
        order.setMaintenanceType("APPLIANCE");
        order.setDescription("空调不制冷");
        order.setUrgencyLevel((byte)2);
        order.setStatus("REPAIRING");
        order.setRepairmanId(100L);
        order.setAppointmentTime(LocalDateTime.now());
        order.setCreateTime(LocalDateTime.now().minusDays(1));
        order.setUpdateTime(LocalDateTime.now().minusHours(1));
        return order;
    }

    @Test
    @DisplayName("更新维修工单 - 完成状态缺少费用承担方")
    void updateMaintenanceOrderById_CompletedStatusMissingCostBearer() {
        // 准备：当前状态为REPAIRING
        MaintenanceOrder repairingOrder = createOrder(ORDER_ID, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                ORDER_NO, REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE, MAINTENANCE_TYPE,
                DESCRIPTION, URGENCY_LEVEL, STATUS_REPAIRING, CREATE_TIME, APPOINTMENT_TIME,
                REPAIRMAN_ID, null, null, null, null);

        // 更新为COMPLETED状态，但不提供费用承担方
        MaintenanceOrder updateRequest = new MaintenanceOrder();
        updateRequest.setId(ORDER_ID);
        updateRequest.setTenantId(TENANT_ID);
        updateRequest.setStatus(STATUS_COMPLETED);
        updateRequest.setCompleteTime(COMPLETE_TIME);
        updateRequest.setCostAmount(COST_AMOUNT);
        updateRequest.setRemark(REMARK);
        // 不设置costBearer

        // 使用安全的Mock方式
        doReturn(repairingOrder).when(maintenanceOrderMapper).selectOne(any());

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> maintenanceOrderService.updateMaintenanceOrderById(updateRequest));
        assertEquals("已完成状态必须填写费用承担方", exception.getMessage());
    }

    @Test
    @DisplayName("更新维修工单 - 派单状态缺少必填字段")
    void updateMaintenanceOrderById_AssignedStatusMissingFields() {
        // 准备
        MaintenanceOrder updateRequest = new MaintenanceOrder();
        updateRequest.setId(ORDER_ID);
        updateRequest.setTenantId(TENANT_ID);
        updateRequest.setStatus(STATUS_ASSIGNED);
        // 不设置repairmanId和appointmentTime

        // 使用安全的Mock方式
        doReturn(testOrder).when(maintenanceOrderMapper).selectOne(any());

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> maintenanceOrderService.updateMaintenanceOrderById(updateRequest));
        assertTrue(exception.getMessage().contains("已派单状态必须填写维修师傅ID") ||
                exception.getMessage().contains("已派单状态必须填写预约维修时间"));
    }

    @Test
    @DisplayName("更新维修工单 - 取消状态自动清空字段")
    void updateMaintenanceOrderById_CancelStatusAutoClearFields() {
        // 准备：当前状态为ASSIGNED
        MaintenanceOrder assignedOrder = createOrder(ORDER_ID, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                ORDER_NO, REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE, MAINTENANCE_TYPE,
                DESCRIPTION, URGENCY_LEVEL, STATUS_ASSIGNED, CREATE_TIME, APPOINTMENT_TIME,
                REPAIRMAN_ID, null, null, null, null);

        // 更新为CANCELED状态
        MaintenanceOrder updateRequest = new MaintenanceOrder();
        updateRequest.setId(ORDER_ID);
        updateRequest.setTenantId(TENANT_ID);
        updateRequest.setStatus(STATUS_CANCELED);

        // 使用安全的Mock方式
        doReturn(assignedOrder).when(maintenanceOrderMapper).selectOne(any());
        doReturn(1).when(maintenanceOrderMapper).updateById(any(MaintenanceOrder.class));

        // 执行
        boolean result = maintenanceOrderService.updateMaintenanceOrderById(updateRequest);

        // 验证
        assertTrue(result);
        verify(maintenanceOrderMapper).updateById(maintenanceOrderCaptor.capture());
        MaintenanceOrder updated = maintenanceOrderCaptor.getValue();
        assertEquals(STATUS_CANCELED, updated.getStatus());
        assertNull(updated.getRepairmanId());
        assertNull(updated.getAppointmentTime());
        assertNull(updated.getCompleteTime());
        assertNull(updated.getCostAmount());
        assertNull(updated.getCostBearer());
        assertNull(updated.getRemark());
    }

    // ==================== removeMaintenanceOrderById 测试 ====================

    @Test
    @DisplayName("删除维修工单 - 成功")
    void removeMaintenanceOrderById_Success() {
        // 使用安全的Mock方式
        doReturn(testOrder).when(maintenanceOrderMapper).selectOne(any());
        doReturn(1).when(maintenanceOrderMapper).deleteById(ORDER_ID);

        // 执行
        boolean result = maintenanceOrderService.removeMaintenanceOrderById(ORDER_ID, TENANT_ID);

        // 验证
        assertTrue(result);
        verify(maintenanceOrderMapper).deleteById(ORDER_ID);
    }

    @Test
    @DisplayName("删除维修工单 - 工单不存在")
    void removeMaintenanceOrderById_NotFound() {
        // 使用安全的Mock方式
        doReturn(null).when(maintenanceOrderMapper).selectOne(any());

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> maintenanceOrderService.removeMaintenanceOrderById(ORDER_ID, TENANT_ID));
        assertEquals("维修工单不存在或无权限操作", exception.getMessage());
    }

    @Test
    @DisplayName("删除维修工单 - 非可删除状态")
    void removeMaintenanceOrderById_InvalidStatus() {
        // 准备：状态为REPAIRING
        MaintenanceOrder repairingOrder = createOrder(ORDER_ID, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                ORDER_NO, REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE, MAINTENANCE_TYPE,
                DESCRIPTION, URGENCY_LEVEL, STATUS_REPAIRING, CREATE_TIME, APPOINTMENT_TIME,
                REPAIRMAN_ID, null, null, null, null);

        // 使用安全的Mock方式
        doReturn(repairingOrder).when(maintenanceOrderMapper).selectOne(any());

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> maintenanceOrderService.removeMaintenanceOrderById(ORDER_ID, TENANT_ID));
        assertTrue(exception.getMessage().contains("仅已提交/已派单状态的工单可删除"));
    }

    // ==================== getMaintenanceOrderById 测试 ====================

    @Test
    @DisplayName("根据ID查询维修工单 - 成功")
    void getMaintenanceOrderById_Success() {
        // 使用安全的Mock方式
        doReturn(testOrder).when(maintenanceOrderMapper).selectOne(any());

        // 执行
        MaintenanceOrder result = maintenanceOrderService.getMaintenanceOrderById(ORDER_ID, TENANT_ID);

        // 验证
        assertNotNull(result);
        assertEquals(ORDER_ID, result.getId());
        assertEquals(TENANT_ID, result.getTenantId());
    }

    @Test
    @DisplayName("根据ID查询维修工单 - 不存在")
    void getMaintenanceOrderById_NotFound() {
        // 使用安全的Mock方式
        doReturn(null).when(maintenanceOrderMapper).selectOne(any());

        // 执行
        MaintenanceOrder result = maintenanceOrderService.getMaintenanceOrderById(ORDER_ID, TENANT_ID);

        // 验证
        assertNull(result);
    }

    // ==================== pageQuery 测试 ====================

    @Test
    @DisplayName("分页查询 - 成功")
    void pageQuery_Success() {
        // 准备
        Page<MaintenanceOrder> page = new Page<>(1, 10);
        Map<String, Object> params = new HashMap<>();
        params.put("houseId", HOUSE_ID);
        params.put("status", STATUS_SUBMITTED);

        Page<MaintenanceOrder> resultPage = new Page<>(1, 10, 1);
        resultPage.setRecords(Collections.singletonList(testOrder));

        // 使用安全的Mock方式
        doReturn(resultPage).when(maintenanceOrderMapper).selectPage(eq(page), any());

        // 执行
        IPage<MaintenanceOrder> result = maintenanceOrderService.pageQuery(page, params, TENANT_ID);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(ORDER_ID, result.getRecords().get(0).getId());
    }

    @Test
    @DisplayName("分页查询 - 空参数")
    void pageQuery_EmptyParams() {
        // 准备
        Page<MaintenanceOrder> page = new Page<>(1, 10);
        Page<MaintenanceOrder> resultPage = new Page<>(1, 10, 0);
        resultPage.setRecords(Collections.emptyList());

        // 使用安全的Mock方式
        doReturn(resultPage).when(maintenanceOrderMapper).selectPage(eq(page), any());

        // 执行
        IPage<MaintenanceOrder> result = maintenanceOrderService.pageQuery(page, null, TENANT_ID);

        // 验证
        assertNotNull(result);
        assertEquals(0, result.getRecords().size());
    }

    @Test
    @DisplayName("分页查询 - 带时间范围")
    void pageQuery_WithDateRange() {
        // 准备
        Page<MaintenanceOrder> page = new Page<>(1, 10);
        Map<String, Object> params = new HashMap<>();
        params.put("startCreateTime", LocalDateTime.of(2024, 1, 1, 0, 0));
        params.put("endCreateTime", LocalDateTime.of(2024, 12, 31, 23, 59));

        Page<MaintenanceOrder> resultPage = new Page<>(1, 10, 1);
        resultPage.setRecords(Collections.singletonList(testOrder));

        // 使用安全的Mock方式
        doReturn(resultPage).when(maintenanceOrderMapper).selectPage(eq(page), any());

        // 执行
        maintenanceOrderService.pageQuery(page, params, TENANT_ID);

        // 验证
        verify(maintenanceOrderMapper).selectPage(eq(page), queryWrapperCaptor.capture());
    }

    // ==================== listByConditions 测试 ====================

    @Test
    @DisplayName("多条件查询列表 - 成功")
    void listByConditions_Success() {
        // 准备
        Map<String, Object> params = new HashMap<>();
        params.put("houseId", HOUSE_ID);
        params.put("maintenanceType", MAINTENANCE_TYPE);

        List<MaintenanceOrder> mockList = Arrays.asList(
                testOrder,
                createOrder(2L, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                        "MO20241126002", REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE,
                        "WATER", "水管漏水", (byte)3, STATUS_SUBMITTED, CREATE_TIME,
                        null, null, null, null, null, null)
        );

        // 使用安全的Mock方式
        doReturn(mockList).when(maintenanceOrderMapper).selectList(any());

        // 执行
        List<MaintenanceOrder> result = maintenanceOrderService.listByConditions(params, TENANT_ID);

        // 验证
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    // ==================== listByHouseId 测试 ====================

    @Test
    @DisplayName("根据房源ID查询工单列表 - 成功")
    void listByHouseId_Success() {
        // 准备
        List<MaintenanceOrder> mockList = Arrays.asList(
                testOrder,
                createOrder(2L, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                        "MO20241126002", REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE,
                        MAINTENANCE_TYPE, "冰箱不制冷", (byte)1, STATUS_COMPLETED,
                        CREATE_TIME.minusDays(1), APPOINTMENT_TIME.minusDays(1),
                        REPAIRMAN_ID, COMPLETE_TIME.minusDays(1), COST_AMOUNT,
                        COST_BEARER, "更换压缩机")
        );

        // 使用安全的Mock方式
        doReturn(mockList).when(maintenanceOrderMapper).selectList(any());

        // 执行
        List<MaintenanceOrder> result = maintenanceOrderService.listByHouseId(HOUSE_ID, TENANT_ID);

        // 验证
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(HOUSE_ID, result.get(0).getHouseId());
    }

    // ==================== listByReporter 测试 ====================

    @Test
    @DisplayName("根据报修人查询工单列表 - 成功")
    void listByReporter_Success() {
        // 准备
        List<MaintenanceOrder> mockList = Arrays.asList(
                testOrder,
                createOrder(2L, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                        "MO20241126002", REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE,
                        "DOOR_WINDOW", "窗户关不严", (byte)1, STATUS_COMPLETED,
                        CREATE_TIME.minusDays(7), APPOINTMENT_TIME.minusDays(6),
                        REPAIRMAN_ID, COMPLETE_TIME.minusDays(6), new BigDecimal("80.00"),
                        "TENANT", "调整合页")
        );

        // 使用安全的Mock方式
        doReturn(mockList).when(maintenanceOrderMapper).selectList(any());

        // 执行
        List<MaintenanceOrder> result = maintenanceOrderService.listByReporter(REPORTER_ID, REPORTER_TYPE, TENANT_ID);

        // 验证
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(REPORTER_ID, result.get(0).getReporterId());
        assertEquals(REPORTER_TYPE, result.get(0).getReporterType());
    }

    // ==================== batchSaveMaintenanceOrders 测试 ====================

    @Test
    @DisplayName("批量新增维修工单 - 成功")
    void batchSaveMaintenanceOrders_Success() {
        // 准备
        List<MaintenanceOrder> orders = Arrays.asList(
                createOrder(null, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                        "MO20241126001", REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE,
                        MAINTENANCE_TYPE, DESCRIPTION, URGENCY_LEVEL, STATUS_SUBMITTED,
                        null, null, null, null, null, null, null),
                createOrder(null, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                        "MO20241126002", REPORTER_TYPE, REPORTER_ID, "13900139000",
                        "WATER", "水管漏水", (byte)3, STATUS_SUBMITTED,
                        null, null, null, null, null, null, null)
        );

        // 使用spy模拟saveBatch方法
        MaintenanceOrderServiceImpl spyService = spy(maintenanceOrderService);
        doReturn(true).when(spyService).saveBatch(orders);

        // 模拟工单编号查询 - 返回空列表
        doReturn(Collections.emptyList()).when(maintenanceOrderMapper).selectList(any());

        // 执行
        boolean result = spyService.batchSaveMaintenanceOrders(orders);

        // 验证
        assertTrue(result);
        verify(spyService).saveBatch(orders);
    }

    @Test
    @DisplayName("批量新增维修工单 - 空列表")
    void batchSaveMaintenanceOrders_EmptyList() {
        // 执行
        boolean result = maintenanceOrderService.batchSaveMaintenanceOrders(Collections.emptyList());

        // 验证
        assertFalse(result);
        verify(maintenanceOrderMapper, never()).insert(any());
    }

    @Test
    @DisplayName("批量新增维修工单 - 不同租户")
    void batchSaveMaintenanceOrders_DifferentTenants() {
        // 准备
        List<MaintenanceOrder> orders = Arrays.asList(
                createOrder(null, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                        "MO20241126001", REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE,
                        MAINTENANCE_TYPE, DESCRIPTION, URGENCY_LEVEL, STATUS_SUBMITTED,
                        null, null, null, null, null, null, null),
                createOrder(null, 9999L, HOUSE_ID, CONTRACT_ID, null,
                        "MO20241126002", REPORTER_TYPE, REPORTER_ID, "13900139000",
                        "WATER", "水管漏水", (byte)3, STATUS_SUBMITTED,
                        null, null, null, null, null, null, null)
        );

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> maintenanceOrderService.batchSaveMaintenanceOrders(orders));
        assertEquals("批量新增的工单必须属于同一租户", exception.getMessage());
    }

    @Test
    @DisplayName("批量新增维修工单 - 重复工单编号")
    void batchSaveMaintenanceOrders_DuplicateOrderNos() {
        // 准备：两个工单有相同的编号
        List<MaintenanceOrder> orders = Arrays.asList(
                createOrder(null, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                        "MO20241126001", REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE,
                        MAINTENANCE_TYPE, DESCRIPTION, URGENCY_LEVEL, STATUS_SUBMITTED,
                        null, null, null, null, null, null, null),
                createOrder(null, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                        "MO20241126001", REPORTER_TYPE, REPORTER_ID, "13900139000",
                        "WATER", "水管漏水", (byte)3, STATUS_SUBMITTED,
                        null, null, null, null, null, null, null)
        );

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> maintenanceOrderService.batchSaveMaintenanceOrders(orders));
        assertEquals("批量工单中存在重复的工单编号", exception.getMessage());
    }

    @Test
    @DisplayName("批量新增维修工单 - 工单编号已存在")
    void batchSaveMaintenanceOrders_OrderNoAlreadyExists() {
        // 准备
        List<MaintenanceOrder> orders = Collections.singletonList(
                createOrder(null, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                        "MO20241126001", REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE,
                        MAINTENANCE_TYPE, DESCRIPTION, URGENCY_LEVEL, STATUS_SUBMITTED,
                        null, null, null, null, null, null, null)
        );

        // 模拟查询返回已存在的工单
        MaintenanceOrder existOrder = new MaintenanceOrder();
        existOrder.setOrderNo("MO20241126001");

        // 使用安全的Mock方式
        doReturn(Collections.singletonList(existOrder)).when(maintenanceOrderMapper).selectList(any());

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> maintenanceOrderService.batchSaveMaintenanceOrders(orders));
        assertTrue(exception.getMessage().contains("以下工单编号已存在"));
    }

    // ==================== batchUpdateOrderStatus 测试 ====================

    @Test
    @DisplayName("批量更新工单状态 - 成功")
    void batchUpdateOrderStatus_Success() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 使用spy来模拟validateOrderIdsBelongToTenant方法
        MaintenanceOrderServiceImpl spyService = spy(maintenanceOrderService);
        doNothing().when(spyService).validateOrderIdsBelongToTenant(TENANT_ID, ids);

        // 模拟查询返回工单状态
        List<MaintenanceOrder> mockOrders = Arrays.asList(
                createOrder(1L, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                        "MO20241126001", REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE,
                        MAINTENANCE_TYPE, DESCRIPTION, URGENCY_LEVEL, STATUS_SUBMITTED,
                        null, null, null, null, null, null, null),
                createOrder(2L, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                        "MO20241126002", REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE,
                        MAINTENANCE_TYPE, DESCRIPTION, URGENCY_LEVEL, STATUS_SUBMITTED,
                        null, null, null, null, null, null, null),
                createOrder(3L, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                        "MO20241126003", REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE,
                        MAINTENANCE_TYPE, DESCRIPTION, URGENCY_LEVEL, STATUS_SUBMITTED,
                        null, null, null, null, null, null, null)
        );

        // 使用安全的Mock方式
        doReturn(mockOrders).when(maintenanceOrderMapper).selectList(any());
        doReturn(3).when(maintenanceOrderMapper).update(any(MaintenanceOrder.class), any());

        // 执行
        boolean result = spyService.batchUpdateOrderStatus(ids, STATUS_ASSIGNED, TENANT_ID);

        // 验证
        assertTrue(result);
        verify(maintenanceOrderMapper).update(maintenanceOrderCaptor.capture(), any());
        assertEquals(STATUS_ASSIGNED, maintenanceOrderCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("批量更新工单状态 - 无效状态")
    void batchUpdateOrderStatus_InvalidStatus() {
        // 准备
        List<Long> ids = Collections.singletonList(ORDER_ID);

        // 执行 & 验证 - 应该抛出异常
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            maintenanceOrderService.batchUpdateOrderStatus(ids, "INVALID_STATUS", TENANT_ID);
        });

        // 验证异常消息
        assertEquals("无效目标状态：INVALID_STATUS", exception.getMessage());

        // 验证没有执行更新操作
        verify(maintenanceOrderMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("批量更新工单状态 - 空ID列表")
    void batchUpdateOrderStatus_EmptyIds() {
        // 执行 & 验证 - 应该抛出异常
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            maintenanceOrderService.batchUpdateOrderStatus(Collections.emptyList(), STATUS_ASSIGNED, TENANT_ID);
        });

        // 验证异常消息
        assertEquals("工单ID列表不能为空", exception.getMessage());

        // 验证没有执行数据库操作
        verify(maintenanceOrderMapper, never()).selectList(any());
        verify(maintenanceOrderMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("批量更新工单状态 - 无效状态流转")
    void batchUpdateOrderStatus_InvalidStatusFlow() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L);

        // 使用spy
        MaintenanceOrderServiceImpl spyService = spy(maintenanceOrderService);
        doNothing().when(spyService).validateOrderIdsBelongToTenant(TENANT_ID, ids);

        // 模拟查询返回工单状态，其中包含一个不能流转到目标状态的工单
        List<MaintenanceOrder> mockOrders = Arrays.asList(
                createOrder(1L, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                        "MO20241126001", REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE,
                        MAINTENANCE_TYPE, DESCRIPTION, URGENCY_LEVEL, STATUS_SUBMITTED,
                        null, null, null, null, null, null, null),
                createOrder(2L, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                        "MO20241126002", REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE,
                        MAINTENANCE_TYPE, DESCRIPTION, URGENCY_LEVEL, STATUS_COMPLETED, // 已完成状态不能流转
                        null, null, null, null, null, null, null)
        );

        // 使用安全的Mock方式
        doReturn(mockOrders).when(maintenanceOrderMapper).selectList(any());

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> spyService.batchUpdateOrderStatus(ids, STATUS_ASSIGNED, TENANT_ID));
        assertTrue(exception.getMessage().contains("以下工单不允许变更为ASSIGNED状态"));
    }

    // ==================== batchRemoveMaintenanceOrders 测试 ====================

    @Test
    @DisplayName("批量删除维修工单 - 成功")
    void batchRemoveMaintenanceOrders_Success() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 使用spy
        MaintenanceOrderServiceImpl spyService = spy(maintenanceOrderService);
        doNothing().when(spyService).validateOrderIdsBelongToTenant(TENANT_ID, ids);

        // 模拟查询返回可删除状态的工单数量
        doReturn(0L).when(maintenanceOrderMapper).selectCount(any());
        doReturn(3).when(maintenanceOrderMapper).deleteBatchIds(ids);

        // 执行
        boolean result = spyService.batchRemoveMaintenanceOrders(ids, TENANT_ID);

        // 验证
        assertTrue(result);
        verify(maintenanceOrderMapper).deleteBatchIds(ids);
    }

    @Test
    @DisplayName("批量删除维修工单 - 存在非可删除状态工单")
    void batchRemoveMaintenanceOrders_NonDeletableStatus() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L);

        // 使用spy
        MaintenanceOrderServiceImpl spyService = spy(maintenanceOrderService);
        doNothing().when(spyService).validateOrderIdsBelongToTenant(TENANT_ID, ids);

        // 模拟查询返回有非可删除状态的工单
        doReturn(1L).when(maintenanceOrderMapper).selectCount(any());

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> spyService.batchRemoveMaintenanceOrders(ids, TENANT_ID));
        assertEquals("存在非已提交/已派单状态的工单，不允许批量删除", exception.getMessage());
    }

    // ==================== validateOrderIdsBelongToTenant 测试 ====================

    @Test
    @DisplayName("验证工单ID属于租户 - 成功")
    void validateOrderIdsBelongToTenant_Success() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        List<MaintenanceOrder> mockOrders = Arrays.asList(
                createOrder(1L, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                        "MO20241126001", REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE,
                        MAINTENANCE_TYPE, DESCRIPTION, URGENCY_LEVEL, STATUS_SUBMITTED,
                        null, null, null, null, null, null, null),
                createOrder(2L, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                        "MO20241126002", REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE,
                        MAINTENANCE_TYPE, DESCRIPTION, URGENCY_LEVEL, STATUS_SUBMITTED,
                        null, null, null, null, null, null, null),
                createOrder(3L, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                        "MO20241126003", REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE,
                        MAINTENANCE_TYPE, DESCRIPTION, URGENCY_LEVEL, STATUS_SUBMITTED,
                        null, null, null, null, null, null, null)
        );

        // 使用安全的Mock方式
        doReturn(mockOrders).when(maintenanceOrderMapper).selectList(any());

        // 执行 - 不应抛出异常
        assertDoesNotThrow(() ->
                maintenanceOrderService.validateOrderIdsBelongToTenant(TENANT_ID, ids));
    }

    @Test
    @DisplayName("验证工单ID属于租户 - 部分ID不存在")
    void validateOrderIdsBelongToTenant_SomeIdsNotExist() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L, 999L);

        List<MaintenanceOrder> mockOrders = Arrays.asList(
                createOrder(1L, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                        "MO20241126001", REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE,
                        MAINTENANCE_TYPE, DESCRIPTION, URGENCY_LEVEL, STATUS_SUBMITTED,
                        null, null, null, null, null, null, null),
                createOrder(2L, TENANT_ID, HOUSE_ID, CONTRACT_ID, null,
                        "MO20241126002", REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE,
                        MAINTENANCE_TYPE, DESCRIPTION, URGENCY_LEVEL, STATUS_SUBMITTED,
                        null, null, null, null, null, null, null)
                // 没有ID为999的记录
        );

        // 使用安全的Mock方式
        doReturn(mockOrders).when(maintenanceOrderMapper).selectList(any());

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> maintenanceOrderService.validateOrderIdsBelongToTenant(TENANT_ID, ids));
        assertTrue(exception.getMessage().contains("以下工单ID不存在"));
    }

    @Test
    @DisplayName("验证工单ID属于租户 - 租户不匹配")
    void validateOrderIdsBelongToTenant_TenantMismatch() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L);

        List<MaintenanceOrder> mockOrders = Arrays.asList(
                createOrder(1L, 9999L, HOUSE_ID, CONTRACT_ID, null,
                        "MO20241126001", REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE,
                        MAINTENANCE_TYPE, DESCRIPTION, URGENCY_LEVEL, STATUS_SUBMITTED,
                        null, null, null, null, null, null, null),
                createOrder(2L, 9999L, HOUSE_ID, CONTRACT_ID, null,
                        "MO20241126002", REPORTER_TYPE, REPORTER_ID, REPORTER_PHONE,
                        MAINTENANCE_TYPE, DESCRIPTION, URGENCY_LEVEL, STATUS_SUBMITTED,
                        null, null, null, null, null, null, null)
        );

        // 使用安全的Mock方式
        doReturn(mockOrders).when(maintenanceOrderMapper).selectList(any());

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> maintenanceOrderService.validateOrderIdsBelongToTenant(TENANT_ID, ids));
        assertTrue(exception.getMessage().contains("无权限操作以下工单ID"));
    }

    // ==================== 辅助方法 ====================

    private MaintenanceOrder createOrder(Long id, Long tenantId, Long houseId, Long contractId,
                                         Long houseHandoverId, String orderNo, String reporterType,
                                         Long reporterId, String reporterPhone, String maintenanceType,
                                         String description, Byte urgencyLevel, String status,
                                         LocalDateTime createTime, LocalDateTime appointmentTime,
                                         Long repairmanId, LocalDateTime completeTime, BigDecimal costAmount,
                                         String costBearer, String remark) {
        MaintenanceOrder order = new MaintenanceOrder();
        order.setId(id);
        order.setTenantId(tenantId);
        order.setHouseId(houseId);
        order.setContractId(contractId);
        order.setHouseHandoverId(houseHandoverId);
        order.setOrderNo(orderNo);
        order.setReporterType(reporterType);
        order.setReporterId(reporterId);
        order.setReporterPhone(reporterPhone);
        order.setMaintenanceType(maintenanceType);
        order.setDescription(description);
        order.setUrgencyLevel(urgencyLevel);
        order.setStatus(status);
        order.setCreateTime(createTime);
        order.setAppointmentTime(appointmentTime);
        order.setRepairmanId(repairmanId);
        order.setCompleteTime(completeTime);
        order.setCostAmount(costAmount);
        order.setCostBearer(costBearer);
        order.setRemark(remark);
        return order;
    }
}
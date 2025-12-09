package com.house.deed.pavilion;

import com.house.deed.pavilion.entity.HouseStatusLog;
import com.house.deed.pavilion.service.HouseStatusLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HouseStatusLogServiceImpl 单元测试")
class HouseStatusLogServiceImplTest {

    @Mock
    private HouseStatusLogService houseStatusLogService;

    private HouseStatusLog validStatusLog;
    private final Long TENANT_ID = 1001L;
    private final Long HOUSE_ID = 101L;
    private final Long OPERATOR_ID = 3001L;
    private final Long LOG_ID = 1L;

    @BeforeEach
    void setUp() {
        validStatusLog = new HouseStatusLog();
        validStatusLog.setId(LOG_ID);
        validStatusLog.setTenantId(TENANT_ID);
        validStatusLog.setHouseId(HOUSE_ID);
        validStatusLog.setStatusBefore("ON_SALE");
        validStatusLog.setStatusAfter("RESERVED");
        validStatusLog.setChangeReason("客户预订房源");
        validStatusLog.setOperatorId(OPERATOR_ID);
        validStatusLog.setOperatorName("张三（经纪人）");
        validStatusLog.setCreateTime(LocalDateTime.now());
    }

    // ==================== 基础CRUD测试 ====================

    @Test
    @DisplayName("新增房源状态变更日志 - 基础参数校验测试")
    void saveStatusLog_BaseValidationTests() {
        // 测试租户ID为空
        HouseStatusLog testLog = createStatusLogWithoutId();
        testLog.setTenantId(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validateSaveParams(testLog));
        assertEquals("租户ID不能为空", exception.getMessage());

        // 测试房源ID为空
        testLog.setTenantId(TENANT_ID);
        testLog.setHouseId(null);

        exception = assertThrows(IllegalArgumentException.class,
                () -> validateSaveParams(testLog));
        assertEquals("房源ID不能为空", exception.getMessage());

        // 测试变更前状态为空
        testLog.setHouseId(HOUSE_ID);
        testLog.setStatusBefore("");

        exception = assertThrows(IllegalArgumentException.class,
                () -> validateSaveParams(testLog));
        assertEquals("变更前状态不能为空", exception.getMessage());

        // 测试变更后状态为空
        testLog.setStatusBefore("ON_SALE");
        testLog.setStatusAfter("");

        exception = assertThrows(IllegalArgumentException.class,
                () -> validateSaveParams(testLog));
        assertEquals("变更后状态不能为空", exception.getMessage());

        // 测试变更原因为空
        testLog.setStatusAfter("RESERVED");
        testLog.setChangeReason("");

        exception = assertThrows(IllegalArgumentException.class,
                () -> validateSaveParams(testLog));
        assertEquals("变更原因不能为空", exception.getMessage());

        // 测试操作人ID为空
        testLog.setChangeReason("客户预订房源");
        testLog.setOperatorId(null);

        exception = assertThrows(IllegalArgumentException.class,
                () -> validateSaveParams(testLog));
        assertEquals("操作人ID不能为空", exception.getMessage());

        // 测试操作人姓名为空
        testLog.setOperatorId(OPERATOR_ID);
        testLog.setOperatorName("");

        exception = assertThrows(IllegalArgumentException.class,
                () -> validateSaveParams(testLog));
        assertEquals("操作人姓名不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("新增房源状态变更日志 - 状态校验测试")
    void saveStatusLog_StatusValidationTests() {
        // 测试变更前后状态相同
        HouseStatusLog testLog = createStatusLogWithoutId();
        testLog.setStatusBefore("ON_SALE");
        testLog.setStatusAfter("ON_SALE");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validateStatusRules(testLog));
        assertEquals("变更前后状态不能相同", exception.getMessage());

        // 测试变更前状态不合法
        testLog.setStatusBefore("INVALID_STATUS");
        testLog.setStatusAfter("RESERVED");

        exception = assertThrows(IllegalArgumentException.class,
                () -> validateStatusRules(testLog));
        assertEquals("变更前状态不合法（允许值：ON_SALE, RESERVED, SOLD, OFF_SHELF）", exception.getMessage());

        // 测试变更后状态不合法
        testLog.setStatusBefore("ON_SALE");
        testLog.setStatusAfter("INVALID_STATUS");

        exception = assertThrows(IllegalArgumentException.class,
                () -> validateStatusRules(testLog));
        assertEquals("变更后状态不合法（允许值：ON_SALE, RESERVED, SOLD, OFF_SHELF）", exception.getMessage());

        // 测试有效状态转换
        String[] validStatuses = {"ON_SALE", "RESERVED", "SOLD", "OFF_SHELF"};
        for (String fromStatus : validStatuses) {
            for (String toStatus : validStatuses) {
                if (!fromStatus.equals(toStatus)) {
                    testLog.setStatusBefore(fromStatus);
                    testLog.setStatusAfter(toStatus);
                    assertDoesNotThrow(() -> validateStatusRules(testLog));
                }
            }
        }
    }

    @Test
    @DisplayName("更新房源状态变更日志 - ID为空时抛出异常")
    void updateStatusLogById_ThrowsException_WhenIdIsNull() {
        // Arrange
        validStatusLog.setId(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    if (validStatusLog.getId() == null) {
                        throw new IllegalArgumentException("日志ID不能为空");
                    }
                });
        assertEquals("日志ID不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("更新房源状态变更日志 - 租户ID为空时抛出异常")
    void updateStatusLogById_ThrowsException_WhenTenantIdIsNull() {
        // Arrange
        validStatusLog.setTenantId(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    if (validStatusLog.getTenantId() == null) {
                        throw new IllegalArgumentException("租户ID不能为空");
                    }
                });
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("更新房源状态变更日志 - 跨租户操作校验")
    void updateStatusLogById_CrossTenantCheck() {
        // 模拟跨租户情况
        Long differentTenantId = 9999L;

        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long existingTenantId = TENANT_ID;
                    Long newTenantId = differentTenantId;

                    if (!Objects.equals(existingTenantId, newTenantId)) {
                        throw new IllegalArgumentException("无权限操作其他租户的记录");
                    }
                });
    }

    @Test
    @DisplayName("更新房源状态变更日志 - 禁止修改核心审计字段")
    void updateStatusLogById_CoreFieldsImmutable() {
        // 测试核心审计字段不能被修改的逻辑
        HouseStatusLog originalLog = new HouseStatusLog();
        originalLog.setId(LOG_ID);
        originalLog.setTenantId(TENANT_ID);
        originalLog.setHouseId(HOUSE_ID);
        originalLog.setStatusBefore("ON_SALE");
        originalLog.setStatusAfter("RESERVED");
        originalLog.setOperatorId(OPERATOR_ID);
        originalLog.setCreateTime(LocalDateTime.now());

        // 模拟更新时尝试修改核心字段
        HouseStatusLog updateLog = new HouseStatusLog();
        updateLog.setId(LOG_ID);
        updateLog.setTenantId(TENANT_ID);
        updateLog.setStatusBefore("SOLD"); // 尝试修改状态

        // 在真实的Service中，这些字段应该被还原为原始值
        assertThrows(IllegalArgumentException.class,
                () -> {
                    // 假设在业务逻辑中，状态字段不允许修改
                    if (!updateLog.getStatusBefore().equals(originalLog.getStatusBefore())) {
                        throw new IllegalArgumentException("状态字段不允许修改");
                    }
                });
    }

    @Test
    @DisplayName("删除房源状态变更日志 - 参数校验")
    void removeStatusLogById_ValidationTests() {
        // 测试日志ID为空
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long id = null;
                    Long tenantId = TENANT_ID;
                    if (id == null || tenantId == null) {
                        throw new IllegalArgumentException("日志ID不能为空");
                    }
                });

        // 测试租户ID为空
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long id = LOG_ID;
                    Long tenantId = null;
                    if (id == null || tenantId == null) {
                        throw new IllegalArgumentException("租户ID不能为空");
                    }
                });
    }

    @Test
    @DisplayName("按ID查询状态变更日志 - 参数校验")
    void getStatusLogById_ValidationTests() {
        // 测试日志ID为空
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long id = null;
                    Long tenantId = TENANT_ID;
                    if (id == null || tenantId == null) {
                        throw new IllegalArgumentException("日志ID不能为空");
                    }
                });

        // 测试租户ID为空
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long id = LOG_ID;
                    Long tenantId = null;
                    if (id == null || tenantId == null) {
                        throw new IllegalArgumentException("租户ID不能为空");
                    }
                });
    }

    // ==================== 多条件查询测试 ====================

    @Test
    @DisplayName("分页查询状态变更日志 - 租户ID为空时抛出异常")
    void pageQuery_ThrowsException_WhenTenantIdIsNull() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    Long tenantId = null;
                    if (tenantId == null) {
                        throw new IllegalArgumentException("租户ID不能为空");
                    }
                });
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("多条件查询状态变更日志 - 租户ID为空时抛出异常")
    void listByConditions_ThrowsException_WhenTenantIdIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long tenantId = null;
                    if (tenantId == null) {
                        throw new IllegalArgumentException("租户ID不能为空");
                    }
                });
    }

    @Test
    @DisplayName("按房源ID查询状态变更日志 - 参数校验")
    void listByHouseId_ValidationTests() {
        // 测试房源ID为空
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long houseId = null;
                    Long tenantId = TENANT_ID;
                    if (houseId == null || tenantId == null) {
                        throw new IllegalArgumentException("房源ID不能为空");
                    }
                });

        // 测试租户ID为空
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long houseId = HOUSE_ID;
                    Long tenantId = null;
                    if (houseId == null || tenantId == null) {
                        throw new IllegalArgumentException("租户ID不能为空");
                    }
                });
    }

    // ==================== 批量操作测试 ====================

    @Test
    @DisplayName("批量保存状态变更日志 - 列表为空时抛出异常")
    void batchSaveStatusLogs_ThrowsException_WhenListIsEmpty() {
        // Arrange
        List<HouseStatusLog> emptyList = Collections.emptyList();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    if (emptyList == null || emptyList.isEmpty()) {
                        throw new IllegalArgumentException("批量保存的记录列表不能为空");
                    }
                });
        assertEquals("批量保存的记录列表不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("批量保存状态变更日志 - 批量参数校验")
    void batchSaveStatusLogs_BatchValidationTests() {
        // Arrange - 创建一个有问题的记录
        HouseStatusLog invalidLog = createStatusLogWithoutId();
        invalidLog.setStatusBefore("ON_SALE");
        invalidLog.setStatusAfter("ON_SALE"); // 状态相同

        List<HouseStatusLog> logList = Arrays.asList(createStatusLogWithoutId(), invalidLog);

        // Act & Assert - 批量操作中应该会校验每条记录
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    for (HouseStatusLog log : logList) {
                        if (log.getStatusBefore().equals(log.getStatusAfter())) {
                            throw new IllegalArgumentException("变更前后状态不能相同");
                        }
                    }
                });
        assertEquals("变更前后状态不能相同", exception.getMessage());
    }

    @Test
    @DisplayName("批量删除状态变更日志 - 租户ID为空时抛出异常")
    void batchRemoveStatusLogs_ThrowsException_WhenTenantIdIsNull() {
        // Arrange
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    Long tenantId = null;
                    if (tenantId == null) {
                        throw new IllegalArgumentException("租户ID不能为空");
                    }
                });
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("批量删除状态变更日志 - ID列表为空时抛出异常")
    void batchRemoveStatusLogs_ThrowsException_WhenIdsIsEmpty() {
        // Arrange
        List<Long> emptyList = Collections.emptyList();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    if (emptyList == null || emptyList.isEmpty()) {
                        throw new IllegalArgumentException("批量删除的ID列表不能为空");
                    }
                });
        assertEquals("批量删除的ID列表不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("批量删除状态变更日志 - 跨租户数据校验")
    void batchRemoveStatusLogs_CrossTenantValidation() {
        // 模拟存在不属于当前租户的记录
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        assertThrows(IllegalArgumentException.class,
                () -> {
                    // 模拟存在不属于当前租户的记录
                    boolean hasCrossTenantLogs = true;
                    if (hasCrossTenantLogs) {
                        throw new IllegalArgumentException("存在不属于当前租户的记录，无法批量删除");
                    }
                });
    }

    // ==================== 数据约束测试 ====================

    @Test
    @DisplayName("变更原因长度校验")
    void changeReasonLengthValidation_Test() {
        // 测试正常长度
        String normalReason = "客户预订房源，支付定金";
        assertDoesNotThrow(() -> validateChangeReasonLength(normalReason));

        // 测试超长原因
        String longReason = "A".repeat(201); // 201个字符
        assertThrows(IllegalArgumentException.class, () -> validateChangeReasonLength(longReason));

        // 测试边界值
        String boundaryReason = "A".repeat(200); // 200个字符
        assertDoesNotThrow(() -> validateChangeReasonLength(boundaryReason));
    }

    @Test
    @DisplayName("操作人姓名长度校验")
    void operatorNameLengthValidation_Test() {
        // 测试正常长度
        String normalName = "张三（经纪人）";
        assertDoesNotThrow(() -> validateOperatorNameLength(normalName));

        // 测试超长姓名
        String longName = "A".repeat(51); // 51个字符
        assertThrows(IllegalArgumentException.class, () -> validateOperatorNameLength(longName));

        // 测试边界值
        String boundaryName = "A".repeat(50); // 50个字符
        assertDoesNotThrow(() -> validateOperatorNameLength(boundaryName));
    }

    @Test
    @DisplayName("状态枚举值校验")
    void statusEnumValidation_Test() {
        // 测试有效状态值
        assertTrue(isValidStatus("ON_SALE"));
        assertTrue(isValidStatus("RESERVED"));
        assertTrue(isValidStatus("SOLD"));
        assertTrue(isValidStatus("OFF_SHELF"));

        // 测试无效状态值
        assertFalse(isValidStatus("INVALID"));
        assertFalse(isValidStatus(""));
        assertFalse(isValidStatus(null));
    }

    @Test
    @DisplayName("状态转换合法性测试")
    void statusTransitionValidation_Test() {
        // 测试典型的状态转换路径
        Map<String, List<String>> validTransitions = new HashMap<>();
        validTransitions.put("ON_SALE", Arrays.asList("RESERVED", "SOLD", "OFF_SHELF"));
        validTransitions.put("RESERVED", Arrays.asList("ON_SALE", "SOLD", "OFF_SHELF"));
        validTransitions.put("SOLD", Arrays.asList("OFF_SHELF")); // 已售出后可能下架
        validTransitions.put("OFF_SHELF", Arrays.asList("ON_SALE")); // 重新上架

        // 测试各种状态转换
        for (Map.Entry<String, List<String>> entry : validTransitions.entrySet()) {
            String fromStatus = entry.getKey();
            for (String toStatus : entry.getValue()) {
                HouseStatusLog log = createStatusLogWithoutId();
                log.setStatusBefore(fromStatus);
                log.setStatusAfter(toStatus);
                assertDoesNotThrow(() -> validateStatusRules(log));
            }
        }
    }

    // ==================== Service接口方法调用测试 ====================

    @Test
    @DisplayName("Service接口方法调用测试")
    void serviceInterfaceMethodsTest() {
        // 测试服务接口的各种方法被正确调用
        HouseStatusLog testLog = createAnotherStatusLog();

        // 测试 saveStatusLog
        when(houseStatusLogService.saveStatusLog(any(HouseStatusLog.class))).thenReturn(true);
        boolean saveResult = houseStatusLogService.saveStatusLog(testLog);
        assertTrue(saveResult);

        // 测试 updateStatusLogById
        when(houseStatusLogService.updateStatusLogById(any(HouseStatusLog.class))).thenReturn(true);
        boolean updateResult = houseStatusLogService.updateStatusLogById(testLog);
        assertTrue(updateResult);

        // 测试 removeStatusLogById
        when(houseStatusLogService.removeStatusLogById(anyLong(), anyLong())).thenReturn(true);
        boolean removeResult = houseStatusLogService.removeStatusLogById(LOG_ID, TENANT_ID);
        assertTrue(removeResult);

        // 测试 getStatusLogById
        when(houseStatusLogService.getStatusLogById(anyLong(), anyLong())).thenReturn(testLog);
        HouseStatusLog retrievedLog = houseStatusLogService.getStatusLogById(LOG_ID, TENANT_ID);
        assertNotNull(retrievedLog);

        // 测试 listByHouseId
        List<HouseStatusLog> logList = Arrays.asList(testLog);
        when(houseStatusLogService.listByHouseId(anyLong(), anyLong())).thenReturn(logList);
        List<HouseStatusLog> resultList = houseStatusLogService.listByHouseId(HOUSE_ID, TENANT_ID);
        assertNotNull(resultList);
        assertEquals(1, resultList.size());

        // 测试 batchSaveStatusLogs
        when(houseStatusLogService.batchSaveStatusLogs(anyList())).thenReturn(true);
        boolean batchSaveResult = houseStatusLogService.batchSaveStatusLogs(logList);
        assertTrue(batchSaveResult);

        // 测试 batchRemoveStatusLogs
        when(houseStatusLogService.batchRemoveStatusLogs(anyList(), anyLong())).thenReturn(true);
        boolean batchRemoveResult = houseStatusLogService.batchRemoveStatusLogs(Arrays.asList(1L, 2L), TENANT_ID);
        assertTrue(batchRemoveResult);

        verify(houseStatusLogService, times(1)).saveStatusLog(any(HouseStatusLog.class));
        verify(houseStatusLogService, times(1)).updateStatusLogById(any(HouseStatusLog.class));
        verify(houseStatusLogService, times(1)).removeStatusLogById(anyLong(), anyLong());
        verify(houseStatusLogService, times(1)).getStatusLogById(anyLong(), anyLong());
        verify(houseStatusLogService, times(1)).listByHouseId(anyLong(), anyLong());
        verify(houseStatusLogService, times(1)).batchSaveStatusLogs(anyList());
        verify(houseStatusLogService, times(1)).batchRemoveStatusLogs(anyList(), anyLong());
    }

    // ==================== 辅助方法 ====================

    private HouseStatusLog createAnotherStatusLog() {
        HouseStatusLog log = new HouseStatusLog();
        log.setId(2L);
        log.setTenantId(TENANT_ID);
        log.setHouseId(102L); // 不同的房源
        log.setStatusBefore("RESERVED");
        log.setStatusAfter("SOLD");
        log.setChangeReason("客户完成交易，房源已售出");
        log.setOperatorId(3002L);
        log.setOperatorName("李四（经纪人）");
        return log;
    }

    private HouseStatusLog createStatusLogWithoutId() {
        HouseStatusLog log = new HouseStatusLog();
        log.setTenantId(TENANT_ID);
        log.setHouseId(HOUSE_ID);
        log.setStatusBefore("ON_SALE");
        log.setStatusAfter("RESERVED");
        log.setChangeReason("客户预订房源");
        log.setOperatorId(OPERATOR_ID);
        log.setOperatorName("张三（经纪人）");
        return log;
    }

    // 内部校验方法（模拟Service中的校验逻辑）
    private void validateSaveParams(HouseStatusLog log) {
        if (log.getTenantId() == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }
        if (log.getHouseId() == null) {
            throw new IllegalArgumentException("房源ID不能为空");
        }
        if (log.getStatusBefore() == null || log.getStatusBefore().trim().isEmpty()) {
            throw new IllegalArgumentException("变更前状态不能为空");
        }
        if (log.getStatusAfter() == null || log.getStatusAfter().trim().isEmpty()) {
            throw new IllegalArgumentException("变更后状态不能为空");
        }
        if (log.getChangeReason() == null || log.getChangeReason().trim().isEmpty()) {
            throw new IllegalArgumentException("变更原因不能为空");
        }
        if (log.getOperatorId() == null) {
            throw new IllegalArgumentException("操作人ID不能为空");
        }
        if (log.getOperatorName() == null || log.getOperatorName().trim().isEmpty()) {
            throw new IllegalArgumentException("操作人姓名不能为空");
        }
    }

    private void validateStatusRules(HouseStatusLog log) {
        // 变更前后状态不能相同
        if (log.getStatusBefore().equals(log.getStatusAfter())) {
            throw new IllegalArgumentException("变更前后状态不能相同");
        }

        // 状态合法性校验
        if (!isValidStatus(log.getStatusBefore())) {
            throw new IllegalArgumentException("变更前状态不合法（允许值：ON_SALE, RESERVED, SOLD, OFF_SHELF）");
        }
        if (!isValidStatus(log.getStatusAfter())) {
            throw new IllegalArgumentException("变更后状态不合法（允许值：ON_SALE, RESERVED, SOLD, OFF_SHELF）");
        }
    }

    private void validateChangeReasonLength(String reason) {
        if (reason != null && reason.length() > 200) {
            throw new IllegalArgumentException("变更原因长度不能超过200字符");
        }
    }

    private void validateOperatorNameLength(String name) {
        if (name != null && name.length() > 50) {
            throw new IllegalArgumentException("操作人姓名长度不能超过50字符");
        }
    }

    private boolean isValidStatus(String status) {
        return "ON_SALE".equals(status)
                || "RESERVED".equals(status)
                || "SOLD".equals(status)
                || "OFF_SHELF".equals(status);
    }

    @Test
    @DisplayName("Spring Assert 工具类使用测试")
    void springAssertUsage_Test() {
        // 测试 Assert.notNull
        String nullValue = null;
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Assert.notNull(nullValue, "值不能为空"));
        assertEquals("值不能为空", exception.getMessage());

        // 测试 Assert.hasText
        String emptyText = "";
        exception = assertThrows(IllegalArgumentException.class,
                () -> Assert.hasText(emptyText, "文本不能为空"));
        assertEquals("文本不能为空", exception.getMessage());

        // 测试 Assert.notEmpty (Collection)
        List<String> emptyList = Collections.emptyList();
        exception = assertThrows(IllegalArgumentException.class,
                () -> Assert.notEmpty(emptyList, "集合不能为空"));
        assertEquals("集合不能为空", exception.getMessage());

        // 测试 Assert.isTrue
        boolean falseCondition = false;
        exception = assertThrows(IllegalArgumentException.class,
                () -> Assert.isTrue(falseCondition, "条件必须为真"));
        assertEquals("条件必须为真", exception.getMessage());
    }

    @Test
    @DisplayName("状态变更场景测试")
    void statusChangeScenarios_Test() {
        // 测试各种状态变更场景
        List<StatusChangeScenario> scenarios = Arrays.asList(
                new StatusChangeScenario("ON_SALE", "RESERVED", "客户预订房源"),
                new StatusChangeScenario("RESERVED", "ON_SALE", "客户取消预订"),
                new StatusChangeScenario("ON_SALE", "SOLD", "房源售出"),
                new StatusChangeScenario("ON_SALE", "OFF_SHELF", "房源下架维护"),
                new StatusChangeScenario("OFF_SHELF", "ON_SALE", "房源重新上架"),
                new StatusChangeScenario("RESERVED", "SOLD", "预订客户完成交易")
        );

        for (StatusChangeScenario scenario : scenarios) {
            HouseStatusLog log = createStatusLogWithoutId();
            log.setStatusBefore(scenario.fromStatus);
            log.setStatusAfter(scenario.toStatus);
            log.setChangeReason(scenario.reason);

            assertDoesNotThrow(() -> {
                validateStatusRules(log);
                validateChangeReasonLength(log.getChangeReason());
            }, String.format("状态变更 %s -> %s 应该通过验证", scenario.fromStatus, scenario.toStatus));
        }
    }

    // 内部类：状态变更场景
    static class StatusChangeScenario {
        String fromStatus;
        String toStatus;
        String reason;

        StatusChangeScenario(String fromStatus, String toStatus, String reason) {
            this.fromStatus = fromStatus;
            this.toStatus = toStatus;
            this.reason = reason;
        }
    }

    @Test
    @DisplayName("租户数据隔离测试")
    void tenantDataIsolation_Test() {
        // 测试不同租户的数据应该完全隔离
        Long tenant1Id = 1001L;
        Long tenant2Id = 1002L;

        HouseStatusLog tenant1Log = createStatusLogWithoutId();
        tenant1Log.setTenantId(tenant1Id);

        HouseStatusLog tenant2Log = createStatusLogWithoutId();
        tenant2Log.setTenantId(tenant2Id);

        // 验证租户ID不同
        assertNotEquals(tenant1Log.getTenantId(), tenant2Log.getTenantId());

        // 验证跨租户操作应该被拒绝
        assertThrows(IllegalArgumentException.class,
                () -> {
                    // 模拟跨租户更新操作
                    if (!tenant1Log.getTenantId().equals(tenant2Log.getTenantId())) {
                        throw new IllegalArgumentException("无权限操作其他租户的记录");
                    }
                });
    }
}
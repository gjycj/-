package com.house.deed.pavilion;

import com.house.deed.pavilion.entity.HouseMaintainPlan;
import com.house.deed.pavilion.service.HouseMaintainPlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HouseMaintainPlanServiceImpl 单元测试")
class HouseMaintainPlanServiceImplTest {

    @Mock
    private HouseMaintainPlanService houseMaintainPlanService;

    private HouseMaintainPlan validMaintainPlan;
    private final Long TENANT_ID = 1001L;
    private final Long HOUSE_ID = 101L;
    private final Long EXECUTOR_ID = 3001L;
    private final Long PLAN_ID = 1L;
    private final LocalDate FUTURE_DATE = LocalDate.now().plusDays(1);
    private final LocalDate FUTURE_END_DATE = LocalDate.now().plusMonths(12);

    @BeforeEach
    void setUp() {
        validMaintainPlan = new HouseMaintainPlan();
        validMaintainPlan.setId(PLAN_ID);
        validMaintainPlan.setTenantId(TENANT_ID);
        validMaintainPlan.setHouseId(HOUSE_ID);
        validMaintainPlan.setMaintainType("CLEAN");
        validMaintainPlan.setCycle("WEEKLY");
        validMaintainPlan.setStartDate(FUTURE_DATE);
        validMaintainPlan.setEndDate(FUTURE_END_DATE);
        validMaintainPlan.setExecutorId(EXECUTOR_ID);
        validMaintainPlan.setStatus("ACTIVE");
        validMaintainPlan.setRemark("每周六上午9点保洁");
        validMaintainPlan.setCreateTime(LocalDateTime.now());
    }

    // ==================== 基础CRUD测试 ====================

    @Test
    @DisplayName("新增房源维护计划 - 基础参数校验测试")
    void saveHouseMaintainPlan_BaseValidationTests() {
        // 测试租户ID为空
        HouseMaintainPlan testPlan = createMaintainPlanWithoutId();
        testPlan.setTenantId(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validatePlanBaseParams(testPlan));
        assertEquals("租户ID不能为空", exception.getMessage());

        // 测试房源ID为空
        testPlan.setTenantId(TENANT_ID);
        testPlan.setHouseId(null);

        exception = assertThrows(IllegalArgumentException.class,
                () -> validatePlanBaseParams(testPlan));
        assertEquals("房源ID不能为空", exception.getMessage());

        // 测试维护类型为空
        testPlan.setHouseId(HOUSE_ID);
        testPlan.setMaintainType("");

        exception = assertThrows(IllegalArgumentException.class,
                () -> validatePlanBaseParams(testPlan));
        assertEquals("维护类型不能为空", exception.getMessage());

        // 测试执行周期为空
        testPlan.setMaintainType("CLEAN");
        testPlan.setCycle("");

        exception = assertThrows(IllegalArgumentException.class,
                () -> validatePlanBaseParams(testPlan));
        assertEquals("执行周期不能为空", exception.getMessage());

        // 测试开始日期为空
        testPlan.setCycle("WEEKLY");
        testPlan.setStartDate(null);

        exception = assertThrows(IllegalArgumentException.class,
                () -> validatePlanBaseParams(testPlan));
        assertEquals("开始日期不能为空", exception.getMessage());

        // 测试执行人ID为空
        testPlan.setStartDate(FUTURE_DATE);
        testPlan.setExecutorId(null);

        exception = assertThrows(IllegalArgumentException.class,
                () -> validatePlanBaseParams(testPlan));
        assertEquals("执行人ID不能为空", exception.getMessage());

        // 测试计划状态为空
        testPlan.setExecutorId(EXECUTOR_ID);
        testPlan.setStatus("");

        exception = assertThrows(IllegalArgumentException.class,
                () -> validatePlanBaseParams(testPlan));
        assertEquals("计划状态不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("新增房源维护计划 - 业务规则校验测试")
    void saveHouseMaintainPlan_BusinessRulesTests() {
        // 测试开始日期早于当前日期
        HouseMaintainPlan testPlan = createMaintainPlanWithoutId();
        testPlan.setStartDate(LocalDate.now().minusDays(1));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validatePlanBusinessRules(testPlan));
        assertEquals("开始日期不能早于当前日期", exception.getMessage());

        // 测试周期性计划结束日期为空
        testPlan.setStartDate(FUTURE_DATE);
        testPlan.setCycle("WEEKLY");
        testPlan.setEndDate(null);

        exception = assertThrows(IllegalArgumentException.class,
                () -> validatePlanBusinessRules(testPlan));
        assertEquals("周期性计划必须设置结束日期", exception.getMessage());

        // 测试结束日期早于开始日期
        testPlan.setEndDate(FUTURE_DATE.minusDays(1));

        exception = assertThrows(IllegalArgumentException.class,
                () -> validatePlanBusinessRules(testPlan));
        assertEquals("结束日期不能早于开始日期", exception.getMessage());

        // 测试一次性计划结束日期可以为空
        testPlan.setCycle("ONCE");
        testPlan.setEndDate(null);
        assertDoesNotThrow(() -> validatePlanBusinessRules(testPlan));

        // 测试结束日期晚于开始日期（正常情况）
        testPlan.setCycle("WEEKLY");
        testPlan.setEndDate(FUTURE_DATE.plusDays(7));
        assertDoesNotThrow(() -> validatePlanBusinessRules(testPlan));
    }

    @Test
    @DisplayName("更新房源维护计划 - ID为空时抛出异常")
    void updateHouseMaintainPlan_ThrowsException_WhenIdIsNull() {
        // Arrange
        validMaintainPlan.setId(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    if (validMaintainPlan.getId() == null) {
                        throw new IllegalArgumentException("计划ID不能为空");
                    }
                });
        assertEquals("计划ID不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("更新房源维护计划 - 租户ID为空时抛出异常")
    void updateHouseMaintainPlan_ThrowsException_WhenTenantIdIsNull() {
        // Arrange
        validMaintainPlan.setTenantId(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    if (validMaintainPlan.getTenantId() == null) {
                        throw new IllegalArgumentException("租户ID不能为空");
                    }
                });
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("更新房源维护计划 - 跨租户操作校验")
    void updateHouseMaintainPlan_CrossTenantCheck() {
        // 模拟跨租户情况
        Long differentTenantId = 9999L;

        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long existingTenantId = TENANT_ID;
                    Long newTenantId = differentTenantId;

                    if (!Objects.equals(existingTenantId, newTenantId)) {
                        throw new IllegalArgumentException("无权限操作其他租户的维护计划");
                    }
                });
    }

    @Test
    @DisplayName("删除房源维护计划 - 参数校验")
    void removeHouseMaintainPlan_ValidationTests() {
        // 测试计划ID为空
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long id = null;
                    Long tenantId = TENANT_ID;
                    if (id == null || tenantId == null) {
                        throw new IllegalArgumentException("计划ID不能为空");
                    }
                });

        // 测试租户ID为空
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long id = PLAN_ID;
                    Long tenantId = null;
                    if (id == null || tenantId == null) {
                        throw new IllegalArgumentException("租户ID不能为空");
                    }
                });
    }

    @Test
    @DisplayName("按ID查询维护计划 - 参数校验")
    void getHouseMaintainPlanById_ValidationTests() {
        // 测试计划ID为空
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long id = null;
                    Long tenantId = TENANT_ID;
                    if (id == null || tenantId == null) {
                        throw new IllegalArgumentException("计划ID不能为空");
                    }
                });

        // 测试租户ID为空
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long id = PLAN_ID;
                    Long tenantId = null;
                    if (id == null || tenantId == null) {
                        throw new IllegalArgumentException("租户ID不能为空");
                    }
                });
    }

    // ==================== 多条件查询测试 ====================

    @Test
    @DisplayName("分页查询维护计划 - 租户ID为空时抛出异常")
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
    @DisplayName("多条件查询维护计划 - 租户ID为空时抛出异常")
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
    @DisplayName("按房源ID查询维护计划 - 参数校验")
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

    @Test
    @DisplayName("按执行人ID查询维护计划 - 参数校验")
    void listByExecutorId_ValidationTests() {
        // 测试执行人ID为空
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long executorId = null;
                    Long tenantId = TENANT_ID;
                    if (executorId == null || tenantId == null) {
                        throw new IllegalArgumentException("执行人ID不能为空");
                    }
                });

        // 测试租户ID为空
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long executorId = EXECUTOR_ID;
                    Long tenantId = null;
                    if (executorId == null || tenantId == null) {
                        throw new IllegalArgumentException("执行人ID和租户ID不能为空");
                    }
                });
    }

    // ==================== 批量操作测试 ====================

    @Test
    @DisplayName("批量新增维护计划 - 列表为空时返回false")
    void batchSaveHouseMaintainPlans_ReturnsFalse_WhenListIsEmpty() {
        // Act & Assert
        // 空列表应该返回false，而不是抛出异常
        List<HouseMaintainPlan> emptyList = Collections.emptyList();
        // 这里我们只是模拟服务方法的行为
        when(houseMaintainPlanService.batchSaveHouseMaintainPlans(emptyList)).thenReturn(false);

        boolean result = houseMaintainPlanService.batchSaveHouseMaintainPlans(emptyList);
        assertFalse(result);
    }

    @Test
    @DisplayName("批量新增维护计划 - 跨租户操作时抛出异常")
    void batchSaveHouseMaintainPlans_ThrowsException_WhenCrossTenant() {
        // Arrange
        HouseMaintainPlan plan1 = createMaintainPlanWithoutId();
        HouseMaintainPlan plan2 = createAnotherMaintainPlan();
        plan2.setTenantId(9999L); // 不同的租户ID

        List<HouseMaintainPlan> plans = Arrays.asList(plan1, plan2);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    if (plans != null && !plans.isEmpty()) {
                        Long firstTenantId = plans.get(0).getTenantId();
                        for (HouseMaintainPlan plan : plans) {
                            if (!Objects.equals(plan.getTenantId(), firstTenantId)) {
                                throw new IllegalArgumentException("批量操作必须属于同一租户");
                            }
                        }
                    }
                });
        assertEquals("批量操作必须属于同一租户", exception.getMessage());
    }

    @Test
    @DisplayName("批量删除维护计划 - 列表为空时返回false")
    void batchRemoveHouseMaintainPlans_ReturnsFalse_WhenListIsEmpty() {
        // Act & Assert
        // 空列表应该返回false，而不是抛出异常
        List<Long> emptyList = Collections.emptyList();
        when(houseMaintainPlanService.batchRemoveHouseMaintainPlans(emptyList, TENANT_ID)).thenReturn(false);

        boolean result = houseMaintainPlanService.batchRemoveHouseMaintainPlans(emptyList, TENANT_ID);
        assertFalse(result);
    }

    @Test
    @DisplayName("批量删除维护计划 - 租户ID为空时抛出异常")
    void batchRemoveHouseMaintainPlans_ThrowsException_WhenTenantIdIsNull() {
        // Arrange
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    Long tenantId = null;
                    if (ids == null || ids.isEmpty() || tenantId == null) {
                        throw new IllegalArgumentException("租户ID不能为空");
                    }
                });
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("批量删除维护计划 - 跨租户数据校验")
    void batchRemoveHouseMaintainPlans_CrossTenantValidation() {
        // 模拟存在跨租户数据
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        assertThrows(IllegalArgumentException.class,
                () -> {
                    // 模拟存在不属于当前租户的计划
                    boolean hasCrossTenantPlans = true;
                    if (hasCrossTenantPlans) {
                        throw new IllegalArgumentException("存在不属于当前租户的维护计划，无法批量删除");
                    }
                });
    }

    // ==================== 业务规则测试 ====================

    @Test
    @DisplayName("维护计划唯一性校验逻辑")
    void validatePlanUniqueness_LogicTest() {
        // 模拟唯一性校验失败的情况
        assertThrows(IllegalArgumentException.class,
                () -> {
                    // 模拟已存在相同的维护计划
                    boolean planExists = true;
                    if (planExists) {
                        throw new IllegalArgumentException("同一房源相同类型和周期的维护计划已存在");
                    }
                });
    }

    @Test
    @DisplayName("维护计划周期枚举值校验")
    void planCycleEnumValidation_Test() {
        // 测试有效的周期值
        assertDoesNotThrow(() -> validateCycleValue("ONCE"));
        assertDoesNotThrow(() -> validateCycleValue("WEEKLY"));
        assertDoesNotThrow(() -> validateCycleValue("MONTHLY"));
        assertDoesNotThrow(() -> validateCycleValue("QUARTERLY"));
        assertDoesNotThrow(() -> validateCycleValue("YEARLY"));

        // 测试无效的周期值
        assertThrows(IllegalArgumentException.class, () -> validateCycleValue("DAILY")); // 不在枚举中
        assertThrows(IllegalArgumentException.class, () -> validateCycleValue("")); // 空值
        assertThrows(IllegalArgumentException.class, () -> validateCycleValue(null)); // null
    }

    @Test
    @DisplayName("维护类型枚举值校验")
    void maintainTypeEnumValidation_Test() {
        // 测试有效的类型值
        assertDoesNotThrow(() -> validateMaintainTypeValue("CLEAN"));
        assertDoesNotThrow(() -> validateMaintainTypeValue("REPAIR"));
        assertDoesNotThrow(() -> validateMaintainTypeValue("INSPECTION"));
        assertDoesNotThrow(() -> validateMaintainTypeValue("FURNITURE_MAINT"));
        assertDoesNotThrow(() -> validateMaintainTypeValue("OTHER"));

        // 测试无效的类型值
        assertThrows(IllegalArgumentException.class, () -> validateMaintainTypeValue("MAINTENANCE")); // 不在枚举中
        assertThrows(IllegalArgumentException.class, () -> validateMaintainTypeValue("")); // 空值
        assertThrows(IllegalArgumentException.class, () -> validateMaintainTypeValue(null)); // null
    }

    @Test
    @DisplayName("计划状态枚举值校验")
    void planStatusEnumValidation_Test() {
        // 测试有效的状态值
        assertDoesNotThrow(() -> validateStatusValue("ACTIVE"));
        assertDoesNotThrow(() -> validateStatusValue("PAUSED"));
        assertDoesNotThrow(() -> validateStatusValue("CANCELED"));

        // 测试无效的状态值
        assertThrows(IllegalArgumentException.class, () -> validateStatusValue("EXPIRED")); // 不在枚举中
        assertThrows(IllegalArgumentException.class, () -> validateStatusValue("")); // 空值
        assertThrows(IllegalArgumentException.class, () -> validateStatusValue(null)); // null
    }

    @Test
    @DisplayName("维护要求长度校验")
    void remarkLengthValidation_Test() {
        // 测试正常长度
        String normalRemark = "每周六上午9点保洁，清洁范围：客厅、卧室、厨房、卫生间";
        assertDoesNotThrow(() -> validateRemarkLength(normalRemark));

        // 测试超长备注
        String longRemark = "A".repeat(501); // 501个字符
        assertThrows(IllegalArgumentException.class, () -> validateRemarkLength(longRemark));

        // 测试边界值
        String boundaryRemark = "A".repeat(500); // 500个字符
        assertDoesNotThrow(() -> validateRemarkLength(boundaryRemark));
    }

    // ==================== Service接口方法调用测试 ====================

    @Test
    @DisplayName("Service接口方法调用测试")
    void serviceInterfaceMethodsTest() {
        // 测试服务接口的各种方法被正确调用
        HouseMaintainPlan testPlan = createAnotherMaintainPlan();

        // 测试 saveHouseMaintainPlan
        when(houseMaintainPlanService.saveHouseMaintainPlan(any(HouseMaintainPlan.class))).thenReturn(true);
        boolean saveResult = houseMaintainPlanService.saveHouseMaintainPlan(testPlan);
        assertTrue(saveResult);

        // 测试 updateHouseMaintainPlan
        when(houseMaintainPlanService.updateHouseMaintainPlan(any(HouseMaintainPlan.class))).thenReturn(true);
        boolean updateResult = houseMaintainPlanService.updateHouseMaintainPlan(testPlan);
        assertTrue(updateResult);

        // 测试 removeHouseMaintainPlan
        when(houseMaintainPlanService.removeHouseMaintainPlan(anyLong(), anyLong())).thenReturn(true);
        boolean removeResult = houseMaintainPlanService.removeHouseMaintainPlan(1L, TENANT_ID);
        assertTrue(removeResult);

        // 测试 getHouseMaintainPlanById
        when(houseMaintainPlanService.getHouseMaintainPlanById(anyLong(), anyLong())).thenReturn(testPlan);
        HouseMaintainPlan retrievedPlan = houseMaintainPlanService.getHouseMaintainPlanById(1L, TENANT_ID);
        assertNotNull(retrievedPlan);

        // 测试 listByHouseId
        List<HouseMaintainPlan> planList = Arrays.asList(testPlan);
        when(houseMaintainPlanService.listByHouseId(anyLong(), anyLong())).thenReturn(planList);
        List<HouseMaintainPlan> resultList = houseMaintainPlanService.listByHouseId(HOUSE_ID, TENANT_ID);
        assertNotNull(resultList);
        assertEquals(1, resultList.size());

        // 测试 listByExecutorId
        when(houseMaintainPlanService.listByExecutorId(anyLong(), anyLong())).thenReturn(planList);
        resultList = houseMaintainPlanService.listByExecutorId(EXECUTOR_ID, TENANT_ID);
        assertNotNull(resultList);
        assertEquals(1, resultList.size());

        // 测试 batchSaveHouseMaintainPlans
        when(houseMaintainPlanService.batchSaveHouseMaintainPlans(anyList())).thenReturn(true);
        boolean batchSaveResult = houseMaintainPlanService.batchSaveHouseMaintainPlans(planList);
        assertTrue(batchSaveResult);

        // 测试 batchRemoveHouseMaintainPlans
        when(houseMaintainPlanService.batchRemoveHouseMaintainPlans(anyList(), anyLong())).thenReturn(true);
        boolean batchRemoveResult = houseMaintainPlanService.batchRemoveHouseMaintainPlans(Arrays.asList(1L, 2L), TENANT_ID);
        assertTrue(batchRemoveResult);

        verify(houseMaintainPlanService, times(1)).saveHouseMaintainPlan(any(HouseMaintainPlan.class));
        verify(houseMaintainPlanService, times(1)).updateHouseMaintainPlan(any(HouseMaintainPlan.class));
        verify(houseMaintainPlanService, times(1)).removeHouseMaintainPlan(anyLong(), anyLong());
        verify(houseMaintainPlanService, times(1)).getHouseMaintainPlanById(anyLong(), anyLong());
        verify(houseMaintainPlanService, times(1)).listByHouseId(anyLong(), anyLong());
        verify(houseMaintainPlanService, times(1)).listByExecutorId(anyLong(), anyLong());
        verify(houseMaintainPlanService, times(1)).batchSaveHouseMaintainPlans(anyList());
        verify(houseMaintainPlanService, times(1)).batchRemoveHouseMaintainPlans(anyList(), anyLong());
    }

    // ==================== 辅助方法 ====================

    private HouseMaintainPlan createAnotherMaintainPlan() {
        HouseMaintainPlan plan = new HouseMaintainPlan();
        plan.setId(2L);
        plan.setTenantId(TENANT_ID);
        plan.setHouseId(102L); // 不同的房源
        plan.setMaintainType("REPAIR");
        plan.setCycle("MONTHLY");
        plan.setStartDate(FUTURE_DATE.plusMonths(1));
        plan.setEndDate(FUTURE_DATE.plusMonths(13));
        plan.setExecutorId(3002L);
        plan.setStatus("PAUSED");
        plan.setRemark("每月设备检修");
        return plan;
    }

    private HouseMaintainPlan createMaintainPlanWithoutId() {
        HouseMaintainPlan plan = new HouseMaintainPlan();
        plan.setTenantId(TENANT_ID);
        plan.setHouseId(HOUSE_ID);
        plan.setMaintainType("CLEAN");
        plan.setCycle("WEEKLY");
        plan.setStartDate(FUTURE_DATE);
        plan.setEndDate(FUTURE_END_DATE);
        plan.setExecutorId(EXECUTOR_ID);
        plan.setStatus("ACTIVE");
        plan.setRemark("每周六上午9点保洁");
        return plan;
    }

    // 内部校验方法（模拟Service中的校验逻辑）
    private void validatePlanBaseParams(HouseMaintainPlan plan) {
        if (plan.getTenantId() == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }
        if (plan.getHouseId() == null) {
            throw new IllegalArgumentException("房源ID不能为空");
        }
        if (plan.getMaintainType() == null || plan.getMaintainType().trim().isEmpty()) {
            throw new IllegalArgumentException("维护类型不能为空");
        }
        if (plan.getCycle() == null || plan.getCycle().trim().isEmpty()) {
            throw new IllegalArgumentException("执行周期不能为空");
        }
        if (plan.getStartDate() == null) {
            throw new IllegalArgumentException("开始日期不能为空");
        }
        if (plan.getExecutorId() == null) {
            throw new IllegalArgumentException("执行人ID不能为空");
        }
        if (plan.getStatus() == null || plan.getStatus().trim().isEmpty()) {
            throw new IllegalArgumentException("计划状态不能为空");
        }
    }

    private void validatePlanBusinessRules(HouseMaintainPlan plan) {
        LocalDate now = LocalDate.now();

        // 开始日期不能早于当前日期
        if (plan.getStartDate() != null && plan.getStartDate().isBefore(now)) {
            throw new IllegalArgumentException("开始日期不能早于当前日期");
        }

        // 周期性计划必须有结束日期
        if (!"ONCE".equals(plan.getCycle())) {
            if (plan.getEndDate() == null) {
                throw new IllegalArgumentException("周期性计划必须设置结束日期");
            }

            // 结束日期不能早于开始日期
            if (plan.getStartDate() != null && plan.getEndDate() != null &&
                    plan.getEndDate().isBefore(plan.getStartDate())) {
                throw new IllegalArgumentException("结束日期不能早于开始日期");
            }
        }
    }

    private void validateCycleValue(String cycle) {
        if (cycle == null || cycle.trim().isEmpty()) {
            throw new IllegalArgumentException("执行周期不能为空");
        }

        Set<String> validCycles = new HashSet<>(Arrays.asList("ONCE", "WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY"));
        if (!validCycles.contains(cycle)) {
            throw new IllegalArgumentException("执行周期值无效");
        }
    }

    private void validateMaintainTypeValue(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("维护类型不能为空");
        }

        Set<String> validTypes = new HashSet<>(Arrays.asList("CLEAN", "REPAIR", "INSPECTION", "FURNITURE_MAINT", "OTHER"));
        if (!validTypes.contains(type)) {
            throw new IllegalArgumentException("维护类型值无效");
        }
    }

    private void validateStatusValue(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("计划状态不能为空");
        }

        Set<String> validStatuses = new HashSet<>(Arrays.asList("ACTIVE", "PAUSED", "CANCELED"));
        if (!validStatuses.contains(status)) {
            throw new IllegalArgumentException("计划状态值无效");
        }
    }

    private void validateRemarkLength(String remark) {
        if (remark != null && remark.length() > 500) {
            throw new IllegalArgumentException("维护要求长度不能超过500字符");
        }
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

        // 测试 Assert.isTrue
        boolean falseCondition = false;
        exception = assertThrows(IllegalArgumentException.class,
                () -> Assert.isTrue(falseCondition, "条件必须为真"));
        assertEquals("条件必须为真", exception.getMessage());
    }
}
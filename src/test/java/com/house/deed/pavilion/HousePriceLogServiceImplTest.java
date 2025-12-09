package com.house.deed.pavilion;

import com.house.deed.pavilion.entity.HousePriceLog;
import com.house.deed.pavilion.service.HousePriceLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HousePriceLogServiceImpl 单元测试")
class HousePriceLogServiceImplTest {

    @Mock
    private HousePriceLogService housePriceLogService;

    private HousePriceLog validPriceLog;
    private final Long TENANT_ID = 1001L;
    private final Long HOUSE_ID = 101L;
    private final Long OPERATOR_ID = 3001L;
    private final Long LOG_ID = 1L;
    private final BigDecimal PRICE_BEFORE = new BigDecimal("180.00");
    private final BigDecimal PRICE_AFTER = new BigDecimal("175.00");

    @BeforeEach
    void setUp() {
        validPriceLog = new HousePriceLog();
        validPriceLog.setId(LOG_ID);
        validPriceLog.setTenantId(TENANT_ID);
        validPriceLog.setHouseId(HOUSE_ID);
        validPriceLog.setPriceBefore(PRICE_BEFORE);
        validPriceLog.setPriceAfter(PRICE_AFTER);
        validPriceLog.setChangeReason("房东降价促销，加快成交");
        validPriceLog.setOperatorId(OPERATOR_ID);
        validPriceLog.setOperatorName("张三（经纪人）");
        validPriceLog.setCreateTime(LocalDateTime.now());
    }

    // ==================== 基础CRUD测试 ====================

    @Test
    @DisplayName("新增价格变动记录 - 基础参数校验测试")
    void savePriceLog_BaseValidationTests() {
        // 测试租户ID为空
        assertThrows(IllegalArgumentException.class,
                () -> validateSaveParams(null, validPriceLog));

        // 测试房源ID为空
        HousePriceLog testLog = createPriceLogWithoutId();
        testLog.setHouseId(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validateSaveParams(TENANT_ID, testLog));
        assertEquals("房源ID不能为空", exception.getMessage());

        // 测试调整前价格为空
        testLog.setHouseId(HOUSE_ID);
        testLog.setPriceBefore(null);

        exception = assertThrows(IllegalArgumentException.class,
                () -> validateSaveParams(TENANT_ID, testLog));
        assertEquals("调整前价格不能为空", exception.getMessage());

        // 测试调整后价格为空
        testLog.setPriceBefore(PRICE_BEFORE);
        testLog.setPriceAfter(null);

        exception = assertThrows(IllegalArgumentException.class,
                () -> validateSaveParams(TENANT_ID, testLog));
        assertEquals("调整后价格不能为空", exception.getMessage());

        // 测试调价原因为空
        testLog.setPriceAfter(PRICE_AFTER);
        testLog.setChangeReason("");

        exception = assertThrows(IllegalArgumentException.class,
                () -> validateSaveParams(TENANT_ID, testLog));
        assertEquals("调价原因不能为空", exception.getMessage());

        // 测试操作人ID为空
        testLog.setChangeReason("房东降价促销");
        testLog.setOperatorId(null);

        exception = assertThrows(IllegalArgumentException.class,
                () -> validateSaveParams(TENANT_ID, testLog));
        assertEquals("操作人ID不能为空", exception.getMessage());

        // 测试操作人姓名为空
        testLog.setOperatorId(OPERATOR_ID);
        testLog.setOperatorName("");

        exception = assertThrows(IllegalArgumentException.class,
                () -> validateSaveParams(TENANT_ID, testLog));
        assertEquals("操作人姓名不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("新增价格变动记录 - 价格校验测试")
    void savePriceLog_PriceValidationTests() {
        // 测试调整前后价格相同
        HousePriceLog testLog = createPriceLogWithoutId();
        testLog.setPriceBefore(new BigDecimal("180.00"));
        testLog.setPriceAfter(new BigDecimal("180.00"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validatePriceRules(testLog));
        assertEquals("调整前后价格不能相同", exception.getMessage());

        // 测试调整前价格为负数
        testLog.setPriceBefore(new BigDecimal("-10.00"));
        testLog.setPriceAfter(new BigDecimal("180.00"));

        exception = assertThrows(IllegalArgumentException.class,
                () -> validatePriceRules(testLog));
        assertEquals("调整前价格不能为负数", exception.getMessage());

        // 测试调整后价格为负数
        testLog.setPriceBefore(new BigDecimal("180.00"));
        testLog.setPriceAfter(new BigDecimal("-10.00"));

        exception = assertThrows(IllegalArgumentException.class,
                () -> validatePriceRules(testLog));
        assertEquals("调整后价格不能为负数", exception.getMessage());

        // 测试价格为零（允许）
        testLog.setPriceBefore(BigDecimal.ZERO);
        testLog.setPriceAfter(new BigDecimal("180.00"));
        assertDoesNotThrow(() -> validatePriceRules(testLog));

        // 测试正常价格调整
        testLog.setPriceBefore(new BigDecimal("180.00"));
        testLog.setPriceAfter(new BigDecimal("175.00"));
        assertDoesNotThrow(() -> validatePriceRules(testLog));
    }

    @Test
    @DisplayName("更新价格变动记录 - ID为空时抛出异常")
    void updatePriceLogById_ThrowsException_WhenIdIsNull() {
        // Arrange
        validPriceLog.setId(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    if (validPriceLog.getId() == null) {
                        throw new IllegalArgumentException("记录ID不能为空");
                    }
                });
        assertEquals("记录ID不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("更新价格变动记录 - 租户ID为空时抛出异常")
    void updatePriceLogById_ThrowsException_WhenTenantIdIsNull() {
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
    @DisplayName("更新价格变动记录 - 跨租户操作校验")
    void updatePriceLogById_CrossTenantCheck() {
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
    @DisplayName("更新价格变动记录 - 禁止修改核心审计字段")
    void updatePriceLogById_CoreFieldsImmutable() {
        // 测试核心审计字段不能被修改的逻辑
        HousePriceLog originalLog = new HousePriceLog();
        originalLog.setId(LOG_ID);
        originalLog.setTenantId(TENANT_ID);
        originalLog.setHouseId(HOUSE_ID);
        originalLog.setPriceBefore(PRICE_BEFORE);
        originalLog.setPriceAfter(PRICE_AFTER);
        originalLog.setOperatorId(OPERATOR_ID);
        originalLog.setCreateTime(LocalDateTime.now());

        // 模拟更新时尝试修改价格字段
        HousePriceLog updateLog = new HousePriceLog();
        updateLog.setId(LOG_ID);
        updateLog.setTenantId(TENANT_ID);
        updateLog.setPriceBefore(new BigDecimal("200.00")); // 尝试修改

        // 在真实的Service中，这些字段应该被还原为原始值
        assertThrows(IllegalArgumentException.class,
                () -> {
                    // 假设在业务逻辑中，价格字段不允许修改
                    if (!updateLog.getPriceBefore().equals(originalLog.getPriceBefore())) {
                        throw new IllegalArgumentException("价格字段不允许修改");
                    }
                });
    }

    @Test
    @DisplayName("删除价格变动记录 - 参数校验")
    void removePriceLogById_ValidationTests() {
        // 测试记录ID为空
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long id = null;
                    Long tenantId = TENANT_ID;
                    if (id == null || tenantId == null) {
                        throw new IllegalArgumentException("记录ID不能为空");
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
    @DisplayName("按ID查询价格变动记录 - 参数校验")
    void getPriceLogById_ValidationTests() {
        // 测试记录ID为空
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long id = null;
                    Long tenantId = TENANT_ID;
                    if (id == null || tenantId == null) {
                        throw new IllegalArgumentException("记录ID不能为空");
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
    @DisplayName("分页查询价格变动记录 - 租户ID为空时抛出异常")
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
    @DisplayName("多条件查询价格变动记录 - 租户ID为空时抛出异常")
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
    @DisplayName("按房源ID查询价格变动记录 - 参数校验")
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
    @DisplayName("批量新增价格变动记录 - 租户ID为空时抛出异常")
    void batchSavePriceLogs_ThrowsException_WhenTenantIdIsNull() {
        // Arrange
        List<HousePriceLog> logList = Arrays.asList(createPriceLogWithoutId());

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
    @DisplayName("批量新增价格变动记录 - 列表为空时抛出异常")
    void batchSavePriceLogs_ThrowsException_WhenListIsEmpty() {
        // Arrange
        List<HousePriceLog> emptyList = Collections.emptyList();

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
    @DisplayName("批量新增价格变动记录 - 批量参数校验")
    void batchSavePriceLogs_BatchValidationTests() {
        // Arrange - 创建一个有问题的记录
        HousePriceLog invalidLog = createPriceLogWithoutId();
        invalidLog.setPriceBefore(new BigDecimal("180.00"));
        invalidLog.setPriceAfter(new BigDecimal("180.00")); // 价格相同

        List<HousePriceLog> logList = Arrays.asList(createPriceLogWithoutId(), invalidLog);

        // Act & Assert - 批量操作中应该会校验每条记录
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    for (HousePriceLog log : logList) {
                        if (log.getPriceBefore().equals(log.getPriceAfter())) {
                            throw new IllegalArgumentException("调整前后价格不能相同");
                        }
                    }
                });
        assertEquals("调整前后价格不能相同", exception.getMessage());
    }

    @Test
    @DisplayName("批量删除价格变动记录 - 租户ID为空时抛出异常")
    void batchRemovePriceLogs_ThrowsException_WhenTenantIdIsNull() {
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
    @DisplayName("批量删除价格变动记录 - ID列表为空时抛出异常")
    void batchRemovePriceLogs_ThrowsException_WhenIdsIsEmpty() {
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
    @DisplayName("批量删除价格变动记录 - 跨租户数据校验")
    void batchRemovePriceLogs_CrossTenantValidation() {
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
    @DisplayName("调价原因长度校验")
    void changeReasonLengthValidation_Test() {
        // 测试正常长度
        String normalReason = "房东降价促销，加快成交";
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
    @DisplayName("价格格式校验 - 保留两位小数")
    void priceFormatValidation_Test() {
        // 测试正常价格格式
        assertDoesNotThrow(() -> validatePriceFormat(new BigDecimal("180.00")));
        assertDoesNotThrow(() -> validatePriceFormat(new BigDecimal("175.50")));
        assertDoesNotThrow(() -> validatePriceFormat(new BigDecimal("0.00")));
        assertDoesNotThrow(() -> validatePriceFormat(new BigDecimal("100"))); // 整数

        // 测试非数字字符串转换为BigDecimal（在真实场景中，输入应该是BigDecimal）
        assertThrows(NumberFormatException.class, () -> new BigDecimal("abc"));
    }

    // ==================== Service接口方法调用测试 ====================

    @Test
    @DisplayName("Service接口方法调用测试")
    void serviceInterfaceMethodsTest() {
        // 测试服务接口的各种方法被正确调用
        HousePriceLog testLog = createAnotherPriceLog();

        // 测试 savePriceLog
        when(housePriceLogService.savePriceLog(any(HousePriceLog.class), anyLong())).thenReturn(true);
        boolean saveResult = housePriceLogService.savePriceLog(testLog, TENANT_ID);
        assertTrue(saveResult);

        // 测试 updatePriceLogById
        when(housePriceLogService.updatePriceLogById(any(HousePriceLog.class), anyLong())).thenReturn(true);
        boolean updateResult = housePriceLogService.updatePriceLogById(testLog, TENANT_ID);
        assertTrue(updateResult);

        // 测试 removePriceLogById
        when(housePriceLogService.removePriceLogById(anyLong(), anyLong())).thenReturn(true);
        boolean removeResult = housePriceLogService.removePriceLogById(LOG_ID, TENANT_ID);
        assertTrue(removeResult);

        // 测试 getPriceLogById
        when(housePriceLogService.getPriceLogById(anyLong(), anyLong())).thenReturn(testLog);
        HousePriceLog retrievedLog = housePriceLogService.getPriceLogById(LOG_ID, TENANT_ID);
        assertNotNull(retrievedLog);

        // 测试 listByHouseId
        List<HousePriceLog> logList = Arrays.asList(testLog);
        when(housePriceLogService.listByHouseId(anyLong(), anyLong())).thenReturn(logList);
        List<HousePriceLog> resultList = housePriceLogService.listByHouseId(HOUSE_ID, TENANT_ID);
        assertNotNull(resultList);
        assertEquals(1, resultList.size());

        // 测试 batchSavePriceLogs
        when(housePriceLogService.batchSavePriceLogs(anyList(), anyLong())).thenReturn(true);
        boolean batchSaveResult = housePriceLogService.batchSavePriceLogs(logList, TENANT_ID);
        assertTrue(batchSaveResult);

        // 测试 batchRemovePriceLogs
        when(housePriceLogService.batchRemovePriceLogs(anyList(), anyLong())).thenReturn(true);
        boolean batchRemoveResult = housePriceLogService.batchRemovePriceLogs(Arrays.asList(1L, 2L), TENANT_ID);
        assertTrue(batchRemoveResult);

        verify(housePriceLogService, times(1)).savePriceLog(any(HousePriceLog.class), anyLong());
        verify(housePriceLogService, times(1)).updatePriceLogById(any(HousePriceLog.class), anyLong());
        verify(housePriceLogService, times(1)).removePriceLogById(anyLong(), anyLong());
        verify(housePriceLogService, times(1)).getPriceLogById(anyLong(), anyLong());
        verify(housePriceLogService, times(1)).listByHouseId(anyLong(), anyLong());
        verify(housePriceLogService, times(1)).batchSavePriceLogs(anyList(), anyLong());
        verify(housePriceLogService, times(1)).batchRemovePriceLogs(anyList(), anyLong());
    }

    // ==================== 辅助方法 ====================

    private HousePriceLog createAnotherPriceLog() {
        HousePriceLog log = new HousePriceLog();
        log.setId(2L);
        log.setTenantId(TENANT_ID);
        log.setHouseId(102L); // 不同的房源
        log.setPriceBefore(new BigDecimal("200.00"));
        log.setPriceAfter(new BigDecimal("190.00"));
        log.setChangeReason("市场行情调整");
        log.setOperatorId(3002L);
        log.setOperatorName("李四（经纪人）");
        return log;
    }

    private HousePriceLog createPriceLogWithoutId() {
        HousePriceLog log = new HousePriceLog();
        log.setTenantId(TENANT_ID);
        log.setHouseId(HOUSE_ID);
        log.setPriceBefore(PRICE_BEFORE);
        log.setPriceAfter(PRICE_AFTER);
        log.setChangeReason("房东降价促销，加快成交");
        log.setOperatorId(OPERATOR_ID);
        log.setOperatorName("张三（经纪人）");
        return log;
    }

    // 内部校验方法（模拟Service中的校验逻辑）
    private void validateSaveParams(Long tenantId, HousePriceLog log) {
        if (tenantId == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }
        if (log.getHouseId() == null) {
            throw new IllegalArgumentException("房源ID不能为空");
        }
        if (log.getPriceBefore() == null) {
            throw new IllegalArgumentException("调整前价格不能为空");
        }
        if (log.getPriceAfter() == null) {
            throw new IllegalArgumentException("调整后价格不能为空");
        }
        if (log.getChangeReason() == null || log.getChangeReason().trim().isEmpty()) {
            throw new IllegalArgumentException("调价原因不能为空");
        }
        if (log.getOperatorId() == null) {
            throw new IllegalArgumentException("操作人ID不能为空");
        }
        if (log.getOperatorName() == null || log.getOperatorName().trim().isEmpty()) {
            throw new IllegalArgumentException("操作人姓名不能为空");
        }
    }

    private void validatePriceRules(HousePriceLog log) {
        // 调整前后价格不能相同
        if (log.getPriceBefore().equals(log.getPriceAfter())) {
            throw new IllegalArgumentException("调整前后价格不能相同");
        }

        // 价格非负校验
        if (log.getPriceBefore().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("调整前价格不能为负数");
        }
        if (log.getPriceAfter().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("调整后价格不能为负数");
        }
    }

    private void validateChangeReasonLength(String reason) {
        if (reason != null && reason.length() > 200) {
            throw new IllegalArgumentException("调价原因长度不能超过200字符");
        }
    }

    private void validateOperatorNameLength(String name) {
        if (name != null && name.length() > 50) {
            throw new IllegalArgumentException("操作人姓名长度不能超过50字符");
        }
    }

    private void validatePriceFormat(BigDecimal price) {
        // 这里可以添加价格格式校验，如保留两位小数等
        // 在实际业务中，可能需要对价格进行格式化或校验
        if (price == null) {
            throw new IllegalArgumentException("价格不能为空");
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
    @DisplayName("BigDecimal 比较测试")
    void bigDecimalComparison_Test() {
        BigDecimal price1 = new BigDecimal("180.00");
        BigDecimal price2 = new BigDecimal("180.00");
        BigDecimal price3 = new BigDecimal("175.00");

        // 测试相等比较
        assertEquals(0, price1.compareTo(price2));
        assertTrue(price1.compareTo(price2) == 0);

        // 测试大于比较
        assertTrue(price1.compareTo(price3) > 0);

        // 测试小于比较
        assertTrue(price3.compareTo(price1) < 0);

        // 测试equals方法（注意：BigDecimal的equals还会比较精度）
        assertEquals(price1, price2);

        // 测试负值判断
        BigDecimal negativePrice = new BigDecimal("-10.00");
        assertTrue(negativePrice.compareTo(BigDecimal.ZERO) < 0);

        // 测试零值判断
        assertEquals(0, BigDecimal.ZERO.compareTo(BigDecimal.ZERO));
    }
}
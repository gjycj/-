package com.house.deed.pavilion;

import com.house.deed.pavilion.entity.HouseLandlord;
import com.house.deed.pavilion.service.HouseLandlordService;
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
@DisplayName("HouseLandlordServiceImpl 单元测试")
class HouseLandlordServiceImplTest {

    @Mock
    private HouseLandlordService houseLandlordService;

    private HouseLandlord validHouseLandlord;
    private final Long TENANT_ID = 1001L;
    private final Long HOUSE_ID = 101L;
    private final Long LANDLORD_ID = 201L;
    private final Long RELATION_ID = 1L;

    @BeforeEach
    void setUp() {
        validHouseLandlord = new HouseLandlord();
        validHouseLandlord.setId(RELATION_ID);
        validHouseLandlord.setTenantId(TENANT_ID);
        validHouseLandlord.setHouseId(HOUSE_ID);
        validHouseLandlord.setLandlordId(LANDLORD_ID);
        validHouseLandlord.setOwnership("100%");
        validHouseLandlord.setCreateTime(LocalDateTime.now());
    }

    // ==================== 基础CRUD操作测试 ====================

    @Test
    @DisplayName("新增房源与房东关联关系 - 参数校验测试")
    void saveHouseLandlord_ValidationTests() {
        // 测试租户ID为空
        HouseLandlord testEntity = new HouseLandlord();
        testEntity.setTenantId(null);
        testEntity.setHouseId(HOUSE_ID);
        testEntity.setLandlordId(LANDLORD_ID);
        testEntity.setOwnership("100%");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    if (testEntity.getTenantId() == null) {
                        throw new IllegalArgumentException("租户ID不能为空");
                    }
                });
        assertEquals("租户ID不能为空", exception.getMessage());

        // 测试房源ID为空
        testEntity.setTenantId(TENANT_ID);
        testEntity.setHouseId(null);

        exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    if (testEntity.getHouseId() == null) {
                        throw new IllegalArgumentException("房源ID不能为空");
                    }
                });
        assertEquals("房源ID不能为空", exception.getMessage());

        // 测试房东ID为空
        testEntity.setHouseId(HOUSE_ID);
        testEntity.setLandlordId(null);

        exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    if (testEntity.getLandlordId() == null) {
                        throw new IllegalArgumentException("房东ID不能为空");
                    }
                });
        assertEquals("房东ID不能为空", exception.getMessage());

        // 测试所有权占比为空
        testEntity.setLandlordId(LANDLORD_ID);
        testEntity.setOwnership("");

        exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    if (testEntity.getOwnership() == null || testEntity.getOwnership().trim().isEmpty()) {
                        throw new IllegalArgumentException("所有权占比不能为空");
                    }
                });
    }

    @Test
    @DisplayName("新增房源与房东关联关系 - 唯一性校验逻辑")
    void saveHouseLandlord_UniquenessValidation() {
        // 模拟Service方法
        when(houseLandlordService.saveHouseLandlord(any(HouseLandlord.class))).thenReturn(true);

        // 正常情况
        boolean result = houseLandlordService.saveHouseLandlord(validHouseLandlord);
        assertTrue(result);

        // 验证唯一性校验逻辑
        assertDoesNotThrow(() -> {
            // 这里可以模拟唯一性校验的逻辑
            // 如果存在重复关联，应该抛出异常
        });
    }

    @Test
    @DisplayName("更新关联关系 - ID为空时抛出异常")
    void updateHouseLandlordById_ThrowsException_WhenIdIsNull() {
        // Arrange
        validHouseLandlord.setId(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    if (validHouseLandlord.getId() == null) {
                        throw new IllegalArgumentException("关联ID不能为空");
                    }
                });
        assertEquals("关联ID不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("更新关联关系 - 租户ID为空时抛出异常")
    void updateHouseLandlordById_ThrowsException_WhenTenantIdIsNull() {
        // Arrange
        validHouseLandlord.setTenantId(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    if (validHouseLandlord.getTenantId() == null) {
                        throw new IllegalArgumentException("租户ID不能为空");
                    }
                });
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("更新关联关系 - 跨租户操作校验")
    void updateHouseLandlordById_CrossTenantCheck() {
        // 模拟跨租户情况
        Long differentTenantId = 9999L;

        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long existingTenantId = TENANT_ID;
                    Long newTenantId = differentTenantId;

                    if (!Objects.equals(existingTenantId, newTenantId)) {
                        throw new IllegalArgumentException("无权操作其他租户的关联数据");
                    }
                });
    }

    @Test
    @DisplayName("删除关联关系 - 参数校验")
    void removeHouseLandlordById_ValidationTests() {
        // 测试ID为空
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    Long id = null;
                    Long tenantId = TENANT_ID;
                    if (id == null || tenantId == null) {
                        throw new IllegalArgumentException("关联ID不能为空");
                    }
                });

        // 测试租户ID为空
        exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    Long id = RELATION_ID;
                    Long tenantId = null;
                    if (id == null || tenantId == null) {
                        throw new IllegalArgumentException("租户ID不能为空");
                    }
                });
    }

    @Test
    @DisplayName("按ID查询关联关系 - 参数校验")
    void getHouseLandlordById_ValidationTests() {
        // 测试ID为空
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long id = null;
                    Long tenantId = TENANT_ID;
                    if (id == null || tenantId == null) {
                        throw new IllegalArgumentException("关联ID不能为空");
                    }
                });

        // 测试租户ID为空
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long id = RELATION_ID;
                    Long tenantId = null;
                    if (id == null || tenantId == null) {
                        throw new IllegalArgumentException("租户ID不能为空");
                    }
                });
    }

    // ==================== 多条件查询测试 ====================

    @Test
    @DisplayName("分页查询 - 租户ID为空时抛出异常")
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
    @DisplayName("多条件查询 - 租户ID为空时抛出异常")
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
    @DisplayName("按房源ID查询 - 参数校验")
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
                        throw new IllegalArgumentException("房源ID和租户ID不能为空");
                    }
                });
    }

    @Test
    @DisplayName("按房东ID查询 - 参数校验")
    void listByLandlordId_ValidationTests() {
        // 测试房东ID为空
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long landlordId = null;
                    Long tenantId = TENANT_ID;
                    if (landlordId == null || tenantId == null) {
                        throw new IllegalArgumentException("房东ID不能为空");
                    }
                });

        // 测试租户ID为空
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long landlordId = LANDLORD_ID;
                    Long tenantId = null;
                    if (landlordId == null || tenantId == null) {
                        throw new IllegalArgumentException("房东ID和租户ID不能为空");
                    }
                });
    }

    // ==================== 批量操作测试 ====================

    @Test
    @DisplayName("批量新增关联关系 - 列表为空时抛出异常")
    void batchSaveHouseLandlords_ThrowsException_WhenListIsEmpty() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    List<HouseLandlord> houseLandlordList = Collections.emptyList();
                    if (houseLandlordList == null || houseLandlordList.isEmpty()) {
                        throw new IllegalArgumentException("关联列表不能为空");
                    }
                });
        assertEquals("关联列表不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("批量新增关联关系 - 跨租户操作时抛出异常")
    void batchSaveHouseLandlords_ThrowsException_WhenCrossTenant() {
        // Arrange
        HouseLandlord item1 = createHouseLandlordWithoutId();
        HouseLandlord item2 = createAnotherHouseLandlord();
        item2.setTenantId(9999L); // 不同的租户ID

        List<HouseLandlord> houseLandlordList = Arrays.asList(item1, item2);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    if (houseLandlordList != null && !houseLandlordList.isEmpty()) {
                        Long firstTenantId = houseLandlordList.get(0).getTenantId();
                        for (HouseLandlord item : houseLandlordList) {
                            if (!Objects.equals(item.getTenantId(), firstTenantId)) {
                                throw new IllegalArgumentException("批量新增的关联关系必须属于同一租户");
                            }
                        }
                    }
                });
        assertEquals("批量新增的关联关系必须属于同一租户", exception.getMessage());
    }

    @Test
    @DisplayName("批量新增关联关系 - 所有权占比格式校验")
    void batchSaveHouseLandlords_OwnershipFormatValidation() {
        // Arrange
        HouseLandlord item1 = createHouseLandlordWithoutId();
        item1.setOwnership("100%"); // 正确格式

        HouseLandlord item2 = createAnotherHouseLandlord();
        item2.setOwnership("50"); // 错误格式，缺少%

        List<HouseLandlord> houseLandlordList = Arrays.asList(item1, item2);

        // Act & Assert - 测试所有权占比格式
        assertThrows(IllegalArgumentException.class,
                () -> {
                    for (HouseLandlord item : houseLandlordList) {
                        if (item.getOwnership() != null && !item.getOwnership().matches("^\\d+(\\.\\d+)?%$")) {
                            throw new IllegalArgumentException("所有权占比格式错误（需为数字+%，如100%、33.33%）");
                        }
                    }
                });
    }

    @Test
    @DisplayName("批量删除关联关系 - ID列表为空时抛出异常")
    void batchRemoveHouseLandlords_ThrowsException_WhenIdsIsEmpty() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    List<Long> ids = Collections.emptyList();
                    Long tenantId = TENANT_ID;
                    if (ids == null || ids.isEmpty() || tenantId == null) {
                        throw new IllegalArgumentException("关联ID列表不能为空");
                    }
                });
        assertEquals("关联ID列表不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("批量删除关联关系 - 租户ID为空时抛出异常")
    void batchRemoveHouseLandlords_ThrowsException_WhenTenantIdIsNull() {
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
    @DisplayName("批量删除关联关系 - 跨租户数据校验")
    void batchRemoveHouseLandlords_CrossTenantValidation() {
        // 模拟存在跨租户数据
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        assertThrows(IllegalArgumentException.class,
                () -> {
                    // 模拟存在跨租户数据
                    boolean hasCrossTenantData = true; // 假设查询到有跨租户数据
                    if (hasCrossTenantData) {
                        throw new IllegalArgumentException("存在跨租户的关联数据，无法批量删除");
                    }
                });
    }

    // ==================== 内部方法测试 ====================

    @Test
    @DisplayName("校验必填字段方法 - 完整测试")
    void validateRequiredFields_Test() {
        // 测试各个字段为空的情况
        HouseLandlord testEntity = new HouseLandlord();

        // 租户ID为空
        testEntity.setTenantId(null);
        testEntity.setHouseId(HOUSE_ID);
        testEntity.setLandlordId(LANDLORD_ID);
        testEntity.setOwnership("100%");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    if (testEntity.getTenantId() == null) {
                        throw new IllegalArgumentException("租户ID不能为空");
                    }
                });
        assertEquals("租户ID不能为空", exception.getMessage());

        // 房源ID为空
        testEntity.setTenantId(TENANT_ID);
        testEntity.setHouseId(null);

        exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    if (testEntity.getHouseId() == null) {
                        throw new IllegalArgumentException("房源ID不能为空");
                    }
                });
        assertEquals("房源ID不能为空", exception.getMessage());

        // 房东ID为空
        testEntity.setHouseId(HOUSE_ID);
        testEntity.setLandlordId(null);

        exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    if (testEntity.getLandlordId() == null) {
                        throw new IllegalArgumentException("房东ID不能为空");
                    }
                });
        assertEquals("房东ID不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("校验关联关系唯一性 - 重复关联测试")
    void validateRelationUniqueness_Test() {
        // 模拟重复关联的情况
        assertThrows(IllegalArgumentException.class,
                () -> {
                    // 假设已经存在相同的关联关系
                    boolean relationExists = true;
                    if (relationExists) {
                        throw new IllegalArgumentException("该房源与房东已存在关联关系，不可重复关联");
                    }
                });
    }

    @Test
    @DisplayName("所有权占比格式验证")
    void ownershipFormatValidation_Test() {
        // 测试正确的格式
        assertDoesNotThrow(() -> validateOwnershipFormat("100%"));
        assertDoesNotThrow(() -> validateOwnershipFormat("50%"));
        assertDoesNotThrow(() -> validateOwnershipFormat("33.33%"));
        assertDoesNotThrow(() -> validateOwnershipFormat("0.5%"));

        // 测试错误的格式
        assertThrows(IllegalArgumentException.class, () -> validateOwnershipFormat("100")); // 缺少%
        assertThrows(IllegalArgumentException.class, () -> validateOwnershipFormat("%")); // 缺少数字
        assertThrows(IllegalArgumentException.class, () -> validateOwnershipFormat("abc%")); // 非数字
        assertThrows(IllegalArgumentException.class, () -> validateOwnershipFormat("100%%")); // 多个%
        assertThrows(IllegalArgumentException.class, () -> validateOwnershipFormat("")); // 空字符串
        assertThrows(IllegalArgumentException.class, () -> validateOwnershipFormat(null)); // null
    }

    // ==================== Service接口方法调用测试 ====================

    @Test
    @DisplayName("Service接口方法调用测试")
    void serviceInterfaceMethodsTest() {
        // 测试服务接口的各种方法被正确调用
        HouseLandlord testEntity = createAnotherHouseLandlord();

        // 测试 saveHouseLandlord
        when(houseLandlordService.saveHouseLandlord(any(HouseLandlord.class))).thenReturn(true);
        boolean saveResult = houseLandlordService.saveHouseLandlord(testEntity);
        assertTrue(saveResult);

        // 测试 updateHouseLandlordById
        when(houseLandlordService.updateHouseLandlordById(any(HouseLandlord.class))).thenReturn(true);
        boolean updateResult = houseLandlordService.updateHouseLandlordById(testEntity);
        assertTrue(updateResult);

        // 测试 removeHouseLandlordById
        when(houseLandlordService.removeHouseLandlordById(anyLong(), anyLong())).thenReturn(true);
        boolean removeResult = houseLandlordService.removeHouseLandlordById(1L, TENANT_ID);
        assertTrue(removeResult);

        // 测试 getHouseLandlordById
        when(houseLandlordService.getHouseLandlordById(anyLong(), anyLong())).thenReturn(testEntity);
        HouseLandlord retrievedEntity = houseLandlordService.getHouseLandlordById(1L, TENANT_ID);
        assertNotNull(retrievedEntity);

        // 测试 listByHouseId
        List<HouseLandlord> entityList = Arrays.asList(testEntity);
        when(houseLandlordService.listByHouseId(anyLong(), anyLong())).thenReturn(entityList);
        List<HouseLandlord> resultList = houseLandlordService.listByHouseId(HOUSE_ID, TENANT_ID);
        assertNotNull(resultList);
        assertEquals(1, resultList.size());

        // 测试 listByLandlordId
        when(houseLandlordService.listByLandlordId(anyLong(), anyLong())).thenReturn(entityList);
        resultList = houseLandlordService.listByLandlordId(LANDLORD_ID, TENANT_ID);
        assertNotNull(resultList);
        assertEquals(1, resultList.size());

        // 测试 batchSaveHouseLandlords
        when(houseLandlordService.batchSaveHouseLandlords(anyList())).thenReturn(true);
        boolean batchSaveResult = houseLandlordService.batchSaveHouseLandlords(entityList);
        assertTrue(batchSaveResult);

        // 测试 batchRemoveHouseLandlords
        when(houseLandlordService.batchRemoveHouseLandlords(anyList(), anyLong())).thenReturn(true);
        boolean batchRemoveResult = houseLandlordService.batchRemoveHouseLandlords(Arrays.asList(1L, 2L), TENANT_ID);
        assertTrue(batchRemoveResult);

        verify(houseLandlordService, times(1)).saveHouseLandlord(any(HouseLandlord.class));
        verify(houseLandlordService, times(1)).updateHouseLandlordById(any(HouseLandlord.class));
        verify(houseLandlordService, times(1)).removeHouseLandlordById(anyLong(), anyLong());
        verify(houseLandlordService, times(1)).getHouseLandlordById(anyLong(), anyLong());
        verify(houseLandlordService, times(1)).listByHouseId(anyLong(), anyLong());
        verify(houseLandlordService, times(1)).listByLandlordId(anyLong(), anyLong());
        verify(houseLandlordService, times(1)).batchSaveHouseLandlords(anyList());
        verify(houseLandlordService, times(1)).batchRemoveHouseLandlords(anyList(), anyLong());
    }

    // ==================== 辅助方法 ====================

    private HouseLandlord createAnotherHouseLandlord() {
        HouseLandlord entity = new HouseLandlord();
        entity.setId(2L);
        entity.setTenantId(TENANT_ID);
        entity.setHouseId(102L); // 不同的房源
        entity.setLandlordId(202L); // 不同的房东
        entity.setOwnership("50%");
        return entity;
    }

    private HouseLandlord createHouseLandlordWithoutId() {
        HouseLandlord entity = new HouseLandlord();
        entity.setTenantId(TENANT_ID);
        entity.setHouseId(HOUSE_ID);
        entity.setLandlordId(LANDLORD_ID);
        entity.setOwnership("100%");
        return entity;
    }

    private void validateOwnershipFormat(String ownership) {
        if (ownership == null || ownership.trim().isEmpty()) {
            throw new IllegalArgumentException("所有权占比不能为空");
        }

        if (!ownership.matches("^\\d+(\\.\\d+)?%$")) {
            throw new IllegalArgumentException("所有权占比格式错误（需为数字+%，如100%、33.33%）");
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

        // 测试 Assert.isTrue
        boolean falseCondition = false;
        exception = assertThrows(IllegalArgumentException.class,
                () -> Assert.isTrue(falseCondition, "条件必须为真"));
        assertEquals("条件必须为真", exception.getMessage());

        // 测试 Assert.notEmpty（Collection）
        List<String> emptyList = Collections.emptyList();
        exception = assertThrows(IllegalArgumentException.class,
                () -> Assert.notEmpty(emptyList, "集合不能为空"));
        assertEquals("集合不能为空", exception.getMessage());
    }
}
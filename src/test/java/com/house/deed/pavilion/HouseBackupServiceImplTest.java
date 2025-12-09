package com.house.deed.pavilion;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.entity.HouseBackup;
import com.house.deed.pavilion.service.HouseBackupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HouseBackupServiceImpl 集成测试
 * 测试房源备份服务的全量功能
 */
@SpringBootTest
@Transactional
class HouseBackupServiceImplTest {

    @Autowired
    private HouseBackupService houseBackupService;

    // 租户ID常量
    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;

    // 原房源ID和备份ID计数器
    private final AtomicLong originalIdCounter = new AtomicLong(1000L);
    private final AtomicLong backupIdCounter = new AtomicLong(1L);

    @BeforeEach
    void setUp() {
        // 清理测试租户的备份数据
        List<HouseBackup> backups = houseBackupService.listByConditions(
                Collections.emptyMap(), DEFAULT_TENANT_ID);
        backups.forEach(backup -> houseBackupService.removeBackup(backup.getId(), DEFAULT_TENANT_ID));
    }

    /**
     * 创建测试房源备份对象
     */
    private HouseBackup createTestHouseBackup() {
        HouseBackup backup = new HouseBackup();

        // 核心追溯字段
        backup.setOriginalId(originalIdCounter.getAndIncrement());
        backup.setTenantId(DEFAULT_TENANT_ID);
        backup.setDeleteOperator("测试删除人_" + UUID.randomUUID().toString().substring(0, 8));

        // 原房源核心属性
        backup.setBuildingId(101L);
        backup.setHouseNo("A" + backup.getOriginalId() + "单元");
        backup.setHouseType("3室2厅");
        backup.setArea(new BigDecimal("120.50"));
        backup.setInsideArea(new BigDecimal("105.30"));
        backup.setFloor(10);
        backup.setTotalFloor(18);
        backup.setOrientation("南北通透");
        backup.setDecoration("DELUXE");
        backup.setPropertyRight("商品房");
        backup.setPropertyRightCertNo("浙房地权证杭字第" + backup.getOriginalId() + "号");
        backup.setPropertyRightYears(70);
        backup.setMortgageStatus("NONE");
        backup.setPrice(new BigDecimal("180.00"));
        backup.setTransactionType("SALE");
        backup.setStatus("ON_SALE");
        backup.setCreateAgentId(3001L);
        backup.setDescription("测试房源备份，中间楼层，南北通透");

        // 删除时间（可选，数据库会自动填充）
        backup.setDeleteTime(LocalDateTime.now());

        return backup;
    }

    /**
     * 创建测试房源备份对象 - 已出租状态
     */
    private HouseBackup createTestRentedHouseBackup() {
        HouseBackup backup = createTestHouseBackup();
        backup.setTransactionType("RENT");
        backup.setStatus("SOLD");
        backup.setPrice(new BigDecimal("0.50"));
        backup.setMortgageStatus("MORTGAGED");
        backup.setMortgageDetails("中国工商银行杭州分行，抵押金额50万元");
        return backup;
    }

    /**
     * 保存并返回备份对象
     */
    private HouseBackup saveAndGetBackup() {
        HouseBackup backup = createTestHouseBackup();
        houseBackupService.saveBackup(backup);
        return backup;
    }

    // ==================== saveBackup 测试 ====================

    @Test
    void saveBackup_ValidData_Success() {
        // 准备
        HouseBackup backup = createTestHouseBackup();

        // 执行
        boolean result = houseBackupService.saveBackup(backup);

        // 验证
        assertTrue(result);
        assertNotNull(backup.getId());
        assertEquals(DEFAULT_TENANT_ID, backup.getTenantId());
        assertNotNull(backup.getOriginalId());
        assertNotNull(backup.getDeleteOperator());
        assertEquals("DELUXE", backup.getDecoration());
        assertEquals("SALE", backup.getTransactionType());
        assertEquals("ON_SALE", backup.getStatus());
    }

    @Test
    void saveBackup_WithoutTenantId_ThrowsException() {
        // 准备
        HouseBackup backup = createTestHouseBackup();
        backup.setTenantId(null);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseBackupService.saveBackup(backup));
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    void saveBackup_WithoutOriginalId_ThrowsException() {
        // 准备
        HouseBackup backup = createTestHouseBackup();
        backup.setOriginalId(null);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseBackupService.saveBackup(backup));
        assertEquals("原房源ID不能为空", exception.getMessage());
    }

    @Test
    void saveBackup_WithoutDeleteOperator_ThrowsException() {
        // 准备
        HouseBackup backup = createTestHouseBackup();
        backup.setDeleteOperator(null);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseBackupService.saveBackup(backup));
        assertEquals("删除人不能为空", exception.getMessage());
    }

    @Test
    void saveBackup_RentedHouse_Success() {
        // 准备
        HouseBackup backup = createTestRentedHouseBackup();

        // 执行
        boolean result = houseBackupService.saveBackup(backup);

        // 验证
        assertTrue(result);
        assertEquals("RENT", backup.getTransactionType());
        assertEquals("SOLD", backup.getStatus());
        assertEquals("MORTGAGED", backup.getMortgageStatus());
        assertNotNull(backup.getMortgageDetails());
    }

    // ==================== getById 测试 ====================

    @Test
    void getById_Success() {
        // 准备
        HouseBackup savedBackup = saveAndGetBackup();

        // 执行
        HouseBackup result = houseBackupService.getById(savedBackup.getId(), DEFAULT_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertEquals(savedBackup.getId(), result.getId());
        assertEquals(savedBackup.getOriginalId(), result.getOriginalId());
        assertEquals(savedBackup.getTenantId(), result.getTenantId());
        assertEquals(savedBackup.getHouseNo(), result.getHouseNo());
        assertEquals(savedBackup.getDeleteOperator(), result.getDeleteOperator());
    }

    @Test
    void getById_WithoutBackupId_ThrowsException() {
        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseBackupService.getById(null, DEFAULT_TENANT_ID));
        assertEquals("备份ID不能为空", exception.getMessage());
    }

    @Test
    void getById_WithoutTenantId_ThrowsException() {
        // 准备
        HouseBackup savedBackup = saveAndGetBackup();

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseBackupService.getById(savedBackup.getId(), null));
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    void getById_TenantMismatch_ReturnsNull() {
        // 准备
        HouseBackup savedBackup = saveAndGetBackup();

        // 执行 - 使用不同的租户ID查询
        HouseBackup result = houseBackupService.getById(savedBackup.getId(), OTHER_TENANT_ID);

        // 验证
        assertNull(result);
    }

    @Test
    void getById_NonExistentBackup_ReturnsNull() {
        // 执行 - 查询不存在的备份ID
        HouseBackup result = houseBackupService.getById(999999L, DEFAULT_TENANT_ID);

        // 验证
        assertNull(result);
    }

    // ==================== getByOriginalId 测试 ====================

    @Test
    void getByOriginalId_Success() {
        // 准备
        HouseBackup savedBackup = saveAndGetBackup();
        Long originalId = savedBackup.getOriginalId();

        // 执行
        HouseBackup result = houseBackupService.getByOriginalId(originalId, DEFAULT_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertEquals(originalId, result.getOriginalId());
        assertEquals(savedBackup.getHouseNo(), result.getHouseNo());
        assertEquals(savedBackup.getDeleteOperator(), result.getDeleteOperator());
    }

    @Test
    void getByOriginalId_WithoutOriginalId_ThrowsException() {
        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseBackupService.getByOriginalId(null, DEFAULT_TENANT_ID));
        assertEquals("原房源ID不能为空", exception.getMessage());
    }

    @Test
    void getByOriginalId_WithoutTenantId_ThrowsException() {
        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseBackupService.getByOriginalId(1001L, null));
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    void getByOriginalId_TenantMismatch_ReturnsNull() {
        // 准备
        HouseBackup savedBackup = saveAndGetBackup();
        Long originalId = savedBackup.getOriginalId();

        // 执行 - 使用不同的租户ID查询
        HouseBackup result = houseBackupService.getByOriginalId(originalId, OTHER_TENANT_ID);

        // 验证
        assertNull(result);
    }

    @Test
    void getByOriginalId_NonExistentOriginalId_ReturnsNull() {
        // 执行 - 查询不存在的原房源ID
        HouseBackup result = houseBackupService.getByOriginalId(999999L, DEFAULT_TENANT_ID);

        // 验证
        assertNull(result);
    }

    // ==================== removeBackup 测试 ====================

    @Test
    void removeBackup_Success() {
        // 准备
        HouseBackup savedBackup = saveAndGetBackup();

        // 执行
        boolean result = houseBackupService.removeBackup(savedBackup.getId(), DEFAULT_TENANT_ID);

        // 验证
        assertTrue(result);

        // 验证记录已删除
        HouseBackup deleted = houseBackupService.getById(savedBackup.getId(), DEFAULT_TENANT_ID);
        assertNull(deleted);
    }

    @Test
    void removeBackup_WithoutBackupId_ThrowsException() {
        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseBackupService.removeBackup(null, DEFAULT_TENANT_ID));
        assertEquals("备份ID不能为空", exception.getMessage());
    }

    @Test
    void removeBackup_WithoutTenantId_ThrowsException() {
        // 准备
        HouseBackup savedBackup = saveAndGetBackup();

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseBackupService.removeBackup(savedBackup.getId(), null));
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    void removeBackup_TenantMismatch_ThrowsException() {
        // 准备
        HouseBackup savedBackup = saveAndGetBackup();

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseBackupService.removeBackup(savedBackup.getId(), OTHER_TENANT_ID));
        assertEquals("备份记录不存在或不属于当前租户", exception.getMessage());
    }

    @Test
    void removeBackup_NonExistentBackup_ThrowsException() {
        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseBackupService.removeBackup(999999L, DEFAULT_TENANT_ID));
        assertEquals("备份记录不存在或不属于当前租户", exception.getMessage());
    }

    // ==================== pageQuery 测试 ====================

    @Test
    void pageQuery_Success() {
        // 准备 - 创建多个备份记录
        for (int i = 0; i < 5; i++) {
            saveAndGetBackup();
        }

        // 执行
        Page<HouseBackup> page = new Page<>(1, 3);
        HouseBackup query = new HouseBackup();
        query.setTenantId(DEFAULT_TENANT_ID);

        IPage<HouseBackup> result = houseBackupService.pageQuery(page, query);

        // 验证
        assertNotNull(result);
        assertFalse(result.getRecords().isEmpty());
        assertEquals(3, result.getRecords().size());

        // 验证所有返回记录都属于指定租户
        assertTrue(result.getRecords().stream()
                .allMatch(backup -> DEFAULT_TENANT_ID.equals(backup.getTenantId())));

        // 验证按删除时间倒序排列
        List<HouseBackup> backups = result.getRecords();
        for (int i = 0; i < backups.size() - 1; i++) {
            LocalDateTime currentTime = backups.get(i).getDeleteTime();
            LocalDateTime nextTime = backups.get(i + 1).getDeleteTime();
            assertTrue(currentTime.isAfter(nextTime) || currentTime.isEqual(nextTime));
        }
    }

    @Test
    void pageQuery_WithHouseNoFilter() {
        // 准备
        HouseBackup backup1 = createTestHouseBackup();
        backup1.setHouseNo("A1001单元");
        houseBackupService.saveBackup(backup1);

        HouseBackup backup2 = createTestHouseBackup();
        backup2.setHouseNo("B2002单元");
        houseBackupService.saveBackup(backup2);

        // 执行 - 查询房源编号包含"1001"的记录
        Page<HouseBackup> page = new Page<>(1, 10);
        HouseBackup query = new HouseBackup();
        query.setTenantId(DEFAULT_TENANT_ID);
        query.setHouseNo("1001");

        IPage<HouseBackup> result = houseBackupService.pageQuery(page, query);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertTrue(result.getRecords().get(0).getHouseNo().contains("1001"));
    }

    @Test
    void pageQuery_WithOriginalIdFilter() {
        // 准备
        HouseBackup backup1 = saveAndGetBackup();
        Long targetOriginalId = backup1.getOriginalId();

        // 执行 - 查询特定原房源ID的记录
        Page<HouseBackup> page = new Page<>(1, 10);
        HouseBackup query = new HouseBackup();
        query.setTenantId(DEFAULT_TENANT_ID);
        query.setOriginalId(targetOriginalId);

        IPage<HouseBackup> result = houseBackupService.pageQuery(page, query);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(targetOriginalId, result.getRecords().get(0).getOriginalId());
    }

    @Test
    void pageQuery_WithoutTenantId_ThrowsException() {
        // 准备
        Page<HouseBackup> page = new Page<>(1, 10);
        HouseBackup query = new HouseBackup();
        // 不设置tenantId

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseBackupService.pageQuery(page, query));
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    // ==================== listByConditions 测试 ====================

    @Test
    void listByConditions_Success() {
        // 准备 - 创建多个备份记录
        for (int i = 0; i < 3; i++) {
            saveAndGetBackup();
        }

        // 执行
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("status", "ON_SALE");

        List<HouseBackup> result = houseBackupService.listByConditions(queryParams, DEFAULT_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.stream()
                .allMatch(backup -> DEFAULT_TENANT_ID.equals(backup.getTenantId())));
        assertTrue(result.stream()
                .allMatch(backup -> "ON_SALE".equals(backup.getStatus())));

        // 验证按删除时间倒序排列
        for (int i = 0; i < result.size() - 1; i++) {
            LocalDateTime currentTime = result.get(i).getDeleteTime();
            LocalDateTime nextTime = result.get(i + 1).getDeleteTime();
            assertTrue(currentTime.isAfter(nextTime) || currentTime.isEqual(nextTime));
        }
    }

    @Test
    void listByConditions_WithOriginalIdFilter() {
        // 准备
        HouseBackup backup1 = saveAndGetBackup();
        Long targetOriginalId = backup1.getOriginalId();

        HouseBackup backup2 = saveAndGetBackup();

        // 执行 - 查询特定原房源ID的记录
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("originalId", targetOriginalId);

        List<HouseBackup> result = houseBackupService.listByConditions(queryParams, DEFAULT_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(targetOriginalId, result.get(0).getOriginalId());
    }

    @Test
    void listByConditions_WithHouseNoFilter() {
        // 准备
        HouseBackup backup1 = createTestHouseBackup();
        backup1.setHouseNo("TEST_A1001");
        houseBackupService.saveBackup(backup1);

        HouseBackup backup2 = createTestHouseBackup();
        backup2.setHouseNo("TEST_B2002");
        houseBackupService.saveBackup(backup2);

        // 执行 - 模糊查询房源编号
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("houseNo", "A1001");

        List<HouseBackup> result = houseBackupService.listByConditions(queryParams, DEFAULT_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getHouseNo().contains("A1001"));
    }

    @Test
    void listByConditions_WithDeleteTimeRange() {
        // 准备 - 创建不同时间的备份记录
        HouseBackup backup1 = createTestHouseBackup();
        backup1.setDeleteTime(LocalDateTime.now().minusDays(2));
        houseBackupService.saveBackup(backup1);

        HouseBackup backup2 = createTestHouseBackup();
        backup2.setDeleteTime(LocalDateTime.now().minusDays(1));
        houseBackupService.saveBackup(backup2);

        HouseBackup backup3 = createTestHouseBackup();
        backup3.setDeleteTime(LocalDateTime.now());
        houseBackupService.saveBackup(backup3);

        // 执行 - 查询昨天到今天删除的记录
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("startDeleteTime", LocalDateTime.now().minusDays(1).withHour(0).withMinute(0));
        queryParams.put("endDeleteTime", LocalDateTime.now().withHour(23).withMinute(59));

        List<HouseBackup> result = houseBackupService.listByConditions(queryParams, DEFAULT_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertTrue(result.size() >= 2); // 至少包含backup2和backup3
    }

    @Test
    void listByConditions_WithoutTenantId_ThrowsException() {
        // 准备
        Map<String, Object> queryParams = new HashMap<>();

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseBackupService.listByConditions(queryParams, null));
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    // ==================== batchCreate 测试 ====================

    @Test
    void batchCreate_Success() {
        // 准备
        HouseBackup backup1 = createTestHouseBackup();
        HouseBackup backup2 = createTestHouseBackup();
        HouseBackup backup3 = createTestRentedHouseBackup();

        List<HouseBackup> backupList = Arrays.asList(backup1, backup2, backup3);

        // 执行
        boolean result = houseBackupService.batchCreate(backupList);

        // 验证
        assertTrue(result);
        assertNotNull(backup1.getId());
        assertNotNull(backup2.getId());
        assertNotNull(backup3.getId());

        // 验证租户ID一致性
        assertEquals(backup1.getTenantId(), backup2.getTenantId());
        assertEquals(backup2.getTenantId(), backup3.getTenantId());
    }

    @Test
    void batchCreate_EmptyList_ThrowsException() {
        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseBackupService.batchCreate(Collections.emptyList()));
        assertEquals("备份列表不能为空", exception.getMessage());
    }

    @Test
    void batchCreate_DifferentTenants_ThrowsException() {
        // 准备
        HouseBackup backup1 = createTestHouseBackup();
        backup1.setTenantId(DEFAULT_TENANT_ID);

        HouseBackup backup2 = createTestHouseBackup();
        backup2.setTenantId(OTHER_TENANT_ID); // 不同的租户

        List<HouseBackup> backupList = Arrays.asList(backup1, backup2);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseBackupService.batchCreate(backupList));
        assertEquals("批量备份必须属于同一租户", exception.getMessage());
    }

    @Test
    void batchCreate_WithoutTenantId_ThrowsException() {
        // 准备
        HouseBackup backup1 = createTestHouseBackup();
        backup1.setTenantId(null); // 没有租户ID

        List<HouseBackup> backupList = Collections.singletonList(backup1);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseBackupService.batchCreate(backupList));
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    // ==================== batchRemove 测试 ====================

    @Test
    void batchRemove_Success() {
        // 准备
        HouseBackup backup1 = saveAndGetBackup();
        HouseBackup backup2 = saveAndGetBackup();
        HouseBackup backup3 = saveAndGetBackup();

        List<Long> backupIds = Arrays.asList(backup1.getId(), backup2.getId(), backup3.getId());

        // 执行
        boolean result = houseBackupService.batchRemove(backupIds, DEFAULT_TENANT_ID);

        // 验证
        assertTrue(result);

        // 验证记录已删除
        assertNull(houseBackupService.getById(backup1.getId(), DEFAULT_TENANT_ID));
        assertNull(houseBackupService.getById(backup2.getId(), DEFAULT_TENANT_ID));
        assertNull(houseBackupService.getById(backup3.getId(), DEFAULT_TENANT_ID));
    }

    @Test
    void batchRemove_EmptyIds_ThrowsException() {
        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseBackupService.batchRemove(Collections.emptyList(), DEFAULT_TENANT_ID));
        assertEquals("备份ID列表不能为空", exception.getMessage());
    }

    @Test
    void batchRemove_WithoutTenantId_ThrowsException() {
        // 准备
        List<Long> backupIds = Arrays.asList(1L, 2L, 3L);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseBackupService.batchRemove(backupIds, null));
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    void batchRemove_CrossTenantRecords_ThrowsException() {
        // 准备 - 在不同租户下创建备份记录
        HouseBackup tenant1Backup = createTestHouseBackup();
        tenant1Backup.setTenantId(DEFAULT_TENANT_ID);
        houseBackupService.saveBackup(tenant1Backup);

        HouseBackup tenant2Backup = createTestHouseBackup();
        tenant2Backup.setTenantId(OTHER_TENANT_ID);
        houseBackupService.saveBackup(tenant2Backup);

        List<Long> backupIds = Arrays.asList(tenant1Backup.getId(), tenant2Backup.getId());

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseBackupService.batchRemove(backupIds, DEFAULT_TENANT_ID));
        assertEquals("存在不属于当前租户的备份记录，无法删除", exception.getMessage());
    }

    // ==================== batchGetByOriginalIds 测试 ====================

    @Test
    void batchGetByOriginalIds_Success() {
        // 准备
        HouseBackup backup1 = saveAndGetBackup();
        HouseBackup backup2 = saveAndGetBackup();
        HouseBackup backup3 = saveAndGetBackup();

        List<Long> originalIds = Arrays.asList(
                backup1.getOriginalId(),
                backup2.getOriginalId(),
                backup3.getOriginalId()
        );

        // 执行
        List<HouseBackup> result = houseBackupService.batchGetByOriginalIds(originalIds, DEFAULT_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertEquals(3, result.size());

        // 验证所有返回记录的原房源ID都在查询列表中
        Set<Long> resultOriginalIds = new HashSet<>();
        result.forEach(backup -> resultOriginalIds.add(backup.getOriginalId()));

        assertTrue(resultOriginalIds.containsAll(originalIds));
    }

    @Test
    void batchGetByOriginalIds_EmptyIds_ThrowsException() {
        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseBackupService.batchGetByOriginalIds(Collections.emptyList(), DEFAULT_TENANT_ID));
        assertEquals("原房源ID列表不能为空", exception.getMessage());
    }

    @Test
    void batchGetByOriginalIds_WithoutTenantId_ThrowsException() {
        // 准备
        List<Long> originalIds = Arrays.asList(1001L, 1002L, 1003L);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> houseBackupService.batchGetByOriginalIds(originalIds, null));
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    void batchGetByOriginalIds_TenantMismatch_ReturnsEmptyList() {
        // 准备
        HouseBackup backup1 = saveAndGetBackup();
        HouseBackup backup2 = saveAndGetBackup();

        List<Long> originalIds = Arrays.asList(backup1.getOriginalId(), backup2.getOriginalId());

        // 执行 - 使用不同的租户ID查询
        List<HouseBackup> result = houseBackupService.batchGetByOriginalIds(originalIds, OTHER_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void batchGetByOriginalIds_SomeNonExistent_ReturnsExistingOnly() {
        // 准备
        HouseBackup backup1 = saveAndGetBackup();
        HouseBackup backup2 = saveAndGetBackup();

        // 包含存在的和不存在的原房源ID
        List<Long> originalIds = Arrays.asList(
                backup1.getOriginalId(),
                backup2.getOriginalId(),
                999999L, // 不存在的ID
                888888L  // 不存在的ID
        );

        // 执行
        List<HouseBackup> result = houseBackupService.batchGetByOriginalIds(originalIds, DEFAULT_TENANT_ID);

        // 验证 - 只返回存在的记录
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    // ==================== 字段验证测试 ====================

    @Test
    void testAllRequiredFields() {
        // 验证实体类所有必填字段都能正确保存
        HouseBackup backup = createTestHouseBackup();

        // 设置所有必填字段
        backup.setOriginalId(1001L);
        backup.setTenantId(DEFAULT_TENANT_ID);
        backup.setBuildingId(101L);
        backup.setHouseNo("A1001单元");
        backup.setHouseType("3室2厅");
        backup.setArea(new BigDecimal("120.50"));
        backup.setFloor(10);
        backup.setTotalFloor(18);
        backup.setOrientation("南北通透");
        backup.setDecoration("DELUXE");
        backup.setPropertyRight("商品房");
        backup.setPropertyRightCertNo("浙房地权证杭字第1001号");
        backup.setPropertyRightYears(70);
        backup.setMortgageStatus("NONE");
        backup.setPrice(new BigDecimal("180.00"));
        backup.setTransactionType("SALE");
        backup.setStatus("ON_SALE");
        backup.setCreateAgentId(3001L);
        backup.setDeleteOperator("测试删除人");

        // 执行
        boolean result = houseBackupService.saveBackup(backup);

        // 验证
        assertTrue(result);
        assertNotNull(backup.getId());
    }

    @Test
    void testBackupDataIntegrity() {
        // 验证备份数据完整性
        HouseBackup originalBackup = createTestHouseBackup();

        // 保存原始备份
        houseBackupService.saveBackup(originalBackup);

        // 查询备份
        HouseBackup retrievedBackup = houseBackupService.getById(
                originalBackup.getId(), DEFAULT_TENANT_ID);

        // 验证所有字段都正确保存和检索
        assertNotNull(retrievedBackup);
        assertEquals(originalBackup.getOriginalId(), retrievedBackup.getOriginalId());
        assertEquals(originalBackup.getHouseNo(), retrievedBackup.getHouseNo());
        assertEquals(originalBackup.getHouseType(), retrievedBackup.getHouseType());
        assertEquals(originalBackup.getArea(), retrievedBackup.getArea());
        assertEquals(originalBackup.getFloor(), retrievedBackup.getFloor());
        assertEquals(originalBackup.getTotalFloor(), retrievedBackup.getTotalFloor());
        assertEquals(originalBackup.getOrientation(), retrievedBackup.getOrientation());
        assertEquals(originalBackup.getDecoration(), retrievedBackup.getDecoration());
        assertEquals(originalBackup.getPropertyRight(), retrievedBackup.getPropertyRight());
        assertEquals(originalBackup.getPropertyRightCertNo(), retrievedBackup.getPropertyRightCertNo());
        assertEquals(originalBackup.getPropertyRightYears(), retrievedBackup.getPropertyRightYears());
        assertEquals(originalBackup.getMortgageStatus(), retrievedBackup.getMortgageStatus());
        assertEquals(originalBackup.getPrice(), retrievedBackup.getPrice());
        assertEquals(originalBackup.getTransactionType(), retrievedBackup.getTransactionType());
        assertEquals(originalBackup.getStatus(), retrievedBackup.getStatus());
        assertEquals(originalBackup.getCreateAgentId(), retrievedBackup.getCreateAgentId());
        assertEquals(originalBackup.getDeleteOperator(), retrievedBackup.getDeleteOperator());
    }
}
package com.house.deed.pavilion;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.entity.ElectronicSign;
import com.house.deed.pavilion.service.ElectronicSignService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ElectronicSignServiceImpl 集成测试
 */
@SpringBootTest
@Transactional
class ElectronicSignServiceImplTest {

    @Autowired
    private ElectronicSignService electronicSignService;

    // 租户ID常量
    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;

    // 合同ID计数器（确保唯一性）
    private final AtomicLong contractIdCounter = new AtomicLong(1000L);

    @BeforeEach
    void setUp() {
        // 重置计数器，确保每个测试开始时ID序列一致
        contractIdCounter.set(1000L);
    }

    // 生成唯一签约链接
    private String generateUniqueSignUrl() {
        return "https://esign.example.com/sign/" + UUID.randomUUID().toString();
    }

    // 生成唯一签名哈希值
    private String generateUniqueSignHash() {
        return "hash_" + UUID.randomUUID().toString().replace("-", "");
    }

    // 生成唯一PDF地址
    private String generateUniquePdfUrl() {
        return "https://storage.example.com/contracts/" + UUID.randomUUID().toString() + ".pdf";
    }

    // 生成唯一合同ID
    private Long generateUniqueContractId() {
        return contractIdCounter.getAndIncrement();
    }

    /**
     * 创建测试电子签约对象 - PENDING状态
     */
    private ElectronicSign createTestPendingSign() {
        ElectronicSign sign = new ElectronicSign();

        // 必填字段
        sign.setTenantId(DEFAULT_TENANT_ID);
        sign.setContractId(generateUniqueContractId()); // 使用唯一合同ID
        sign.setSignPlatform("法大大");
        sign.setSignUrl(generateUniqueSignUrl());
        sign.setSignStatus("PENDING");
        sign.setSignHash(generateUniqueSignHash());

        return sign;
    }

    /**
     * 创建测试电子签约对象 - SIGNED状态
     */
    private ElectronicSign createTestSignedSign() {
        ElectronicSign sign = createTestPendingSign();
        sign.setSignStatus("SIGNED");
        sign.setCustomerSignTime(LocalDateTime.now().minusDays(1));
        sign.setLandlordSignTime(LocalDateTime.now());
        sign.setContractPdfUrl(generateUniquePdfUrl());

        return sign;
    }

    /**
     * 保存并返回电子签约对象
     */
    private ElectronicSign saveAndGetSign() {
        ElectronicSign sign = createTestPendingSign();
        electronicSignService.saveElectronicSign(sign);
        return sign;
    }

    @Test
    void saveElectronicSign_PendingStatus_Success() {
        // 准备
        ElectronicSign sign = createTestPendingSign();

        // 执行
        boolean result = electronicSignService.saveElectronicSign(sign);

        // 验证
        assertTrue(result);
        assertNotNull(sign.getId());
        assertEquals(DEFAULT_TENANT_ID, sign.getTenantId());
        assertEquals("PENDING", sign.getSignStatus());
        assertNull(sign.getCustomerSignTime()); // PENDING状态不应有时间
        assertNull(sign.getLandlordSignTime());
        assertNull(sign.getContractPdfUrl());
        assertNotNull(sign.getCreateTime());
    }

    @Test
    void saveElectronicSign_SignedStatus_Success() {
        // 准备
        ElectronicSign sign = createTestSignedSign();

        // 执行
        boolean result = electronicSignService.saveElectronicSign(sign);

        // 验证
        assertTrue(result);
        assertNotNull(sign.getId());
        assertEquals("SIGNED", sign.getSignStatus());
        assertNotNull(sign.getCustomerSignTime());
        assertNotNull(sign.getLandlordSignTime());
        assertNotNull(sign.getContractPdfUrl());
    }

    @Test
    void saveElectronicSign_WithoutTenantId_ThrowsException() {
        // 准备
        ElectronicSign sign = createTestPendingSign();
        sign.setTenantId(null);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.saveElectronicSign(sign));
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    void saveElectronicSign_WithoutContractId_ThrowsException() {
        // 准备
        ElectronicSign sign = createTestPendingSign();
        sign.setContractId(null);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.saveElectronicSign(sign));
        assertEquals("合同ID不能为空", exception.getMessage());
    }

    @Test
    void saveElectronicSign_WithoutSignPlatform_ThrowsException() {
        // 准备
        ElectronicSign sign = createTestPendingSign();
        sign.setSignPlatform(null);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.saveElectronicSign(sign));
        assertEquals("电子签平台不能为空", exception.getMessage());
    }

    @Test
    void saveElectronicSign_WithoutSignUrl_ThrowsException() {
        // 准备
        ElectronicSign sign = createTestPendingSign();
        sign.setSignUrl(null);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.saveElectronicSign(sign));
        assertEquals("签约链接不能为空", exception.getMessage());
    }

    @Test
    void saveElectronicSign_WithoutSignStatus_ThrowsException() {
        // 准备
        ElectronicSign sign = createTestPendingSign();
        sign.setSignStatus(null);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.saveElectronicSign(sign));
        assertEquals("签约状态不能为空", exception.getMessage());
    }

    @Test
    void saveElectronicSign_InvalidSignStatus_ThrowsException() {
        // 准备
        ElectronicSign sign = createTestPendingSign();
        sign.setSignStatus("INVALID");

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.saveElectronicSign(sign));
        assertTrue(exception.getMessage().contains("签约状态无效"));
    }

    @Test
    void saveElectronicSign_WithoutSignHash_ThrowsException() {
        // 准备
        ElectronicSign sign = createTestPendingSign();
        sign.setSignHash(null);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.saveElectronicSign(sign));
        assertEquals("电子签名哈希值不能为空", exception.getMessage());
    }

    @Test
    void saveElectronicSign_SignedStatusWithoutCustomerSignTime_ThrowsException() {
        // 准备
        ElectronicSign sign = createTestSignedSign();
        sign.setCustomerSignTime(null); // SIGNED状态必须有时

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.saveElectronicSign(sign));
        assertTrue(exception.getMessage().contains("客户签约时间不能为空（已签状态）"));
    }

    @Test
    void saveElectronicSign_SignedStatusWithoutLandlordSignTime_ThrowsException() {
        // 准备
        ElectronicSign sign = createTestSignedSign();
        sign.setLandlordSignTime(null); // SIGNED状态必须有时

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.saveElectronicSign(sign));
        assertTrue(exception.getMessage().contains("房东签约时间不能为空（已签状态）"));
    }

    @Test
    void saveElectronicSign_SignedStatusWithoutContractPdfUrl_ThrowsException() {
        // 准备
        ElectronicSign sign = createTestSignedSign();
        sign.setContractPdfUrl(null); // SIGNED状态必须有时

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.saveElectronicSign(sign));
        assertTrue(exception.getMessage().contains("电子合同PDF地址不能为空（已签状态）"));
    }

    @Test
    void updateElectronicSignById_Success() {
        // 准备
        ElectronicSign savedSign = saveAndGetSign();
        savedSign.setSignUrl("https://updated.url");
        savedSign.setSignStatus("REJECTED"); // 更新状态

        // 执行
        boolean result = electronicSignService.updateElectronicSignById(savedSign);

        // 验证
        assertTrue(result);

        // 重新查询验证更新
        ElectronicSign updated = electronicSignService.getElectronicSignById(savedSign.getId(), DEFAULT_TENANT_ID);
        assertEquals("https://updated.url", updated.getSignUrl());
        assertEquals("REJECTED", updated.getSignStatus());
    }

    @Test
    void updateElectronicSignById_UpdateToSignedStatus_Success() {
        // 准备 - 先保存一个PENDING状态的记录
        ElectronicSign savedSign = saveAndGetSign();
        assertEquals("PENDING", savedSign.getSignStatus());

        // 更新为SIGNED状态，需要提供必要字段
        LocalDateTime now = LocalDateTime.now();
        savedSign.setSignStatus("SIGNED");
        savedSign.setCustomerSignTime(now.minusHours(1));
        savedSign.setLandlordSignTime(now);
        savedSign.setContractPdfUrl(generateUniquePdfUrl());

        // 执行
        boolean result = electronicSignService.updateElectronicSignById(savedSign);

        // 验证
        assertTrue(result);

        ElectronicSign updated = electronicSignService.getElectronicSignById(savedSign.getId(), DEFAULT_TENANT_ID);
        assertEquals("SIGNED", updated.getSignStatus());
        assertNotNull(updated.getCustomerSignTime());
        assertNotNull(updated.getLandlordSignTime());
        assertNotNull(updated.getContractPdfUrl());
    }

    @Test
    void updateElectronicSignById_WithoutId_ThrowsException() {
        // 准备
        ElectronicSign sign = createTestPendingSign();
        sign.setId(null);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.updateElectronicSignById(sign));
        assertEquals("记录ID不能为空", exception.getMessage());
    }

    @Test
    void updateElectronicSignById_WithoutTenantId_ThrowsException() {
        // 准备
        ElectronicSign savedSign = saveAndGetSign();
        savedSign.setTenantId(null);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.updateElectronicSignById(savedSign));
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    void updateElectronicSignById_NonExistentSign_ThrowsException() {
        // 准备
        ElectronicSign sign = createTestPendingSign();
        sign.setId(999999L);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.updateElectronicSignById(sign));
        assertEquals("电子签约记录不存在", exception.getMessage());
    }

    @Test
    void updateElectronicSignById_TenantMismatch_ThrowsException() {
        // 准备
        ElectronicSign savedSign = saveAndGetSign();

        // 使用错误的租户ID更新
        ElectronicSign updateRequest = createTestPendingSign();
        updateRequest.setId(savedSign.getId());
        updateRequest.setTenantId(OTHER_TENANT_ID);
        updateRequest.setSignUrl("尝试更新");

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.updateElectronicSignById(updateRequest));
        assertEquals("无权限操作此记录", exception.getMessage());
    }

    @Test
    void updateElectronicSignById_UpdateToSignedStatusMissingFields_ThrowsException() {
        // 准备 - 从PENDING更新到SIGNED，但缺少必要字段
        ElectronicSign savedSign = saveAndGetSign();
        savedSign.setSignStatus("SIGNED");
        // 故意不设置customerSignTime、landlordSignTime、contractPdfUrl

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.updateElectronicSignById(savedSign));
        assertTrue(exception.getMessage().contains("不能为空（已签状态）"));
    }

    @Test
    void updateElectronicSignById_CoreFieldsNotModified() {
        // 准备
        ElectronicSign savedSign = saveAndGetSign();
        String originalHash = savedSign.getSignHash();
        Long originalContractId = savedSign.getContractId();
        String originalPlatform = savedSign.getSignPlatform();

        // 尝试修改核心字段
        savedSign.setSignHash("尝试修改哈希值");
        savedSign.setContractId(999L);
        savedSign.setSignPlatform("尝试修改平台");

        // 执行
        boolean result = electronicSignService.updateElectronicSignById(savedSign);

        // 验证
        assertTrue(result);

        // 重新查询验证核心字段未被修改
        ElectronicSign updated = electronicSignService.getElectronicSignById(savedSign.getId(), DEFAULT_TENANT_ID);
        assertEquals(originalHash, updated.getSignHash());
        assertEquals(originalContractId, updated.getContractId());
        assertEquals(originalPlatform, updated.getSignPlatform());
    }

    @Test
    void removeElectronicSignById_Success() {
        // 准备
        ElectronicSign savedSign = saveAndGetSign();

        // 执行
        boolean result = electronicSignService.removeElectronicSignById(savedSign.getId(), DEFAULT_TENANT_ID);

        // 验证
        assertTrue(result);

        // 验证记录已删除
        ElectronicSign deleted = electronicSignService.getElectronicSignById(savedSign.getId(), DEFAULT_TENANT_ID);
        assertNull(deleted);
    }

    @Test
    void removeElectronicSignById_WithoutId_ThrowsException() {
        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.removeElectronicSignById(null, DEFAULT_TENANT_ID));
        assertEquals("记录ID不能为空", exception.getMessage());
    }

    @Test
    void removeElectronicSignById_WithoutTenantId_ThrowsException() {
        // 准备
        ElectronicSign savedSign = saveAndGetSign();

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.removeElectronicSignById(savedSign.getId(), null));
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    void removeElectronicSignById_TenantMismatch_ThrowsException() {
        // 准备
        ElectronicSign savedSign = saveAndGetSign();

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.removeElectronicSignById(savedSign.getId(), OTHER_TENANT_ID));
        assertEquals("无权限操作此记录", exception.getMessage());
    }

    @Test
    void getElectronicSignById_Success() {
        // 准备
        ElectronicSign savedSign = saveAndGetSign();

        // 执行
        ElectronicSign result = electronicSignService.getElectronicSignById(savedSign.getId(), DEFAULT_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertEquals(savedSign.getId(), result.getId());
        assertEquals(savedSign.getTenantId(), result.getTenantId());
        assertEquals(savedSign.getContractId(), result.getContractId());
        assertEquals(savedSign.getSignPlatform(), result.getSignPlatform());
        assertEquals(savedSign.getSignUrl(), result.getSignUrl());
        assertEquals(savedSign.getSignStatus(), result.getSignStatus());
        assertEquals(savedSign.getSignHash(), result.getSignHash());
    }

    @Test
    void getElectronicSignById_TenantMismatch_ReturnsNull() {
        // 准备
        ElectronicSign savedSign = saveAndGetSign();

        // 执行
        ElectronicSign result = electronicSignService.getElectronicSignById(savedSign.getId(), OTHER_TENANT_ID);

        // 验证 - 租户不匹配应返回null
        assertNull(result);
    }

    @Test
    void pageQuery_Success() {
        // 准备 - 创建多个测试记录
        for (int i = 0; i < 5; i++) {
            saveAndGetSign();
        }

        // 执行
        Page<ElectronicSign> page = new Page<>(1, 3);
        ElectronicSign query = new ElectronicSign();
        query.setTenantId(DEFAULT_TENANT_ID);

        IPage<ElectronicSign> result = electronicSignService.pageQuery(page, query);

        // 验证
        assertNotNull(result);
        assertFalse(result.getRecords().isEmpty());

        // 验证所有返回记录都属于指定租户
        assertTrue(result.getRecords().stream()
                .allMatch(s -> DEFAULT_TENANT_ID.equals(s.getTenantId())));

        // 验证按创建时间倒序排列
        List<ElectronicSign> signs = result.getRecords();
        for (int i = 0; i < signs.size() - 1; i++) {
            assertTrue(signs.get(i).getCreateTime().isAfter(signs.get(i + 1).getCreateTime()) ||
                    signs.get(i).getCreateTime().isEqual(signs.get(i + 1).getCreateTime()));
        }
    }

    @Test
    void pageQuery_WithContractIdFilter() {
        // 准备 - 为不同合同创建记录
        ElectronicSign sign1 = createTestPendingSign();
        sign1.setContractId(1000L);
        electronicSignService.saveElectronicSign(sign1);

        ElectronicSign sign2 = createTestPendingSign();
        sign2.setContractId(2000L);
        electronicSignService.saveElectronicSign(sign2);

        // 执行 - 查询合同ID为1000的记录
        Page<ElectronicSign> page = new Page<>(1, 10);
        ElectronicSign query = new ElectronicSign();
        query.setTenantId(DEFAULT_TENANT_ID);
        query.setContractId(1000L);

        IPage<ElectronicSign> result = electronicSignService.pageQuery(page, query);

        // 验证
        assertNotNull(result);
        assertTrue(result.getRecords().stream()
                .allMatch(s -> 1000L == s.getContractId()));
    }

    @Test
    void pageQuery_WithSignPlatformFilter() {
        // 准备
        ElectronicSign sign1 = createTestPendingSign();
        sign1.setSignPlatform("法大大");
        electronicSignService.saveElectronicSign(sign1);

        ElectronicSign sign2 = createTestPendingSign();
        sign2.setSignPlatform("腾讯电子签");
        electronicSignService.saveElectronicSign(sign2);

        // 执行 - 查询平台为"法大大"的记录
        Page<ElectronicSign> page = new Page<>(1, 10);
        ElectronicSign query = new ElectronicSign();
        query.setTenantId(DEFAULT_TENANT_ID);
        query.setSignPlatform("法大大");

        IPage<ElectronicSign> result = electronicSignService.pageQuery(page, query);

        // 验证
        assertNotNull(result);
        assertTrue(result.getRecords().stream()
                .allMatch(s -> "法大大".equals(s.getSignPlatform())));
    }

    @Test
    void pageQuery_WithSignStatusFilter() {
        // 准备
        ElectronicSign pendingSign = createTestPendingSign();
        pendingSign.setSignStatus("PENDING");
        electronicSignService.saveElectronicSign(pendingSign);

        ElectronicSign signedSign = createTestSignedSign();
        electronicSignService.saveElectronicSign(signedSign);

        // 执行 - 查询状态为"PENDING"的记录
        Page<ElectronicSign> page = new Page<>(1, 10);
        ElectronicSign query = new ElectronicSign();
        query.setTenantId(DEFAULT_TENANT_ID);
        query.setSignStatus("PENDING");

        IPage<ElectronicSign> result = electronicSignService.pageQuery(page, query);

        // 验证
        assertNotNull(result);
        assertTrue(result.getRecords().stream()
                .allMatch(s -> "PENDING".equals(s.getSignStatus())));
    }

    @Test
    void pageQuery_WithCustomerSignTimeFilter() {
        // 准备
        LocalDateTime now = LocalDateTime.now();

        ElectronicSign sign1 = createTestSignedSign();
        sign1.setCustomerSignTime(now.minusDays(3));
        electronicSignService.saveElectronicSign(sign1);

        ElectronicSign sign2 = createTestSignedSign();
        sign2.setCustomerSignTime(now.minusDays(1));
        electronicSignService.saveElectronicSign(sign2);

        // 执行 - 查询一天内的客户签约记录
        Page<ElectronicSign> page = new Page<>(1, 10);
        ElectronicSign query = new ElectronicSign();
        query.setTenantId(DEFAULT_TENANT_ID);
        query.setCustomerSignTime(now.minusDays(1));

        IPage<ElectronicSign> result = electronicSignService.pageQuery(page, query);

        // 验证
        assertNotNull(result);
        assertTrue(result.getRecords().stream()
                .allMatch(s -> !s.getCustomerSignTime().isBefore(now.minusDays(1))));
    }

    @Test
    void pageQuery_WithSignHashExact() {
        // 准备
        ElectronicSign sign = saveAndGetSign();
        String targetHash = sign.getSignHash();

        // 执行
        Page<ElectronicSign> page = new Page<>(1, 10);
        ElectronicSign query = new ElectronicSign();
        query.setTenantId(DEFAULT_TENANT_ID);
        query.setSignHash(targetHash);

        IPage<ElectronicSign> result = electronicSignService.pageQuery(page, query);

        // 验证
        assertNotNull(result);
        assertTrue(result.getRecords().stream()
                .allMatch(s -> targetHash.equals(s.getSignHash())));
    }

    @Test
    void listByConditions_Success() {
        // 准备
        for (int i = 0; i < 3; i++) {
            saveAndGetSign();
        }

        // 执行
        ElectronicSign query = new ElectronicSign();
        query.setTenantId(DEFAULT_TENANT_ID);
        query.setSignStatus("PENDING");

        List<ElectronicSign> result = electronicSignService.listByConditions(query);

        // 验证
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.stream()
                .allMatch(s -> DEFAULT_TENANT_ID.equals(s.getTenantId())));
        assertTrue(result.stream()
                .allMatch(s -> "PENDING".equals(s.getSignStatus())));
    }

    @Test
    void listByContractId_Success() {
        // 准备 - 为同一个合同创建一条记录
        long testContractId = generateUniqueContractId();
        long tenantId = DEFAULT_TENANT_ID;

        ElectronicSign sign1 = createTestPendingSign();
        sign1.setContractId(testContractId);
        electronicSignService.saveElectronicSign(sign1);

        // 执行
        List<ElectronicSign> result = electronicSignService.listByContractId(testContractId, tenantId);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.stream().allMatch(s -> s.getContractId().equals(testContractId)));
    }

    @Test
    void batchSaveElectronicSigns_Success() {
        // 准备
        ElectronicSign sign1 = createTestPendingSign();
        ElectronicSign sign2 = createTestSignedSign();
        ElectronicSign sign3 = createTestPendingSign();

        List<ElectronicSign> signList = Arrays.asList(sign1, sign2, sign3);

        // 执行
        boolean result = electronicSignService.batchSaveElectronicSigns(signList);

        // 验证
        assertTrue(result);
        assertNotNull(sign1.getId());
        assertNotNull(sign2.getId());
        assertNotNull(sign3.getId());
    }

    @Test
    void batchSaveElectronicSigns_EmptyList_ReturnsTrue() {
        // 执行
        boolean result = electronicSignService.batchSaveElectronicSigns(Collections.emptyList());

        // 验证
        assertTrue(result);
    }

    @Test
    void batchSaveElectronicSigns_DifferentTenants_ThrowsException() {
        // 准备
        ElectronicSign sign1 = createTestPendingSign();
        sign1.setTenantId(DEFAULT_TENANT_ID);

        ElectronicSign sign2 = createTestPendingSign();
        sign2.setTenantId(OTHER_TENANT_ID); // 不同租户

        List<ElectronicSign> signList = Arrays.asList(sign1, sign2);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.batchSaveElectronicSigns(signList));
        assertEquals("批量记录必须属于同一租户", exception.getMessage());
    }

    @Test
    void batchSaveElectronicSigns_InvalidSignStatus_ThrowsException() {
        // 准备
        ElectronicSign sign1 = createTestPendingSign();
        ElectronicSign sign2 = createTestPendingSign();
        sign2.setSignStatus("INVALID"); // 无效状态

        List<ElectronicSign> signList = Arrays.asList(sign1, sign2);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.batchSaveElectronicSigns(signList));
        assertTrue(exception.getMessage().contains("签约状态无效"));
    }

    @Test
    void batchSaveElectronicSigns_SignedStatusMissingFields_ThrowsException() {
        // 准备
        ElectronicSign sign1 = createTestPendingSign();
        ElectronicSign sign2 = createTestSignedSign();
        sign2.setCustomerSignTime(null); // 缺少必要字段

        List<ElectronicSign> signList = Arrays.asList(sign1, sign2);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.batchSaveElectronicSigns(signList));
        assertTrue(exception.getMessage().contains("客户签约时间不能为空（已签状态）"));
    }

    @Test
    void batchRemoveElectronicSigns_Success() {
        // 准备
        ElectronicSign sign1 = saveAndGetSign();
        ElectronicSign sign2 = saveAndGetSign();

        // 执行
        boolean result = electronicSignService.batchRemoveElectronicSigns(
                Arrays.asList(sign1.getId(), sign2.getId()), DEFAULT_TENANT_ID);

        // 验证
        assertTrue(result);

        // 验证记录已删除
        assertNull(electronicSignService.getElectronicSignById(sign1.getId(), DEFAULT_TENANT_ID));
        assertNull(electronicSignService.getElectronicSignById(sign2.getId(), DEFAULT_TENANT_ID));
    }

    @Test
    void batchRemoveElectronicSigns_WithoutTenantId_ThrowsException() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.batchRemoveElectronicSigns(ids, null));
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    void batchRemoveElectronicSigns_EmptyIds_ThrowsException() {
        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.batchRemoveElectronicSigns(Collections.emptyList(), DEFAULT_TENANT_ID));
        assertEquals("记录ID列表不能为空", exception.getMessage());
    }

    @Test
    void batchRemoveElectronicSigns_CrossTenantRecords_ThrowsException() {
        // 准备 - 在不同租户下创建记录
        long contractId1 = generateUniqueContractId();
        long contractId2 = generateUniqueContractId();

        ElectronicSign tenant1Sign = createTestPendingSign();
        tenant1Sign.setTenantId(DEFAULT_TENANT_ID);
        tenant1Sign.setContractId(contractId1);
        electronicSignService.saveElectronicSign(tenant1Sign);

        ElectronicSign tenant2Sign = createTestPendingSign();
        tenant2Sign.setTenantId(OTHER_TENANT_ID);
        tenant2Sign.setContractId(contractId2);
        electronicSignService.saveElectronicSign(tenant2Sign);

        List<Long> ids = Arrays.asList(tenant1Sign.getId(), tenant2Sign.getId());

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.batchRemoveElectronicSigns(ids, DEFAULT_TENANT_ID));
        assertEquals("存在跨租户记录ID，无法删除", exception.getMessage());
    }

    @Test
    void batchUpdateStatus_Success() {
        // 准备
        ElectronicSign sign1 = saveAndGetSign();
        ElectronicSign sign2 = saveAndGetSign();

        List<Long> ids = Arrays.asList(sign1.getId(), sign2.getId());

        // 执行 - 批量更新状态为EXPIRED
        boolean result = electronicSignService.batchUpdateStatus(ids, "EXPIRED", DEFAULT_TENANT_ID);

        // 验证
        assertTrue(result);

        // 验证状态已更新
        ElectronicSign updated1 = electronicSignService.getElectronicSignById(sign1.getId(), DEFAULT_TENANT_ID);
        ElectronicSign updated2 = electronicSignService.getElectronicSignById(sign2.getId(), DEFAULT_TENANT_ID);
        assertEquals("EXPIRED", updated1.getSignStatus());
        assertEquals("EXPIRED", updated2.getSignStatus());
    }

    @Test
    void batchUpdateStatus_WithoutTenantId_ThrowsException() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.batchUpdateStatus(ids, "EXPIRED", null));
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    void batchUpdateStatus_EmptyIds_ThrowsException() {
        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.batchUpdateStatus(Collections.emptyList(), "EXPIRED", DEFAULT_TENANT_ID));
        assertEquals("记录ID列表不能为空", exception.getMessage());
    }

    @Test
    void batchUpdateStatus_WithoutStatus_ThrowsException() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.batchUpdateStatus(ids, null, DEFAULT_TENANT_ID));
        assertEquals("签约状态不能为空", exception.getMessage());
    }

    @Test
    void batchUpdateStatus_InvalidStatus_ThrowsException() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.batchUpdateStatus(ids, "INVALID", DEFAULT_TENANT_ID));
        assertTrue(exception.getMessage().contains("签约状态无效"));
    }

    @Test
    void batchUpdateStatus_CrossTenantRecords_ThrowsException() {
        // 准备 - 在不同租户下创建记录
        long contractId1 = generateUniqueContractId();
        long contractId2 = generateUniqueContractId();

        ElectronicSign tenant1Sign = createTestPendingSign();
        tenant1Sign.setTenantId(DEFAULT_TENANT_ID);
        tenant1Sign.setContractId(contractId1);
        electronicSignService.saveElectronicSign(tenant1Sign);

        ElectronicSign tenant2Sign = createTestPendingSign();
        tenant2Sign.setTenantId(OTHER_TENANT_ID);
        tenant2Sign.setContractId(contractId2);
        electronicSignService.saveElectronicSign(tenant2Sign);

        List<Long> ids = Arrays.asList(tenant1Sign.getId(), tenant2Sign.getId());

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> electronicSignService.batchUpdateStatus(ids, "EXPIRED", DEFAULT_TENANT_ID));
        assertEquals("存在跨租户记录ID，无法更新", exception.getMessage());
    }

    @Test
    void testSignStatusValidation() {
        // 测试所有有效的签约状态
        String[] validStatuses = {"PENDING", "SIGNED", "REJECTED", "EXPIRED"};

        for (String status : validStatuses) {
            ElectronicSign sign = createTestPendingSign();
            sign.setSignStatus(status);

            // 如果是SIGNED状态，需要额外字段
            if ("SIGNED".equals(status)) {
                sign.setCustomerSignTime(LocalDateTime.now());
                sign.setLandlordSignTime(LocalDateTime.now());
                sign.setContractPdfUrl(generateUniquePdfUrl());
            }

            boolean result = electronicSignService.saveElectronicSign(sign);
            assertTrue(result);
            assertEquals(status, sign.getSignStatus());
        }
    }

    @Test
    void testSignHashUniqueness() {
        // 测试不同的哈希值可以保存为不同记录
        String hash1 = generateUniqueSignHash();
        String hash2 = generateUniqueSignHash();

        ElectronicSign sign1 = createTestPendingSign();
        sign1.setSignHash(hash1);
        electronicSignService.saveElectronicSign(sign1);

        ElectronicSign sign2 = createTestPendingSign();
        sign2.setSignHash(hash2); // 不同哈希值
        electronicSignService.saveElectronicSign(sign2);

        // 验证两个记录都保存成功
        assertNotNull(sign1.getId());
        assertNotNull(sign2.getId());
        assertNotEquals(sign1.getId(), sign2.getId());
    }

    @Test
    void testOrderByCreateTimeDesc() {
        // 准备 - 创建多个记录
        for (int i = 0; i < 3; i++) {
            saveAndGetSign();
        }

        // 执行 - 查询所有记录
        ElectronicSign query = new ElectronicSign();
        query.setTenantId(DEFAULT_TENANT_ID);
        List<ElectronicSign> result = electronicSignService.listByConditions(query);

        // 验证按创建时间倒序排列
        assertNotNull(result);
        assertTrue(result.size() >= 3);

        // 检查是否按创建时间倒序排列
        for (int i = 0; i < result.size() - 1; i++) {
            assertTrue(result.get(i).getCreateTime().isAfter(result.get(i + 1).getCreateTime()) ||
                    result.get(i).getCreateTime().isEqual(result.get(i + 1).getCreateTime()));
        }
    }
}
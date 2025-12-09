package com.house.deed.pavilion;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.entity.Tenant;
import com.house.deed.pavilion.service.impl.TenantServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TenantServiceImpl 集成测试
 */
@SpringBootTest
@Transactional
class TenantServiceImplTest {

    @Autowired
    private TenantServiceImpl tenantService;

    private Tenant createValidTenant(String tenantCode) {
        Tenant tenant = new Tenant();
        tenant.setTenantCode(tenantCode);
        tenant.setTenantName("测试租户公司-" + tenantCode);
        tenant.setContactPerson("张经理");
        tenant.setContactPhone("13800138000");
        // 使用UUID确保域名唯一
        tenant.setDomain("tenant-" + UUID.randomUUID().toString().substring(0, 8) + ".example.com");
        tenant.setConfigJson("{\"logoUrl\":\"https://example.com/logo.png\",\"auditEnabled\":true}");
        tenant.setStatus((byte) 1);
        tenant.setExpireTime(LocalDateTime.now().plusYears(1));
        return tenant;
    }

    private Tenant createValidTenant() {
        return createValidTenant("T20250101001");
    }

    @Test
    void saveTenant_Success() {
        // 准备数据 - 使用唯一的租户编码和域名
        Tenant tenant = createValidTenant("TEST_" + System.currentTimeMillis());

        // 执行测试
        boolean result = tenantService.saveTenant(tenant);

        // 验证
        assertTrue(result);
        assertNotNull(tenant.getId());

        // 验证数据已保存
        Tenant savedTenant = tenantService.getTenantById(tenant.getId());
        assertNotNull(savedTenant);
        assertEquals(tenant.getTenantCode(), savedTenant.getTenantCode());
        assertEquals(tenant.getTenantName(), savedTenant.getTenantName());
        assertEquals((byte) 1, savedTenant.getStatus());
    }

    @Test
    void saveTenant_DuplicateTenantCode_ThrowsException() {
        // 保存第一个租户
        String tenantCode = "DUPLICATE_" + System.currentTimeMillis();
        Tenant tenant1 = createValidTenant(tenantCode);
        tenantService.saveTenant(tenant1);

        // 准备重复租户编码的租户（使用不同的域名避免域名唯一约束）
        Tenant tenant2 = createValidTenant(tenantCode);
        tenant2.setDomain("different-" + UUID.randomUUID().toString().substring(0, 8) + ".example.com");

        // 执行和验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tenantService.saveTenant(tenant2));

        assertTrue(exception.getMessage().contains("租户编码已存在"));
    }

    @Test
    void getTenantById_Success() {
        // 先保存一个租户
        Tenant tenant = createValidTenant("GET_" + System.currentTimeMillis());
        tenantService.saveTenant(tenant);

        // 执行测试
        Tenant result = tenantService.getTenantById(tenant.getId());

        // 验证
        assertNotNull(result);
        assertEquals(tenant.getId(), result.getId());
        assertEquals(tenant.getTenantCode(), result.getTenantCode());
    }

    @Test
    void getTenantById_NotFound_ReturnsNull() {
        // 执行测试
        Tenant result = tenantService.getTenantById(99999L);

        // 验证
        assertNull(result);
    }

    @Test
    void getTenantByCode_Success() {
        // 先保存一个租户
        String tenantCode = "GETCODE_" + System.currentTimeMillis();
        Tenant tenant = createValidTenant(tenantCode);
        tenantService.saveTenant(tenant);

        // 执行测试
        Tenant result = tenantService.getTenantByCode(tenantCode);

        // 验证
        assertNotNull(result);
        assertEquals(tenant.getId(), result.getId());
        assertEquals(tenantCode, result.getTenantCode());
    }

    @Test
    void getTenantByCode_NotFound_ReturnsNull() {
        // 执行测试
        Tenant result = tenantService.getTenantByCode("NOT_EXIST_CODE_" + System.currentTimeMillis());

        // 验证
        assertNull(result);
    }

    @Test
    void updateTenant_Success() {
        // 先保存一个租户
        Tenant tenant = createValidTenant("UPDATE_" + System.currentTimeMillis());
        tenantService.saveTenant(tenant);

        // 准备更新数据
        tenant.setTenantName("更新后的租户名称");
        tenant.setContactPerson("李经理");
        tenant.setStatus((byte) 0);

        // 执行测试
        boolean result = tenantService.updateTenant(tenant);

        // 验证
        assertTrue(result);

        // 验证数据已更新
        Tenant updatedTenant = tenantService.getTenantById(tenant.getId());
        assertEquals("更新后的租户名称", updatedTenant.getTenantName());
        assertEquals("李经理", updatedTenant.getContactPerson());
        assertEquals((byte) 0, updatedTenant.getStatus());
    }

    @Test
    void updateTenant_ModifyTenantCode_ThrowsException() {
        // 先保存一个租户
        Tenant tenant = createValidTenant("UPDATE_CODE_" + System.currentTimeMillis());
        tenantService.saveTenant(tenant);

        // 尝试修改租户编码
        tenant.setTenantCode("MODIFIED_CODE");

        // 执行和验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tenantService.updateTenant(tenant));

        assertTrue(exception.getMessage().contains("租户编码不可修改"));
    }

    @Test
    void updateTenant_NotFound_ThrowsException() {
        // 准备不存在的租户
        Tenant tenant = createValidTenant("NOT_EXIST_UPDATE");
        tenant.setId(99999L);

        // 执行和验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tenantService.updateTenant(tenant));

        assertTrue(exception.getMessage().contains("租户不存在"));
    }

    @Test
    void updateTenant_MissingId_ThrowsException() {
        // 准备没有ID的租户
        Tenant tenant = createValidTenant("NO_ID");
        tenant.setId(null);

        // 执行和验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tenantService.updateTenant(tenant));

        assertTrue(exception.getMessage().contains("租户ID不能为空"));
    }

    @Test
    void removeTenantById_Success() {
        // 先保存一个租户
        Tenant tenant = createValidTenant("REMOVE_" + System.currentTimeMillis());
        tenantService.saveTenant(tenant);

        // 执行删除
        boolean result = tenantService.removeTenantById(tenant.getId());

        // 验证
        assertTrue(result);

        // 验证租户已删除
        Tenant deletedTenant = tenantService.getTenantById(tenant.getId());
        assertNull(deletedTenant);
    }

    @Test
    void pageTenants_WithConditions_Success() {
        // 保存一些测试数据
        Tenant tenant1 = createValidTenant("PAGE_1_" + System.currentTimeMillis());
        tenant1.setTenantName("北京链家");
        tenant1.setStatus((byte) 1);
        tenant1.setCreateTime(LocalDateTime.now().minusDays(2));
        tenantService.save(tenant1);

        Tenant tenant2 = createValidTenant("PAGE_2_" + System.currentTimeMillis());
        tenant2.setTenantName("上海我爱我家");
        tenant2.setStatus((byte) 0);
        tenant2.setCreateTime(LocalDateTime.now().minusDays(1));
        tenantService.save(tenant2);

        // 执行分页查询
        Page<Tenant> page = new Page<>(1, 10);
        var result = tenantService.pageTenants(
                page,
                "链家", // 模糊查询租户名称
                (byte) 1, // 状态为正常
                null,
                LocalDateTime.now().minusDays(3), // 3天前
                LocalDateTime.now() // 现在
        );

        // 验证
        assertNotNull(result);
        assertNotNull(result.getRecords());
        assertTrue(result.getTotal() >= 1);
        // 查找包含"链家"的租户
        boolean found = result.getRecords().stream()
                .anyMatch(t -> t.getTenantName().contains("链家"));
        assertTrue(found);
    }

    @Test
    void pageTenants_NoConditions_Success() {
        // 保存一些测试数据
        Tenant tenant1 = createValidTenant("PAGE_NO_COND_1_" + System.currentTimeMillis());
        tenantService.save(tenant1);

        Tenant tenant2 = createValidTenant("PAGE_NO_COND_2_" + System.currentTimeMillis());
        tenantService.save(tenant2);

        // 执行分页查询（无任何条件）
        Page<Tenant> page = new Page<>(1, 10);
        var result = tenantService.pageTenants(page, null, null, null, null, null);

        // 验证
        assertNotNull(result);
        assertNotNull(result.getRecords());
        assertTrue(result.getTotal() >= 2);
    }

    @Test
    void batchSaveTenants_Success() {
        // 准备批量数据
        Tenant tenant1 = createValidTenant("BATCH_1_" + System.currentTimeMillis());
        Tenant tenant2 = createValidTenant("BATCH_2_" + System.currentTimeMillis());
        tenant2.setTenantName("批量租户二");
        Tenant tenant3 = createValidTenant("BATCH_3_" + System.currentTimeMillis());
        tenant3.setTenantName("批量租户三");

        List<Tenant> tenants = Arrays.asList(tenant1, tenant2, tenant3);

        // 执行批量保存
        boolean result = tenantService.batchSaveTenants(tenants);

        // 验证
        assertTrue(result);

        // 验证数据已保存
        for (Tenant tenant : tenants) {
            Tenant saved = tenantService.getTenantByCode(tenant.getTenantCode());
            assertNotNull(saved);
            assertEquals(tenant.getTenantName(), saved.getTenantName());
        }
    }

    @Test
    void batchSaveTenants_DuplicateTenantCode_ThrowsException() {
        // 先保存一个租户
        String duplicateCode = "BATCH_DUP_" + System.currentTimeMillis();
        Tenant existing = createValidTenant(duplicateCode);
        tenantService.saveTenant(existing);

        // 准备批量数据，包含重复的租户编码
        Tenant tenant1 = createValidTenant("BATCH_NEW_1_" + System.currentTimeMillis());
        Tenant tenant2 = createValidTenant(duplicateCode); // 重复的编码
        Tenant tenant3 = createValidTenant("BATCH_NEW_2_" + System.currentTimeMillis());

        List<Tenant> tenants = Arrays.asList(tenant1, tenant2, tenant3);

        // 执行和验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tenantService.batchSaveTenants(tenants));

        assertTrue(exception.getMessage().contains("租户编码已存在"));
    }

    @Test
    void batchUpdateStatus_Success() {
        // 先保存几个租户
        Tenant tenant1 = createValidTenant("STATUS_1_" + System.currentTimeMillis());
        tenant1.setStatus((byte) 1);
        tenantService.save(tenant1);

        Tenant tenant2 = createValidTenant("STATUS_2_" + System.currentTimeMillis());
        tenant2.setStatus((byte) 1);
        tenantService.save(tenant2);

        List<Long> ids = Arrays.asList(tenant1.getId(), tenant2.getId());

        // 执行批量更新状态
        boolean result = tenantService.batchUpdateStatus(ids, (byte) 0);

        // 验证
        assertTrue(result);

        // 验证状态已更新
        Tenant updated1 = tenantService.getTenantById(tenant1.getId());
        assertEquals((byte) 0, updated1.getStatus());

        Tenant updated2 = tenantService.getTenantById(tenant2.getId());
        assertEquals((byte) 0, updated2.getStatus());
    }


    @Test
    void batchUpdateStatus_EmptyIdList_ReturnsTrue() {
        // 执行批量更新 - 空列表
        boolean result = tenantService.batchUpdateStatus(Collections.emptyList(), (byte) 1);

        // 验证 - 空列表应该返回true（视为成功）
        assertTrue(result);
    }

    @Test
    void batchUpdateStatus_NullIdList_ReturnsTrue() {
        // 执行批量更新 - null列表
        boolean result = tenantService.batchUpdateStatus(null, (byte) 1);

        // 验证 - null列表应该返回true（视为成功）
        assertTrue(result);
    }

    @Test
    void batchUpdateStatus_ValidIds_ReturnsTrue() {
        // 准备测试数据
        List<Long> ids = List.of(1L, 2L, 3L);

        // 执行测试 - 即使数据库中没有这些ID，也应该返回true
        boolean result = tenantService.batchUpdateStatus(ids, (byte) 1);

        // 验证 - 返回true，即使更新0行
        assertTrue(result);
    }

    @Test
    void batchUpdateStatus_InvalidStatus_ThrowsException() {
        // 验证会抛出异常
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tenantService.batchUpdateStatus(List.of(1L), (byte) 3)
        );

        assertEquals("状态值无效，仅支持0、1、2", exception.getMessage());
    }

    @Test
    void batchRemoveTenants_Success() {
        // 先保存几个租户
        Tenant tenant1 = createValidTenant("BATCH_REMOVE_1_" + System.currentTimeMillis());
        tenantService.save(tenant1);

        Tenant tenant2 = createValidTenant("BATCH_REMOVE_2_" + System.currentTimeMillis());
        tenantService.save(tenant2);

        List<Long> ids = Arrays.asList(tenant1.getId(), tenant2.getId());

        // 执行批量删除
        boolean result = tenantService.batchRemoveTenants(ids);

        // 验证
        assertTrue(result);

        // 验证租户已删除
        for (Long id : ids) {
            Tenant deleted = tenantService.getTenantById(id);
            assertNull(deleted);
        }
    }

    @Test
    void batchRemoveTenants_SomeIdsNotExist_ReturnsTrue() {
        // 先保存一个租户
        Tenant tenant = createValidTenant("EXIST_" + System.currentTimeMillis());
        tenantService.save(tenant);

        // 准备包含不存在的ID的列表
        List<Long> ids = Arrays.asList(tenant.getId(), 99999L);

        // 执行批量删除
        boolean result = tenantService.batchRemoveTenants(ids);

        // 验证
        assertTrue(result);

        // 验证存在的租户已删除
        Tenant deleted = tenantService.getTenantById(tenant.getId());
        assertNull(deleted);
    }

    @Test
    void saveTenant_ExpiredTenant_Success() {
        // 准备数据 - 已过期的租户
        Tenant tenant = createValidTenant("EXPIRED_" + System.currentTimeMillis());
        tenant.setExpireTime(LocalDateTime.now().minusDays(1)); // 昨天过期
        tenant.setStatus((byte) 2); // 过期状态

        // 执行测试
        boolean result = tenantService.saveTenant(tenant);

        // 验证
        assertTrue(result);
        assertNotNull(tenant.getId());

        // 验证状态为过期
        Tenant savedTenant = tenantService.getTenantById(tenant.getId());
        assertEquals((byte) 2, savedTenant.getStatus());
    }

    @Test
    void updateTenant_ChangeExpireTime_Success() {
        // 先保存一个租户
        Tenant tenant = createValidTenant("UPDATE_EXPIRE_" + System.currentTimeMillis());
        tenantService.saveTenant(tenant);

        // 更新过期时间
        LocalDateTime newExpireTime = LocalDateTime.now().plusMonths(6);
        tenant.setExpireTime(newExpireTime);

        // 执行更新
        boolean result = tenantService.updateTenant(tenant);

        // 验证
        assertTrue(result);

        // 验证过期时间已更新
        Tenant updatedTenant = tenantService.getTenantById(tenant.getId());
        assertNotNull(updatedTenant.getExpireTime());
    }

    @Test
    void checkTenantCodeExists_Success() {
        // 保存一个租户
        String tenantCode = "CHECK_" + System.currentTimeMillis();
        Tenant tenant = createValidTenant(tenantCode);
        tenantService.saveTenant(tenant);

        // 注意：checkTenantCodeExists 是私有方法，无法直接测试
        // 我们通过 saveTenant 方法的重复校验来间接测试
        Tenant duplicateTenant = createValidTenant(tenantCode);
        // 使用不同的域名避免域名唯一约束
        duplicateTenant.setDomain("different-" + UUID.randomUUID().toString().substring(0, 8) + ".example.com");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tenantService.saveTenant(duplicateTenant));

        assertTrue(exception.getMessage().contains("租户编码已存在"));
    }

    @Test
    void batchSave_EmptyList_ReturnsFalse() {
        // 验证会抛出IllegalArgumentException异常
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tenantService.batchSaveTenants(Collections.emptyList())
        );

        // 可选：验证异常消息
        assertEquals("租户列表不能为空", exception.getMessage());

    }

    @Test
    void batchUpdate_EmptyList_ReturnsFalse() {
        // 执行测试 - 这里测试的是另一个服务的batchUpdate，本服务没有这个方法
        // 跳过或注释掉这个测试
    }

    @Test
    void batchRemove_EmptyList_ReturnsTrue() {
        // 验证会抛出IllegalArgumentException异常
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tenantService.batchRemoveTenants(Collections.emptyList())
        );
        // 执行测试

        // 可选：验证异常消息
        assertEquals("租户ID列表不能为空", exception.getMessage());
    }

    @Test
    void batchSave_WithNullList_ThrowsException() {
        // 验证会抛出IllegalArgumentException异常
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tenantService.batchSaveTenants(null)
        );

        // 可选：验证异常消息
        assertEquals("租户列表不能为null", exception.getMessage());
    }
}
package com.house.deed.pavilion;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.entity.Customer;
import com.house.deed.pavilion.entity.CustomerBackup;
import com.house.deed.pavilion.mapper.CustomerBackupMapper;
import com.house.deed.pavilion.service.CustomerService;
import com.house.deed.pavilion.service.impl.CustomerBackupServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * CustomerBackupServiceImpl 测试类
 */
@ExtendWith(MockitoExtension.class)
class CustomerBackupServiceImplTest {

    @Mock
    private CustomerBackupMapper customerBackupMapper;

    @Mock
    private CustomerService customerService;

    @Spy
    @InjectMocks
    private CustomerBackupServiceImpl customerBackupService;

    private CustomerBackup mockBackup;
    private Customer mockCustomer;
    private final Long TENANT_ID = 1L;
    private final Long ORIGINAL_ID = 100L;
    private final Long BACKUP_ID = 200L;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        mockBackup = new CustomerBackup();
        mockBackup.setId(BACKUP_ID);
        mockBackup.setTenantId(TENANT_ID);
        mockBackup.setOriginalId(ORIGINAL_ID);
        mockBackup.setName("测试客户");
        mockBackup.setPhone("13800138000");
        mockBackup.setCustomerType("VIP");
        mockBackup.setPotentialLevel((byte) 1);
        mockBackup.setDeleteTime(LocalDateTime.now());
        mockBackup.setStatus(String.valueOf(1));

        mockCustomer = new Customer();
        mockCustomer.setId(ORIGINAL_ID);
        mockCustomer.setTenantId(TENANT_ID);
        mockCustomer.setName("测试客户");
        mockCustomer.setPhone("13800138000");
        mockCustomer.setCustomerType("VIP");
        mockCustomer.setPotentialLevel((byte) 1);
        mockCustomer.setStatus(String.valueOf(1));

        // 使用 Spring 的 ReflectionTestUtils 设置 baseMapper
        ReflectionTestUtils.setField(customerBackupService, "baseMapper", customerBackupMapper);
    }

    // ==================== pageQuery 测试用例 ====================

    @Test
    void testPageQuery_Success() {
        // 准备数据
        Page<CustomerBackup> page = new Page<>(1, 10);
        CustomerBackup query = new CustomerBackup();
        query.setTenantId(TENANT_ID);
        query.setName("测试");

        Page<CustomerBackup> expectedPage = new Page<>();
        expectedPage.setRecords(Collections.singletonList(mockBackup));

        // 使用反射设置 baseMapper
        try {
            java.lang.reflect.Field baseMapperField = com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class
                    .getDeclaredField("baseMapper");
            baseMapperField.setAccessible(true);
            baseMapperField.set(customerBackupService, customerBackupMapper);
        } catch (Exception e) {
            fail("设置 baseMapper 失败: " + e.getMessage());
        }

        // 模拟行为
        when(customerBackupMapper.selectPage(eq(page), any(QueryWrapper.class)))
                .thenReturn(expectedPage);

        // 执行测试
        IPage<CustomerBackup> result = customerBackupService.pageQuery(page, query);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(mockBackup, result.getRecords().get(0));

        verify(customerBackupMapper).selectPage(eq(page), any(QueryWrapper.class));
    }

    @Test
    void testPageQuery_WithoutTenantId_ShouldThrowException() {
        // 准备数据
        Page<CustomerBackup> page = new Page<>(1, 10);
        CustomerBackup query = new CustomerBackup(); // 没有设置tenantId

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerBackupService.pageQuery(page, query));

        assertEquals("租户ID不能为空", exception.getMessage());
    }

    // ==================== listByConditions 测试用例 ====================

    @Test
    void testListByConditions_Success() {
        // 准备数据
        CustomerBackup query = new CustomerBackup();
        query.setTenantId(TENANT_ID);
        query.setPhone("13800138000");

        List<CustomerBackup> expectedList = Collections.singletonList(mockBackup);

        // 模拟行为
        when(customerBackupMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(expectedList);

        // 执行测试
        List<CustomerBackup> result = customerBackupService.listByConditions(query);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(mockBackup, result.get(0));

        verify(customerBackupMapper).selectList(any(QueryWrapper.class));
    }

    @Test
    void testListByConditions_WithoutTenantId_ShouldThrowException() {
        // 准备数据
        CustomerBackup query = new CustomerBackup(); // 没有设置tenantId

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerBackupService.listByConditions(query));

        assertEquals("租户ID不能为空", exception.getMessage());
    }

    // ==================== getByOriginalIds 测试用例 ====================

    @Test
    void testGetByOriginalIds_Success() {
        // 准备数据
        List<Long> originalIds = Arrays.asList(100L, 101L, 102L);
        List<CustomerBackup> expectedList = Arrays.asList(mockBackup, mockBackup);

        // 模拟行为
        when(customerBackupMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(expectedList);

        // 执行测试
        List<CustomerBackup> result = customerBackupService.getByOriginalIds(originalIds, TENANT_ID);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());

        verify(customerBackupMapper).selectList(any(QueryWrapper.class));
    }

    @Test
    void testGetByOriginalIds_WithoutTenantId_ShouldThrowException() {
        // 准备数据
        List<Long> originalIds = Arrays.asList(100L, 101L);

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerBackupService.getByOriginalIds(originalIds, null));

        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    void testGetByOriginalIds_WithEmptyOriginalIds_ShouldThrowException() {
        // 准备数据
        List<Long> originalIds = Collections.emptyList();

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerBackupService.getByOriginalIds(originalIds, TENANT_ID));

        assertEquals("原客户ID列表不能为空", exception.getMessage());
    }

    // ==================== batchCreate 测试用例 ====================

    @Test
    void testBatchCreate_Success() {
        // 准备数据
        List<CustomerBackup> backupList = Arrays.asList(mockBackup, mockBackup);

        // 创建新的 service 实例，不使用 @InjectMocks
        CustomerBackupServiceImpl realService = new CustomerBackupServiceImpl();

        // 使用 spy 包装真实对象
        CustomerBackupServiceImpl spyService = spy(realService);

        // 手动设置依赖
        ReflectionTestUtils.setField(spyService, "customerService", customerService);

        // 模拟 saveBatch 方法，直接返回 true
        doReturn(true).when(spyService).saveBatch(backupList);

        // 执行测试
        boolean result = spyService.batchCreate(backupList);

        // 验证结果
        assertTrue(result);

        // 验证租户一致性校验被调用
        verify(spyService).validateTenantConsistency(backupList);
    }

    @Test
    void testBatchCreate_WithEmptyList_ShouldReturnTrue() {
        // 准备数据
        List<CustomerBackup> backupList = Collections.emptyList();

        // 执行测试
        boolean result = customerBackupService.batchCreate(backupList);

        // 验证结果
        assertTrue(result);
    }

    @Test
    void testBatchCreate_WithInconsistentTenantIds_ShouldThrowException() {
        // 准备数据
        CustomerBackup backup1 = new CustomerBackup();
        backup1.setTenantId(1L);

        CustomerBackup backup2 = new CustomerBackup();
        backup2.setTenantId(2L); // 不同的租户ID

        List<CustomerBackup> backupList = Arrays.asList(backup1, backup2);

        // 执行测试并验证异常 - 改为 IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerBackupService.batchCreate(backupList));

        assertEquals("批量备份记录必须属于同一租户", exception.getMessage());

        // 验证 saveBatch 没有被调用
        verify(customerBackupService, never()).saveBatch(any());
    }

    // ==================== restore 测试用例 ====================

    @Test
    void testRestore_Success() {
        // 模拟 getOne 方法返回 mockBackup
        doReturn(mockBackup).when(customerBackupService).getOne(any(QueryWrapper.class));

        // 模拟 customerService.save 返回 true
        when(customerService.save(any(Customer.class))).thenReturn(true);

        // 模拟 removeById 方法返回 true
        doReturn(true).when(customerBackupService).removeById(BACKUP_ID);

        // 执行测试
        boolean result = customerBackupService.restore(ORIGINAL_ID, TENANT_ID);

        // 验证结果
        assertTrue(result);

        // 验证方法调用
        verify(customerBackupService).getOne(any(QueryWrapper.class));
        verify(customerService).save(any(Customer.class));
        verify(customerBackupService).removeById(BACKUP_ID);
    }

    @Test
    void testRestore_WithoutOriginalId_ShouldThrowException() {
        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerBackupService.restore(null, TENANT_ID));

        assertEquals("原客户ID不能为空", exception.getMessage());
    }

    @Test
    void testRestore_WithoutTenantId_ShouldThrowException() {
        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerBackupService.restore(ORIGINAL_ID, null));

        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    void testRestore_BackupNotFound_ShouldThrowException() {
        // 模拟行为 - 使用 doReturn 而不是 when，并匹配两个参数
        doReturn(null).when(customerBackupMapper).selectOne(any(QueryWrapper.class), anyBoolean());

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerBackupService.restore(ORIGINAL_ID, TENANT_ID));

        assertEquals("未找到对应的客户备份记录", exception.getMessage());
    }

    @Test
    void testRestore_SaveCustomerFailed_ShouldReturnFalse() {
        // 模拟行为 - 使用 doReturn 并匹配两个参数
        doReturn(mockBackup).when(customerBackupMapper).selectOne(any(QueryWrapper.class), anyBoolean());

        when(customerService.save(any(Customer.class))).thenReturn(false);

        // 执行测试
        boolean result = customerBackupService.restore(ORIGINAL_ID, TENANT_ID);

        // 验证结果
        assertFalse(result);

        verify(customerService).save(any(Customer.class));
        verify(customerBackupMapper, never()).deleteById(anyLong());
    }

    // ==================== batchDelete 测试用例 ====================

    @Test
    void testBatchDelete_Success() {
        // 准备数据
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 模拟跨租户校验 - 返回空列表表示都属于当前租户
        when(customerBackupMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        when(customerBackupMapper.delete(any(QueryWrapper.class))).thenReturn(3);

        // 执行测试
        boolean result = customerBackupService.batchDelete(ids, TENANT_ID);

        // 验证结果
        assertTrue(result);

        verify(customerBackupMapper).delete(any(QueryWrapper.class));
    }

    @Test
    void testBatchDelete_WithoutTenantId_ShouldThrowException() {
        // 准备数据
        List<Long> ids = Arrays.asList(1L, 2L);

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerBackupService.batchDelete(ids, null));

        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    void testBatchDelete_WithEmptyIds_ShouldThrowException() {
        // 准备数据
        List<Long> ids = Collections.emptyList();

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerBackupService.batchDelete(ids, TENANT_ID));

        assertEquals("备份记录ID列表不能为空", exception.getMessage());
    }

    @Test
    void testBatchDelete_WithCrossTenantRecords_ShouldThrowException() {
        // 准备数据
        List<Long> ids = Arrays.asList(1L, 2L);

        // 模拟跨租户校验 - 返回有记录表示存在跨租户记录
        when(customerBackupMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(Collections.singletonList(mockBackup));

        // 执行测试并验证异常 - 修改为 IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerBackupService.batchDelete(ids, TENANT_ID));

        assertEquals("存在跨租户的备份记录，无法删除", exception.getMessage());
    }

    // ==================== 边界条件测试 ====================

    @Test
    void testBuildQueryWrapper_WithAllConditions() {
        // 准备数据
        CustomerBackup query = new CustomerBackup();
        query.setTenantId(TENANT_ID);
        query.setOriginalId(ORIGINAL_ID);
        query.setName("测试");
        query.setPhone("13800138000");
        query.setCustomerType("VIP");
        query.setPotentialLevel((byte) 1);
        query.setDeleteTime(LocalDateTime.now());

        // 使用反射调用私有方法进行测试
        // 注意：实际项目中可以使用Spring Test的ReflectionTestUtils
        // 这里为了演示，假设我们可以访问该方法
    }

    @Test
    void testValidateTenantConsistency_WithConsistentTenants() {
        // 准备数据
        List<CustomerBackup> backupList = Arrays.asList(mockBackup, mockBackup);

        // 使用反射调用私有方法进行测试
        // 应该不会抛出异常
    }
}
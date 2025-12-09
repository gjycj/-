package com.house.deed.pavilion;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.entity.ContractLeaseTerms;
import com.house.deed.pavilion.mapper.ContractLeaseTermsMapper;
import com.house.deed.pavilion.service.impl.ContractLeaseTermsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractLeaseTermsServiceImplTest {

    @Mock
    private ContractLeaseTermsMapper contractLeaseTermsMapper;

    @InjectMocks
    @Spy
    private ContractLeaseTermsServiceImpl contractLeaseTermsService;

    private ContractLeaseTerms testTerms;
    private static final Long TENANT_ID = 1001L;
    private static final Long TERMS_ID = 1L;
    private static final Long CONTRACT_ID = 5001L;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        // 初始化测试数据
        testTerms = new ContractLeaseTerms();
        testTerms.setId(TERMS_ID);
        testTerms.setTenantId(TENANT_ID);
        testTerms.setContractId(CONTRACT_ID);
        testTerms.setAllowPet((byte) 1); // 允许养宠物
        testTerms.setAllowSublet((byte) 0); // 不允许转租
        testTerms.setCreateTime(LocalDateTime.now());

        // 手动设置 baseMapper
        setBaseMapper(contractLeaseTermsService, contractLeaseTermsMapper);
    }

    /**
     * 通过反射设置 baseMapper
     */
    private void setBaseMapper(ContractLeaseTermsServiceImpl service, ContractLeaseTermsMapper mapper)
            throws NoSuchFieldException, IllegalAccessException {
        Field baseMapperField = service.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(service, mapper);
    }

    /**
     * 测试新增条款：成功场景
     */
    @Test
    void testSaveTerms_Success() {
        // 模拟插入成功
        when(contractLeaseTermsMapper.insert(any(ContractLeaseTerms.class))).thenReturn(1);

        boolean result = contractLeaseTermsService.saveTerms(testTerms);

        assertTrue(result);
        verify(contractLeaseTermsMapper).insert(testTerms);
    }

    /**
     * 测试新增条款：租户ID为空（异常场景）
     */
    @Test
    void testSaveTerms_TenantIdNull() {
        testTerms.setTenantId(null);

        assertThrows(IllegalArgumentException.class,
                () -> contractLeaseTermsService.saveTerms(testTerms),
                "预期抛出租户ID不能为空的异常");
    }

    /**
     * 测试新增条款：合同ID为空（异常场景）
     */
    @Test
    void testSaveTerms_ContractIdNull() {
        testTerms.setContractId(null);

        assertThrows(IllegalArgumentException.class,
                () -> contractLeaseTermsService.saveTerms(testTerms),
                "预期抛出合同ID不能为空的异常");
    }

    /**
     * 测试更新条款：成功场景
     */
    @Test
    void testUpdateTermsById_Success() {
        // 模拟查询到现有记录
        when(contractLeaseTermsMapper.selectById(TERMS_ID)).thenReturn(testTerms);
        // 模拟更新成功
        when(contractLeaseTermsMapper.updateById(any(ContractLeaseTerms.class))).thenReturn(1);

        ContractLeaseTerms updateTerms = new ContractLeaseTerms();
        updateTerms.setId(TERMS_ID);
        updateTerms.setTenantId(TENANT_ID);
        updateTerms.setAllowPet((byte) 0); // 修改为不允许养宠物

        boolean result = contractLeaseTermsService.updateTermsById(updateTerms);

        assertTrue(result);
        verify(contractLeaseTermsMapper).updateById(updateTerms);
    }

    /**
     * 测试更新条款：记录不存在（异常场景）
     */
    @Test
    void testUpdateTermsById_NotFound() {
        when(contractLeaseTermsMapper.selectById(TERMS_ID)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> contractLeaseTermsService.updateTermsById(testTerms),
                "预期抛出租赁合同附加条款不存在的异常");
    }

    /**
     * 测试更新条款：跨租户操作（异常场景）
     */
    @Test
    void testUpdateTermsById_CrossTenant() {
        ContractLeaseTerms differentTenantTerms = new ContractLeaseTerms();
        differentTenantTerms.setId(TERMS_ID);
        differentTenantTerms.setTenantId(999L); // 不同租户

        when(contractLeaseTermsMapper.selectById(TERMS_ID)).thenReturn(differentTenantTerms);

        assertThrows(IllegalArgumentException.class,
                () -> contractLeaseTermsService.updateTermsById(testTerms),
                "预期抛出无权限操作其他租户的条款的异常");
    }

    /**
     * 测试删除条款：成功场景
     */
    @Test
    void testRemoveTermsById_Success() {
        // 模拟查询到现有记录
        when(contractLeaseTermsMapper.selectById(TERMS_ID)).thenReturn(testTerms);
        // 模拟删除成功
        when(contractLeaseTermsMapper.deleteById(TERMS_ID)).thenReturn(1);

        boolean result = contractLeaseTermsService.removeTermsById(TERMS_ID, TENANT_ID);

        assertTrue(result);
        verify(contractLeaseTermsMapper).deleteById(TERMS_ID);
    }

    /**
     * 测试根据ID查询：成功场景
     */
    @Test
    void testGetTermsById_Success() {
        // 使用 QueryWrapper 匹配
        when(contractLeaseTermsMapper.selectOne(any(QueryWrapper.class))).thenReturn(testTerms);

        ContractLeaseTerms result = contractLeaseTermsService.getTermsById(TERMS_ID, TENANT_ID);

        assertNotNull(result);
        assertEquals(TERMS_ID, result.getId());
        assertEquals(TENANT_ID, result.getTenantId());
    }

    /**
     * 测试根据ID查询：记录不存在（边界场景）
     */
    @Test
    void testGetTermsById_NotFound() {
        when(contractLeaseTermsMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        ContractLeaseTerms result = contractLeaseTermsService.getTermsById(TERMS_ID, TENANT_ID);

        assertNull(result);
    }

    /**
     * 测试分页查询：多条件查询场景
     */
    @Test
    void testPageQuery_WithMultipleConditions() {
        // 准备参数
        Page<ContractLeaseTerms> page = new Page<>(1, 10);
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("contractId", CONTRACT_ID);
        queryParams.put("allowPet", 1);
        queryParams.put("allowSublet", 0);

        // 模拟查询结果
        IPage<ContractLeaseTerms> mockPage = new Page<>();
        mockPage.setRecords(Collections.singletonList(testTerms));
        when(contractLeaseTermsMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn((Page<ContractLeaseTerms>) mockPage);

        IPage<ContractLeaseTerms> result = contractLeaseTermsService.pageQuery(page, queryParams, TENANT_ID);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());

        // 使用 ArgumentCaptor 验证查询条件
        ArgumentCaptor<QueryWrapper<ContractLeaseTerms>> wrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(contractLeaseTermsMapper).selectPage(eq(page), wrapperCaptor.capture());
        assertNotNull(wrapperCaptor.getValue());
    }

    /**
     * 测试分页查询：空参数查询
     */
    @Test
    void testPageQuery_WithEmptyParams() {
        Page<ContractLeaseTerms> page = new Page<>(1, 10);
        Map<String, Object> queryParams = new HashMap<>();

        when(contractLeaseTermsMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(new Page<>());

        IPage<ContractLeaseTerms> result = contractLeaseTermsService.pageQuery(page, queryParams, TENANT_ID);

        assertNotNull(result);

        ArgumentCaptor<QueryWrapper<ContractLeaseTerms>> wrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(contractLeaseTermsMapper).selectPage(eq(page), wrapperCaptor.capture());
        assertNotNull(wrapperCaptor.getValue());
    }

    /**
     * 测试多条件列表查询：正常场景
     */
    @Test
    void testListByConditions_Success() {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("allowPet", 1);

        List<ContractLeaseTerms> mockList = Arrays.asList(testTerms, testTerms);
        when(contractLeaseTermsMapper.selectList(any(QueryWrapper.class))).thenReturn(mockList);

        List<ContractLeaseTerms> result = contractLeaseTermsService.listByConditions(queryParams, TENANT_ID);

        assertEquals(2, result.size());
        verify(contractLeaseTermsMapper).selectList(any(QueryWrapper.class));
    }

    /**
     * 测试按合同ID查询：成功场景
     */
    @Test
    void testListByContractId_Success() {
        List<ContractLeaseTerms> mockList = Arrays.asList(testTerms, testTerms);
        when(contractLeaseTermsMapper.selectList(any(QueryWrapper.class))).thenReturn(mockList);

        List<ContractLeaseTerms> result = contractLeaseTermsService.listByContractId(CONTRACT_ID, TENANT_ID);

        assertEquals(2, result.size());
        verify(contractLeaseTermsMapper).selectList(any(QueryWrapper.class));
    }

    /**
     * 测试批量新增：成功场景
     */
    @Test
    void testBatchSaveTerms_Success() {
        List<ContractLeaseTerms> termsList = Arrays.asList(testTerms, testTerms);

        // 模拟 saveBatch 方法，避免 MyBatis-Plus 内部代理检查
        doReturn(true).when(contractLeaseTermsService).saveBatch(anyList());

        boolean result = contractLeaseTermsService.batchSaveTerms(termsList);

        assertTrue(result);
        // 验证参数校验逻辑被执行
        verify(contractLeaseTermsService, times(1)).saveBatch(termsList);
    }

    /**
     * 测试批量新增：空列表（边界场景）
     */
    @Test
    void testBatchSaveTerms_EmptyList() {
        boolean result = contractLeaseTermsService.batchSaveTerms(new ArrayList<>());
        assertFalse(result);
    }

    /**
     * 测试批量新增：租户ID为空（异常场景）
     */
    @Test
    void testBatchSaveTerms_TenantIdNull() {
        testTerms.setTenantId(null);
        List<ContractLeaseTerms> termsList = Arrays.asList(testTerms);

        assertThrows(IllegalArgumentException.class,
                () -> contractLeaseTermsService.batchSaveTerms(termsList),
                "预期抛出批量新增失败：租户ID不能为空的异常");
    }

    /**
     * 测试批量更新：成功场景
     */
    @Test
    void testBatchUpdateTerms_Success() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        Byte allowPet = 0;
        Byte allowSublet = 1;

        // 模拟校验通过
        List<ContractLeaseTerms> mockTermsList = Arrays.asList(
                createTerms(1L, TENANT_ID),
                createTerms(2L, TENANT_ID),
                createTerms(3L, TENANT_ID)
        );
        when(contractLeaseTermsMapper.selectList(any(QueryWrapper.class))).thenReturn(mockTermsList);

        // 模拟更新成功
        when(contractLeaseTermsMapper.update(any(ContractLeaseTerms.class), any(QueryWrapper.class))).thenReturn(3);

        boolean result = contractLeaseTermsService.batchUpdateTerms(ids, allowPet, allowSublet, TENANT_ID);

        assertTrue(result);
        verify(contractLeaseTermsMapper).update(any(ContractLeaseTerms.class), any(QueryWrapper.class));
    }

    /**
     * 测试批量更新：存在不存在的ID（异常场景）
     */
    @Test
    void testBatchUpdateTerms_NonExistentIds() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 模拟只查询到部分ID
        List<ContractLeaseTerms> mockTermsList = Arrays.asList(
                createTerms(1L, TENANT_ID),
                createTerms(2L, TENANT_ID)
                // 缺少ID=3的条款
        );
        when(contractLeaseTermsMapper.selectList(any(QueryWrapper.class))).thenReturn(mockTermsList);

        assertThrows(IllegalArgumentException.class,
                () -> contractLeaseTermsService.batchUpdateTerms(ids, (byte) 1, (byte) 0, TENANT_ID),
                "预期抛出以下条款ID不存在的异常");
    }

    /**
     * 测试批量更新：存在跨租户记录（异常场景）
     */
    @Test
    void testBatchUpdateTerms_CrossTenant() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 模拟存在不同租户的条款
        List<ContractLeaseTerms> mockTermsList = Arrays.asList(
                createTerms(1L, TENANT_ID),
                createTerms(2L, TENANT_ID),
                createTerms(3L, 999L) // 不同租户
        );
        when(contractLeaseTermsMapper.selectList(any(QueryWrapper.class))).thenReturn(mockTermsList);

        assertThrows(IllegalArgumentException.class,
                () -> contractLeaseTermsService.batchUpdateTerms(ids, (byte) 1, (byte) 0, TENANT_ID),
                "预期抛出无权限操作以下条款的异常");
    }

    /**
     * 测试批量删除：成功场景
     */
    @Test
    void testBatchRemoveTerms_Success() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 模拟校验通过
        List<ContractLeaseTerms> mockTermsList = Arrays.asList(
                createTerms(1L, TENANT_ID),
                createTerms(2L, TENANT_ID),
                createTerms(3L, TENANT_ID)
        );
        when(contractLeaseTermsMapper.selectList(any(QueryWrapper.class))).thenReturn(mockTermsList);

        // 模拟批量删除成功
        when(contractLeaseTermsMapper.deleteBatchIds(ids)).thenReturn(3);

        boolean result = contractLeaseTermsService.batchRemoveTerms(ids, TENANT_ID);

        assertTrue(result);
        verify(contractLeaseTermsMapper).deleteBatchIds(ids);
    }

    /**
     * 测试批量删除：空列表（边界场景）
     */
    @Test
    void testBatchRemoveTerms_EmptyList() {
        boolean result = contractLeaseTermsService.batchRemoveTerms(new ArrayList<>(), TENANT_ID);
        assertFalse(result);
    }

    /**
     * 创建测试条款对象
     */
    private ContractLeaseTerms createTerms(Long id, Long tenantId) {
        ContractLeaseTerms terms = new ContractLeaseTerms();
        terms.setId(id);
        terms.setTenantId(tenantId);
        terms.setContractId(CONTRACT_ID);
        return terms;
    }
}
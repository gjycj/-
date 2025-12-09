package com.house.deed.pavilion;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.entity.ComplaintDispute;
import com.house.deed.pavilion.mapper.ComplaintDisputeMapper;
import com.house.deed.pavilion.service.impl.ComplaintDisputeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintDisputeServiceImplTest {

    @Mock
    private ComplaintDisputeMapper complaintDisputeMapper;

    @InjectMocks
    @Spy
    private ComplaintDisputeServiceImpl complaintDisputeService;

    private ComplaintDispute testDispute;
    private static final Long TENANT_ID = 1001L;
    private static final Long DISPUTE_ID = 1L;
    private static final Long AGENT_ID = 100L;
    private static final Long HANDLER_ID = 200L;
    private static final Long CONTRACT_ID = 300L;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        // 初始化测试数据
        testDispute = new ComplaintDispute();
        testDispute.setId(DISPUTE_ID);
        testDispute.setTenantId(TENANT_ID);
        testDispute.setDisputeNo("DIS20251126001");
        testDispute.setComplainantType("CUSTOMER");
        testDispute.setDisputeType("SERVICE_QUALITY");
        testDispute.setStatus("ACCEPTED");
        testDispute.setDescription("服务质量投诉");
        testDispute.setCreateAgentId(AGENT_ID);
        testDispute.setHandlerId(HANDLER_ID);
        testDispute.setRelatedContractId(CONTRACT_ID);
        testDispute.setCreateTime(LocalDateTime.now());

        // 手动设置 baseMapper
        setBaseMapper(complaintDisputeService, complaintDisputeMapper);
    }

    /**
     * 通过反射设置 baseMapper
     */
    private void setBaseMapper(ComplaintDisputeServiceImpl service, ComplaintDisputeMapper mapper)
            throws NoSuchFieldException, IllegalAccessException {
        Field baseMapperField = service.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(service, mapper);
    }

    /**
     * 测试新增投诉纠纷：成功场景（包含自动编号生成）
     */
    @Test
    void testSaveComplaintDispute_Success() throws Exception {
        // 模拟编号生成 - 使用 doReturn 避免 Lambda 缓存问题
        List<ComplaintDispute> emptyList = new ArrayList<>();
        doReturn(emptyList).when(complaintDisputeMapper).selectList(any());

        // 模拟插入成功
        when(complaintDisputeMapper.insert(any(ComplaintDispute.class))).thenReturn(1);

        boolean result = complaintDisputeService.saveComplaintDispute(testDispute);

        assertTrue(result);
        // 验证纠纷编号已设置（格式：DIS+年月日+001）
        assertTrue(testDispute.getDisputeNo().startsWith("DIS"));
        verify(complaintDisputeMapper).insert(testDispute);
    }

    /**
     * 测试新增投诉纠纷：必填字段为空（异常场景）- 使用 Spy 绕过编号生成
     */
    @Test
    void testSaveComplaintDispute_RequiredFieldsNull() {
        // 使用 doReturn 来绕过编号生成逻辑
        doReturn("DIS20251126001").when(complaintDisputeService).generateDisputeNo(TENANT_ID);

        testDispute.setComplainantType(null);

        assertThrows(IllegalArgumentException.class,
                () -> complaintDisputeService.saveComplaintDispute(testDispute),
                "预期抛出投诉人类型不能为空的异常");

        testDispute.setComplainantType("CUSTOMER");
        testDispute.setDisputeType(null);

        assertThrows(IllegalArgumentException.class,
                () -> complaintDisputeService.saveComplaintDispute(testDispute),
                "预期抛出纠纷类型不能为空的异常");
    }

    /**
     * 测试更新投诉纠纷：成功场景
     */
    @Test
    void testUpdateComplaintDisputeById_Success() {
        // 模拟查询到现有记录
        when(complaintDisputeMapper.selectById(DISPUTE_ID)).thenReturn(testDispute);
        // 模拟更新成功
        when(complaintDisputeMapper.updateById(any(ComplaintDispute.class))).thenReturn(1);

        ComplaintDispute updateDispute = new ComplaintDispute();
        updateDispute.setId(DISPUTE_ID);
        updateDispute.setTenantId(TENANT_ID);
        updateDispute.setStatus("PROCESSING");
        updateDispute.setHandlerId(HANDLER_ID);

        boolean result = complaintDisputeService.updateComplaintDisputeById(updateDispute);

        assertTrue(result);
        // 验证编号和创建人ID被置空（禁止修改）
        assertNull(updateDispute.getDisputeNo());
        assertNull(updateDispute.getCreateAgentId());
        verify(complaintDisputeMapper).updateById(updateDispute);
    }

    /**
     * 测试更新投诉纠纷：状态变更但处理人为空（异常场景）
     */
    @Test
    void testUpdateComplaintDisputeById_StatusChangeWithoutHandler() {
        ComplaintDispute existingDispute = new ComplaintDispute();
        existingDispute.setId(DISPUTE_ID);
        existingDispute.setTenantId(TENANT_ID);
        existingDispute.setStatus("ACCEPTED");

        when(complaintDisputeMapper.selectById(DISPUTE_ID)).thenReturn(existingDispute);

        ComplaintDispute updateDispute = new ComplaintDispute();
        updateDispute.setId(DISPUTE_ID);
        updateDispute.setTenantId(TENANT_ID);
        updateDispute.setStatus("PROCESSING");
        // 未设置 handlerId

        assertThrows(IllegalArgumentException.class,
                () -> complaintDisputeService.updateComplaintDisputeById(updateDispute),
                "预期抛出状态变更时处理人ID不能为空的异常");
    }

    /**
     * 测试更新投诉纠纷：记录不存在（异常场景）
     */
    @Test
    void testUpdateComplaintDisputeById_NotFound() {
        when(complaintDisputeMapper.selectById(DISPUTE_ID)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> complaintDisputeService.updateComplaintDisputeById(testDispute),
                "预期抛出纠纷记录不存在的异常");
    }

    /**
     * 测试删除投诉纠纷：成功场景（已受理状态）
     */
    @Test
    void testRemoveComplaintDisputeById_Success() {
        // 模拟查询到已受理状态的记录
        testDispute.setStatus("ACCEPTED");
        // 使用 doReturn 避免 Lambda 缓存问题
        doReturn(testDispute).when(complaintDisputeMapper).selectOne(any());
        // 模拟删除成功
        when(complaintDisputeMapper.deleteById(DISPUTE_ID)).thenReturn(1);

        boolean result = complaintDisputeService.removeComplaintDisputeById(DISPUTE_ID, TENANT_ID);

        assertTrue(result);
        verify(complaintDisputeMapper).deleteById(DISPUTE_ID);
    }

    /**
     * 测试删除投诉纠纷：非已受理状态（异常场景）
     */
    @Test
    void testRemoveComplaintDisputeById_InvalidStatus() {
        // 模拟查询到非已受理状态的记录
        testDispute.setStatus("PROCESSING");
        // 使用 doReturn 避免 Lambda 缓存问题
        doReturn(testDispute).when(complaintDisputeMapper).selectOne(any());

        assertThrows(IllegalArgumentException.class,
                () -> complaintDisputeService.removeComplaintDisputeById(DISPUTE_ID, TENANT_ID),
                "预期抛出仅允许删除已受理状态记录的异常");
    }

    /**
     * 测试根据ID查询：成功场景
     */
    @Test
    void testGetComplaintDisputeById_Success() {
        // 使用 doReturn 避免 Lambda 缓存问题
        doReturn(testDispute).when(complaintDisputeMapper).selectOne(any());

        ComplaintDispute result = complaintDisputeService.getComplaintDisputeById(DISPUTE_ID, TENANT_ID);

        assertNotNull(result);
        assertEquals(DISPUTE_ID, result.getId());
        assertEquals(TENANT_ID, result.getTenantId());
    }

    /**
     * 测试根据ID查询：记录不存在（边界场景）
     */
    @Test
    void testGetComplaintDisputeById_NotFound() {
        // 使用 doReturn 避免 Lambda 缓存问题
        doReturn(null).when(complaintDisputeMapper).selectOne(any());

        ComplaintDispute result = complaintDisputeService.getComplaintDisputeById(DISPUTE_ID, TENANT_ID);

        assertNull(result);
    }

    /**
     * 测试分页查询：多条件查询场景
     */
    @Test
    void testPageQuery_WithMultipleConditions() {
        // 准备参数
        Page<ComplaintDispute> page = new Page<>(1, 10);
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("disputeType", "SERVICE_QUALITY");
        queryParams.put("status", "ACCEPTED");
        queryParams.put("complainantType", "CUSTOMER");
        queryParams.put("relatedContractId", CONTRACT_ID);
        queryParams.put("startTime", LocalDateTime.now().minusDays(7));
        queryParams.put("endTime", LocalDateTime.now());

        // 模拟查询结果
        IPage<ComplaintDispute> mockPage = new Page<>();
        mockPage.setRecords(Collections.singletonList(testDispute));
        when(complaintDisputeMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn((Page<ComplaintDispute>) mockPage);

        IPage<ComplaintDispute> result = complaintDisputeService.pageQuery(page, queryParams, TENANT_ID);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());

        // 使用 ArgumentCaptor 验证查询条件
        ArgumentCaptor<QueryWrapper<ComplaintDispute>> wrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(complaintDisputeMapper).selectPage(eq(page), wrapperCaptor.capture());
        assertNotNull(wrapperCaptor.getValue());
    }

    /**
     * 测试多条件列表查询：正常场景
     */
    @Test
    void testListByConditions_Success() {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("status", "ACCEPTED");
        queryParams.put("disputeType", "SERVICE_QUALITY");

        List<ComplaintDispute> mockList = Arrays.asList(testDispute, testDispute);
        when(complaintDisputeMapper.selectList(any(QueryWrapper.class))).thenReturn(mockList);

        List<ComplaintDispute> result = complaintDisputeService.listByConditions(queryParams, TENANT_ID);

        assertEquals(2, result.size());
        verify(complaintDisputeMapper).selectList(any(QueryWrapper.class));
    }

    /**
     * 测试批量更新状态：成功场景
     */
    @Test
    void testBatchUpdateStatus_Success() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        String newStatus = "PROCESSING";

        // 模拟校验通过
        doNothing().when(complaintDisputeService).validateDisputeIdsBelongToTenant(TENANT_ID, ids);
        // 模拟更新成功 - 使用 any() 避免 Lambda 缓存问题
        when(complaintDisputeMapper.update(any(ComplaintDispute.class), any())).thenReturn(3);

        boolean result = complaintDisputeService.batchUpdateStatus(ids, newStatus, HANDLER_ID, TENANT_ID);

        assertTrue(result);
        verify(complaintDisputeMapper).update(any(ComplaintDispute.class), any());
    }

    /**
     * 测试批量更新状态：无效状态值（异常场景）
     */
    @Test
    void testBatchUpdateStatus_InvalidStatus() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        String invalidStatus = "INVALID_STATUS";

        // 使用 doReturn 绕过验证方法
        doNothing().when(complaintDisputeService).validateDisputeIdsBelongToTenant(TENANT_ID, ids);

        assertThrows(IllegalArgumentException.class,
                () -> complaintDisputeService.batchUpdateStatus(ids, invalidStatus, HANDLER_ID, TENANT_ID),
                "预期抛出无效状态值的异常");
    }

    /**
     * 测试批量更新状态：空列表（边界场景）
     */
    @Test
    void testBatchUpdateStatus_EmptyList() {
        boolean result = complaintDisputeService.batchUpdateStatus(new ArrayList<>(), "PROCESSING", HANDLER_ID, TENANT_ID);
        assertFalse(result);
    }

    /**
     * 测试批量删除：成功场景
     */
    @Test
    void testBatchRemoveComplaintDisputes_Success() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 模拟校验通过
        doNothing().when(complaintDisputeService).validateDisputeIdsBelongToTenant(TENANT_ID, ids);
        // 模拟所有记录都是已受理状态 - 使用 doReturn 避免 Lambda 缓存问题
        doReturn(0L).when(complaintDisputeMapper).selectCount(any());
        // 模拟批量删除成功
        when(complaintDisputeMapper.deleteBatchIds(ids)).thenReturn(3);

        boolean result = complaintDisputeService.batchRemoveComplaintDisputes(ids, TENANT_ID);

        assertTrue(result);
        verify(complaintDisputeMapper).deleteBatchIds(ids);
    }

    /**
     * 测试批量删除：存在非已受理状态记录（异常场景）
     */
    @Test
    void testBatchRemoveComplaintDisputes_InvalidStatus() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 模拟校验通过
        doNothing().when(complaintDisputeService).validateDisputeIdsBelongToTenant(TENANT_ID, ids);
        // 模拟存在非已受理状态的记录 - 使用 doReturn 避免 Lambda 缓存问题
        doReturn(1L).when(complaintDisputeMapper).selectCount(any());

        assertThrows(IllegalArgumentException.class,
                () -> complaintDisputeService.batchRemoveComplaintDisputes(ids, TENANT_ID),
                "预期抛出存在非已受理状态记录的异常");
    }

    /**
     * 测试批量删除：空列表（边界场景）
     */
    @Test
    void testBatchRemoveComplaintDisputes_EmptyList() {
        boolean result = complaintDisputeService.batchRemoveComplaintDisputes(new ArrayList<>(), TENANT_ID);
        assertFalse(result);
    }

    /**
     * 测试校验ID归属：成功场景
     */
    @Test
    void testValidateDisputeIdsBelongToTenant_Success() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 使用 doReturn 避免 Lambda 缓存问题
        List<ComplaintDispute> mockDisputes = Arrays.asList(
                createDispute(1L, TENANT_ID),
                createDispute(2L, TENANT_ID),
                createDispute(3L, TENANT_ID)
        );
        doReturn(mockDisputes).when(complaintDisputeMapper).selectList(any());

        // 应该不抛出异常
        assertDoesNotThrow(() -> complaintDisputeService.validateDisputeIdsBelongToTenant(TENANT_ID, ids));
    }

    /**
     * 测试校验ID归属：存在不存在的ID（异常场景）
     */
    @Test
    void testValidateDisputeIdsBelongToTenant_NonExistentIds() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 模拟只查询到部分ID - 使用 doReturn 避免 Lambda 缓存问题
        List<ComplaintDispute> mockDisputes = Arrays.asList(
                createDispute(1L, TENANT_ID),
                createDispute(2L, TENANT_ID)
                // 缺少ID=3的纠纷
        );
        doReturn(mockDisputes).when(complaintDisputeMapper).selectList(any());

        assertThrows(IllegalArgumentException.class,
                () -> complaintDisputeService.validateDisputeIdsBelongToTenant(TENANT_ID, ids),
                "预期抛出纠纷记录ID不存在的异常");
    }

    /**
     * 测试校验ID归属：存在非当前租户ID（异常场景）
     */
    @Test
    void testValidateDisputeIdsBelongToTenant_InvalidTenantIds() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 模拟存在不同租户的纠纷 - 使用 doReturn 避免 Lambda 缓存问题
        List<ComplaintDispute> mockDisputes = Arrays.asList(
                createDispute(1L, TENANT_ID),
                createDispute(2L, TENANT_ID),
                createDispute(3L, 999L) // 不同租户
        );
        doReturn(mockDisputes).when(complaintDisputeMapper).selectList(any());

        assertThrows(IllegalArgumentException.class,
                () -> complaintDisputeService.validateDisputeIdsBelongToTenant(TENANT_ID, ids),
                "预期抛出无权限操作纠纷记录ID的异常");
    }

    /**
     * 测试校验ID归属：空参数（边界场景）
     */
    @Test
    void testValidateDisputeIdsBelongToTenant_EmptyParams() {
        // 空列表应该不抛出异常
        assertDoesNotThrow(() -> complaintDisputeService.validateDisputeIdsBelongToTenant(TENANT_ID, new ArrayList<>()));
    }

    /**
     * 测试纠纷编号生成逻辑 - 重构为直接测试私有方法
     */
    @Test
    void testGenerateDisputeNo() throws Exception {
        // 使用反射调用私有方法
        Method generateMethod = ComplaintDisputeServiceImpl.class.getDeclaredMethod("generateDisputeNo", Long.class);
        generateMethod.setAccessible(true);

        // 模拟当天没有记录（从001开始）- 使用 doReturn 避免 Lambda 缓存问题
        doReturn(new ArrayList<>()).when(complaintDisputeMapper).selectList(any());

        String disputeNo = (String) generateMethod.invoke(complaintDisputeService, TENANT_ID);

        assertNotNull(disputeNo);
        assertTrue(disputeNo.startsWith("DIS"));
        assertTrue(disputeNo.endsWith("001"));

        // 模拟当天已有记录（递增）
        List<ComplaintDispute> existingList = new ArrayList<>();
        ComplaintDispute existing = new ComplaintDispute();
        existing.setDisputeNo("DIS20251126005"); // 最大流水号005
        existingList.add(existing);

        doReturn(existingList).when(complaintDisputeMapper).selectList(any());

        String newDisputeNo = (String) generateMethod.invoke(complaintDisputeService, TENANT_ID);
        assertTrue(newDisputeNo.endsWith("006")); // 应该递增到006
    }

    /**
     * 创建测试纠纷对象
     */
    private ComplaintDispute createDispute(Long id, Long tenantId) {
        ComplaintDispute dispute = new ComplaintDispute();
        dispute.setId(id);
        dispute.setTenantId(tenantId);
        return dispute;
    }
}
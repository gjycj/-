package com.house.deed.pavilion;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.entity.LoanInfo;
import com.house.deed.pavilion.mapper.LoanInfoMapper;
import com.house.deed.pavilion.service.impl.LoanInfoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LoanInfoServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("贷款信息服务测试")
class LoanInfoServiceImplTest {

    @Mock
    private LoanInfoMapper loanInfoMapper;

    @InjectMocks
    private LoanInfoServiceImpl loanInfoService;

    private LoanInfo testLoanInfo;
    private static final Long TEST_TENANT_ID = 1001L;
    private static final Long TEST_LOAN_ID = 1L;
    private static final Long TEST_CONTRACT_ID = 10001L;
    private static final String TEST_LOAN_TYPE = "COMMERCIAL";
    private static final String TEST_LOAN_STATUS = "APPLYING";
    private static final String TEST_BANK_NAME = "中国银行";
    private static final BigDecimal TEST_LOAN_AMOUNT = new BigDecimal("1000000.00");
    private static final LocalDateTime TEST_APPLY_TIME = LocalDateTime.now();
    private static final LocalDateTime TEST_APPROVE_TIME = LocalDateTime.now().plusDays(7);

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testLoanInfo = new LoanInfo();
        testLoanInfo.setId(TEST_LOAN_ID);
        testLoanInfo.setTenantId(TEST_TENANT_ID);
        testLoanInfo.setContractId(TEST_CONTRACT_ID);
        testLoanInfo.setLoanType(TEST_LOAN_TYPE);
        testLoanInfo.setLoanStatus(TEST_LOAN_STATUS);
        testLoanInfo.setBankName(TEST_BANK_NAME);
        testLoanInfo.setLoanAmount(TEST_LOAN_AMOUNT);
        testLoanInfo.setApplyTime(TEST_APPLY_TIME);
        testLoanInfo.setApproveTime(null); // APPLYING状态审批时间为空

        // 手动设置 baseMapper
        ReflectionTestUtils.setField(loanInfoService, "baseMapper", loanInfoMapper);
    }

    @Test
    @DisplayName("新增贷款信息 - 成功")
    void saveLoanInfo_Success() {
        // 准备
        when(loanInfoMapper.selectCount(any())).thenReturn(0L);
        when(loanInfoMapper.insert(any(LoanInfo.class))).thenReturn(1);

        // 执行
        boolean result = loanInfoService.saveLoanInfo(testLoanInfo);

        // 验证
        assertTrue(result);
        verify(loanInfoMapper).selectCount(any());
        verify(loanInfoMapper).insert(testLoanInfo);
    }

    @Test
    @DisplayName("新增贷款信息 - 无效贷款类型")
    void saveLoanInfo_InvalidLoanType_ShouldThrowException() {
        // 准备
        testLoanInfo.setLoanType("INVALID_TYPE");

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> loanInfoService.saveLoanInfo(testLoanInfo)
        );
        assertTrue(exception.getMessage().contains("无效贷款类型"));
    }

    @Test
    @DisplayName("新增贷款信息 - 无效贷款状态")
    void saveLoanInfo_InvalidLoanStatus_ShouldThrowException() {
        // 准备
        testLoanInfo.setLoanStatus("INVALID_STATUS");

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> loanInfoService.saveLoanInfo(testLoanInfo)
        );
        assertTrue(exception.getMessage().contains("无效贷款状态"));
    }

    @Test
    @DisplayName("新增贷款信息 - 审批通过状态必须填写审批时间")
    void saveLoanInfo_ApprovedWithoutApproveTime_ShouldThrowException() {
        // 准备
        testLoanInfo.setLoanStatus("APPROVED");
        testLoanInfo.setApproveTime(null);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> loanInfoService.saveLoanInfo(testLoanInfo)
        );
        assertTrue(exception.getMessage().contains("必须填写审批时间"));
    }

    @Test
    @DisplayName("新增贷款信息 - 非审批通过状态填写了审批时间")
    void saveLoanInfo_NonApprovedWithApproveTime_ShouldThrowException() {
        // 准备
        testLoanInfo.setLoanStatus("APPLYING");
        testLoanInfo.setApproveTime(TEST_APPROVE_TIME);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> loanInfoService.saveLoanInfo(testLoanInfo)
        );
        assertTrue(exception.getMessage().contains("不允许填写审批时间"));
    }

    @Test
    @DisplayName("新增贷款信息 - 合同+贷款类型重复")
    void saveLoanInfo_DuplicateContractAndLoanType_ShouldThrowException() {
        // 准备
        when(loanInfoMapper.selectCount(any())).thenReturn(1L);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> loanInfoService.saveLoanInfo(testLoanInfo)
        );
        assertTrue(exception.getMessage().contains("已存在"));
    }

    @Test
    @DisplayName("更新贷款信息 - 成功")
    void updateLoanInfoById_Success() {
        // 准备
        LoanInfo updateRequest = new LoanInfo();
        updateRequest.setId(TEST_LOAN_ID);
        updateRequest.setTenantId(TEST_TENANT_ID);
        updateRequest.setLoanStatus("APPROVED");
        updateRequest.setApproveTime(TEST_APPROVE_TIME);

        when(loanInfoMapper.selectById(TEST_LOAN_ID)).thenReturn(testLoanInfo);
        when(loanInfoMapper.updateById(any(LoanInfo.class))).thenReturn(1);

        // 执行
        boolean result = loanInfoService.updateLoanInfoById(updateRequest);

        // 验证
        assertTrue(result);
        verify(loanInfoMapper).selectById(TEST_LOAN_ID);
        verify(loanInfoMapper).updateById(any(LoanInfo.class));
    }

    @Test
    @DisplayName("更新贷款信息 - 记录不存在")
    void updateLoanInfoById_NotFound_ShouldThrowException() {
        // 准备
        LoanInfo updateRequest = new LoanInfo();
        updateRequest.setId(999L);
        updateRequest.setTenantId(TEST_TENANT_ID);

        when(loanInfoMapper.selectById(999L)).thenReturn(null);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> loanInfoService.updateLoanInfoById(updateRequest)
        );
        assertEquals("贷款记录不存在或无权限操作", exception.getMessage());
    }

    @Test
    @DisplayName("更新贷款信息 - 租户不一致")
    void updateLoanInfoById_TenantMismatch_ShouldThrowException() {
        // 准备
        LoanInfo updateRequest = new LoanInfo();
        updateRequest.setId(TEST_LOAN_ID);
        updateRequest.setTenantId(9999L); // 不同租户

        testLoanInfo.setTenantId(TEST_TENANT_ID); // 原有租户
        when(loanInfoMapper.selectById(TEST_LOAN_ID)).thenReturn(testLoanInfo);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> loanInfoService.updateLoanInfoById(updateRequest)
        );
        assertEquals("贷款记录不存在或无权限操作", exception.getMessage());
    }

    @Test
    @DisplayName("更新贷款信息 - 修改贷款类型")
    void updateLoanInfoById_ChangeLoanType_ShouldThrowException() {
        // 准备
        LoanInfo updateRequest = new LoanInfo();
        updateRequest.setId(TEST_LOAN_ID);
        updateRequest.setTenantId(TEST_TENANT_ID);
        updateRequest.setLoanType("FUND"); // 尝试修改贷款类型

        when(loanInfoMapper.selectById(TEST_LOAN_ID)).thenReturn(testLoanInfo);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> loanInfoService.updateLoanInfoById(updateRequest)
        );
        assertEquals("贷款类型不可修改", exception.getMessage());
    }

    @Test
    @DisplayName("删除贷款信息 - 成功")
    void removeLoanInfoById_Success() {
        // 准备
        when(loanInfoMapper.selectById(TEST_LOAN_ID)).thenReturn(testLoanInfo);
        when(loanInfoMapper.deleteById(TEST_LOAN_ID)).thenReturn(1);

        // 执行
        boolean result = loanInfoService.removeLoanInfoById(TEST_LOAN_ID, TEST_TENANT_ID);

        // 验证
        assertTrue(result);
        verify(loanInfoMapper).selectById(TEST_LOAN_ID);
        verify(loanInfoMapper).deleteById(TEST_LOAN_ID);
    }

    @Test
    @DisplayName("删除贷款信息 - 记录不存在")
    void removeLoanInfoById_NotFound_ShouldThrowException() {
        // 准备
        when(loanInfoMapper.selectById(TEST_LOAN_ID)).thenReturn(null);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> loanInfoService.removeLoanInfoById(TEST_LOAN_ID, TEST_TENANT_ID)
        );
        assertEquals("贷款记录不存在或无权限操作", exception.getMessage());
    }

    @Test
    @DisplayName("根据ID查询贷款信息 - 成功")
    void getLoanInfoById_Success() {
        // 准备
        when(loanInfoMapper.selectOne(any())).thenReturn(testLoanInfo);

        // 执行
        LoanInfo result = loanInfoService.getLoanInfoById(TEST_LOAN_ID, TEST_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertEquals(TEST_LOAN_ID, result.getId());
        assertEquals(TEST_TENANT_ID, result.getTenantId());
    }

    @Test
    @DisplayName("根据ID查询贷款信息 - 记录不存在")
    void getLoanInfoById_NotFound() {
        // 准备
        when(loanInfoMapper.selectOne(any())).thenReturn(null);

        // 执行
        LoanInfo result = loanInfoService.getLoanInfoById(TEST_LOAN_ID, TEST_TENANT_ID);

        // 验证
        assertNull(result);
    }

    @Test
    @DisplayName("多条件分页查询 - 成功")
    void pageQuery_Success() {
        // 准备
        Page<LoanInfo> page = new Page<>(1, 10);
        LoanInfo query = new LoanInfo();
        query.setBankName("银行");
        query.setLoanType(TEST_LOAN_TYPE);
        query.setContractId(TEST_CONTRACT_ID);

        Page<LoanInfo> expectedPage = new Page<>(1, 10);
        expectedPage.setRecords(Collections.singletonList(testLoanInfo));
        expectedPage.setTotal(1);

        when(loanInfoMapper.selectPage(eq(page), any())).thenReturn(expectedPage);

        // 执行
        IPage<LoanInfo> result = loanInfoService.pageQuery(page, query, TEST_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(TEST_BANK_NAME, result.getRecords().get(0).getBankName());
        verify(loanInfoMapper).selectPage(eq(page), any());
    }

    @Test
    @DisplayName("批量新增贷款信息 - 成功")
    void batchSaveLoanInfos_Success() {
        // 准备
        LoanInfo loanInfo1 = createTestLoanInfo();
        loanInfo1.setContractId(10001L);
        loanInfo1.setLoanType("COMMERCIAL");

        LoanInfo loanInfo2 = createTestLoanInfo();
        loanInfo2.setContractId(10002L);
        loanInfo2.setLoanType("FUND");

        List<LoanInfo> loanInfos = Arrays.asList(loanInfo1, loanInfo2);

        // 创建 spy 对象
        LoanInfoServiceImpl spyService = Mockito.spy(loanInfoService);

        // 模拟 saveBatch 方法返回 true
        doReturn(true).when(spyService).saveBatch(loanInfos);

        // 执行
        boolean result = spyService.batchSaveLoanInfos(loanInfos);

        // 验证
        assertTrue(result);
        verify(spyService).saveBatch(loanInfos);
    }

    @Test
    @DisplayName("批量新增贷款信息 - 列表为空")
    void batchSaveLoanInfos_EmptyList() {
        // 执行
        boolean result = loanInfoService.batchSaveLoanInfos(Collections.emptyList());

        // 验证
        assertFalse(result);
    }

    @Test
    @DisplayName("批量新增贷款信息 - 存在无效贷款类型")
    void batchSaveLoanInfos_InvalidLoanType_ShouldThrowException() {
        // 准备
        LoanInfo loanInfo = createTestLoanInfo();
        loanInfo.setLoanType("INVALID_TYPE");

        List<LoanInfo> loanInfos = Collections.singletonList(loanInfo);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> loanInfoService.batchSaveLoanInfos(loanInfos)
        );
        assertTrue(exception.getMessage().contains("无效贷款类型"));
    }

    @Test
    @DisplayName("批量新增贷款信息 - 存在重复合同+贷款类型组合")
    void batchSaveLoanInfos_DuplicateCombination_ShouldThrowException() {
        // 准备
        LoanInfo loanInfo1 = createTestLoanInfo();
        loanInfo1.setContractId(10001L);
        loanInfo1.setLoanType("COMMERCIAL");

        LoanInfo loanInfo2 = createTestLoanInfo();
        loanInfo2.setContractId(10001L); // 相同合同ID
        loanInfo2.setLoanType("COMMERCIAL"); // 相同贷款类型

        List<LoanInfo> loanInfos = Arrays.asList(loanInfo1, loanInfo2);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> loanInfoService.batchSaveLoanInfos(loanInfos)
        );
        assertTrue(exception.getMessage().contains("重复的合同+贷款类型组合"));
    }

    @Test
    @DisplayName("批量删除贷款信息 - 成功")
    void batchRemoveLoanInfos_Success() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 模拟 selectList 返回数据
        List<LoanInfo> mockLoans = Arrays.asList(
                createLoanInfoWithId(1L, TEST_TENANT_ID),
                createLoanInfoWithId(2L, TEST_TENANT_ID),
                createLoanInfoWithId(3L, TEST_TENANT_ID)
        );

        when(loanInfoMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(mockLoans);

        when(loanInfoMapper.deleteBatchIds(ids)).thenReturn(3);

        // 执行
        boolean result = loanInfoService.batchRemoveLoanInfos(ids, TEST_TENANT_ID);

        // 验证
        assertTrue(result);
        verify(loanInfoMapper).selectList(any(QueryWrapper.class));
        verify(loanInfoMapper).deleteBatchIds(ids);
    }

    @Test
    @DisplayName("批量删除贷款信息 - 列表为空")
    void batchRemoveLoanInfos_EmptyList() {
        // 执行
        boolean result = loanInfoService.batchRemoveLoanInfos(Collections.emptyList(), TEST_TENANT_ID);

        // 验证
        assertFalse(result);
        verify(loanInfoMapper, never()).selectList(any());
        verify(loanInfoMapper, never()).deleteBatchIds(anyList());
    }

    @Test
    @DisplayName("批量删除贷款信息 - 部分ID不存在")
    void batchRemoveLoanInfos_SomeIdsNotExist_ShouldThrowException() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 只返回一个贷款记录
        when(loanInfoMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(Collections.singletonList(testLoanInfo));

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> loanInfoService.batchRemoveLoanInfos(ids, TEST_TENANT_ID)
        );
        assertTrue(exception.getMessage().contains("贷款ID不存在"));
    }

    @Test
    @DisplayName("批量删除贷款信息 - 租户权限不符")
    void batchRemoveLoanInfos_TenantMismatch_ShouldThrowException() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L);

        // 创建属于其他租户的贷款记录
        List<LoanInfo> mockLoans = Arrays.asList(
                createLoanInfoWithId(1L, 9999L), // 不同租户
                createLoanInfoWithId(2L, 9999L)  // 不同租户
        );

        when(loanInfoMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(mockLoans);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> loanInfoService.batchRemoveLoanInfos(ids, TEST_TENANT_ID)
        );
        assertTrue(exception.getMessage().contains("无权限操作贷款ID"));
    }

    @Test
    @DisplayName("验证贷款ID属于租户 - 成功")
    void validateLoanIdsBelongToTenant_Success() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        List<LoanInfo> mockLoans = Arrays.asList(
                createLoanInfoWithId(1L, TEST_TENANT_ID),
                createLoanInfoWithId(2L, TEST_TENANT_ID),
                createLoanInfoWithId(3L, TEST_TENANT_ID)
        );

        when(loanInfoMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(mockLoans);

        // 执行 - 不应该抛出异常
        assertDoesNotThrow(() -> loanInfoService.validateLoanIdsBelongToTenant(TEST_TENANT_ID, ids));
    }

    @Test
    @DisplayName("验证贷款ID属于租户 - ID列表为空")
    void validateLoanIdsBelongToTenant_EmptyList() {
        // 执行 - 不应该抛出异常
        assertDoesNotThrow(() -> loanInfoService.validateLoanIdsBelongToTenant(TEST_TENANT_ID, Collections.emptyList()));

        // 验证未调用查询方法
        verify(loanInfoMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("验证贷款ID属于租户 - ID不存在")
    void validateLoanIdsBelongToTenant_IdsNotExist_ShouldThrowException() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 只返回一个记录
        when(loanInfoMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(Collections.singletonList(testLoanInfo));

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> loanInfoService.validateLoanIdsBelongToTenant(TEST_TENANT_ID, ids)
        );
        assertTrue(exception.getMessage().contains("贷款ID不存在"));
    }

    @Test
    @DisplayName("验证贷款ID属于租户 - 租户不一致")
    void validateLoanIdsBelongToTenant_TenantMismatch_ShouldThrowException() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L);

        // 创建属于其他租户的记录
        List<LoanInfo> mockLoans = Arrays.asList(
                createLoanInfoWithId(1L, 9999L),
                createLoanInfoWithId(2L, 9999L)
        );

        when(loanInfoMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(mockLoans);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> loanInfoService.validateLoanIdsBelongToTenant(TEST_TENANT_ID, ids)
        );
        assertTrue(exception.getMessage().contains("无权限操作贷款ID"));
    }

    @Test
    @DisplayName("验证状态与审批时间 - 审批通过状态必须填写审批时间")
    void validateStatusAndApproveTime_ApprovedWithoutTime_ShouldThrowException() {
        // 准备
        LoanInfoServiceImpl service = new LoanInfoServiceImpl();

        // 使用反射调用私有方法
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "validateStatusAndApproveTime", "APPROVED", null)
        );
        assertTrue(exception.getMessage().contains("必须填写审批时间"));
    }

    @Test
    @DisplayName("验证状态与审批时间 - 非审批通过状态不允许填写审批时间")
    void validateStatusAndApproveTime_NonApprovedWithTime_ShouldThrowException() {
        // 准备
        LoanInfoServiceImpl service = new LoanInfoServiceImpl();

        // 使用反射调用私有方法
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "validateStatusAndApproveTime", "APPLYING", TEST_APPROVE_TIME)
        );
        assertTrue(exception.getMessage().contains("不允许填写审批时间"));
    }

    @Test
    @DisplayName("验证状态与审批时间 - 审批通过状态有审批时间")
    void validateStatusAndApproveTime_ApprovedWithTime_Success() {
        // 准备
        LoanInfoServiceImpl service = new LoanInfoServiceImpl();

        // 使用反射调用私有方法，不应该抛出异常
        assertDoesNotThrow(() ->
                ReflectionTestUtils.invokeMethod(service, "validateStatusAndApproveTime", "APPROVED", TEST_APPROVE_TIME)
        );
    }

    @Test
    @DisplayName("验证状态与审批时间 - 非审批通过状态无审批时间")
    void validateStatusAndApproveTime_NonApprovedWithoutTime_Success() {
        // 准备
        LoanInfoServiceImpl service = new LoanInfoServiceImpl();

        // 使用反射调用私有方法，不应该抛出异常
        assertDoesNotThrow(() ->
                ReflectionTestUtils.invokeMethod(service, "validateStatusAndApproveTime", "APPLYING", null)
        );
    }

    // 辅助方法：创建测试用贷款信息
    private LoanInfo createTestLoanInfo() {
        LoanInfo loanInfo = new LoanInfo();
        loanInfo.setTenantId(TEST_TENANT_ID);
        loanInfo.setContractId(TEST_CONTRACT_ID);
        loanInfo.setLoanType(TEST_LOAN_TYPE);
        loanInfo.setLoanStatus(TEST_LOAN_STATUS);
        loanInfo.setBankName(TEST_BANK_NAME);
        loanInfo.setLoanAmount(TEST_LOAN_AMOUNT);
        loanInfo.setApplyTime(TEST_APPLY_TIME);
        return loanInfo;
    }

    // 辅助方法：创建指定ID的贷款信息
    private LoanInfo createLoanInfoWithId(Long id, Long tenantId) {
        LoanInfo loanInfo = new LoanInfo();
        loanInfo.setId(id);
        loanInfo.setTenantId(tenantId);
        loanInfo.setContractId(TEST_CONTRACT_ID);
        loanInfo.setLoanType(TEST_LOAN_TYPE);
        loanInfo.setLoanStatus(TEST_LOAN_STATUS);
        loanInfo.setBankName(TEST_BANK_NAME);
        return loanInfo;
    }
}
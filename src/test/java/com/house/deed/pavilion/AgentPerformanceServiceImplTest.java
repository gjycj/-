package com.house.deed.pavilion;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.entity.AgentPerformance;
import com.house.deed.pavilion.mapper.AgentPerformanceMapper;
import com.house.deed.pavilion.service.impl.AgentPerformanceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentPerformanceServiceImplTest {

    @Mock
    private AgentPerformanceMapper performanceMapper;

    @InjectMocks
    private AgentPerformanceServiceImpl performanceService;

    // 测试数据：租户ID
    private static final Long TENANT_ID = 1001L;
    // 测试数据：经纪人ID
    private static final Long AGENT_ID = 3001L;
    // 测试数据：合同ID
    private static final Long CONTRACT_ID = 5001L;
    // 日期格式化器（用于转换LocalDate到performanceMonth格式）
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        // 手动设置 baseMapper
        setBaseMapper(performanceService, performanceMapper);
    }

    /**
     * 通过反射设置 baseMapper
     */
    private void setBaseMapper(AgentPerformanceServiceImpl service, AgentPerformanceMapper mapper)
            throws NoSuchFieldException, IllegalAccessException {
        Field baseMapperField = service.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(service, mapper);
    }

    /**
     * 测试 pageQuery 方法：多条件分页查询（包含所有参数）
     */
    @Test
    void testPageQuery_WithAllParams() {
        // 1. 准备参数
        Page<AgentPerformance> page = new Page<>(1, 10);
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("agentId", AGENT_ID);
        queryParams.put("startDate", LocalDate.of(2025, 11, 1));
        queryParams.put("endDate", LocalDate.of(2025, 11, 30));
        queryParams.put("contractId", CONTRACT_ID);

        // 2. 模拟Mapper返回
        IPage<AgentPerformance> mockPage = new Page<>();
        List<AgentPerformance> records = Collections.singletonList(buildPerformance());
        mockPage.setRecords(records);
        when(performanceMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn((Page) mockPage);

        // 3. 执行方法
        IPage<AgentPerformance> result = performanceService.pageQuery(page, queryParams, TENANT_ID);

        // 4. 断言结果
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());

        // 5. 使用 ArgumentCaptor 捕获 QueryWrapper 进行验证
        ArgumentCaptor<QueryWrapper<AgentPerformance>> wrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(performanceMapper).selectPage(eq(page), wrapperCaptor.capture());

        QueryWrapper<AgentPerformance> capturedWrapper = wrapperCaptor.getValue();
        assertNotNull(capturedWrapper);
    }

    /**
     * 测试 pageQuery 方法：空参数查询（仅租户ID）
     */
    @Test
    void testPageQuery_WithEmptyParams() {
        // 1. 准备参数
        Page<AgentPerformance> page = new Page<>(1, 10);
        Map<String, Object> queryParams = new HashMap<>(); // 空参数

        // 2. 模拟Mapper返回
        when(performanceMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(new Page<>());

        // 3. 执行方法
        IPage<AgentPerformance> result = performanceService.pageQuery(page, queryParams, TENANT_ID);

        // 4. 断言结果
        assertNotNull(result);

        // 5. 使用 ArgumentCaptor 捕获 QueryWrapper 进行验证
        ArgumentCaptor<QueryWrapper<AgentPerformance>> wrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(performanceMapper).selectPage(eq(page), wrapperCaptor.capture());

        QueryWrapper<AgentPerformance> capturedWrapper = wrapperCaptor.getValue();
        assertNotNull(capturedWrapper);
    }

    /**
     * 测试 listByAgentId 方法：时间范围内查询
     */
    @Test
    void testListByAgentId_WithTimeRange() {
        // 1. 准备参数（时间范围转换为月份格式）
        LocalDate startTime = LocalDate.of(2025, 11, 1);
        LocalDate endTime = LocalDate.of(2025, 12, 31);

        // 2. 模拟Mapper返回
        List<AgentPerformance> mockList = Arrays.asList(buildPerformance(), buildPerformance());
        when(performanceMapper.selectList(any(QueryWrapper.class))).thenReturn(mockList);

        // 3. 执行方法
        List<AgentPerformance> result = performanceService.listByAgentId(AGENT_ID, startTime, endTime, TENANT_ID);

        // 4. 断言结果
        assertEquals(2, result.size());

        // 5. 使用 ArgumentCaptor 捕获 QueryWrapper 进行验证
        ArgumentCaptor<QueryWrapper<AgentPerformance>> wrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(performanceMapper).selectList(wrapperCaptor.capture());

        QueryWrapper<AgentPerformance> capturedWrapper = wrapperCaptor.getValue();
        assertNotNull(capturedWrapper);
    }

    /**
     * 测试 sumByCycle 方法：按月统计业绩总和
     */
    @Test
    void testSumByCycle_Month() {
        // 1. 准备参数
        String cycleType = "MONTH";
        LocalDate statisticDate = LocalDate.of(2025, 11, 1);

        // 2. 模拟Mapper返回
        List<Map<String, Object>> mockSumList = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("agent_id", AGENT_ID);
        item.put("total_amount", new BigDecimal("50000.00"));
        mockSumList.add(item);
        when(performanceMapper.sumPerformanceByCycle(eq(cycleType), eq(statisticDate), eq(TENANT_ID))).thenReturn(mockSumList);

        // 3. 执行方法
        Map<Long, BigDecimal> result = performanceService.sumByCycle(cycleType, statisticDate, TENANT_ID);

        // 4. 断言结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(new BigDecimal("50000.00"), result.get(AGENT_ID));

        // 5. 验证方法调用
        verify(performanceMapper).sumPerformanceByCycle(cycleType, statisticDate, TENANT_ID);
    }

    /**
     * 测试 listByContractId 方法：按合同ID查询
     */
    @Test
    void testListByContractId() {
        // 1. 准备参数
        Long contractId = CONTRACT_ID;
        Long tenantId = TENANT_ID;

        // 2. 模拟Mapper返回
        List<AgentPerformance> mockList = Collections.singletonList(buildPerformance());
        when(performanceMapper.selectList(any(QueryWrapper.class))).thenReturn(mockList);

        // 3. 执行方法
        List<AgentPerformance> result = performanceService.listByContractId(contractId, tenantId);

        // 4. 断言结果
        assertEquals(1, result.size());
        assertEquals(contractId, result.get(0).getContractId());

        // 5. 使用 ArgumentCaptor 捕获 QueryWrapper 进行验证
        ArgumentCaptor<QueryWrapper<AgentPerformance>> wrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(performanceMapper).selectList(wrapperCaptor.capture());

        QueryWrapper<AgentPerformance> capturedWrapper = wrapperCaptor.getValue();
        assertNotNull(capturedWrapper);
    }

    /**
     * 测试批量删除：成功场景
     */
    @Test
    void testBatchRemove_Success() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 模拟校验通过（没有跨租户记录）
        when(performanceMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        // 模拟删除成功
        when(performanceMapper.deleteBatchIds(ids)).thenReturn(3);

        boolean result = performanceService.batchRemove(ids, TENANT_ID);

        assertTrue(result);
        verify(performanceMapper).deleteBatchIds(ids);
    }

    /**
     * 测试批量删除：存在跨租户记录（异常场景）
     */
    @Test
    void testBatchRemove_CrossTenant() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 模拟存在跨租户记录
        when(performanceMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        assertThrows(IllegalArgumentException.class,
                () -> performanceService.batchRemove(ids, TENANT_ID),
                "预期抛出存在不属于当前租户的业绩记录的异常");
    }

    /**
     * 测试批量删除：空列表（边界场景）
     */
    @Test
    void testBatchRemove_EmptyList() {
        boolean result = performanceService.batchRemove(new ArrayList<>(), TENANT_ID);
        assertFalse(result);
    }

    /**
     * 测试批量更新状态：成功场景
     */
    @Test
    void testBatchUpdateStatus_Success() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        String newStatus = "SETTLED";
        LocalDateTime settleTime = LocalDateTime.now();

        // 模拟校验通过（没有跨租户记录）
        when(performanceMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        // 模拟更新成功
        when(performanceMapper.update(any(AgentPerformance.class), any(QueryWrapper.class))).thenReturn(3);

        boolean result = performanceService.batchUpdateStatus(ids, newStatus, settleTime, TENANT_ID);

        assertTrue(result);
        verify(performanceMapper).update(any(AgentPerformance.class), any(QueryWrapper.class));
    }

    /**
     * 测试批量更新状态：无效状态（异常场景）
     */
    @Test
    void testBatchUpdateStatus_InvalidStatus() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        String invalidStatus = "INVALID_STATUS";

        assertThrows(IllegalArgumentException.class,
                () -> performanceService.batchUpdateStatus(ids, invalidStatus, null, TENANT_ID),
                "预期抛出业绩状态错误的异常");
    }

    /**
     * 测试批量更新状态：已结算状态但未提供结算时间（异常场景）
     */
    @Test
    void testBatchUpdateStatus_SettledWithoutTime() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        String status = "SETTLED";

        assertThrows(IllegalArgumentException.class,
                () -> performanceService.batchUpdateStatus(ids, status, null, TENANT_ID),
                "预期抛出结算时间不能为空的异常");
    }

    /**
     * 构建测试用的业绩实体
     */
    private AgentPerformance buildPerformance() {
        AgentPerformance performance = new AgentPerformance();
        performance.setId(1L);
        performance.setTenantId(TENANT_ID);
        performance.setAgentId(AGENT_ID);
        performance.setContractId(CONTRACT_ID);
        performance.setPerformanceMonth("202511"); // 实体类必填字段（yyyyMM格式）
        performance.setDealAmount(new BigDecimal("120.50")); // 实体类必填（万元）
        performance.setCommissionAmount(new BigDecimal("36000.00")); // 实体类必填（元）
        performance.setPerformanceStatus("UNSETTLED"); // 实体类必填（枚举值）
        performance.setCreateTime(LocalDateTime.now()); // 自动填充字段
        return performance;
    }
}
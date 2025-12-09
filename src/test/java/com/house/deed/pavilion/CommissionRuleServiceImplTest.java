package com.house.deed.pavilion;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.entity.CommissionRule;
import com.house.deed.pavilion.mapper.CommissionRuleMapper;
import com.house.deed.pavilion.service.impl.CommissionRuleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommissionRuleServiceImplTest {

    @Mock
    private CommissionRuleMapper commissionRuleMapper;

    @InjectMocks
    @Spy
    private CommissionRuleServiceImpl commissionRuleService;

    private CommissionRule testRule;
    private static final Long TENANT_ID = 1001L;
    private static final Long RULE_ID = 1L;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        // 初始化测试数据
        testRule = new CommissionRule();
        testRule.setId(RULE_ID);
        testRule.setTenantId(TENANT_ID);
        testRule.setRuleName("标准佣金规则");
        testRule.setApplicableType("RESIDENTIAL");
        testRule.setCommissionRate(new BigDecimal("2.5"));
        testRule.setStatus((byte) 1);
        testRule.setCreateTime(LocalDateTime.now());
        testRule.setUpdateTime(LocalDateTime.now());

        // 手动设置 baseMapper
        setBaseMapper(commissionRuleService, commissionRuleMapper);
    }

    /**
     * 通过反射设置 baseMapper
     */
    private void setBaseMapper(CommissionRuleServiceImpl service, CommissionRuleMapper mapper)
            throws NoSuchFieldException, IllegalAccessException {
        Field baseMapperField = service.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(service, mapper);
    }

    /**
     * 测试新增佣金规则：成功场景
     */
    @Test
    void testSaveCommissionRule_Success() {
        // 模拟规则名称唯一性校验通过
        when(commissionRuleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        // 模拟插入成功
        when(commissionRuleMapper.insert(any(CommissionRule.class))).thenReturn(1);

        boolean result = commissionRuleService.saveCommissionRule(testRule);

        assertTrue(result);
        verify(commissionRuleMapper).insert(testRule);
    }

    /**
     * 测试新增佣金规则：规则名称重复（异常场景）
     */
    @Test
    void testSaveCommissionRule_DuplicateRuleName() {
        // 模拟规则名称已存在
        when(commissionRuleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(IllegalArgumentException.class,
                () -> commissionRuleService.saveCommissionRule(testRule),
                "预期抛出规则名称重复的异常");
    }

    /**
     * 测试更新佣金规则：成功场景
     */
    @Test
    void testUpdateCommissionRuleById_Success() {
        // 模拟查询到现有规则
        when(commissionRuleMapper.selectById(RULE_ID)).thenReturn(testRule);
        // 模拟规则名称唯一性校验通过
        when(commissionRuleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        // 模拟更新成功
        when(commissionRuleMapper.updateById(any(CommissionRule.class))).thenReturn(1);

        boolean result = commissionRuleService.updateCommissionRuleById(testRule);

        assertTrue(result);
        verify(commissionRuleMapper).updateById(testRule);
    }

    /**
     * 测试更新佣金规则：规则不存在（异常场景）
     */
    @Test
    void testUpdateCommissionRuleById_RuleNotFound() {
        when(commissionRuleMapper.selectById(RULE_ID)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> commissionRuleService.updateCommissionRuleById(testRule),
                "预期抛出规则不存在的异常");
    }

    /**
     * 测试更新佣金规则：租户不匹配（异常场景）
     */
    @Test
    void testUpdateCommissionRuleById_TenantMismatch() {
        CommissionRule existingRule = new CommissionRule();
        existingRule.setId(RULE_ID);
        existingRule.setTenantId(999L); // 不同租户

        when(commissionRuleMapper.selectById(RULE_ID)).thenReturn(existingRule);

        assertThrows(IllegalArgumentException.class,
                () -> commissionRuleService.updateCommissionRuleById(testRule),
                "预期抛出无权限操作的异常");
    }

    /**
     * 测试更新佣金规则：新规则名称重复（异常场景）
     */
    @Test
    void testUpdateCommissionRuleById_DuplicateNewRuleName() {
        // 模拟查询到现有规则
        when(commissionRuleMapper.selectById(RULE_ID)).thenReturn(testRule);
        // 模拟新规则名称已存在
        when(commissionRuleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        testRule.setRuleName("新规则名称");

        assertThrows(IllegalArgumentException.class,
                () -> commissionRuleService.updateCommissionRuleById(testRule),
                "预期抛出新规则名称重复的异常");
    }

    /**
     * 测试删除佣金规则：成功场景
     */
    @Test
    void testRemoveCommissionRuleById_Success() {
        // 模拟查询到现有规则
        when(commissionRuleMapper.selectById(RULE_ID)).thenReturn(testRule);
        // 模拟删除成功
        when(commissionRuleMapper.deleteById(RULE_ID)).thenReturn(1);

        boolean result = commissionRuleService.removeCommissionRuleById(RULE_ID, TENANT_ID);

        assertTrue(result);
        verify(commissionRuleMapper).deleteById(RULE_ID);
    }

    /**
     * 测试删除佣金规则：规则不存在（异常场景）
     */
    @Test
    void testRemoveCommissionRuleById_RuleNotFound() {
        when(commissionRuleMapper.selectById(RULE_ID)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> commissionRuleService.removeCommissionRuleById(RULE_ID, TENANT_ID),
                "预期抛出规则不存在的异常");
    }

    /**
     * 测试根据ID查询规则：成功场景
     */
    @Test
    void testGetCommissionRuleById_Success() {
        // 模拟查询结果
        when(commissionRuleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testRule);

        CommissionRule result = commissionRuleService.getCommissionRuleById(RULE_ID, TENANT_ID);

        assertNotNull(result);
        assertEquals(RULE_ID, result.getId());
        assertEquals(TENANT_ID, result.getTenantId());
    }

    /**
     * 测试根据ID查询规则：规则不存在（边界场景）
     */
    @Test
    void testGetCommissionRuleById_NotFound() {
        when(commissionRuleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        CommissionRule result = commissionRuleService.getCommissionRuleById(RULE_ID, TENANT_ID);

        assertNull(result);
    }

    /**
     * 测试分页查询：多条件查询场景
     */
    @Test
    void testPageQuery_WithMultipleConditions() {
        // 准备参数
        Page<CommissionRule> page = new Page<>(1, 10);
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("applicableType", "RESIDENTIAL");
        queryParams.put("status", 1);
        queryParams.put("ruleName", "标准");
        queryParams.put("minRate", new BigDecimal("1.0"));
        queryParams.put("maxRate", new BigDecimal("5.0"));

        // 模拟查询结果
        IPage<CommissionRule> mockPage = new Page<>();
        mockPage.setRecords(Collections.singletonList(testRule));
        when(commissionRuleMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn((Page<CommissionRule>) mockPage);

        IPage<CommissionRule> result = commissionRuleService.pageQuery(page, queryParams, TENANT_ID);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());

        // 使用 ArgumentCaptor 验证查询条件
        ArgumentCaptor<QueryWrapper<CommissionRule>> wrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(commissionRuleMapper).selectPage(eq(page), wrapperCaptor.capture());
        assertNotNull(wrapperCaptor.getValue());
    }

    /**
     * 测试分页查询：空参数场景
     */
    @Test
    void testPageQuery_EmptyParams() {
        Page<CommissionRule> page = new Page<>(1, 10);
        when(commissionRuleMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(new Page<>());

        IPage<CommissionRule> result = commissionRuleService.pageQuery(page, new HashMap<>(), TENANT_ID);

        assertNotNull(result);
        verify(commissionRuleMapper).selectPage(any(Page.class), any(QueryWrapper.class));
    }

    /**
     * 测试多条件列表查询：正常场景
     */
    @Test
    void testListByConditions_Success() {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("status", 1);
        queryParams.put("applicableType", "COMMERCIAL");

        List<CommissionRule> mockList = Arrays.asList(testRule, testRule);
        when(commissionRuleMapper.selectList(any(QueryWrapper.class))).thenReturn(mockList);

        List<CommissionRule> result = commissionRuleService.listByConditions(queryParams, TENANT_ID);

        assertEquals(2, result.size());
        verify(commissionRuleMapper).selectList(any(QueryWrapper.class));
    }

    /**
     * 测试批量新增规则：成功场景
     */
    @Test
    void testBatchSaveCommissionRules_Success() {
        List<CommissionRule> ruleList = Arrays.asList(testRule, testRule);

        // 模拟唯一性校验通过
        when(commissionRuleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        // 模拟批量保存成功
        doReturn(true).when(commissionRuleService).saveBatch(ruleList);

        boolean result = commissionRuleService.batchSaveCommissionRules(ruleList);

        assertTrue(result);
        verify(commissionRuleService).saveBatch(ruleList);
    }

    /**
     * 测试批量新增规则：空列表（边界场景）
     */
    @Test
    void testBatchSaveCommissionRules_EmptyList() {
        boolean result = commissionRuleService.batchSaveCommissionRules(new ArrayList<>());
        assertFalse(result);
    }

    /**
     * 测试批量新增规则：规则名称重复（异常场景）
     */
    @Test
    void testBatchSaveCommissionRules_DuplicateRuleName() {
        List<CommissionRule> ruleList = Arrays.asList(testRule, testRule);

        // 模拟规则名称已存在
        when(commissionRuleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(IllegalArgumentException.class,
                () -> commissionRuleService.batchSaveCommissionRules(ruleList),
                "预期抛出规则名称重复的异常");
    }

    /**
     * 测试批量更新状态：成功场景
     */
    @Test
    void testBatchUpdateStatus_Success() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        Byte newStatus = 0; // 设置为失效

        // 模拟校验通过
        doNothing().when(commissionRuleService).validateRuleIdsBelongToTenant(TENANT_ID, ids);
        // 模拟更新成功
        when(commissionRuleMapper.update(any(CommissionRule.class), any(LambdaQueryWrapper.class))).thenReturn(3);

        boolean result = commissionRuleService.batchUpdateStatus(ids, newStatus, TENANT_ID);

        assertTrue(result);
        verify(commissionRuleMapper).update(any(CommissionRule.class), any(LambdaQueryWrapper.class));
    }

    /**
     * 测试批量更新状态：空列表（边界场景）
     */
    @Test
    void testBatchUpdateStatus_EmptyList() {
        boolean result = commissionRuleService.batchUpdateStatus(new ArrayList<>(), (byte) 1, TENANT_ID);
        assertFalse(result);
    }

    /**
     * 测试批量删除规则：成功场景
     */
    @Test
    void testBatchRemoveCommissionRules_Success() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 模拟校验通过
        doNothing().when(commissionRuleService).validateRuleIdsBelongToTenant(TENANT_ID, ids);
        // 模拟批量删除成功
        when(commissionRuleMapper.deleteBatchIds(ids)).thenReturn(3);

        boolean result = commissionRuleService.batchRemoveCommissionRules(ids, TENANT_ID);

        assertTrue(result);
        verify(commissionRuleMapper).deleteBatchIds(ids);
    }

    /**
     * 测试批量删除规则：空列表（边界场景）
     */
    @Test
    void testBatchRemoveCommissionRules_EmptyList() {
        boolean result = commissionRuleService.batchRemoveCommissionRules(new ArrayList<>(), TENANT_ID);
        assertFalse(result);
    }

    /**
     * 测试校验规则ID归属：成功场景 - 修复 Lambda 缓存问题
     */
    @Test
    void testValidateRuleIdsBelongToTenant_Success() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 修复：使用 QueryWrapper 而不是 LambdaQueryWrapper 来避免 Lambda 缓存问题
        List<CommissionRule> mockRules = Arrays.asList(
                createRule(1L, TENANT_ID),
                createRule(2L, TENANT_ID),
                createRule(3L, TENANT_ID)
        );

        // 关键修复：使用 any(QueryWrapper.class) 而不是 any(LambdaQueryWrapper.class)
        when(commissionRuleMapper.selectList(any(QueryWrapper.class))).thenReturn(mockRules);

        // 应该不抛出异常
        assertDoesNotThrow(() -> commissionRuleService.validateRuleIdsBelongToTenant(TENANT_ID, ids));
    }

    /**
     * 测试校验规则ID归属：存在不存在的ID（异常场景）- 使用 Answer
     */
    @Test
    void testValidateRuleIdsBelongToTenant_NonExistentIds() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 模拟只查询到部分ID
        List<CommissionRule> mockRules = Arrays.asList(
                createRule(1L, TENANT_ID),
                createRule(2L, TENANT_ID)
        );

        // 使用 Answer 来避免 Lambda 缓存问题
        when(commissionRuleMapper.selectList(any())).thenAnswer(invocation -> {
            // 直接返回模拟数据，不关心具体的 Wrapper 类型
            return mockRules;
        });

        assertThrows(IllegalArgumentException.class,
                () -> commissionRuleService.validateRuleIdsBelongToTenant(TENANT_ID, ids),
                "预期抛出规则ID不存在的异常");
    }

    /**
     * 测试校验规则ID归属：存在非当前租户ID（异常场景）- 修复 Lambda 缓存问题
     */
    @Test
    void testValidateRuleIdsBelongToTenant_InvalidTenantIds() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 模拟存在不同租户的规则
        List<CommissionRule> mockRules = Arrays.asList(
                createRule(1L, TENANT_ID),
                createRule(2L, TENANT_ID),
                createRule(3L, 999L) // 不同租户
        );

        // 关键修复：使用 any(QueryWrapper.class)
        when(commissionRuleMapper.selectList(any(QueryWrapper.class))).thenReturn(mockRules);

        assertThrows(IllegalArgumentException.class,
                () -> commissionRuleService.validateRuleIdsBelongToTenant(TENANT_ID, ids),
                "预期抛出无权限操作规则ID的异常");
    }

    /**
     * 测试校验规则ID归属：空参数（边界场景）
     */
    @Test
    void testValidateRuleIdsBelongToTenant_EmptyParams() {
        // 空列表应该不抛出异常
        assertDoesNotThrow(() -> commissionRuleService.validateRuleIdsBelongToTenant(TENANT_ID, new ArrayList<>()));
    }

    /**
     * 创建测试规则对象
     */
    private CommissionRule createRule(Long id, Long tenantId) {
        CommissionRule rule = new CommissionRule();
        rule.setId(id);
        rule.setTenantId(tenantId);
        return rule;
    }
}
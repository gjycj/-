package com.house.deed.pavilion;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.entity.CustomerHistoryDeal;
import com.house.deed.pavilion.service.impl.CustomerHistoryDealServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CustomerHistoryDealServiceImpl 集成测试 - 修正版本（考虑唯一约束）
 */
@SpringBootTest
@Transactional
class CustomerHistoryDealServiceImplIntegrationTest {

    @Autowired
    private CustomerHistoryDealServiceImpl customerHistoryDealService;

    // 测试数据生成器 - 使用不同的组合避免唯一约束冲突
    private long customerCounter = 100L;
    private long contractCounter = 200L;

    @Test
    void saveHistoryDeal_Success() {
        // 准备
        CustomerHistoryDeal deal = createUniqueTestDeal();

        // 执行
        boolean result = customerHistoryDealService.saveHistoryDeal(deal);

        // 验证
        assertTrue(result);
        assertNotNull(deal.getId());
    }

    @Test
    void saveHistoryDeal_WithoutTenantId_ThrowsException() {
        // 准备
        CustomerHistoryDeal deal = createUniqueTestDeal();
        deal.setTenantId(null);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> customerHistoryDealService.saveHistoryDeal(deal));
        assertTrue(exception.getMessage().contains("租户ID不能为空"));
    }

    @Test
    void saveHistoryDeal_WithoutCustomerId_ThrowsException() {
        // 准备
        CustomerHistoryDeal deal = createUniqueTestDeal();
        deal.setCustomerId(null);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> customerHistoryDealService.saveHistoryDeal(deal));
        assertTrue(exception.getMessage().contains("客户ID不能为空"));
    }

    @Test
    void saveHistoryDeal_WithoutContractId_ThrowsException() {
        // 准备
        CustomerHistoryDeal deal = createUniqueTestDeal();
        deal.setContractId(null);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> customerHistoryDealService.saveHistoryDeal(deal));
        assertTrue(exception.getMessage().contains("合同ID不能为空"));
    }

    @Test
    void saveHistoryDeal_WithoutDealTime_ThrowsException() {
        // 准备
        CustomerHistoryDeal deal = createUniqueTestDeal();
        deal.setDealTime(null);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> customerHistoryDealService.saveHistoryDeal(deal));
        assertTrue(exception.getMessage().contains("成交日期不能为空"));
    }

    @Test
    void saveHistoryDeal_WithoutHouseInfo_ThrowsException() {
        // 准备
        CustomerHistoryDeal deal = createUniqueTestDeal();
        deal.setHouseInfo(null);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> customerHistoryDealService.saveHistoryDeal(deal));
        assertTrue(exception.getMessage().contains("成交房源信息不能为空"));
    }

    @Test
    void saveHistoryDeal_WithoutDealType_ThrowsException() {
        // 准备
        CustomerHistoryDeal deal = createUniqueTestDeal();
        deal.setDealType(null);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> customerHistoryDealService.saveHistoryDeal(deal));
        assertTrue(exception.getMessage().contains("成交类型不能为空"));
    }

    @Test
    void saveHistoryDeal_InvalidDealType_ThrowsException() {
        // 准备
        CustomerHistoryDeal deal = createUniqueTestDeal();
        deal.setDealType("INVALID"); // 无效的成交类型

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> customerHistoryDealService.saveHistoryDeal(deal));
        assertTrue(exception.getMessage().contains("成交类型必须为SALE（买卖）或RENT（租赁）"));
    }

    @Test
    void saveHistoryDeal_ValidDealType_SALE() {
        // 准备
        CustomerHistoryDeal deal = createUniqueTestDeal();
        deal.setDealType("SALE"); // 有效的成交类型 - 买卖

        // 执行
        boolean result = customerHistoryDealService.saveHistoryDeal(deal);

        // 验证
        assertTrue(result);
    }

    @Test
    void saveHistoryDeal_ValidDealType_RENT() {
        // 准备
        CustomerHistoryDeal deal = createUniqueTestDeal();
        deal.setDealType("RENT"); // 有效的成交类型 - 租赁

        // 执行
        boolean result = customerHistoryDealService.saveHistoryDeal(deal);

        // 验证
        assertTrue(result);
    }

    @Test
    void updateHistoryDealById_Success() {
        // 准备 - 先保存一条记录
        CustomerHistoryDeal saved = createAndSaveUniqueDeal();
        saved.setHouseInfo("更新后的房源信息");

        // 执行
        boolean result = customerHistoryDealService.updateHistoryDealById(saved);

        // 验证
        assertTrue(result);
    }

    @Test
    void updateHistoryDealById_TenantMismatch_ThrowsException() {
        // 准备
        CustomerHistoryDeal saved = createAndSaveUniqueDeal();

        // 创建更新请求，使用错误的租户ID
        CustomerHistoryDeal updateRequest = new CustomerHistoryDeal();
        updateRequest.setId(saved.getId());
        updateRequest.setTenantId(999L); // 错误的租户ID
        updateRequest.setCustomerId(saved.getCustomerId());
        updateRequest.setContractId(saved.getContractId());
        updateRequest.setDealTime(saved.getDealTime());
        updateRequest.setHouseInfo("更新内容");
        updateRequest.setDealType("SALE");

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> customerHistoryDealService.updateHistoryDealById(updateRequest));
        assertEquals("无权限操作此记录，记录属于其他租户", exception.getMessage());
    }

    @Test
    void updateHistoryDealById_RecordNotExists_ThrowsException() {
        // 准备 - 创建一个不存在的记录
        CustomerHistoryDeal deal = createUniqueTestDeal();
        deal.setId(999999L); // 不存在的ID

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> customerHistoryDealService.updateHistoryDealById(deal));
        assertEquals("成交记录不存在", exception.getMessage());
    }

    @Test
    void removeHistoryDealById_Success() {
        // 准备
        CustomerHistoryDeal saved = createAndSaveUniqueDeal();

        // 执行
        boolean result = customerHistoryDealService.removeHistoryDealById(saved.getId(), saved.getTenantId());

        // 验证
        assertTrue(result);
    }

    @Test
    void getHistoryDealById_Success() {
        // 准备
        CustomerHistoryDeal saved = createAndSaveUniqueDeal();

        // 执行
        CustomerHistoryDeal result = customerHistoryDealService.getHistoryDealById(saved.getId(), saved.getTenantId());

        // 验证
        assertNotNull(result);
        assertEquals(saved.getId(), result.getId());
        assertEquals(saved.getHouseInfo(), result.getHouseInfo());
        assertEquals(saved.getDealType(), result.getDealType());
    }

    @Test
    void pageQuery_Success() {
        // 准备 - 创建测试数据（使用不同的客户ID和合同ID组合）
        createAndSaveUniqueDeal(); // 记录1
        createAndSaveUniqueDeal(); // 记录2

        // 执行
        Page<CustomerHistoryDeal> page = new Page<>(1, 10);
        CustomerHistoryDeal query = new CustomerHistoryDeal();
        query.setTenantId(1L);

        IPage<CustomerHistoryDeal> result = customerHistoryDealService.pageQuery(page, query);

        // 验证
        assertNotNull(result);
    }

    @Test
    void pageQuery_WithCustomerIdFilter() {
        // 准备
        CustomerHistoryDeal deal1 = createAndSaveUniqueDeal();
        long specificCustomerId = deal1.getCustomerId();

        // 创建另一个客户的记录
        CustomerHistoryDeal deal2 = createUniqueTestDeal();
        deal2.setCustomerId(999999L); // 不同的客户
        deal2.setContractId(999999L); // 不同的合同
        customerHistoryDealService.saveHistoryDeal(deal2);

        // 执行 - 只查询特定客户
        Page<CustomerHistoryDeal> page = new Page<>(1, 10);
        CustomerHistoryDeal query = new CustomerHistoryDeal();
        query.setTenantId(1L);
        query.setCustomerId(specificCustomerId);

        IPage<CustomerHistoryDeal> result = customerHistoryDealService.pageQuery(page, query);

        // 验证
        assertNotNull(result);
    }

    @Test
    void listByCustomerId_Success() {
        // 准备 - 为同一个客户创建多条记录（使用不同的合同ID）
        long testCustomerId = customerCounter++;
        long tenantId = 1L;

        CustomerHistoryDeal deal1 = createUniqueTestDeal();
        deal1.setCustomerId(testCustomerId);
        customerHistoryDealService.saveHistoryDeal(deal1);

        CustomerHistoryDeal deal2 = createUniqueTestDeal();
        deal2.setCustomerId(testCustomerId);
        customerHistoryDealService.saveHistoryDeal(deal2);

        // 执行
        List<CustomerHistoryDeal> result = customerHistoryDealService.listByCustomerId(testCustomerId, tenantId);

        // 验证
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(d -> d.getCustomerId().equals(testCustomerId)));
    }

    @Test
    void listByContractId_Success() {
        // 准备
        CustomerHistoryDeal saved = createAndSaveUniqueDeal();

        // 执行
        List<CustomerHistoryDeal> result = customerHistoryDealService.listByContractId(
                saved.getContractId(), saved.getTenantId());

        // 验证
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(saved.getContractId(), result.get(0).getContractId());
    }

    @Test
    void batchSaveHistoryDeals_Success() {
        // 准备 - 使用不同的客户ID和合同ID组合
        CustomerHistoryDeal deal1 = createUniqueTestDeal();
        CustomerHistoryDeal deal2 = createUniqueTestDeal();

        List<CustomerHistoryDeal> dealList = Arrays.asList(deal1, deal2);

        // 执行
        boolean result = customerHistoryDealService.batchSaveHistoryDeals(dealList);

        // 验证
        assertTrue(result);
    }

    @Test
    void batchSaveHistoryDeals_EmptyList_ReturnsTrue() {
        // 执行
        boolean result = customerHistoryDealService.batchSaveHistoryDeals(Collections.emptyList());

        // 验证
        assertTrue(result);
    }

    @Test
    void batchSaveHistoryDeals_DifferentTenants_ThrowsException() {
        // 准备不同租户的记录，使用不同的客户ID和合同ID
        CustomerHistoryDeal deal1 = createUniqueTestDeal();
        deal1.setTenantId(1L);

        CustomerHistoryDeal deal2 = createUniqueTestDeal();
        deal2.setTenantId(2L); // 不同的租户

        List<CustomerHistoryDeal> dealList = Arrays.asList(deal1, deal2);

        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> customerHistoryDealService.batchSaveHistoryDeals(dealList));
        assertEquals("批量记录必须属于同一租户，存在不一致的租户ID", exception.getMessage());
    }

    @Test
    void batchRemoveHistoryDeals_Success() {
        // 准备 - 先保存几条记录
        CustomerHistoryDeal saved1 = createAndSaveUniqueDeal();
        CustomerHistoryDeal saved2 = createAndSaveUniqueDeal();

        // 执行
        boolean result = customerHistoryDealService.batchRemoveHistoryDeals(
                Arrays.asList(saved1.getId(), saved2.getId()), 1L);

        // 验证
        assertTrue(result);
    }

    @Test
    void batchRemoveHistoryDeals_EmptyIds_ThrowsException() {
        // 执行 & 验证
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> customerHistoryDealService.batchRemoveHistoryDeals(Collections.emptyList(), 1L));
        assertEquals("记录ID列表不能为空", exception.getMessage());
    }

    @Test
    void batchRemoveHistoryDeals_CrossTenantRecords_ThrowsException() {
        // 准备 - 在不同租户下创建记录，使用不同的客户ID和合同ID
        CustomerHistoryDeal deal1 = createUniqueTestDeal();
        deal1.setTenantId(1L);
        customerHistoryDealService.saveHistoryDeal(deal1);

        CustomerHistoryDeal deal2 = createUniqueTestDeal();
        deal2.setTenantId(2L); // 不同租户
        customerHistoryDealService.saveHistoryDeal(deal2);

        // 尝试删除跨租户的记录
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> customerHistoryDealService.batchRemoveHistoryDeals(
                        Arrays.asList(deal1.getId(), deal2.getId()), 1L));

        // 更新断言以匹配实际的异常消息
        assertEquals("存在跨租户记录，无法删除，请检查数据权限", exception.getMessage());
    }

    @Test
    void listByConditions_WithDealTypeFilter() {
        // 准备不同成交类型的数据
        CustomerHistoryDeal saleDeal = createUniqueTestDeal();
        saleDeal.setDealType("SALE");
        customerHistoryDealService.saveHistoryDeal(saleDeal);

        CustomerHistoryDeal rentDeal = createUniqueTestDeal();
        rentDeal.setDealType("RENT");
        customerHistoryDealService.saveHistoryDeal(rentDeal);

        // 查询SALE类型
        CustomerHistoryDeal query = new CustomerHistoryDeal();
        query.setTenantId(1L);
        query.setDealType("SALE");

        List<CustomerHistoryDeal> results = customerHistoryDealService.listByConditions(query);

        // 验证
        assertNotNull(results);
        assertTrue(results.size() >= 1);
        assertTrue(results.stream().allMatch(d -> "SALE".equals(d.getDealType())));
    }

    @Test
    void listByConditions_WithHouseInfoLike() {
        // 准备
        CustomerHistoryDeal deal1 = createUniqueTestDeal();
        deal1.setHouseInfo("滨江花园 3室2厅 120㎡");
        customerHistoryDealService.saveHistoryDeal(deal1);

        CustomerHistoryDeal deal2 = createUniqueTestDeal();
        deal2.setHouseInfo("西山庭院 2室1厅 80㎡");
        customerHistoryDealService.saveHistoryDeal(deal2);

        // 执行 - 模糊查询包含"滨江"的房源
        CustomerHistoryDeal query = new CustomerHistoryDeal();
        query.setTenantId(1L);
        query.setHouseInfo("滨江");

        List<CustomerHistoryDeal> result = customerHistoryDealService.listByConditions(query);

        // 验证
        assertNotNull(result);
        assertTrue(result.stream().anyMatch(d -> d.getHouseInfo().contains("滨江")));
    }

    // 辅助方法 - 生成唯一的测试数据
    private CustomerHistoryDeal createUniqueTestDeal() {
        CustomerHistoryDeal deal = new CustomerHistoryDeal();
        deal.setTenantId(1L);
        deal.setCustomerId(customerCounter++);
        deal.setContractId(contractCounter++);
        deal.setDealTime(LocalDate.now());
        deal.setHouseInfo("测试房源 " + System.currentTimeMillis());
        deal.setDealType("SALE");
        return deal;
    }

    private CustomerHistoryDeal createAndSaveUniqueDeal() {
        CustomerHistoryDeal deal = createUniqueTestDeal();
        customerHistoryDealService.saveHistoryDeal(deal);
        return deal;
    }
}
package com.house.deed.pavilion;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.entity.TransactionFee;
import com.house.deed.pavilion.service.TransactionFeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成测试 - 使用真实数据库
 * 测试完整的数据库操作流程
 */
@SpringBootTest
@Transactional
@Rollback
public class TransactionFeeIntegrationTest {

    @Autowired
    private TransactionFeeService transactionFeeService;

    @Test
    void batchSave_Integration_Success() {
        // 准备测试数据
        List<TransactionFee> fees = createTestFees(3, 1001L);

        // 执行测试
        boolean result = transactionFeeService.batchSave(fees);

        // 验证
        assertTrue(result);

        // 验证数据已保存
        for (TransactionFee fee : fees) {
            assertNotNull(fee.getId());
        }
    }

    @Test
    void batchSave_MultipleTenants_Integration_ThrowsException() {
        // 准备测试数据 - 混合不同租户
        List<TransactionFee> fees = new ArrayList<>();

        TransactionFee fee1 = createFee(1001L, 5001L);
        TransactionFee fee2 = createFee(1002L, 5002L); // 不同租户

        fees.add(fee1);
        fees.add(fee2);

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionFeeService.batchSave(fees)
        );

        assertTrue(exception.getMessage().contains("必须属于同一租户"));
    }

    @Test
    void batchUpdateStatus_Integration_Success() {
        // 先保存测试数据
        List<TransactionFee> fees = createTestFees(2, 1001L);
        transactionFeeService.batchSave(fees);

        List<Long> ids = fees.stream()
                .map(TransactionFee::getId)
                .toList();

        // 执行状态更新
        boolean result = transactionFeeService.batchUpdateStatus(ids, "PAID", 1001L);

        // 验证
        assertTrue(result);

        // 验证状态已更新
        List<TransactionFee> updatedFees = transactionFeeService.listByIds(ids);
        for (TransactionFee fee : updatedFees) {
            assertEquals("PAID", fee.getPaymentStatus());
            assertNotNull(fee.getPaymentTime());
        }
    }

    @Test
    void batchRemove_Integration_Success() {
        // 先保存测试数据
        List<TransactionFee> fees = createTestFees(3, 1001L);
        transactionFeeService.batchSave(fees);

        List<Long> ids = fees.stream()
                .map(TransactionFee::getId)
                .toList();

        // 执行删除
        boolean result = transactionFeeService.batchRemoveByIds(ids, 1001L);

        // 验证
        assertTrue(result);

        // 验证数据已删除
        List<TransactionFee> remainingFees = transactionFeeService.listByIds(ids);
        assertTrue(remainingFees.isEmpty());
    }

    @Test
    void pageQuery_Integration_Success() {
        // 先保存测试数据
        List<TransactionFee> fees = createTestFees(5, 1001L);
        transactionFeeService.batchSave(fees);

        // 执行分页查询
        Page<TransactionFee> page = new Page<>(1, 10);
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("feeType", "AGENCY_FEE");

        IPage<TransactionFee> result = transactionFeeService.pageQuery(page, queryParams, 1001L);

        // 验证
        assertNotNull(result);
        assertTrue(result.getTotal() >= 5);
    }

    @Test
    void listByConditions_Integration_Success() {
        // 先保存测试数据
        TransactionFee fee = createFee(1001L, 5001L);
        fee.setFeeType("TAX");
        fee.setPaymentStatus("PAID");
        fee.setPaymentTime(LocalDateTime.now());

        List<TransactionFee> fees = Collections.singletonList(fee);
        transactionFeeService.batchSave(fees);

        // 执行条件查询
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("feeType", "TAX");
        queryParams.put("paymentStatus", "PAID");

        List<TransactionFee> result = transactionFeeService.listByConditions(queryParams, 1001L);

        // 验证
        assertNotNull(result);
        assertFalse(result.isEmpty());

        TransactionFee foundFee = result.get(0);
        assertEquals("TAX", foundFee.getFeeType());
        assertEquals("PAID", foundFee.getPaymentStatus());
    }

    // 辅助方法
    private List<TransactionFee> createTestFees(int count, Long tenantId) {
        List<TransactionFee> fees = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            fees.add(createFee(tenantId, 5000L + i));
        }
        return fees;
    }

    private TransactionFee createFee(Long tenantId, Long contractId) {
        TransactionFee fee = new TransactionFee();
        fee.setTenantId(tenantId);
        fee.setContractId(contractId);
        fee.setFeeType("AGENCY_FEE");
        fee.setAmount(new BigDecimal("3000.00"));
        fee.setPayer("CUSTOMER");
        fee.setPaymentStatus("UNPAID");
        fee.setRemark("测试费用 - " + LocalDateTime.now());
        return fee;
    }
}
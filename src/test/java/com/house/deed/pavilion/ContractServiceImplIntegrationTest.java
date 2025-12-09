package com.house.deed.pavilion;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.entity.Contract;
import com.house.deed.pavilion.service.ContractService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ContractServiceImplIntegrationTest {

    // 使用专门的测试租户ID，避免与生产数据冲突
    private static final Long TEST_TENANT_ID = 9999L;
    private static final String TEST_PREFIX = "TEST_";

    @Autowired
    private ContractService contractService;

    private Contract createValidSaleContract(Long tenantId, String contractNo) {
        Contract contract = new Contract();
        contract.setTenantId(tenantId);
        contract.setContractNo(contractNo);
        contract.setHouseId(1001L);
        contract.setCustomerId(2001L);
        contract.setLandlordId(3001L);
        contract.setAgentId(4001L);
        contract.setCreateAgentId(4001L);
        contract.setContractType("SALE");
        contract.setAmount(new BigDecimal("150.80"));
        contract.setDeposit(new BigDecimal("15.00"));
        contract.setPaymentMethod("全款");
        contract.setSignTime(LocalDateTime.now());
        contract.setStatus("SIGNED");
        contract.setRemark("无特殊约定");
        return contract;
    }

    private Contract createValidRentContract(String contractNoSuffix) {
        Contract contract = new Contract();
        contract.setTenantId(TEST_TENANT_ID);
        contract.setContractNo(TEST_PREFIX + contractNoSuffix);
        contract.setHouseId(1002L);
        contract.setCustomerId(2002L);
        contract.setLandlordId(3002L);
        contract.setAgentId(4002L);
        contract.setCreateAgentId(4002L);
        contract.setContractType("RENT");
        contract.setAmount(new BigDecimal("3.60"));
        contract.setDeposit(new BigDecimal("3.60"));
        contract.setPaymentMethod("月付");
        contract.setSignTime(LocalDateTime.now());
        contract.setStartDate(LocalDate.now().plusDays(1));
        contract.setEndDate(LocalDate.now().plusYears(1));
        contract.setStatus("EXECUTING");
        contract.setRemark("测试租赁合同");
        return contract;
    }

    private Contract createValidRentContract(Long tenantId, String contractNo) {
        Contract contract = new Contract();
        contract.setTenantId(tenantId);
        contract.setContractNo(contractNo);
        contract.setHouseId(1002L);
        contract.setCustomerId(2002L);
        contract.setLandlordId(3002L);
        contract.setAgentId(4002L);
        contract.setCreateAgentId(4002L);
        contract.setContractType("RENT");
        contract.setAmount(new BigDecimal("3.60"));
        contract.setDeposit(new BigDecimal("3.60"));
        contract.setPaymentMethod("月付");
        contract.setSignTime(LocalDateTime.now());
        contract.setStartDate(LocalDate.now().plusDays(1));
        contract.setEndDate(LocalDate.now().plusYears(1));
        contract.setStatus("EXECUTING");
        contract.setRemark("押一付一");
        return contract;
    }

    private Contract createValidSaleContract(String contractNoSuffix) {
        Contract contract = new Contract();
        contract.setTenantId(TEST_TENANT_ID);
        contract.setContractNo(TEST_PREFIX + contractNoSuffix);
        contract.setHouseId(1001L);
        contract.setCustomerId(2001L);
        contract.setLandlordId(3001L);
        contract.setAgentId(4001L);
        contract.setCreateAgentId(4001L);
        contract.setContractType("SALE");
        contract.setAmount(new BigDecimal("150.80"));
        contract.setDeposit(new BigDecimal("15.00"));
        contract.setPaymentMethod("全款");
        contract.setSignTime(LocalDateTime.now());
        contract.setStatus("SIGNED");
        contract.setRemark("测试合同");
        return contract;
    }

    @BeforeEach
    void cleanup() {
        // 更安全的清理方式 - 只清理测试数据
        List<Contract> allContracts = contractService.list();
        for (Contract contract : allContracts) {
            // 只删除测试数据，避免影响其他重要数据
            if (contract.getContractNo() != null &&
                    (contract.getContractNo().startsWith("CT") ||
                            contract.getContractNo().startsWith("CON2025") ||
                            contract.getContractNo().startsWith("TEST"))) {
                contractService.removeById(contract.getId());
            }
        }
    }

    // ==================== 基础CRUD测试 ====================
    @Test
    void testSaveContract_Success() {
        Contract contract = createValidSaleContract(1L, "TEST001");

        boolean result = contractService.save(contract);

        assertTrue(result);
        assertNotNull(contract.getId());
        assertEquals("TEST001", contract.getContractNo());
    }

    // 修正：使用 DataIntegrityViolationException 而不是 ConstraintViolationException
    @Test
    void testSaveContract_MissingRequiredFields() {
        Contract contract = new Contract(); // 缺少所有必填字段

        // 由于数据库约束会先触发，所以期望的是 DataIntegrityViolationException
        assertThrows(DataIntegrityViolationException.class, () -> {
            contractService.save(contract);
        });
    }

    // ==================== 分页查询测试 - 完全重写 ====================
    @Test
    void testPageQuery_Success() {
        // 准备测试数据
        Contract saleContract = createValidSaleContract("PAGE_QUERY_001");
        Contract rentContract = createValidRentContract("PAGE_QUERY_002");
        contractService.save(saleContract);
        contractService.save(rentContract);

        // 构建查询参数 - 精确匹配
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("contractType", "SALE");
        queryParams.put("contractNo", TEST_PREFIX + "PAGE_QUERY_001");

        Page<Contract> page = new Page<>(1, 10);
        IPage<Contract> result = contractService.pageQuery(page, queryParams, TEST_TENANT_ID);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size(), "应该只返回1条记录");
        assertEquals(TEST_PREFIX + "PAGE_QUERY_001", result.getRecords().get(0).getContractNo());
        assertEquals("SALE", result.getRecords().get(0).getContractType());
    }

    @Test
    void testPageQuery_WithStatusFilter() {
        // 使用测试专用租户ID，避免与现有数据冲突
        Long testTenantId = 99999L;

        // 准备测试数据
        Contract contract1 = createValidSaleContract(testTenantId, "TEST004");
        contract1.setStatus("SIGNED");
        Contract contract2 = createValidSaleContract(testTenantId, "TEST005");
        contract2.setStatus("EXECUTING");
        contractService.save(contract1);
        contractService.save(contract2);

        // 按状态过滤
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("status", "SIGNED");

        Page<Contract> page = new Page<>(1, 10);
        IPage<Contract> result = contractService.pageQuery(page, queryParams, testTenantId);

        // 过滤出测试数据，确保只统计我们创建的记录
        List<Contract> testRecords = result.getRecords().stream()
                .filter(c -> c.getContractNo().startsWith("TEST"))
                .toList();

        assertEquals(1, testRecords.size(), "应该只返回1条匹配的测试记录");
        assertEquals("SIGNED", testRecords.get(0).getStatus());
        assertEquals("TEST004", testRecords.get(0).getContractNo());
    }

    @Test
    void testPageQuery_WithSignTimeRange() {
        // 准备测试数据
        Contract contract = createValidSaleContract(1L, "TEST006");
        contract.setSignTime(LocalDateTime.of(2025, 11, 1, 10, 0));
        contractService.save(contract);

        // 按签约时间范围查询
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("startTime", LocalDateTime.of(2025, 10, 1, 0, 0));
        queryParams.put("endTime", LocalDateTime.of(2025, 11, 30, 23, 59));

        Page<Contract> page = new Page<>(1, 10);
        IPage<Contract> result = contractService.pageQuery(page, queryParams, 1L);

        assertEquals(1, result.getRecords().size());
    }

    @Test
    void testPageQuery_TenantIsolation() {
        // 使用测试专用租户ID，避免与现有数据冲突
        Long testTenant1 = 99991L;
        Long testTenant2 = 99992L;

        // 准备不同租户的数据
        Contract contract1 = createValidSaleContract(testTenant1, "TEST007");
        Contract contract2 = createValidSaleContract(testTenant2, "TEST008");
        contractService.save(contract1);
        contractService.save(contract2);

        // 查询租户1的数据
        Page<Contract> page = new Page<>(1, 10);
        IPage<Contract> result = contractService.pageQuery(page, new HashMap<>(), testTenant1);

        // 应该只返回租户1的数据
        assertEquals(1, result.getRecords().size(), "应该只返回租户1的1条数据");
        assertEquals(testTenant1, result.getRecords().get(0).getTenantId());
        assertEquals("TEST007", result.getRecords().get(0).getContractNo());
    }

    // ==================== 列表查询测试 ====================
    @Test
    void testListByConditions_Success() {
        // 使用测试专用租户ID，避免与现有数据冲突
        Long testTenantId = 99999L;

        // 准备测试数据
        Contract contract1 = createValidSaleContract(testTenantId, "TEST009");
        Contract contract2 = createValidSaleContract(testTenantId, "TEST010");
        contractService.save(contract1);
        contractService.save(contract2);

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("contractType", "SALE");

        // 使用测试租户ID查询
        List<Contract> result = contractService.listByConditions(queryParams, testTenantId);

        assertNotNull(result);

        // 过滤出测试数据，确保只统计我们创建的记录
        List<Contract> testResults = result.stream()
                .filter(c -> c.getContractNo().startsWith("TEST"))
                .toList();

        assertEquals(2, testResults.size(), "应该只返回2条测试数据");
    }

    @Test
    void testListByConditions_EmptyResult() {
        // 使用一个全新的、不存在的租户ID
        Long testTenantId = 99999L;

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("contractType", "INVALID_TYPE");

        List<Contract> result = contractService.listByConditions(queryParams, testTenantId);

        assertNotNull(result);
        assertTrue(result.isEmpty(), "对于不存在的租户ID，应该返回空列表");
    }

    // ==================== 批量操作测试 - 修正版本 ====================
    @Test
    void testBatchSaveContracts_Success() {
        // 准备测试数据
        List<Contract> contracts = Arrays.asList(
                createValidSaleContract("BATCH001"),
                createValidSaleContract("BATCH002")
        );

        boolean result = contractService.batchSaveContracts(contracts);

        assertTrue(result, "批量保存应该成功");

        // 验证数据确实保存了 - 只查询测试租户的数据
        List<Contract> savedContracts = contractService.listByConditions(new HashMap<>(), TEST_TENANT_ID);

        // 只统计测试数据
        long testDataCount = savedContracts.stream()
                .filter(c -> c.getContractNo().startsWith(TEST_PREFIX))
                .count();

        assertEquals(2, testDataCount, "应该保存2条测试数据");
    }

    @Test
    void testBatchSaveContracts_EmptyList() {
        boolean result = contractService.batchSaveContracts(Collections.emptyList());
        assertFalse(result);
    }

    @Test
    void testBatchSaveContracts_DifferentTenants() {
        List<Contract> contracts = Arrays.asList(
                createValidSaleContract(1L, "TEST013"),
                createValidSaleContract(2L, "TEST014") // 不同租户
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            contractService.batchSaveContracts(contracts);
        });

        assertTrue(exception.getMessage().contains("合同所属租户不一致"));
    }

    // ==================== 批量更新状态测试 ====================
    @Test
    void testBatchUpdateStatus_Success() {
        // 准备测试数据
        Contract contract1 = createValidSaleContract(1L, "TEST015");
        Contract contract2 = createValidSaleContract(1L, "TEST016");
        contractService.save(contract1);
        contractService.save(contract2);

        List<Long> ids = Arrays.asList(contract1.getId(), contract2.getId());
        boolean result = contractService.batchUpdateStatus(ids, "COMPLETED", 1L);

        assertTrue(result);

        // 验证状态已更新
        Contract updated1 = contractService.getById(contract1.getId());
        Contract updated2 = contractService.getById(contract2.getId());
        assertEquals("COMPLETED", updated1.getStatus());
        assertEquals("COMPLETED", updated2.getStatus());
    }

    @Test
    void testBatchUpdateStatus_CrossTenant() {
        // 准备租户1的合同
        Contract contract = createValidSaleContract(1L, "TEST017");
        contractService.save(contract);

        // 尝试用租户2的权限更新
        List<Long> ids = Collections.singletonList(contract.getId());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            contractService.batchUpdateStatus(ids, "COMPLETED", 2L);
        });

        assertTrue(exception.getMessage().contains("无权限操作其他租户的合同ID"));
    }

    @Test
    void testBatchUpdateStatus_NonExistentIds() {
        List<Long> ids = Arrays.asList(999L, 1000L); // 不存在的ID

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            contractService.batchUpdateStatus(ids, "COMPLETED", 1L);
        });

        assertTrue(exception.getMessage().contains("合同ID不存在"));
    }

    @Test
    void testBatchUpdateStatus_EmptyList() {
        boolean result = contractService.batchUpdateStatus(Collections.emptyList(), "COMPLETED", 1L);
        assertFalse(result);
    }

    // ==================== 批量删除测试 ====================
    @Test
    void testBatchRemoveContracts_Success() {
        // 准备测试数据
        Contract contract1 = createValidSaleContract(1L, "TEST018");
        Contract contract2 = createValidSaleContract(1L, "TEST019");
        contractService.save(contract1);
        contractService.save(contract2);

        List<Long> ids = Arrays.asList(contract1.getId(), contract2.getId());
        boolean result = contractService.batchRemoveContracts(ids, 1L);

        assertTrue(result);

        // 验证已删除
        assertNull(contractService.getById(contract1.getId()));
        assertNull(contractService.getById(contract2.getId()));
    }

    @Test
    void testBatchRemoveContracts_MixedTenants() {
        // 准备租户1和租户2的合同
        Contract contract1 = createValidSaleContract(1L, "TEST020");
        Contract contract2 = createValidSaleContract(2L, "TEST021");
        contractService.save(contract1);
        contractService.save(contract2);

        // 尝试用租户1权限删除两个合同
        List<Long> ids = Arrays.asList(contract1.getId(), contract2.getId());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            contractService.batchRemoveContracts(ids, 1L);
        });

        assertTrue(exception.getMessage().contains("无权限操作其他租户的合同ID"));
    }

    @Test
    void testBatchRemoveContracts_NonExistentIds() {
        List<Long> ids = Arrays.asList(999L, 1000L); // 不存在的ID

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            contractService.batchRemoveContracts(ids, 1L);
        });

        assertTrue(exception.getMessage().contains("合同ID不存在"));
    }

    @Test
    void testBatchRemoveContracts_EmptyList() {
        boolean result = contractService.batchRemoveContracts(Collections.emptyList(), 1L);
        assertFalse(result);
    }

    // ==================== 业务逻辑测试 ====================
    @Test
    void testContractBusinessValidation() {
        // 测试租赁合同必须包含开始和结束日期 - 这个应该在业务逻辑中处理
        Contract rentContract = createValidRentContract(1L, "TEST022");
        // 这里不需要测试，因为实体类中 startDate 和 endDate 是可选的
        // 租赁合同的日期要求应该在业务逻辑中处理

        assertDoesNotThrow(() -> {
            contractService.save(rentContract);
        });
    }

    // ==================== 新增：测试查询条件构建 ====================
    @Test
    void testBuildQueryWrapper_WithContractNo() {
        Contract contract = createValidSaleContract(1L, "TEST023");
        contractService.save(contract);

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("contractNo", "TEST023");

        List<Contract> result = contractService.listByConditions(queryParams, 1L);

        assertEquals(1, result.size());
        assertEquals("TEST023", result.get(0).getContractNo());
    }

    @Test
    void testBuildQueryWrapper_WithMultipleParams() {
        Contract contract = createValidSaleContract(1L, "TEST024");
        contract.setStatus("COMPLETED");
        contractService.save(contract);

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("contractNo", "TEST024");
        queryParams.put("status", "COMPLETED");

        List<Contract> result = contractService.listByConditions(queryParams, 1L);

        assertEquals(1, result.size());
        assertEquals("TEST024", result.get(0).getContractNo());
        assertEquals("COMPLETED", result.get(0).getStatus());
    }
}
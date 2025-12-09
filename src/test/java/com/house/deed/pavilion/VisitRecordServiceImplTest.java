package com.house.deed.pavilion;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.entity.VisitRecord;
import com.house.deed.pavilion.service.VisitRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VisitRecordService 集成测试
 * 使用真实数据库测试完整的业务逻辑和数据操作
 */
@SpringBootTest
@Transactional
@Rollback
public class VisitRecordServiceImplTest {

    @Autowired
    private VisitRecordService visitRecordService;

    // 测试用的固定租户ID
    private static final Long TEST_TENANT_ID = 1001L;
    // 其他测试用的固定ID（假设数据库中已存在这些数据）
    private static final Long TEST_HOUSE_ID = 101L;
    private static final Long TEST_CUSTOMER_ID = 4001L;
    private static final Long TEST_AGENT_ID = 3001L;
    private static final Long TEST_ANOTHER_HOUSE_ID = 102L;
    private static final Long TEST_ANOTHER_CUSTOMER_ID = 4002L;
    private static final Long TEST_ANOTHER_AGENT_ID = 3002L;

    @BeforeEach
    void setUp() {
        // 在每个测试开始前，清理测试数据（如果需要）
        // 由于使用了 @Transactional 和 @Rollback，测试数据会自动回滚
    }

    @Test
    void saveVisitRecord_Success() {
        // 准备测试数据
        VisitRecord visitRecord = createVisitRecord(TEST_TENANT_ID, TEST_HOUSE_ID,
                TEST_CUSTOMER_ID, TEST_AGENT_ID);

        // 执行测试
        boolean result = visitRecordService.saveVisitRecord(visitRecord);

        // 验证
        assertTrue(result);
        assertNotNull(visitRecord.getId()); // ID 应该被自动生成
        assertEquals(TEST_TENANT_ID, visitRecord.getTenantId());
        assertEquals("OFFLINE", visitRecord.getVisitType());
        assertEquals((byte) 2, visitRecord.getIntentionLevel());
    }

    @Test
    void saveVisitRecord_NullTenantId_ThrowsException() {
        // 准备测试数据 - 没有设置租户ID
        VisitRecord visitRecord = createVisitRecord(null, TEST_HOUSE_ID,
                TEST_CUSTOMER_ID, TEST_AGENT_ID);

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> visitRecordService.saveVisitRecord(visitRecord)
        );

        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    void getByIdWithTenant_Success() {
        // 先保存一条记录
        VisitRecord visitRecord = createVisitRecord(TEST_TENANT_ID, TEST_HOUSE_ID,
                TEST_CUSTOMER_ID, TEST_AGENT_ID);
        visitRecordService.saveVisitRecord(visitRecord);
        Long savedId = visitRecord.getId();

        // 执行查询
        VisitRecord found = visitRecordService.getByIdWithTenant(savedId, TEST_TENANT_ID);

        // 验证
        assertNotNull(found);
        assertEquals(savedId, found.getId());
        assertEquals(TEST_TENANT_ID, found.getTenantId());
        assertEquals(TEST_HOUSE_ID, found.getHouseId());
    }

    @Test
    void getByIdWithTenant_DifferentTenant_ReturnsNull() {
        // 先保存一条记录，租户ID为1001
        VisitRecord visitRecord = createVisitRecord(TEST_TENANT_ID, TEST_HOUSE_ID,
                TEST_CUSTOMER_ID, TEST_AGENT_ID);
        visitRecordService.saveVisitRecord(visitRecord);
        Long savedId = visitRecord.getId();

        // 使用不同的租户ID查询
        VisitRecord found = visitRecordService.getByIdWithTenant(savedId, 9999L);

        // 验证 - 应该返回null，因为租户ID不匹配
        assertNull(found);
    }

    @Test
    void updateVisitRecord_Success() {
        // 先保存一条记录
        VisitRecord visitRecord = createVisitRecord(TEST_TENANT_ID, TEST_HOUSE_ID,
                TEST_CUSTOMER_ID, TEST_AGENT_ID);
        visitRecordService.saveVisitRecord(visitRecord);
        Long savedId = visitRecord.getId();

        // 准备更新数据
        VisitRecord updateRecord = new VisitRecord();
        updateRecord.setId(savedId);
        updateRecord.setTenantId(TEST_TENANT_ID);
        updateRecord.setHouseId(TEST_ANOTHER_HOUSE_ID); // 修改房源
        updateRecord.setCustomerId(TEST_ANOTHER_CUSTOMER_ID); // 修改客户
        updateRecord.setAgentId(TEST_ANOTHER_AGENT_ID); // 修改经纪人
        updateRecord.setVisitTime(LocalDateTime.now().plusDays(1));
        updateRecord.setVisitType("VR"); // 修改带看方式
        updateRecord.setCustomerFeedback("VR看房体验很好，很真实");
        updateRecord.setIntentionLevel((byte) 3); // 修改意向级别

        // 执行更新
        boolean result = visitRecordService.updateVisitRecord(updateRecord);

        // 验证
        assertTrue(result);

        // 验证数据已更新
        VisitRecord updated = visitRecordService.getByIdWithTenant(savedId, TEST_TENANT_ID);
        assertNotNull(updated);
        assertEquals("VR", updated.getVisitType());
        assertEquals((byte) 3, updated.getIntentionLevel());
        assertEquals(TEST_ANOTHER_HOUSE_ID, updated.getHouseId());
    }

    @Test
    void updateVisitRecord_DifferentTenant_ThrowsException() {
        // 先保存一条记录，租户ID为1001
        VisitRecord visitRecord = createVisitRecord(TEST_TENANT_ID, TEST_HOUSE_ID,
                TEST_CUSTOMER_ID, TEST_AGENT_ID);
        visitRecordService.saveVisitRecord(visitRecord);
        Long savedId = visitRecord.getId();

        // 准备更新数据，使用不同的租户ID
        VisitRecord updateRecord = new VisitRecord();
        updateRecord.setId(savedId);
        updateRecord.setTenantId(9999L); // 错误的租户ID

        // 执行测试并验证异常
        SecurityException exception = assertThrows(
                SecurityException.class,
                () -> visitRecordService.updateVisitRecord(updateRecord)
        );

        assertEquals("无权操作其他租户的带看记录", exception.getMessage());
    }

    @Test
    void removeByIdWithTenant_Success() {
        // 先保存一条记录
        VisitRecord visitRecord = createVisitRecord(TEST_TENANT_ID, TEST_HOUSE_ID,
                TEST_CUSTOMER_ID, TEST_AGENT_ID);
        visitRecordService.saveVisitRecord(visitRecord);
        Long savedId = visitRecord.getId();

        // 执行删除
        boolean result = visitRecordService.removeByIdWithTenant(savedId, TEST_TENANT_ID);

        // 验证
        assertTrue(result, "使用正确租户ID删除应返回true，表示成功删除了行");

        // 验证数据已被删除
        VisitRecord deleted = visitRecordService.getByIdWithTenant(savedId, TEST_TENANT_ID);
        assertNull(deleted, "使用正确租户ID删除后，数据应不存在");
    }

    @Test
    void removeByIdWithTenant_DifferentTenant_ReturnsFalse() {
        // 先保存一条记录，租户ID为1001
        VisitRecord visitRecord = createVisitRecord(TEST_TENANT_ID, TEST_HOUSE_ID,
                TEST_CUSTOMER_ID, TEST_AGENT_ID);
        visitRecordService.saveVisitRecord(visitRecord);
        Long savedId = visitRecord.getId();

        // 使用不同的租户ID删除
        boolean result = visitRecordService.removeByIdWithTenant(savedId, 9999L);

        // 验证 - 应该返回false，因为租户ID不匹配，没有删除任何行
        assertFalse(result, "使用不同租户ID删除应返回false，表示没有删除任何行");

        // 验证数据仍然存在
        VisitRecord stillExists = visitRecordService.getByIdWithTenant(savedId, TEST_TENANT_ID);
        assertNotNull(stillExists, "使用不同租户ID删除后，原数据应仍然存在");
    }

    @Test
    void pageQuery_Success() {
        // 准备测试数据 - 插入多条记录
        for (int i = 0; i < 15; i++) {
            VisitRecord visitRecord = createVisitRecord(TEST_TENANT_ID,
                    TEST_HOUSE_ID + i,
                    TEST_CUSTOMER_ID + i,
                    TEST_AGENT_ID);
            visitRecord.setVisitTime(LocalDateTime.now().minusDays(i));
            visitRecordService.saveVisitRecord(visitRecord);
        }

        // 执行分页查询
        Page<VisitRecord> page = new Page<>(1, 10); // 第一页，每页10条
        IPage<VisitRecord> result = visitRecordService.pageQuery(
                page, TEST_TENANT_ID, null, null, null, null, null, null, null);

        // 验证
        assertNotNull(result);
        assertTrue(result.getTotal() >= 15); // 至少15条记录
        assertEquals(10, result.getRecords().size()); // 第一页应该有10条记录
        assertTrue(result.getPages() >= 2); // 至少2页

        // 验证按带看时间倒序排列
        LocalDateTime previousTime = null;
        for (VisitRecord record : result.getRecords()) {
            if (previousTime != null) {
                assertTrue(record.getVisitTime().isBefore(previousTime) ||
                        record.getVisitTime().isEqual(previousTime));
            }
            previousTime = record.getVisitTime();
        }
    }

    @Test
    void pageQuery_WithConditions_Success() {
        // 准备测试数据
        VisitRecord record1 = createVisitRecord(TEST_TENANT_ID, TEST_HOUSE_ID,
                TEST_CUSTOMER_ID, TEST_AGENT_ID);
        record1.setVisitType("OFFLINE");
        record1.setIntentionLevel((byte) 2);
        visitRecordService.saveVisitRecord(record1);

        VisitRecord record2 = createVisitRecord(TEST_TENANT_ID, TEST_ANOTHER_HOUSE_ID,
                TEST_ANOTHER_CUSTOMER_ID, TEST_ANOTHER_AGENT_ID);
        record2.setVisitType("VR");
        record2.setIntentionLevel((byte) 3);
        visitRecordService.saveVisitRecord(record2);

        // 执行带条件的查询
        Page<VisitRecord> page = new Page<>(1, 10);
        LocalDateTime startTime = LocalDateTime.now().minusDays(1);
        LocalDateTime endTime = LocalDateTime.now().plusDays(1);

        // 查询意向程度为2的记录
        IPage<VisitRecord> result = visitRecordService.pageQuery(
                page, TEST_TENANT_ID, null, null, null, startTime, endTime, null, (byte) 2);

        // 验证
        assertNotNull(result);
        assertTrue(result.getTotal() >= 1);
        for (VisitRecord record : result.getRecords()) {
            assertEquals((byte) 2, record.getIntentionLevel());
        }
    }

    @Test
    void pageQuery_NullTenantId_ThrowsException() {
        // 准备分页参数
        Page<VisitRecord> page = new Page<>(1, 10);

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> visitRecordService.pageQuery(page, null, null, null, null, null, null, null, null)
        );

        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    void saveBatchVisitRecord_Success() {
        // 准备批量测试数据
        List<VisitRecord> visitRecords = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            VisitRecord record = createVisitRecord(TEST_TENANT_ID,
                    TEST_HOUSE_ID + i,
                    TEST_CUSTOMER_ID + i,
                    TEST_AGENT_ID);
            visitRecords.add(record);
        }

        // 执行批量保存
        boolean result = visitRecordService.saveBatchVisitRecord(visitRecords);

        // 验证
        assertTrue(result);

        // 验证所有记录都已保存
        for (VisitRecord record : visitRecords) {
            assertNotNull(record.getId());
            assertEquals(TEST_TENANT_ID, record.getTenantId());
        }
    }

    @Test
    void saveBatchVisitRecord_EmptyList_ReturnsTrue() {
        // 执行批量保存空列表
        boolean result = visitRecordService.saveBatchVisitRecord(new ArrayList<>());

        // 验证 - 空列表应该返回true
        assertTrue(result);
    }

    @Test
    void saveBatchVisitRecord_DifferentTenants_ThrowsException() {
        // 准备批量测试数据 - 包含不同租户的记录
        List<VisitRecord> visitRecords = new ArrayList<>();

        VisitRecord record1 = createVisitRecord(TEST_TENANT_ID, TEST_HOUSE_ID,
                TEST_CUSTOMER_ID, TEST_AGENT_ID);
        VisitRecord record2 = createVisitRecord(9999L, TEST_HOUSE_ID,
                TEST_CUSTOMER_ID, TEST_AGENT_ID); // 不同租户

        visitRecords.add(record1);
        visitRecords.add(record2);

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> visitRecordService.saveBatchVisitRecord(visitRecords)
        );

        assertEquals("批量新增的带看记录必须属于同一租户", exception.getMessage());
    }

    @Test
    void saveBatchVisitRecord_NullTenantId_ThrowsException() {
        // 准备批量测试数据 - 第一条记录没有租户ID
        List<VisitRecord> visitRecords = new ArrayList<>();

        VisitRecord record = createVisitRecord(null, TEST_HOUSE_ID,
                TEST_CUSTOMER_ID, TEST_AGENT_ID);
        visitRecords.add(record);

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> visitRecordService.saveBatchVisitRecord(visitRecords)
        );

        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    void updateBatchVisitRecord_Success() {
        // 先批量保存一些记录
        List<VisitRecord> toSave = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            VisitRecord record = createVisitRecord(TEST_TENANT_ID,
                    TEST_HOUSE_ID + i,
                    TEST_CUSTOMER_ID + i,
                    TEST_AGENT_ID);
            toSave.add(record);
        }
        visitRecordService.saveBatchVisitRecord(toSave);

        // 准备批量更新数据
        List<VisitRecord> toUpdate = toSave.stream().map(record -> {
            VisitRecord update = new VisitRecord();
            update.setId(record.getId());
            update.setTenantId(record.getTenantId());
            update.setHouseId(record.getHouseId());
            update.setCustomerId(record.getCustomerId());
            update.setAgentId(record.getAgentId());
            update.setVisitTime(record.getVisitTime());
            update.setVisitType("VR"); // 修改带看方式
            update.setCustomerFeedback("已更新为VR带看");
            update.setIntentionLevel((byte) 3); // 提高意向级别
            return update;
        }).collect(Collectors.toList());

        // 执行批量更新
        boolean result = visitRecordService.updateBatchVisitRecord(toUpdate);

        // 验证
        assertTrue(result);

        // 验证所有记录都已更新
        for (VisitRecord original : toSave) {
            VisitRecord updated = visitRecordService.getByIdWithTenant(original.getId(), TEST_TENANT_ID);
            assertNotNull(updated);
            assertEquals("VR", updated.getVisitType());
            assertEquals((byte) 3, updated.getIntentionLevel());
        }
    }

    @Test
    void updateBatchVisitRecord_DifferentTenants_ThrowsException() {
        // 先保存两条记录
        VisitRecord record1 = createVisitRecord(TEST_TENANT_ID, TEST_HOUSE_ID,
                TEST_CUSTOMER_ID, TEST_AGENT_ID);
        VisitRecord record2 = createVisitRecord(9999L, TEST_HOUSE_ID,
                TEST_CUSTOMER_ID, TEST_AGENT_ID); // 不同租户
        visitRecordService.saveVisitRecord(record1);
        visitRecordService.saveVisitRecord(record2);

        // 准备批量更新数据 - 包含不同租户的记录
        List<VisitRecord> toUpdate = new ArrayList<>();
        toUpdate.add(record1);
        toUpdate.add(record2);

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> visitRecordService.updateBatchVisitRecord(toUpdate)
        );

        assertEquals("批量更新的带看记录必须包含ID且属于同一租户", exception.getMessage());
    }

    @Test
    void removeBatchByIdsWithTenant_Success() {
        // 先批量保存一些记录
        List<VisitRecord> toSave = new ArrayList<>();
        List<Long> ids = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            VisitRecord record = createVisitRecord(TEST_TENANT_ID,
                    TEST_HOUSE_ID + i,
                    TEST_CUSTOMER_ID + i,
                    TEST_AGENT_ID);
            toSave.add(record);
        }
        visitRecordService.saveBatchVisitRecord(toSave);

        // 收集ID
        for (VisitRecord record : toSave) {
            ids.add(record.getId());
        }

        // 执行批量删除
        boolean result = visitRecordService.removeBatchByIdsWithTenant(ids, TEST_TENANT_ID);

        // 验证 - 应该返回true，表示成功删除了行
        assertTrue(result, "批量删除应返回true，表示成功删除了行");

        // 验证所有记录已被删除
        for (Long id : ids) {
            VisitRecord deleted = visitRecordService.getByIdWithTenant(id, TEST_TENANT_ID);
            assertNull(deleted, "批量删除后，所有数据应不存在");
        }
    }

    @Test
    void removeBatchByIdsWithTenant_EmptyList_ThrowsException() {
        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> visitRecordService.removeBatchByIdsWithTenant(new ArrayList<>(), TEST_TENANT_ID)
        );

        assertEquals("租户ID和记录ID列表不能为空", exception.getMessage());
    }

    @Test
    void removeBatchByIdsWithTenant_NullTenantId_ThrowsException() {
        // 准备ID列表
        List<Long> ids = List.of(1L, 2L, 3L);

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> visitRecordService.removeBatchByIdsWithTenant(ids, null)
        );

        assertEquals("租户ID和记录ID列表不能为空", exception.getMessage());
    }

    /**
     * 创建测试用的带看记录
     */
    private VisitRecord createVisitRecord(Long tenantId, Long houseId, Long customerId, Long agentId) {
        VisitRecord record = new VisitRecord();
        record.setTenantId(tenantId);
        record.setHouseId(houseId);
        record.setCustomerId(customerId);
        record.setAgentId(agentId);
        record.setVisitTime(LocalDateTime.now());
        record.setVisitType("OFFLINE");
        record.setCustomerFeedback("客户对房源表示满意，尤其喜欢户型和采光");
        record.setIntentionLevel((byte) 2); // 中等意向
        record.setContractId(null); // 未关联合同
        return record;
    }
}
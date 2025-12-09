package com.house.deed.pavilion;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.house.deed.pavilion.entity.OperationLog;
import com.house.deed.pavilion.mapper.OperationLogMapper;
import com.house.deed.pavilion.service.OperationLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@DisplayName("OperationLogServiceImpl 单元测试")
public class OperationLogServiceImplTest {

    @MockBean
    private OperationLogMapper operationLogMapper;

    @Autowired
    private OperationLogService operationLogService;

    private static final Long SYSTEM_TENANT_ID = 0L;
    private static final Long REGULAR_TENANT_ID = 1001L;
    private static final Long LOG_ID = 1L;
    private static final Long OPERATOR_ID = 2001L;

    @BeforeEach
    void setUp() {
        reset(operationLogMapper);
    }

    @Test
    @DisplayName("保存操作日志 - 系统级日志成功")
    void saveOperationLog_SystemLog_Success() {
        // Arrange
        OperationLog log = createValidOperationLog(SYSTEM_TENANT_ID);
        when(operationLogMapper.insert(any(OperationLog.class))).thenReturn(1);

        // Act
        boolean result = operationLogService.saveOperationLog(log);

        // Assert
        assertTrue(result);
        verify(operationLogMapper, times(1)).insert(any(OperationLog.class));
    }

    @Test
    @DisplayName("更新操作日志 - 修改操作人姓名成功")
    void updateOperationLogById_UpdateOperatorName_Success() {
        // Arrange
        Long logId = LOG_ID;
        Long tenantId = REGULAR_TENANT_ID;

        // Mock数据库中已存在的日志
        OperationLog existingLog = createValidOperationLog(tenantId);
        existingLog.setId(logId);
        existingLog.setOperatorName("原始操作人");

        when(operationLogMapper.selectOne(any(QueryWrapper.class)))
                .thenReturn(existingLog);

        when(operationLogMapper.update(any(OperationLog.class), any(QueryWrapper.class)))
                .thenReturn(1);

        // 创建更新对象
        OperationLog updateLog = new OperationLog();
        updateLog.setId(logId);
        updateLog.setTenantId(tenantId);
        updateLog.setOperatorName("新的操作人姓名");

        // Act
        boolean result = operationLogService.updateOperationLogById(updateLog);

        // Assert
        assertTrue(result);
        verify(operationLogMapper, times(1)).selectOne(any(QueryWrapper.class));
        verify(operationLogMapper, times(1)).update(any(OperationLog.class), any(QueryWrapper.class));
    }

    @Test
    @DisplayName("删除操作日志 - 系统级日志失败")
    void removeOperationLogById_SystemLog_Fails() {
        // Arrange
        Long logId = LOG_ID;
        OperationLog systemLog = createValidOperationLog(SYSTEM_TENANT_ID);
        systemLog.setId(logId);
        systemLog.setCreateTime(LocalDateTime.now().minusDays(10));

        when(operationLogMapper.selectOne(any(QueryWrapper.class)))
                .thenReturn(systemLog);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> operationLogService.removeOperationLogById(logId, SYSTEM_TENANT_ID));

        assertEquals("系统级操作日志禁止删除", exception.getMessage());
        verify(operationLogMapper, times(1)).selectOne(any(QueryWrapper.class));
    }

    @Test
    @DisplayName("删除操作日志 - 7天前日志成功")
    void removeOperationLogById_OldLog_Success() {
        // Arrange
        Long logId = LOG_ID;
        OperationLog oldLog = createValidOperationLog(REGULAR_TENANT_ID);
        oldLog.setId(logId);
        oldLog.setCreateTime(LocalDateTime.now().minusDays(10));

        when(operationLogMapper.selectOne(any(QueryWrapper.class)))
                .thenReturn(oldLog);
        when(operationLogMapper.deleteById(logId)).thenReturn(1);

        // Act
        boolean result = operationLogService.removeOperationLogById(logId, REGULAR_TENANT_ID);

        // Assert
        assertTrue(result);
        verify(operationLogMapper, times(1)).selectOne(any(QueryWrapper.class));
        verify(operationLogMapper, times(1)).deleteById(logId);
    }

    @Test
    @DisplayName("批量保存操作日志 - 租户不一致失败")
    void batchSaveOperationLogs_TenantInconsistent_Fails() {
        // Arrange
        List<OperationLog> logs = Arrays.asList(
                createValidOperationLog(REGULAR_TENANT_ID),
                createValidOperationLog(2002L) // 不同租户
        );

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> operationLogService.batchSaveOperationLogs(logs));

        assertTrue(exception.getMessage().contains("批量新增的日志必须属于同一租户/系统级"));
        verify(operationLogMapper, never()).insert(any(OperationLog.class));
    }

    @Test
    @DisplayName("批量删除操作日志 - 空ID列表失败")
    void batchRemoveOperationLogs_EmptyIds_Fails() {
        // Arrange
        LocalDateTime beforeTime = LocalDateTime.now().minusDays(10);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> operationLogService.batchRemoveOperationLogs(Collections.emptyList(), REGULAR_TENANT_ID, beforeTime));

        assertEquals("日志ID列表不能为空", exception.getMessage());
        verify(operationLogMapper, never()).selectList(any());
        verify(operationLogMapper, never()).deleteBatchIds(any());
    }

    @Test
    @DisplayName("批量删除操作日志 - 包含系统级日志失败")
    void batchRemoveOperationLogs_ContainsSystemLog_Fails() {
        // Arrange
        List<Long> ids = Arrays.asList(1L, 2L);
        LocalDateTime beforeTime = LocalDateTime.now().minusDays(10);

        // 创建两个不同的返回列表
        List<OperationLog> logsForValidation = Arrays.asList(
                createOperationLog(1L, REGULAR_TENANT_ID, null), // 验证时只需要id和tenant_id
                createOperationLog(2L, SYSTEM_TENANT_ID, null)
        );

        List<OperationLog> logsForDeletionCheck = Arrays.asList(
                createOperationLog(1L, REGULAR_TENANT_ID, beforeTime.minusDays(1)),
                createOperationLog(2L, SYSTEM_TENANT_ID, beforeTime.minusDays(2))
        );

        // Mock两次selectList调用，返回不同的结果
        when(operationLogMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(logsForValidation)  // 第一次调用：validateLogIdsBelongToTenant
                .thenReturn(logsForDeletionCheck); // 第二次调用：batchRemoveOperationLogs方法体

        // Act & Assert - 使用系统级租户（0）来调用
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> operationLogService.batchRemoveOperationLogs(ids, SYSTEM_TENANT_ID, beforeTime));

        System.out.println("异常信息: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("以下日志不允许删除"));
        assertTrue(exception.getMessage().contains("系统级/未到删除时间"));

        verify(operationLogMapper, times(2)).selectList(any(QueryWrapper.class));
        verify(operationLogMapper, never()).deleteBatchIds(any());
    }

    @Test
    @DisplayName("验证日志ID归属租户 - 租户不匹配失败")
    void validateLogIdsBelongToTenant_TenantMismatch_Fails() {
        // Arrange
        List<Long> ids = Arrays.asList(1L, 2L);

        List<OperationLog> logs = Arrays.asList(
                createOperationLog(1L, REGULAR_TENANT_ID, LocalDateTime.now()),
                createOperationLog(2L, 2002L, LocalDateTime.now()) // 不同租户
        );

        when(operationLogMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(logs);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> operationLogService.validateLogIdsBelongToTenant(REGULAR_TENANT_ID, ids));

        assertTrue(exception.getMessage().contains("无权限操作以下日志ID"));

        verify(operationLogMapper, times(1)).selectList(any(QueryWrapper.class));
    }

    @Test
    @DisplayName("验证日志ID归属租户 - 日志不存在失败")
    void validateLogIdsBelongToTenant_LogNotExists_Fails() {
        // Arrange
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        List<OperationLog> logs = Arrays.asList(
                createOperationLog(1L, REGULAR_TENANT_ID, LocalDateTime.now()),
                createOperationLog(2L, REGULAR_TENANT_ID, LocalDateTime.now())
                // 注意：这里只返回2个，不返回ID为3的日志
        );

        when(operationLogMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(logs);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> operationLogService.validateLogIdsBelongToTenant(REGULAR_TENANT_ID, ids));

        assertTrue(exception.getMessage().contains("以下日志ID不存在"));

        verify(operationLogMapper, times(1)).selectList(any(QueryWrapper.class));
    }

    // ==================== 辅助方法 ====================

    private OperationLog createValidOperationLog(Long tenantId) {
        OperationLog log = new OperationLog();
        log.setId(LOG_ID);
        log.setTenantId(tenantId);
        log.setModule("HOUSE_MANAGE");
        log.setOperationType("ADD");
        log.setOperationContent("新增房源");
        log.setOperatorId(OPERATOR_ID);
        log.setOperatorName("张三");
        log.setIpAddress("192.168.1.1");
        log.setCreateTime(LocalDateTime.now());
        return log;
    }

    private OperationLog createOperationLog(Long id, Long tenantId, LocalDateTime createTime) {
        OperationLog log = new OperationLog();
        log.setId(id);
        log.setTenantId(tenantId);
        log.setModule("HOUSE_MANAGE");
        log.setOperationType("ADD");
        log.setOperationContent("测试操作");
        log.setOperatorId(OPERATOR_ID);
        log.setOperatorName("测试用户");
        log.setIpAddress("192.168.1.1");
        log.setCreateTime(createTime);
        return log;
    }
}
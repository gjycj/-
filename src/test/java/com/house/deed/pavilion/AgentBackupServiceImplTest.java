package com.house.deed.pavilion;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.entity.Agent;
import com.house.deed.pavilion.entity.AgentBackup;
import com.house.deed.pavilion.mapper.AgentBackupMapper;
import com.house.deed.pavilion.service.AgentService;
import com.house.deed.pavilion.service.impl.AgentBackupServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentBackupServiceImplTest {

    @Mock
    private AgentBackupMapper agentBackupMapper;

    @Mock
    private AgentService agentService;

    @InjectMocks
    @Spy
    private AgentBackupServiceImpl agentBackupService;

    private AgentBackup testBackup;
    private List<AgentBackup> backupList;
    private Long tenantId = 1001L;
    private Long originalId = 1L;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        // 初始化测试数据
        testBackup = new AgentBackup();
        testBackup.setId(1L);
        testBackup.setOriginalId(originalId);
        testBackup.setTenantId(tenantId);
        testBackup.setStoreId(201L);
        testBackup.setAgentCode("BJ001");
        testBackup.setName("张三");
        testBackup.setPhone("13800138000");
        testBackup.setIdCard("330106199001011234");
        testBackup.setPosition("经纪人");
        testBackup.setLevel("SENIOR");
        testBackup.setEntryTime(LocalDate.of(2020, 1, 1));
        testBackup.setStatus((byte) 1);
        testBackup.setDeleteTime(LocalDateTime.now());
        testBackup.setDeleteOperator("系统管理员");

        backupList = new ArrayList<>(Arrays.asList(testBackup));

        // 手动设置 baseMapper
        setBaseMapper(agentBackupService, agentBackupMapper);
    }

    /**
     * 通过反射设置 baseMapper
     */
    private void setBaseMapper(AgentBackupServiceImpl service, AgentBackupMapper mapper)
            throws NoSuchFieldException, IllegalAccessException {
        Field baseMapperField = service.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(service, mapper);
    }

    /**
     * 测试分页查询：正常场景（带条件）
     */
    @Test
    void testPageQuery_Success() {
        // 准备参数
        Page<AgentBackup> page = new Page<>(1, 10);
        AgentBackup query = new AgentBackup();
        query.setTenantId(tenantId);
        query.setName("张三");
        query.setOriginalId(originalId);

        // 模拟查询结果
        IPage<AgentBackup> mockPage = new Page<>();
        mockPage.setRecords(backupList);
        when(agentBackupMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn((Page<AgentBackup>) mockPage);

        // 执行测试
        IPage<AgentBackup> result = agentBackupService.pageQuery(page, query);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals("张三", result.getRecords().get(0).getName());
        verify(agentBackupMapper).selectPage(any(Page.class), any(QueryWrapper.class));
    }

    /**
     * 测试分页查询：租户ID为空（异常场景）
     */
    @Test
    void testPageQuery_TenantIdNull() {
        // 准备参数（租户ID为空）
        Page<AgentBackup> page = new Page<>(1, 10);
        AgentBackup query = new AgentBackup();

        // 验证异常
        assertThrows(IllegalArgumentException.class, () -> agentBackupService.pageQuery(page, query),
                "预期抛出租户ID不能为空的异常");
    }

    /**
     * 测试批量创建：正常场景 - 使用 doReturn 避免调用真实方法
     */
    @Test
    void testBatchCreate_Success() {
        // 模拟 saveBatch 返回成功
        doReturn(true).when(agentBackupService).saveBatch(backupList);

        boolean result = agentBackupService.batchCreate(backupList);
        assertTrue(result);
        verify(agentBackupService).saveBatch(backupList);
    }

    /**
     * 测试批量创建：租户ID不一致（异常场景）
     */
    @Test
    void testBatchCreate_TenantIdMismatch() {
        // 构造租户ID不一致的列表
        AgentBackup invalidBackup = new AgentBackup();
        BeanUtils.copyProperties(testBackup, invalidBackup);
        invalidBackup.setTenantId(999L); // 不同租户ID

        List<AgentBackup> invalidList = new ArrayList<>();
        invalidList.add(testBackup);
        invalidList.add(invalidBackup);

        assertThrows(IllegalArgumentException.class, () -> agentBackupService.batchCreate(invalidList),
                "预期抛出租户ID不一致的异常");
    }

    /**
     * 测试批量创建：空列表（边界场景）
     */
    @Test
    void testBatchCreate_EmptyList() {
        boolean result = agentBackupService.batchCreate(new ArrayList<>());
        assertTrue(result); // 空列表应返回成功（无操作视为成功）

        // 验证：空列表时不会调用批量插入方法
        verify(agentBackupService, never()).saveBatch(anyList());
    }

    /**
     * 测试根据原ID查询：正常场景
     */
    @Test
    void testGetByOriginalIds_Success() {
        List<Long> originalIds = Arrays.asList(originalId);
        when(agentBackupMapper.selectList(any(QueryWrapper.class))).thenReturn(backupList);

        List<AgentBackup> result = agentBackupService.getByOriginalIds(originalIds, tenantId);
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(agentBackupMapper).selectList(any(QueryWrapper.class));
    }

    /**
     * 测试根据原ID查询：原ID列表为空（异常场景）
     */
    @Test
    void testGetByOriginalIds_EmptyOriginalIds() {
        assertThrows(IllegalArgumentException.class,
                () -> agentBackupService.getByOriginalIds(new ArrayList<>(), tenantId),
                "预期抛原ID列表不能为空的异常");
    }

    /**
     * 测试恢复备份：正常场景 - 关键修复：模拟 removeById 方法
     */
    @Test
    void testRestore_Success() {
        // 模拟查询备份记录
        when(agentBackupMapper.selectOne(any(QueryWrapper.class), anyBoolean())).thenReturn(testBackup);

        // 模拟保存到主表成功
        when(agentService.save(any(Agent.class))).thenReturn(true);

        // 关键修复：模拟 removeById 方法，避免 TableInfo 初始化问题
        doReturn(true).when(agentBackupService).removeById(anyLong());

        boolean result = agentBackupService.restore(originalId, tenantId);
        assertTrue(result);
        verify(agentService).save(any(Agent.class));
        verify(agentBackupService).removeById(testBackup.getId());
    }

    /**
     * 测试恢复备份：备份记录不存在（异常场景）
     */
    @Test
    void testRestore_BackupNotFound() {
        // 模拟查询备份记录返回null
        when(agentBackupMapper.selectOne(any(QueryWrapper.class), anyBoolean())).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> agentBackupService.restore(originalId, tenantId),
                "预期抛出未找到备份记录的异常");
        verify(agentService, never()).save(any(Agent.class));
        verify(agentBackupMapper, never()).deleteById(anyLong());
    }

    /**
     * 测试批量删除：正常场景
     */
    @Test
    void testBatchDelete_Success() {
        List<Long> ids = Arrays.asList(1L);
        when(agentBackupMapper.delete(any(QueryWrapper.class))).thenReturn(1);

        boolean result = agentBackupService.batchDelete(ids, tenantId);
        assertTrue(result);
        verify(agentBackupMapper).delete(any(QueryWrapper.class));
    }

    /**
     * 测试批量删除：ID列表为空（异常场景）
     */
    @Test
    void testBatchDelete_EmptyIds() {
        assertThrows(IllegalArgumentException.class,
                () -> agentBackupService.batchDelete(new ArrayList<>(), tenantId),
                "预期抛出ID列表不能为空的异常");
    }
}
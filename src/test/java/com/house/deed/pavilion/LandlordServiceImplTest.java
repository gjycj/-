package com.house.deed.pavilion;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.entity.Landlord;
import com.house.deed.pavilion.mapper.LandlordMapper;
import com.house.deed.pavilion.service.impl.LandlordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LandlordServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("房东信息服务测试")
class LandlordServiceImplTest {

    @Mock
    private LandlordMapper landlordMapper;

    @InjectMocks
    private LandlordServiceImpl landlordService;

    private Landlord testLandlord;
    private static final Long TEST_TENANT_ID = 1001L;
    private static final Long TEST_LANDLORD_ID = 1L;
    private static final String TEST_PHONE = "13800138000";
    private static final String TEST_ID_CARD = "44030119900101001X";
    private static final String TEST_NAME = "张三";
    private static final String TEST_ADDRESS = "北京市朝阳区";

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testLandlord = new Landlord();
        testLandlord.setId(TEST_LANDLORD_ID);
        testLandlord.setTenantId(TEST_TENANT_ID);
        testLandlord.setName(TEST_NAME);
        testLandlord.setPhone(TEST_PHONE);
        testLandlord.setIdCard(TEST_ID_CARD);
        testLandlord.setAddress(TEST_ADDRESS);
        testLandlord.setCreateAgentId(100L);
        testLandlord.setCreateTime(LocalDateTime.now());

        // 手动设置 baseMapper
        ReflectionTestUtils.setField(landlordService, "baseMapper", landlordMapper);
    }

    @Test
    @DisplayName("新增房东 - 成功")
    void saveLandlord_Success() {
        // 准备
        when(landlordMapper.selectCount(any())).thenReturn(0L);
        when(landlordMapper.insert(any(Landlord.class))).thenReturn(1);

        // 执行
        boolean result = landlordService.saveLandlord(testLandlord);

        // 验证
        assertTrue(result);
        verify(landlordMapper, atLeastOnce()).selectCount(any());
        verify(landlordMapper).insert(testLandlord);
    }

    @Test
    @DisplayName("新增房东 - 手机号已存在")
    void saveLandlord_PhoneExists_ShouldThrowException() {
        // 准备
        when(landlordMapper.selectCount(any())).thenReturn(1L);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> landlordService.saveLandlord(testLandlord)
        );
        assertTrue(exception.getMessage().contains("手机号已存在"));
    }

    @Test
    @DisplayName("新增房东 - 身份证号已存在")
    void saveLandlord_IdCardExists_ShouldThrowException() {
        // 准备
        when(landlordMapper.selectCount(any()))
                .thenReturn(0L)  // 第一次调用（手机号校验）
                .thenReturn(1L); // 第二次调用（身份证号校验）

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> landlordService.saveLandlord(testLandlord)
        );
        assertTrue(exception.getMessage().contains("身份证号已存在"));
    }

    @Test
    @DisplayName("更新房东 - 成功")
    void updateLandlordById_Success() {
        // 准备
        Landlord updateRequest = new Landlord();
        updateRequest.setId(TEST_LANDLORD_ID);
        updateRequest.setTenantId(TEST_TENANT_ID);
        updateRequest.setName("李四");
        updateRequest.setPhone("13900139000");
        updateRequest.setIdCard("44030119900101002X");
        updateRequest.setAddress("上海市浦东新区");

        when(landlordMapper.selectById(TEST_LANDLORD_ID)).thenReturn(testLandlord);
        when(landlordMapper.selectCount(any())).thenReturn(0L);
        when(landlordMapper.updateById(any(Landlord.class))).thenReturn(1);

        // 执行
        boolean result = landlordService.updateLandlordById(updateRequest);

        // 验证
        assertTrue(result);
        verify(landlordMapper).selectById(TEST_LANDLORD_ID);
        verify(landlordMapper, atLeastOnce()).selectCount(any());
        verify(landlordMapper).updateById(any(Landlord.class));
    }

    @Test
    @DisplayName("更新房东 - 记录不存在")
    void updateLandlordById_NotFound_ShouldThrowException() {
        // 准备
        Landlord updateRequest = new Landlord();
        updateRequest.setId(999L);
        updateRequest.setTenantId(TEST_TENANT_ID);
        updateRequest.setPhone("13900139000");

        when(landlordMapper.selectById(999L)).thenReturn(null);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> landlordService.updateLandlordById(updateRequest)
        );
        assertEquals("房东不存在或无权限操作", exception.getMessage());
    }

    @Test
    @DisplayName("更新房东 - 租户不一致")
    void updateLandlordById_TenantMismatch_ShouldThrowException() {
        // 准备
        Landlord updateRequest = new Landlord();
        updateRequest.setId(TEST_LANDLORD_ID);
        updateRequest.setTenantId(9999L); // 不同的租户ID

        testLandlord.setTenantId(TEST_TENANT_ID); // 原有租户
        when(landlordMapper.selectById(TEST_LANDLORD_ID)).thenReturn(testLandlord);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> landlordService.updateLandlordById(updateRequest)
        );
        assertEquals("房东不存在或无权限操作", exception.getMessage());
    }

    @Test
    @DisplayName("更新房东 - 手机号冲突")
    void updateLandlordById_PhoneConflict_ShouldThrowException() {
        // 准备
        Landlord updateRequest = new Landlord();
        updateRequest.setId(TEST_LANDLORD_ID);
        updateRequest.setTenantId(TEST_TENANT_ID);
        updateRequest.setPhone("13900139000"); // 新的手机号

        when(landlordMapper.selectById(TEST_LANDLORD_ID)).thenReturn(testLandlord);
        when(landlordMapper.selectCount(any())).thenReturn(1L);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> landlordService.updateLandlordById(updateRequest)
        );
        assertTrue(exception.getMessage().contains("新手机号已存在"));
    }

    @Test
    @DisplayName("删除房东 - 成功")
    void removeLandlordById_Success() {
        // 准备
        when(landlordMapper.selectById(TEST_LANDLORD_ID)).thenReturn(testLandlord);
        when(landlordMapper.deleteById(TEST_LANDLORD_ID)).thenReturn(1);

        // 执行
        boolean result = landlordService.removeLandlordById(TEST_LANDLORD_ID, TEST_TENANT_ID);

        // 验证
        assertTrue(result);
        verify(landlordMapper).selectById(TEST_LANDLORD_ID);
        verify(landlordMapper).deleteById(TEST_LANDLORD_ID);
    }

    @Test
    @DisplayName("删除房东 - 记录不存在")
    void removeLandlordById_NotFound_ShouldThrowException() {
        // 准备
        when(landlordMapper.selectById(TEST_LANDLORD_ID)).thenReturn(null);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> landlordService.removeLandlordById(TEST_LANDLORD_ID, TEST_TENANT_ID)
        );
        assertEquals("房东不存在或无权限操作", exception.getMessage());
    }

    @Test
    @DisplayName("根据ID查询房东 - 成功")
    void getLandlordById_Success() {
        // 准备
        when(landlordMapper.selectOne(any())).thenReturn(testLandlord);

        // 执行
        Landlord result = landlordService.getLandlordById(TEST_LANDLORD_ID, TEST_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertEquals(TEST_LANDLORD_ID, result.getId());
        assertEquals(TEST_TENANT_ID, result.getTenantId());
    }

    @Test
    @DisplayName("根据ID查询房东 - 记录不存在")
    void getLandlordById_NotFound() {
        // 准备
        when(landlordMapper.selectOne(any())).thenReturn(null);

        // 执行
        Landlord result = landlordService.getLandlordById(TEST_LANDLORD_ID, TEST_TENANT_ID);

        // 验证
        assertNull(result);
    }

    @Test
    @DisplayName("多条件分页查询 - 成功")
    void pageQuery_Success() {
        // 准备
        Page<Landlord> page = new Page<>(1, 10);
        Landlord query = new Landlord();
        query.setName("张");
        query.setPhone(TEST_PHONE);

        Page<Landlord> expectedPage = new Page<>(1, 10);
        expectedPage.setRecords(Collections.singletonList(testLandlord));
        expectedPage.setTotal(1);

        when(landlordMapper.selectPage(eq(page), any())).thenReturn(expectedPage);

        // 执行
        IPage<Landlord> result = landlordService.pageQuery(page, query, TEST_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(TEST_NAME, result.getRecords().get(0).getName());
        verify(landlordMapper).selectPage(eq(page), any());
    }

    @Test
    @DisplayName("批量新增房东 - 成功")
    void batchSaveLandlords_Success() {
        // 准备
        Landlord landlord1 = createTestLandlord();
        landlord1.setPhone("13800138001");
        landlord1.setIdCard("44030119900101002X");

        Landlord landlord2 = createTestLandlord();
        landlord2.setPhone("13800138002");
        landlord2.setIdCard("44030119900101003X");

        List<Landlord> landlords = Arrays.asList(landlord1, landlord2);

        when(landlordMapper.selectCount(any())).thenReturn(0L);

        // 创建 landlordService 的 spy 版本
        LandlordServiceImpl spyService = Mockito.spy(landlordService);

        // 模拟 saveBatch 方法返回 true
        doReturn(true).when(spyService).saveBatch(landlords);

        // 执行
        boolean result = spyService.batchSaveLandlords(landlords);

        // 验证
        assertTrue(result);
        verify(landlordMapper, times(4)).selectCount(any());
        verify(spyService).saveBatch(landlords);
    }

    @Test
    @DisplayName("批量新增房东 - 列表为空")
    void batchSaveLandlords_EmptyList() {
        // 执行
        boolean result = landlordService.batchSaveLandlords(Collections.emptyList());

        // 验证
        assertFalse(result);
        verify(landlordMapper, never()).selectCount(any());
    }

    @Test
    @DisplayName("批量新增房东 - 手机号冲突")
    void batchSaveLandlords_PhoneConflict_ShouldThrowException() {
        // 准备
        Landlord landlord1 = createTestLandlord();
        landlord1.setPhone(TEST_PHONE);
        landlord1.setIdCard("44030119900101002X");

        List<Landlord> landlords = Collections.singletonList(landlord1);

        when(landlordMapper.selectCount(any())).thenReturn(1L);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> landlordService.batchSaveLandlords(landlords)
        );
        assertTrue(exception.getMessage().contains("手机号已存在"));
    }

    @Test
    @DisplayName("批量删除房东 - 部分ID不存在")
    void batchRemoveLandlords_SomeIdsNotExist_ShouldThrowException() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 只返回一个房东，模拟部分ID不存在的情况
        when(landlordMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(Collections.singletonList(testLandlord));

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> landlordService.batchRemoveLandlords(ids, TEST_TENANT_ID)
        );
        assertTrue(exception.getMessage().contains("房东ID不存在"));

        // 验证未调用删除方法
        verify(landlordMapper, never()).deleteBatchIds(anyList());
    }

    @Test
    @DisplayName("批量删除房东 - 租户权限不符")
    void batchRemoveLandlords_TenantMismatch_ShouldThrowException() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L);

        // 创建属于其他租户的房东
        Landlord landlord1 = createLandlordWithId(1L, 9999L); // 不同租户
        Landlord landlord2 = createLandlordWithId(2L, 9999L); // 不同租户

        when(landlordMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(Arrays.asList(landlord1, landlord2));

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> landlordService.batchRemoveLandlords(ids, TEST_TENANT_ID)
        );
        assertTrue(exception.getMessage().contains("无权限操作房东ID"));

        // 验证未调用删除方法
        verify(landlordMapper, never()).deleteBatchIds(anyList());
    }

    @Test
    @DisplayName("批量删除房东 - 成功")
    void batchRemoveLandlords_Success() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 创建属于同一租户的房东列表
        List<Landlord> landlords = Arrays.asList(
                createLandlordWithId(1L, TEST_TENANT_ID),
                createLandlordWithId(2L, TEST_TENANT_ID),
                createLandlordWithId(3L, TEST_TENANT_ID)
        );

        when(landlordMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(landlords);

        when(landlordMapper.deleteBatchIds(ids)).thenReturn(3);

        // 执行
        boolean result = landlordService.batchRemoveLandlords(ids, TEST_TENANT_ID);

        // 验证
        assertTrue(result);
        verify(landlordMapper).selectList(any(QueryWrapper.class));
        verify(landlordMapper).deleteBatchIds(ids);
    }

    // 辅助方法
    private Landlord createTestLandlord() {
        Landlord landlord = new Landlord();
        landlord.setTenantId(TEST_TENANT_ID);
        landlord.setName(TEST_NAME);
        landlord.setPhone(TEST_PHONE);
        landlord.setIdCard(TEST_ID_CARD);
        landlord.setAddress(TEST_ADDRESS);
        landlord.setCreateAgentId(100L);
        return landlord;
    }

    // 辅助方法：创建指定ID的房东对象
    private Landlord createLandlordWithId(Long id, Long tenantId) {
        Landlord landlord = new Landlord();
        landlord.setId(id);
        landlord.setTenantId(tenantId);
        landlord.setName("测试房东" + id);
        landlord.setPhone("1380013800" + id);
        landlord.setIdCard("4403011990010100" + id + "X");
        landlord.setAddress("测试地址" + id);
        return landlord;
    }
}
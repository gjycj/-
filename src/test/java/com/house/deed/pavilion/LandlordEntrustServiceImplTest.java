package com.house.deed.pavilion;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.entity.LandlordEntrust;
import com.house.deed.pavilion.mapper.LandlordEntrustMapper;
import com.house.deed.pavilion.service.impl.LandlordEntrustServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LandlordEntrustServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("房东委托信息服务测试")
@MockitoSettings(strictness = Strictness.LENIENT)  // 设置为宽松模式
class LandlordEntrustServiceImplTest {

    @Mock
    private LandlordEntrustMapper landlordEntrustMapper;

    @InjectMocks
    @Spy
    private LandlordEntrustServiceImpl landlordEntrustService;

    private LandlordEntrust mockEntrust;
    private final Long TEST_TENANT_ID = 1001L;
    private final Long TEST_RECORD_ID = 1L;
    private final Long TEST_HOUSE_ID = 101L;
    private final Long TEST_LANDLORD_ID = 201L;
    private final String TEST_ENTRUST_TYPE = "FULL_MANAGEMENT";
    private static final String TEST_ENTRUST_TYPE_EXCLUSIVE = "EXCLUSIVE";
    private static final String TEST_ENTRUST_TYPE_NON_EXCLUSIVE = "NON_EXCLUSIVE";
    private final LocalDate TODAY = LocalDate.now();
    private final LocalDate TOMORROW = LocalDate.now().plusDays(1);
    private final LocalDate NEXT_WEEK = LocalDate.now().plusDays(7);

    @BeforeEach
    void setUp() {
        // 准备测试数据
        mockEntrust = mock(LandlordEntrust.class);
        when(mockEntrust.getId()).thenReturn(TEST_RECORD_ID);
        when(mockEntrust.getTenantId()).thenReturn(TEST_TENANT_ID);
        when(mockEntrust.getLandlordId()).thenReturn(TEST_LANDLORD_ID);
        when(mockEntrust.getHouseId()).thenReturn(TEST_HOUSE_ID);
        when(mockEntrust.getEntrustType()).thenReturn(TEST_ENTRUST_TYPE_EXCLUSIVE);
        when(mockEntrust.getEntrustStartTime()).thenReturn(TODAY);
        when(mockEntrust.getEntrustEndTime()).thenReturn(NEXT_WEEK);
        when(mockEntrust.getRenewRemind()).thenReturn((byte) 1);
        when(mockEntrust.getStatus()).thenReturn((byte) 1);
        when(mockEntrust.getRemark()).thenReturn("测试备注");

        // 手动设置 baseMapper
        ReflectionTestUtils.setField(landlordEntrustService, "baseMapper", landlordEntrustMapper);

        // 统一设置 selectOne 的模拟，避免每个测试重复设置
        Mockito.lenient()
                .when(landlordEntrustMapper.selectOne(any(QueryWrapper.class), anyBoolean()))
                .thenReturn(mockEntrust);

        Mockito.lenient()
                .when(landlordEntrustMapper.selectOne(any(QueryWrapper.class)))
                .thenReturn(mockEntrust);
    }

    @Test
    @DisplayName("新增房东委托记录 - 成功")
    void saveEntrust_Success() {
        // 准备
        when(landlordEntrustMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        when(landlordEntrustMapper.insert(any(LandlordEntrust.class))).thenReturn(1);

        // 执行
        boolean result = landlordEntrustService.saveEntrust(mockEntrust);

        // 验证
        assertTrue(result);
        verify(landlordEntrustMapper).selectCount(any(QueryWrapper.class));
        verify(landlordEntrustMapper).insert(mockEntrust);
    }

    @Test
    @DisplayName("新增房东委托记录 - 房源委托时间冲突")
    void saveEntrust_HouseEntrustConflict_ShouldThrowException() {
        // 准备
        when(landlordEntrustMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> landlordEntrustService.saveEntrust(mockEntrust)
        );
        assertTrue(exception.getMessage().contains("已存在有效委托"));
        verify(landlordEntrustMapper, never()).insert(any(LandlordEntrust.class));
    }

    @Test
    @DisplayName("新增房东委托记录 - 参数校验：租户ID为空")
    void saveEntrust_TenantIdNull_ShouldThrowException() {
        // 准备
        LandlordEntrust entrust = createTestEntrust();
        entrust.setTenantId(null);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> landlordEntrustService.saveEntrust(entrust)
        );
        assertEquals("租户ID不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("新增房东委托记录 - 参数校验：房东ID为空")
    void saveEntrust_LandlordIdNull_ShouldThrowException() {
        // 准备
        LandlordEntrust entrust = createTestEntrust();
        entrust.setLandlordId(null);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> landlordEntrustService.saveEntrust(entrust)
        );
        assertEquals("房东ID不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("新增房东委托记录 - 参数校验：房源ID为空")
    void saveEntrust_HouseIdNull_ShouldThrowException() {
        // 准备
        LandlordEntrust entrust = createTestEntrust();
        entrust.setHouseId(null);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> landlordEntrustService.saveEntrust(entrust)
        );
        assertEquals("房源ID不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("新增房东委托记录 - 参数校验：委托类型为空")
    void saveEntrust_EntrustTypeEmpty_ShouldThrowException() {
        // 准备
        LandlordEntrust entrust = createTestEntrust();
        entrust.setEntrustType("");

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> landlordEntrustService.saveEntrust(entrust)
        );
        assertEquals("委托类型不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("新增房东委托记录 - 参数校验：开始时间晚于结束时间")
    void saveEntrust_StartTimeAfterEndTime_ShouldThrowException() {
        // 准备
        LandlordEntrust entrust = createTestEntrust();
        entrust.setEntrustStartTime(NEXT_WEEK);
        entrust.setEntrustEndTime(TODAY);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> landlordEntrustService.saveEntrust(entrust)
        );
        assertEquals("委托结束时间不能早于开始时间", exception.getMessage());
    }

    @Test
    @DisplayName("按ID查询委托记录 - 成功")
    void getById_Success() {
        // 准备 - 已在上面的 setUp 方法中统一设置

        // 执行
        LandlordEntrust result = landlordEntrustService.getById(TEST_RECORD_ID, TEST_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertEquals(TEST_RECORD_ID, result.getId());
        assertEquals(TEST_TENANT_ID, result.getTenantId());
    }

    @Test
    @DisplayName("按ID查询委托记录 - ID为空")
    void getById_IdNull_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> landlordEntrustService.getById(null, TEST_TENANT_ID));
    }

    @Test
    @DisplayName("按ID查询委托记录 - 租户ID为空")
    void getById_TenantIdNull_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> landlordEntrustService.getById(TEST_RECORD_ID, null));
    }

    @Test
    @DisplayName("更新委托记录 - 成功（修改时间触发冲突校验）")
    void updateEntrust_Success_WithTimeChange() {
        LandlordEntrust updateRequest = new LandlordEntrust();
        updateRequest.setId(mockEntrust.getId());
        updateRequest.setTenantId(mockEntrust.getTenantId());
        updateRequest.setEntrustType("PARTIAL_MANAGEMENT");
        updateRequest.setStatus((byte) 0);
        updateRequest.setLandlordId(mockEntrust.getLandlordId());
        updateRequest.setHouseId(mockEntrust.getHouseId());
        // 修改时间字段，以触发冲突校验逻辑
        updateRequest.setEntrustStartTime(mockEntrust.getEntrustStartTime().plusDays(1));
        updateRequest.setEntrustEndTime(mockEntrust.getEntrustEndTime().plusDays(1));
        updateRequest.setRenewRemind(mockEntrust.getRenewRemind());

        // 此时，selectCount 的存根就会被使用
        when(landlordEntrustMapper.selectOne(any(QueryWrapper.class), anyBoolean())).thenReturn(mockEntrust);
        when(landlordEntrustMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L); // 这个存根现在必要了
        when(landlordEntrustMapper.updateById(any(LandlordEntrust.class))).thenReturn(1);

        boolean result = landlordEntrustService.updateEntrust(updateRequest);
        assertTrue(result);
        verify(landlordEntrustMapper).updateById(any(LandlordEntrust.class));
        verify(landlordEntrustMapper).selectCount(any(QueryWrapper.class)); // 可验证被调用
    }

    @Test
    @DisplayName("更新委托记录 - 记录不存在")
    void updateEntrust_NotFound_ShouldThrowException() {
        // 准备一个完整的更新请求（但记录在DB中不存在）
        LandlordEntrust updateRequest = new LandlordEntrust();
        // 1. 主键和租户ID
        updateRequest.setId(TEST_RECORD_ID);
        updateRequest.setTenantId(TEST_TENANT_ID);
        // 2. 【关键】补全业务校验所需的必填字段（值可以复用mockEntrust或使用其他合法值）
        updateRequest.setLandlordId(mockEntrust.getLandlordId());
        updateRequest.setHouseId(mockEntrust.getHouseId());
        updateRequest.setEntrustType(mockEntrust.getEntrustType());
        updateRequest.setEntrustStartTime(mockEntrust.getEntrustStartTime());
        updateRequest.setEntrustEndTime(mockEntrust.getEntrustEndTime());
        updateRequest.setRenewRemind(mockEntrust.getRenewRemind());
        updateRequest.setStatus(mockEntrust.getStatus());

        // 修正：模拟 `selectOne` 方法返回null，以模拟“记录不存在”的场景
        // 注意：这里必须匹配两个参数 (QueryWrapper, boolean)
        when(landlordEntrustMapper.selectOne(any(QueryWrapper.class), anyBoolean())).thenReturn(null);

        // 执行 & 验证：应抛出“记录不存在”的异常
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> landlordEntrustService.updateEntrust(updateRequest)
        );
        assertEquals("委托记录不存在或不属于当前租户", exception.getMessage());
    }

    @Test
    @DisplayName("更新委托记录 - 修改时间范围导致冲突")
    void updateEntrust_TimeConflict_ShouldThrowException() {
        // 准备
        LandlordEntrust updateRequest = new LandlordEntrust();
        updateRequest.setId(TEST_RECORD_ID);
        updateRequest.setTenantId(TEST_TENANT_ID);
        updateRequest.setHouseId(TEST_HOUSE_ID);
        updateRequest.setLandlordId(TEST_LANDLORD_ID);
        updateRequest.setEntrustType(TEST_ENTRUST_TYPE_EXCLUSIVE);
        updateRequest.setEntrustStartTime(TODAY.plusDays(1));
        updateRequest.setEntrustEndTime(NEXT_WEEK.plusDays(1));
        updateRequest.setRenewRemind((byte) 1);
        updateRequest.setStatus((byte) 1);
        updateRequest.setRemark("更新备注");

        // 模拟冲突检查返回 1（有冲突）
        when(landlordEntrustMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        // 模拟 updateById 方法
        when(landlordEntrustMapper.updateById(any(LandlordEntrust.class))).thenReturn(1);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> landlordEntrustService.updateEntrust(updateRequest)
        );

        System.out.println("实际异常信息: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("已存在有效委托"));
    }

    @Test
    @DisplayName("删除委托记录 - 成功")
    void removeEntrust_Success() {
        // 准备
        // 关键修正：将 selectOne 的存根改为匹配两个参数 (QueryWrapper, boolean)
        when(landlordEntrustMapper.selectOne(any(QueryWrapper.class), anyBoolean())).thenReturn(mockEntrust);

        // 使用 doReturn-when 模式来模拟 removeById，避免潜在的 TableInfo 问题
        doReturn(true).when(landlordEntrustService).removeById(TEST_RECORD_ID);

        // 执行
        boolean result = landlordEntrustService.removeEntrust(TEST_RECORD_ID, TEST_TENANT_ID);

        // 验证
        assertTrue(result);
        verify(landlordEntrustService).removeById(TEST_RECORD_ID);
        // 可选：验证 selectOne 确实被调用了
        verify(landlordEntrustMapper).selectOne(any(QueryWrapper.class), anyBoolean());
    }

    @Test
    @DisplayName("分页多条件查询 - 成功")
    void pageQuery_Success() {
        // 准备
        Page<LandlordEntrust> page = new Page<>(1, 10);
        LandlordEntrust query = new LandlordEntrust();
        query.setHouseId(TEST_HOUSE_ID);

        Page<LandlordEntrust> expectedPage = new Page<>(1, 10);
        expectedPage.setRecords(Collections.singletonList(mockEntrust));
        expectedPage.setTotal(1);

        when(landlordEntrustMapper.selectPage(eq(page), any(QueryWrapper.class))).thenReturn(expectedPage);

        // 执行
        IPage<LandlordEntrust> result = landlordEntrustService.pageQuery(page, query, TEST_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        verify(landlordEntrustMapper).selectPage(eq(page), any(QueryWrapper.class));
    }

    @Test
    @DisplayName("多条件列表查询 - 成功")
    void listByConditions_Success() {
        // 准备
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("houseId", TEST_HOUSE_ID);

        List<LandlordEntrust> expectedList = Arrays.asList(mockEntrust);
        doReturn(expectedList).when(landlordEntrustService).list(any(QueryWrapper.class));

        // 执行
        List<LandlordEntrust> result = landlordEntrustService.listByConditions(queryParams, TEST_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("按房源ID查询委托记录 - 成功")
    void listByHouseId_Success() {
        // 准备
        List<LandlordEntrust> expectedList = Arrays.asList(mockEntrust);
        doReturn(expectedList).when(landlordEntrustService).list(any(QueryWrapper.class));

        // 执行
        List<LandlordEntrust> result = landlordEntrustService.listByHouseId(TEST_HOUSE_ID, TEST_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("按房东ID查询委托记录 - 成功")
    void listByLandlordId_Success() {
        // 准备
        List<LandlordEntrust> expectedList = Arrays.asList(mockEntrust);
        doReturn(expectedList).when(landlordEntrustService).list(any(QueryWrapper.class));

        // 执行
        List<LandlordEntrust> result = landlordEntrustService.listByLandlordId(TEST_LANDLORD_ID, TEST_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("批量创建委托记录 - 成功")
    void batchCreate_Success() {
        // 准备
        LandlordEntrust entrust1 = createTestEntrust();
        LandlordEntrust entrust2 = createTestEntrust();
        entrust2.setHouseId(102L);
        List<LandlordEntrust> entrustList = Arrays.asList(entrust1, entrust2);

        when(landlordEntrustMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        doReturn(true).when(landlordEntrustService).saveBatch(entrustList);

        // 执行
        boolean result = landlordEntrustService.batchCreate(entrustList);

        // 验证
        assertTrue(result);
        verify(landlordEntrustMapper, times(2)).selectCount(any(QueryWrapper.class));
        verify(landlordEntrustService).saveBatch(entrustList);
    }

    @Test
    @DisplayName("批量创建委托记录 - 租户不一致")
    void batchCreate_DifferentTenants_ShouldThrowException() {
        // 准备
        LandlordEntrust entrust1 = createTestEntrust();
        LandlordEntrust entrust2 = createTestEntrust();
        entrust2.setTenantId(1002L); // 不同租户
        List<LandlordEntrust> entrustList = Arrays.asList(entrust1, entrust2);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> landlordEntrustService.batchCreate(entrustList)
        );
        assertEquals("批量创建的记录必须属于同一租户", exception.getMessage());
    }

    @Test
    @DisplayName("批量创建委托记录 - 列表为空")
    void batchCreate_EmptyList_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> landlordEntrustService.batchCreate(Collections.emptyList()));
    }

    @Test
    @DisplayName("批量删除委托记录 - 成功")
    void batchRemove_Success() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        when(landlordEntrustMapper.selectCount(any(QueryWrapper.class))).thenReturn((long) ids.size());
        doReturn(true).when(landlordEntrustService).removeByIds(ids);

        // 执行
        boolean result = landlordEntrustService.batchRemove(ids, TEST_TENANT_ID);

        // 验证
        assertTrue(result);
        verify(landlordEntrustMapper).selectCount(any(QueryWrapper.class));
        verify(landlordEntrustService).removeByIds(ids);
    }

    @Test
    @DisplayName("批量删除委托记录 - 存在不属于当前租户的记录")
    void batchRemove_ContainsOtherTenant_ShouldThrowException() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L);

        when(landlordEntrustMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> landlordEntrustService.batchRemove(ids, TEST_TENANT_ID)
        );
        assertEquals("存在无效的记录ID或不属于当前租户的记录", exception.getMessage());
    }

    @Test
    @DisplayName("批量更新委托状态 - 成功")
    void batchUpdateStatus_Success() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        Byte newStatus = (byte) 0;

        when(landlordEntrustMapper.selectCount(any(QueryWrapper.class))).thenReturn((long) ids.size());
        doReturn(true).when(landlordEntrustService).update(any(LandlordEntrust.class), any(QueryWrapper.class));

        // 执行
        boolean result = landlordEntrustService.batchUpdateStatus(ids, newStatus, TEST_TENANT_ID);

        // 验证
        assertTrue(result);
        verify(landlordEntrustMapper).selectCount(any(QueryWrapper.class));
        verify(landlordEntrustService).update(any(LandlordEntrust.class), any(QueryWrapper.class));
    }

    @Test
    @DisplayName("批量更新委托状态 - 状态值非法")
    void batchUpdateStatus_InvalidStatus_ShouldThrowException() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L);
        Byte invalidStatus = (byte) 2;

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> landlordEntrustService.batchUpdateStatus(ids, invalidStatus, TEST_TENANT_ID)
        );
        assertEquals("状态只能是0（过期/取消）或1（有效）", exception.getMessage());
    }

    @Test
    @DisplayName("批量更新委托状态 - 状态为空")
    void batchUpdateStatus_StatusNull_ShouldThrowException() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L);

        // 执行 & 验证
        assertThrows(IllegalArgumentException.class,
                () -> landlordEntrustService.batchUpdateStatus(ids, null, TEST_TENANT_ID));
    }

    @Test
    @DisplayName("时间冲突校验 - 完全重叠")
    void validateHouseEntrustConflict_FullOverlap_ShouldThrowException() {
        // 准备
        LandlordEntrust newEntrust = createTestEntrust();
        newEntrust.setEntrustStartTime(TODAY.plusDays(1));
        newEntrust.setEntrustEndTime(NEXT_WEEK.minusDays(1));

        when(landlordEntrustMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> landlordEntrustService.saveEntrust(newEntrust)
        );
        assertTrue(exception.getMessage().contains("已存在有效委托"));
    }

    @Test
    @DisplayName("时间冲突校验 - 开始时间重叠")
    void validateHouseEntrustConflict_StartTimeOverlap_ShouldThrowException() {
        // 准备
        LandlordEntrust newEntrust = createTestEntrust();
        newEntrust.setEntrustStartTime(NEXT_WEEK.minusDays(3));
        newEntrust.setEntrustEndTime(NEXT_WEEK.plusDays(7));

        when(landlordEntrustMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> landlordEntrustService.saveEntrust(newEntrust)
        );
        assertTrue(exception.getMessage().contains("已存在有效委托"));
    }

    @Test
    @DisplayName("时间冲突校验 - 结束时间重叠")
    void validateHouseEntrustConflict_EndTimeOverlap_ShouldThrowException() {
        // 准备
        LandlordEntrust newEntrust = createTestEntrust();
        newEntrust.setEntrustStartTime(TODAY.minusDays(7));
        newEntrust.setEntrustEndTime(TODAY.plusDays(3));

        when(landlordEntrustMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> landlordEntrustService.saveEntrust(newEntrust)
        );
        assertTrue(exception.getMessage().contains("已存在有效委托"));
    }

    @Test
    @DisplayName("新增委托 - 状态校验：状态值非法")
    void saveEntrust_InvalidStatus_ShouldThrowException() {
        // 准备
        LandlordEntrust entrust = createTestEntrust();
        entrust.setStatus((byte) 2); // 非法状态值

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> landlordEntrustService.saveEntrust(entrust)
        );
        assertEquals("委托状态只能是0或1", exception.getMessage());
    }

    @Test
    @DisplayName("新增委托 - 状态校验：状态为空")
    void saveEntrust_StatusNull_ShouldThrowException() {
        // 准备
        LandlordEntrust entrust = createTestEntrust();
        entrust.setStatus(null);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> landlordEntrustService.saveEntrust(entrust)
        );
        assertEquals("委托状态不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("新增委托 - 到期提醒校验：值非法")
    void saveEntrust_InvalidRenewRemind_ShouldThrowException() {
        // 准备
        LandlordEntrust entrust = createTestEntrust();
        entrust.setRenewRemind((byte) 2); // 非法值

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> landlordEntrustService.saveEntrust(entrust)
        );
        assertEquals("是否到期提醒只能是0或1", exception.getMessage());
    }

    @Test
    @DisplayName("按房源ID查询 - 参数为空校验")
    void listByHouseId_ParameterNull_ShouldThrowException() {
        // houseId 为空
        assertThrows(IllegalArgumentException.class,
                () -> landlordEntrustService.listByHouseId(null, TEST_TENANT_ID));

        // tenantId 为空
        assertThrows(IllegalArgumentException.class,
                () -> landlordEntrustService.listByHouseId(TEST_HOUSE_ID, null));
    }

    @Test
    @DisplayName("按房东ID查询 - 参数为空校验")
    void listByLandlordId_ParameterNull_ShouldThrowException() {
        // landlordId 为空
        assertThrows(IllegalArgumentException.class,
                () -> landlordEntrustService.listByLandlordId(null, TEST_TENANT_ID));

        // tenantId 为空
        assertThrows(IllegalArgumentException.class,
                () -> landlordEntrustService.listByLandlordId(TEST_LANDLORD_ID, null));
    }

    @Test
    @DisplayName("分页查询 - 租户ID为空")
    void pageQuery_TenantIdNull_ShouldThrowException() {
        Page<LandlordEntrust> page = new Page<>(1, 10);
        LandlordEntrust query = new LandlordEntrust();

        assertThrows(IllegalArgumentException.class,
                () -> landlordEntrustService.pageQuery(page, query, null));
    }

    @Test
    @DisplayName("列表查询 - 租户ID为空")
    void listByConditions_TenantIdNull_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> landlordEntrustService.listByConditions(new HashMap<>(), null));
    }

    // 辅助方法
    private LandlordEntrust createTestEntrust() {
        LandlordEntrust entrust = new LandlordEntrust();
        entrust.setTenantId(TEST_TENANT_ID);
        entrust.setLandlordId(TEST_LANDLORD_ID);
        entrust.setHouseId(TEST_HOUSE_ID);
        entrust.setEntrustType(TEST_ENTRUST_TYPE_EXCLUSIVE); // 使用正确的类型
        entrust.setEntrustStartTime(TODAY);
        entrust.setEntrustEndTime(NEXT_WEEK);
        entrust.setRenewRemind((byte) 1);
        entrust.setStatus((byte) 1);
        entrust.setRemark("测试委托备注"); // 添加备注字段
        return entrust;
    }
}
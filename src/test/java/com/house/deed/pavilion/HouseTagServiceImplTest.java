package com.house.deed.pavilion;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.entity.HouseTag;
import com.house.deed.pavilion.mapper.HouseTagMapper;
import com.house.deed.pavilion.service.impl.HouseTagServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * HouseTagServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("房源标签关联服务测试")
class HouseTagServiceImplTest {

    @Mock
    private HouseTagMapper houseTagMapper;

    @Spy
    @InjectMocks
    private HouseTagServiceImpl houseTagService;

    private HouseTag mockHouseTag;
    private final Long TEST_TENANT_ID = 1001L;
    private final Long TEST_HOUSE_ID = 101L;
    private final Long TEST_TAG_ID = 501L;
    private final Long TEST_RECORD_ID = 1L;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        mockHouseTag = new HouseTag();
        mockHouseTag.setId(TEST_RECORD_ID);
        mockHouseTag.setTenantId(TEST_TENANT_ID);
        mockHouseTag.setHouseId(TEST_HOUSE_ID);
        mockHouseTag.setTagId(TEST_TAG_ID);
        mockHouseTag.setCreateTime(LocalDateTime.now());

        // 手动设置 baseMapper
        ReflectionTestUtils.setField(houseTagService, "baseMapper", houseTagMapper);
    }

    @Test
    @DisplayName("新增房源标签关联 - 成功")
    void saveHouseTag_Success() {
        // 准备
        when(houseTagMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(houseTagMapper.insert(any(HouseTag.class))).thenReturn(1);

        // 执行
        boolean result = houseTagService.saveHouseTag(mockHouseTag);

        // 验证
        assertTrue(result);
        verify(houseTagMapper).selectCount(any(LambdaQueryWrapper.class));
        verify(houseTagMapper).insert(mockHouseTag);
    }

    @Test
    @DisplayName("新增房源标签关联 - 重复添加相同标签")
    void saveHouseTag_Duplicate_ShouldThrowException() {
        // 准备
        when(houseTagMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> houseTagService.saveHouseTag(mockHouseTag)
        );
        assertEquals("当前房源已添加该标签，无需重复关联", exception.getMessage());
        verify(houseTagMapper, never()).insert(any(HouseTag.class));
    }

    @Test
    @DisplayName("新增房源标签关联 - 字段为空校验")
    void saveHouseTag_FieldNull_ShouldThrowException() {
        // 测试 tenantId 为空
        HouseTag tagWithoutTenant = new HouseTag();
        tagWithoutTenant.setHouseId(TEST_HOUSE_ID);
        tagWithoutTenant.setTagId(TEST_TAG_ID);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> houseTagService.saveHouseTag(tagWithoutTenant)
        );
        assertEquals("租户ID不能为空", exception.getMessage());

        // 测试 houseId 为空
        HouseTag tagWithoutHouse = new HouseTag();
        tagWithoutHouse.setTenantId(TEST_TENANT_ID);
        tagWithoutHouse.setTagId(TEST_TAG_ID);

        exception = assertThrows(
                IllegalArgumentException.class,
                () -> houseTagService.saveHouseTag(tagWithoutHouse)
        );
        assertEquals("房源ID不能为空", exception.getMessage());

        // 测试 tagId 为空
        HouseTag tagWithoutTag = new HouseTag();
        tagWithoutTag.setTenantId(TEST_TENANT_ID);
        tagWithoutTag.setHouseId(TEST_HOUSE_ID);

        exception = assertThrows(
                IllegalArgumentException.class,
                () -> houseTagService.saveHouseTag(tagWithoutTag)
        );
        assertEquals("标签ID不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("更新房源标签关联 - 成功")
    void updateHouseTagById_Success() {
        // 准备
        HouseTag updateRequest = new HouseTag();
        updateRequest.setId(TEST_RECORD_ID);
        updateRequest.setTenantId(TEST_TENANT_ID);

        when(houseTagMapper.selectById(TEST_RECORD_ID)).thenReturn(mockHouseTag);
        when(houseTagMapper.updateById(any(HouseTag.class))).thenReturn(1);

        // 执行
        boolean result = houseTagService.updateHouseTagById(updateRequest);

        // 验证
        assertTrue(result);
        verify(houseTagMapper).updateById(argThat(tag -> {
            // 验证核心字段被锁定
            return TEST_HOUSE_ID.equals(tag.getHouseId())
                    && TEST_TAG_ID.equals(tag.getTagId())
                    && TEST_TENANT_ID.equals(tag.getTenantId());
        }));
    }

    @Test
    @DisplayName("更新房源标签关联 - 记录不存在")
    void updateHouseTagById_NotFound_ShouldThrowException() {
        // 准备
        HouseTag updateRequest = new HouseTag();
        updateRequest.setId(TEST_RECORD_ID);
        updateRequest.setTenantId(TEST_TENANT_ID);

        when(houseTagMapper.selectById(TEST_RECORD_ID)).thenReturn(null);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> houseTagService.updateHouseTagById(updateRequest)
        );
        assertEquals("房源标签关联记录不存在", exception.getMessage());
    }

    @Test
    @DisplayName("更新房源标签关联 - 跨租户操作")
    void updateHouseTagById_DifferentTenant_ShouldThrowException() {
        // 准备
        HouseTag updateRequest = new HouseTag();
        updateRequest.setId(TEST_RECORD_ID);
        updateRequest.setTenantId(9999L); // 不同的租户

        when(houseTagMapper.selectById(TEST_RECORD_ID)).thenReturn(mockHouseTag);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> houseTagService.updateHouseTagById(updateRequest)
        );
        assertEquals("无权限操作其他租户的关联记录", exception.getMessage());
    }

    @Test
    @DisplayName("按ID删除关联 - 成功")
    void removeHouseTagById_Success() {
        // 准备
        when(houseTagMapper.selectById(TEST_RECORD_ID)).thenReturn(mockHouseTag);

        // 模拟 removeById 方法，避免 TableInfo 问题
        doReturn(true).when(houseTagService).removeById(TEST_RECORD_ID);

        // 执行
        boolean result = houseTagService.removeHouseTagById(TEST_RECORD_ID, TEST_TENANT_ID);

        // 验证
        assertTrue(result);
        verify(houseTagMapper).selectById(TEST_RECORD_ID);
        verify(houseTagService).removeById(TEST_RECORD_ID);
    }

    @Test
    @DisplayName("按ID查询关联 - 成功")
    void getHouseTagById_Success() {
        // 准备 - 注意：getOne 方法内部调用 selectOne(wrapper, true)
        when(houseTagMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(mockHouseTag);

        // 执行
        HouseTag result = houseTagService.getHouseTagById(TEST_RECORD_ID, TEST_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertEquals(TEST_RECORD_ID, result.getId());
        assertEquals(TEST_TENANT_ID, result.getTenantId());
    }

    @Test
    @DisplayName("按房源ID查询关联标签")
    void listByHouseId_Success() {
        // 准备
        List<HouseTag> expectedList = Arrays.asList(mockHouseTag);
        when(houseTagMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(expectedList);

        // 执行
        List<HouseTag> result = houseTagService.listByHouseId(TEST_HOUSE_ID, TEST_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("按标签ID查询关联房源")
    void listByTagId_Success() {
        // 准备
        List<HouseTag> expectedList = Arrays.asList(mockHouseTag);
        when(houseTagMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(expectedList);

        // 执行
        List<HouseTag> result = houseTagService.listByTagId(TEST_TAG_ID, TEST_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("批量新增关联 - 成功")
    void batchSaveHouseTags_Success() {
        // 准备
        HouseTag tag1 = createTestTag(TEST_TENANT_ID, 101L, 501L);
        HouseTag tag2 = createTestTag(TEST_TENANT_ID, 102L, 502L);
        List<HouseTag> tagList = Arrays.asList(tag1, tag2);

        when(houseTagMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        // 模拟 saveBatch 方法
        doReturn(true).when(houseTagService).saveBatch(tagList);

        // 执行
        boolean result = houseTagService.batchSaveHouseTags(tagList);

        // 验证
        assertTrue(result);
        verify(houseTagMapper, times(2)).selectCount(any(LambdaQueryWrapper.class));
        verify(houseTagService).saveBatch(tagList);
    }

    @Test
    @DisplayName("批量新增关联 - 租户不一致")
    void batchSaveHouseTags_DifferentTenants_ShouldThrowException() {
        // 准备
        HouseTag tag1 = createTestTag(1001L, 101L, 501L);
        HouseTag tag2 = createTestTag(1002L, 102L, 502L); // 不同租户
        List<HouseTag> tagList = Arrays.asList(tag1, tag2);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> houseTagService.batchSaveHouseTags(tagList)
        );
        assertEquals("批量操作的关联记录必须属于同一租户", exception.getMessage());
    }

    @Test
    @DisplayName("批量新增关联 - 存在重复关联")
    void batchSaveHouseTags_Duplicate_ShouldThrowException() {
        // 准备
        HouseTag tag1 = createTestTag(TEST_TENANT_ID, 101L, 501L);
        List<HouseTag> tagList = Arrays.asList(tag1);

        when(houseTagMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> houseTagService.batchSaveHouseTags(tagList)
        );
        assertTrue(exception.getMessage().contains("已关联标签"));
    }

    @Test
    @DisplayName("批量删除关联 - 成功")
    void batchRemoveHouseTags_Success() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 模拟查询验证：所有记录都属于当前租户
        when(houseTagMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // 模拟 removeByIds 方法
        doReturn(true).when(houseTagService).removeByIds(ids);

        // 执行
        boolean result = houseTagService.batchRemoveHouseTags(ids, TEST_TENANT_ID);

        // 验证
        assertTrue(result);
        verify(houseTagMapper).selectCount(any(LambdaQueryWrapper.class));
        verify(houseTagService).removeByIds(ids);
    }

    @Test
    @DisplayName("批量删除关联 - 包含其他租户记录")
    void batchRemoveHouseTags_ContainsOtherTenant_ShouldThrowException() {
        // 准备
        List<Long> ids = Arrays.asList(1L, 2L);

        when(houseTagMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // 执行 & 验证
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> houseTagService.batchRemoveHouseTags(ids, TEST_TENANT_ID)
        );
        assertEquals("存在不属于当前租户的关联记录，无法批量删除", exception.getMessage());
    }

    @Test
    @DisplayName("批量删除房源的所有标签关联")
    void batchRemoveByHouseId_Success() {
        // 准备
        when(houseTagMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(3);

        // 执行
        boolean result = houseTagService.batchRemoveByHouseId(TEST_HOUSE_ID, TEST_TENANT_ID);

        // 验证
        assertTrue(result);
        verify(houseTagMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("分页多条件查询")
    void pageQuery_Success() {
        // 准备
        Page<HouseTag> page = new Page<>(1, 10);
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("houseId", TEST_HOUSE_ID);

        // 创建一个真实的 Page 对象用于返回
        Page<HouseTag> expectedPage = new Page<>(1, 10);
        expectedPage.setRecords(Collections.singletonList(mockHouseTag));
        expectedPage.setTotal(1);

        when(houseTagMapper.selectPage(eq(page), any(LambdaQueryWrapper.class))).thenReturn(expectedPage);

        // 执行
        IPage<HouseTag> result = houseTagService.pageQuery(page, queryParams, TEST_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        verify(houseTagMapper).selectPage(eq(page), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("多条件列表查询")
    void listByConditions_Success() {
        // 准备
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("tagId", TEST_TAG_ID);

        List<HouseTag> expectedList = Arrays.asList(mockHouseTag);
        when(houseTagMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(expectedList);

        // 执行
        List<HouseTag> result = houseTagService.listByConditions(queryParams, TEST_TENANT_ID);

        // 验证
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("租户ID为空校验")
    void tenantIdNull_ShouldThrowException() {
        // 测试各种需要租户ID的方法
        assertThrows(IllegalArgumentException.class,
                () -> houseTagService.getHouseTagById(1L, null));

        assertThrows(IllegalArgumentException.class,
                () -> houseTagService.listByHouseId(1L, null));

        assertThrows(IllegalArgumentException.class,
                () -> houseTagService.batchRemoveHouseTags(Arrays.asList(1L), null));

        assertThrows(IllegalArgumentException.class,
                () -> houseTagService.pageQuery(new Page<>(), new HashMap<>(), null));

        assertThrows(IllegalArgumentException.class,
                () -> houseTagService.listByConditions(new HashMap<>(), null));
    }

    @Test
    @DisplayName("ID为空校验")
    void idNull_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> houseTagService.removeHouseTagById(null, TEST_TENANT_ID));

        assertThrows(IllegalArgumentException.class,
                () -> houseTagService.getHouseTagById(null, TEST_TENANT_ID));
    }

    @Test
    @DisplayName("列表为空校验")
    void listEmpty_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> houseTagService.batchSaveHouseTags(null));

        assertThrows(IllegalArgumentException.class,
                () -> houseTagService.batchSaveHouseTags(Collections.emptyList()));

        assertThrows(IllegalArgumentException.class,
                () -> houseTagService.batchRemoveHouseTags(null, TEST_TENANT_ID));

        assertThrows(IllegalArgumentException.class,
                () -> houseTagService.batchRemoveHouseTags(Collections.emptyList(), TEST_TENANT_ID));
    }

    @Test
    @DisplayName("按房源ID删除所有标签 - 参数为空")
    void batchRemoveByHouseId_ParameterNull_ShouldThrowException() {
        // houseId 为空
        assertThrows(IllegalArgumentException.class,
                () -> houseTagService.batchRemoveByHouseId(null, TEST_TENANT_ID));

        // tenantId 为空
        assertThrows(IllegalArgumentException.class,
                () -> houseTagService.batchRemoveByHouseId(TEST_HOUSE_ID, null));
    }

    @Test
    @DisplayName("批量新增 - 单个记录字段为空")
    void batchSaveHouseTags_SingleFieldNull_ShouldThrowException() {
        // houseId 为空
        HouseTag tag1 = createTestTag(TEST_TENANT_ID, null, 501L);
        List<HouseTag> tagList = Arrays.asList(tag1);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> houseTagService.batchSaveHouseTags(tagList)
        );
        assertEquals("房源ID不能为空", exception.getMessage());

        // tagId 为空
        HouseTag tag2 = createTestTag(TEST_TENANT_ID, 101L, null);
        List<HouseTag> tagList2 = Arrays.asList(tag2);

        exception = assertThrows(
                IllegalArgumentException.class,
                () -> houseTagService.batchSaveHouseTags(tagList2)
        );
        assertEquals("标签ID不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("更新操作 - 更新字段校验")
    void updateHouseTagById_UpdateFields_ShouldBeLocked() {
        // 准备更新请求，尝试修改核心字段
        HouseTag updateRequest = new HouseTag();
        updateRequest.setId(TEST_RECORD_ID);
        updateRequest.setTenantId(TEST_TENANT_ID);
        updateRequest.setHouseId(999L);  // 尝试修改房源ID
        updateRequest.setTagId(888L);    // 尝试修改标签ID

        when(houseTagMapper.selectById(TEST_RECORD_ID)).thenReturn(mockHouseTag);
        when(houseTagMapper.updateById(any(HouseTag.class))).thenReturn(1);

        // 执行
        boolean result = houseTagService.updateHouseTagById(updateRequest);

        // 验证
        assertTrue(result);
        verify(houseTagMapper).updateById(argThat(tag -> {
            // 核心字段应该被锁定为原值
            return TEST_HOUSE_ID.equals(tag.getHouseId())  // 应该还是原来的101
                    && TEST_TAG_ID.equals(tag.getTagId())   // 应该还是原来的501
                    && TEST_TENANT_ID.equals(tag.getTenantId());
        }));
    }

    // 辅助方法
    private HouseTag createTestTag(Long tenantId, Long houseId, Long tagId) {
        HouseTag tag = new HouseTag();
        tag.setTenantId(tenantId);
        tag.setHouseId(houseId);
        tag.setTagId(tagId);
        return tag;
    }
}
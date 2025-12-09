package com.house.deed.pavilion;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.entity.Region;
import com.house.deed.pavilion.service.impl.RegionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegionServiceImpl 单元测试")
public class RegionServiceImplTest {

    @Mock
    private com.house.deed.pavilion.mapper.RegionMapper regionMapper;

    @InjectMocks
    private RegionServiceImpl regionService;

    private static final Long SYSTEM_TENANT_ID = 0L;
    private static final Long TENANT_ID = 1001L;
    private static final Long ANOTHER_TENANT_ID = 1002L;
    private static final Long REGION_ID = 1L;
    private static final Long ROOT_PARENT_ID = 0L; // 顶级父级ID
    private static final Long NON_ROOT_PARENT_ID = 1L; // 非顶级父级ID
    private static final Byte REGION_LEVEL_PROVINCE = 1;
    private static final Byte REGION_LEVEL_CITY = 2;
    private static final String REGION_NAME = "测试区域";
    private static final String REGION_CODE = "110000";

    @BeforeEach
    void setUp() throws Exception {
        reset(regionMapper);

        // 手动设置 baseMapper
        Field baseMapperField = regionService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(regionService, regionMapper);
    }

    @Test
    @DisplayName("按父级ID查询子区域 - 成功（父级为顶级）")
    void listChildrenByParentId_ParentIsRoot_Success() {
        // Arrange
        List<Region> children = Arrays.asList(
                createRegion(2L, TENANT_ID, REGION_LEVEL_PROVINCE, "子区域1"),
                createRegion(3L, TENANT_ID, REGION_LEVEL_PROVINCE, "子区域2")
        );

        // 父级ID为0（顶级），不会检查父级区域是否存在
        doReturn(children).when(regionMapper).selectList(any(QueryWrapper.class));

        // Act
        List<Region> result = regionService.listChildrenByParentId(ROOT_PARENT_ID, TENANT_ID);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        // 父级ID为0，不会调用 selectOne
        verify(regionMapper, never()).selectOne(any(QueryWrapper.class));
        verify(regionMapper, times(1)).selectList(any(QueryWrapper.class));
    }

    @Test
    @DisplayName("按父级ID查询子区域 - 成功（父级非顶级）")
    void listChildrenByParentId_ParentNotRoot_Success() {
        // Arrange
        List<Region> children = Arrays.asList(
                createRegion(2L, TENANT_ID, REGION_LEVEL_CITY, "子区域1"),
                createRegion(3L, TENANT_ID, REGION_LEVEL_CITY, "子区域2")
        );

        // Mock 父级区域存在
        Region parentRegion = createRegion(NON_ROOT_PARENT_ID, TENANT_ID, REGION_LEVEL_PROVINCE, "父区域");
        doReturn(parentRegion).when(regionMapper).selectOne(any(QueryWrapper.class));

        // Mock 子区域查询
        doReturn(children).when(regionMapper).selectList(any(QueryWrapper.class));

        // Act
        List<Region> result = regionService.listChildrenByParentId(NON_ROOT_PARENT_ID, TENANT_ID);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(regionMapper, times(1)).selectOne(any(QueryWrapper.class));
        verify(regionMapper, times(1)).selectList(any(QueryWrapper.class));
    }

    @Test
    @DisplayName("按父级ID查询子区域 - 父级不存在失败")
    void listChildrenByParentId_ParentNotExists_Fails() {
        // Arrange
        // Mock 父级区域不存在
        doReturn(null).when(regionMapper).selectOne(any(QueryWrapper.class));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> regionService.listChildrenByParentId(NON_ROOT_PARENT_ID, TENANT_ID));

        assertEquals("父级区域不存在", exception.getMessage());
        verify(regionMapper, times(1)).selectOne(any(QueryWrapper.class));
        verify(regionMapper, never()).selectList(any(QueryWrapper.class));
    }

    // 其他测试方法保持不变...

    @Test
    @DisplayName("更新区域 - 成功")
    void updateRegionById_Success() {
        // Arrange
        Region existingRegion = createRegion(REGION_ID, TENANT_ID, REGION_LEVEL_PROVINCE, "旧名称");
        existingRegion.setParentId(ROOT_PARENT_ID);
        existingRegion.setSort(1);
        existingRegion.setCreateTime(LocalDateTime.now().minusDays(1));

        Region updateRegion = new Region();
        updateRegion.setId(REGION_ID);
        updateRegion.setRegionName("新名称");
        updateRegion.setSort(2);
        // 注意：不要设置 tenantId，因为 validateImmutableFields 会检查

        // Mock 查询现有区域
        doReturn(existingRegion).when(regionMapper).selectOne(any(QueryWrapper.class));

        // Mock 名称唯一性检查
        doReturn(0L).when(regionMapper).selectCount(any(QueryWrapper.class));

        // Mock 更新操作
        doReturn(1).when(regionMapper).updateById(any(Region.class));

        // Act
        boolean result = regionService.updateRegionById(updateRegion);

        // Assert
        assertTrue(result);
        verify(regionMapper, times(1)).selectOne(any(QueryWrapper.class));
        verify(regionMapper, times(1)).selectCount(any(QueryWrapper.class));
        verify(regionMapper, times(1)).updateById(any(Region.class));
    }

    @Test
    @DisplayName("批量保存区域 - 成功")
    void batchSaveRegions_Success() {
        // Arrange
        List<Region> regionList = Arrays.asList(
                createRegion(null, TENANT_ID, REGION_LEVEL_PROVINCE, "区域1"),
                createRegion(null, TENANT_ID, REGION_LEVEL_PROVINCE, "区域2")
        );

        // Mock 名称唯一性检查（每个区域调用一次）
        doReturn(0L).when(regionMapper).selectCount(any(QueryWrapper.class));

        // 创建 spy 对象
        RegionServiceImpl spyService = spy(regionService);
        doReturn(true).when(spyService).saveBatch(anyList());

        // Act
        boolean result = spyService.batchSaveRegions(regionList);

        // Assert
        assertTrue(result);
        verify(regionMapper, times(2)).selectCount(any(QueryWrapper.class));
        verify(spyService, times(1)).saveBatch(anyList());
    }

    @Test
    @DisplayName("批量删除区域 - 成功")
    void batchRemoveRegions_Success() {
        // Arrange
        List<Long> ids = Arrays.asList(1L, 2L);

        // Mock 区域验证查询
        List<Region> existingRegions = Arrays.asList(
                createRegion(1L, TENANT_ID, REGION_LEVEL_PROVINCE, "区域1"),
                createRegion(2L, TENANT_ID, REGION_LEVEL_PROVINCE, "区域2")
        );

        doReturn(existingRegions).when(regionMapper).selectList(any(QueryWrapper.class));

        // Mock 检查是否有子区域
        doReturn(0L).when(regionMapper).selectCount(any(QueryWrapper.class));

        // Mock 批量删除
        doReturn(2).when(regionMapper).deleteBatchIds(ids);

        // Act
        boolean result = regionService.batchRemoveRegions(ids, TENANT_ID);

        // Assert
        assertTrue(result);
        verify(regionMapper, times(1)).selectList(any(QueryWrapper.class));
        verify(regionMapper, times(1)).selectCount(any(QueryWrapper.class));
        verify(regionMapper, times(1)).deleteBatchIds(ids);
    }

    @Test
    @DisplayName("批量删除区域 - 存在子区域失败")
    void batchRemoveRegions_HasChildren_Fails() {
        // Arrange
        List<Long> ids = Arrays.asList(1L, 2L);

        // Mock 区域验证查询
        List<Region> existingRegions = Arrays.asList(
                createRegion(1L, TENANT_ID, REGION_LEVEL_PROVINCE, "区域1"),
                createRegion(2L, TENANT_ID, REGION_LEVEL_PROVINCE, "区域2")
        );

        doReturn(existingRegions).when(regionMapper).selectList(any(QueryWrapper.class));

        // Mock 检查是否有子区域（有子区域）
        doReturn(3L).when(regionMapper).selectCount(any(QueryWrapper.class));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> regionService.batchRemoveRegions(ids, TENANT_ID));

        assertEquals("部分区域存在下级子区域，禁止批量删除", exception.getMessage());
        verify(regionMapper, times(1)).selectList(any(QueryWrapper.class));
        verify(regionMapper, times(1)).selectCount(any(QueryWrapper.class));
        verify(regionMapper, never()).deleteBatchIds(any());
    }

    @Test
    @DisplayName("批量删除区域 - 区域不存在失败")
    void batchRemoveRegions_RegionNotExists_Fails() {
        // Arrange
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 只返回两个区域，ID为3的不存在
        List<Region> existingRegions = Arrays.asList(
                createRegion(1L, TENANT_ID, REGION_LEVEL_PROVINCE, "区域1"),
                createRegion(2L, TENANT_ID, REGION_LEVEL_PROVINCE, "区域2")
        );

        doReturn(existingRegions).when(regionMapper).selectList(any(QueryWrapper.class));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> regionService.batchRemoveRegions(ids, TENANT_ID));

        assertTrue(exception.getMessage().contains("以下区域ID不存在"));
        verify(regionMapper, times(1)).selectList(any(QueryWrapper.class));
        verify(regionMapper, never()).deleteBatchIds(any());
    }

    @Test
    @DisplayName("批量更新排序 - 成功")
    void batchUpdateRegionSort_Success() {
        // Arrange
        List<Region> regionList = Arrays.asList(
                createSortUpdateRegion(1L, TENANT_ID, 10),
                createSortUpdateRegion(2L, TENANT_ID, 20)
        );

        // Mock 区域验证查询
        List<Region> existingRegions = Arrays.asList(
                createRegion(1L, TENANT_ID, REGION_LEVEL_PROVINCE, "区域1"),
                createRegion(2L, TENANT_ID, REGION_LEVEL_PROVINCE, "区域2")
        );

        doReturn(existingRegions).when(regionMapper).selectList(any(QueryWrapper.class));

        // 创建 spy 对象
        RegionServiceImpl spyService = spy(regionService);
        doReturn(true).when(spyService).updateBatchById(anyList());

        // Act
        boolean result = spyService.batchUpdateRegionSort(regionList);

        // Assert
        assertTrue(result);
        verify(regionMapper, times(1)).selectList(any(QueryWrapper.class));
        verify(spyService, times(1)).updateBatchById(anyList());
    }

    // ==================== 辅助方法 ====================

    private Region createRegion(Long id, Long tenantId, Byte regionLevel, String regionName) {
        return createRegion(id, tenantId, regionLevel, regionName, ROOT_PARENT_ID);
    }

    private Region createRegion(Long id, Long tenantId, Byte regionLevel, String regionName, Long parentId) {
        Region region = new Region();
        region.setId(id);
        region.setTenantId(tenantId);
        region.setRegionLevel(regionLevel);
        region.setRegionName(regionName);
        region.setParentId(parentId);
        region.setSort(0);
        region.setCreateTime(LocalDateTime.now());
        return region;
    }

    private Region createSortUpdateRegion(Long id, Long tenantId, Integer sort) {
        Region region = new Region();
        region.setId(id);
        region.setTenantId(tenantId);
        region.setSort(sort);
        return region;
    }
}
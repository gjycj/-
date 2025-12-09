package com.house.deed.pavilion;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.entity.Building;
import com.house.deed.pavilion.mapper.BuildingMapper;
import com.house.deed.pavilion.service.impl.BuildingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuildingServiceImplTest {

    @Mock
    private BuildingMapper buildingMapper;

    @InjectMocks
    @Spy
    private BuildingServiceImpl buildingService;

    private Building testBuilding;
    private static final Long TENANT_ID = 1001L;
    private static final Long BUILDING_ID = 1L;
    private static final Long PROPERTY_ID = 100L;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        // 初始化测试数据
        testBuilding = new Building();
        testBuilding.setId(BUILDING_ID);
        testBuilding.setTenantId(TENANT_ID);
        testBuilding.setPropertyId(PROPERTY_ID);
        testBuilding.setBuildingNo("A栋");
        testBuilding.setBuildingType("RESIDENTIAL");
        testBuilding.setTotalFloor(18);
        testBuilding.setUnitCount(2);
        testBuilding.setCreateTime(LocalDateTime.now());

        // 手动设置 baseMapper
        setBaseMapper(buildingService, buildingMapper);
    }

    /**
     * 通过反射设置 baseMapper
     */
    private void setBaseMapper(BuildingServiceImpl service, BuildingMapper mapper)
            throws NoSuchFieldException, IllegalAccessException {
        Field baseMapperField = service.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(service, mapper);
    }

    /**
     * 测试新增楼栋：成功场景
     */
    @Test
    void testSaveBuilding_Success() {
        // 模拟楼栋号唯一性校验通过
        when(buildingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        // 模拟插入成功
        when(buildingMapper.insert(any(Building.class))).thenReturn(1);

        boolean result = buildingService.saveBuilding(testBuilding);

        assertTrue(result);
        verify(buildingMapper).insert(testBuilding);
    }

    /**
     * 测试新增楼栋：租户ID为空（异常场景）
     */
    @Test
    void testSaveBuilding_TenantIdNull() {
        testBuilding.setTenantId(null);

        assertThrows(IllegalArgumentException.class,
                () -> buildingService.saveBuilding(testBuilding),
                "预期抛出租户ID不能为空的异常");
    }

    /**
     * 测试新增楼栋：楼栋号重复（异常场景）
     */
    @Test
    void testSaveBuilding_DuplicateBuildingNo() {
        // 模拟楼栋号已存在
        when(buildingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(IllegalArgumentException.class,
                () -> buildingService.saveBuilding(testBuilding),
                "预期抛出楼栋号重复的异常");
    }

    /**
     * 测试更新楼栋：成功场景
     */
    @Test
    void testUpdateBuildingById_Success() {
        // 模拟查询到现有楼栋
        when(buildingMapper.selectById(BUILDING_ID)).thenReturn(testBuilding);
        // 模拟楼栋号唯一性校验通过
        when(buildingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        // 模拟更新成功
        when(buildingMapper.updateById(any(Building.class))).thenReturn(1);

        boolean result = buildingService.updateBuildingById(testBuilding);

        assertTrue(result);
        verify(buildingMapper).updateById(testBuilding);
    }

    /**
     * 测试更新楼栋：楼栋不存在（异常场景）
     */
    @Test
    void testUpdateBuildingById_BuildingNotFound() {
        when(buildingMapper.selectById(BUILDING_ID)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> buildingService.updateBuildingById(testBuilding),
                "预期抛出楼栋不存在的异常");
    }

    /**
     * 测试更新楼栋：租户不匹配（异常场景）
     */
    @Test
    void testUpdateBuildingById_TenantMismatch() {
        Building existingBuilding = new Building();
        existingBuilding.setId(BUILDING_ID);
        existingBuilding.setTenantId(999L); // 不同租户

        when(buildingMapper.selectById(BUILDING_ID)).thenReturn(existingBuilding);

        assertThrows(IllegalArgumentException.class,
                () -> buildingService.updateBuildingById(testBuilding),
                "预期抛出无权限操作的异常");
    }

    /**
     * 测试删除楼栋：成功场景
     */
    @Test
    void testRemoveBuildingById_Success() {
        // 模拟查询到现有楼栋
        when(buildingMapper.selectById(BUILDING_ID)).thenReturn(testBuilding);
        // 模拟删除成功
        when(buildingMapper.deleteById(BUILDING_ID)).thenReturn(1);

        boolean result = buildingService.removeBuildingById(BUILDING_ID, TENANT_ID);

        assertTrue(result);
        verify(buildingMapper).deleteById(BUILDING_ID);
    }

    /**
     * 测试删除楼栋：参数为空（异常场景）
     */
    @Test
    void testRemoveBuildingById_ParamsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> buildingService.removeBuildingById(null, TENANT_ID),
                "预期抛出楼栋ID不能为空的异常");

        assertThrows(IllegalArgumentException.class,
                () -> buildingService.removeBuildingById(BUILDING_ID, null),
                "预期抛出租户ID不能为空的异常");
    }

    /**
     * 测试根据ID查询楼栋：成功场景
     */
    @Test
    void testGetBuildingById_Success() {
        // 模拟查询结果
        when(buildingMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testBuilding);

        Building result = buildingService.getBuildingById(BUILDING_ID, TENANT_ID);

        assertNotNull(result);
        assertEquals(BUILDING_ID, result.getId());
        assertEquals(TENANT_ID, result.getTenantId());
    }

    /**
     * 测试根据ID查询楼栋：参数为空（边界场景）
     */
    @Test
    void testGetBuildingById_ParamsNull() {
        Building result = buildingService.getBuildingById(null, TENANT_ID);
        assertNull(result);

        result = buildingService.getBuildingById(BUILDING_ID, null);
        assertNull(result);
    }

    /**
     * 测试分页查询：正常场景
     */
    @Test
    void testPageQuery_Success() {
        // 准备参数
        Page<Building> page = new Page<>(1, 10);
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("propertyId", PROPERTY_ID);
        queryParams.put("buildingNo", "A");

        // 模拟查询结果
        IPage<Building> mockPage = new Page<>();
        mockPage.setRecords(Collections.singletonList(testBuilding));
        when(buildingMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn((Page<Building>) mockPage);

        IPage<Building> result = buildingService.pageQuery(page, queryParams, TENANT_ID);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        verify(buildingMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    /**
     * 测试分页查询：空参数场景
     */
    @Test
    void testPageQuery_EmptyParams() {
        Page<Building> page = new Page<>(1, 10);
        when(buildingMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(new Page<>());

        IPage<Building> result = buildingService.pageQuery(page, new HashMap<>(), TENANT_ID);

        assertNotNull(result);
        verify(buildingMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    /**
     * 测试多条件查询列表：正常场景
     */
    @Test
    void testListByConditions_Success() {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("buildingType", "RESIDENTIAL");
        queryParams.put("totalFloor", 18);

        List<Building> mockList = Arrays.asList(testBuilding, testBuilding);
        when(buildingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(mockList);

        List<Building> result = buildingService.listByConditions(queryParams, TENANT_ID);

        assertEquals(2, result.size());
        verify(buildingMapper).selectList(any(LambdaQueryWrapper.class));
    }

    /**
     * 测试根据楼盘ID查询：成功场景
     */
    @Test
    void testListByPropertyId_Success() {
        List<Building> mockList = Arrays.asList(testBuilding, testBuilding);
        when(buildingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(mockList);

        List<Building> result = buildingService.listByPropertyId(PROPERTY_ID, TENANT_ID);

        assertEquals(2, result.size());
        verify(buildingMapper).selectList(any(LambdaQueryWrapper.class));
    }

    /**
     * 测试根据楼盘ID查询：参数为空（边界场景）
     */
    @Test
    void testListByPropertyId_ParamsNull() {
        List<Building> result = buildingService.listByPropertyId(null, TENANT_ID);
        assertTrue(result.isEmpty());

        result = buildingService.listByPropertyId(PROPERTY_ID, null);
        assertTrue(result.isEmpty());
    }

    /**
     * 测试批量新增楼栋：成功场景
     */
    @Test
    void testBatchSaveBuildings_Success() {
        List<Building> buildingList = Arrays.asList(testBuilding, testBuilding);

        // 模拟唯一性校验通过
        when(buildingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        // 模拟批量保存成功
        doReturn(true).when(buildingService).saveBatch(buildingList);

        boolean result = buildingService.batchSaveBuildings(buildingList);

        assertTrue(result);
        verify(buildingService).saveBatch(buildingList);
    }

    /**
     * 测试批量新增楼栋：空列表（边界场景）
     */
    @Test
    void testBatchSaveBuildings_EmptyList() {
        boolean result = buildingService.batchSaveBuildings(new ArrayList<>());
        assertFalse(result);
    }

    /**
     * 测试批量新增楼栋：租户ID不一致（异常场景）
     */
    @Test
    void testBatchSaveBuildings_TenantIdMismatch() {
        Building building2 = new Building();
        building2.setTenantId(999L); // 不同租户

        List<Building> buildingList = Arrays.asList(testBuilding, building2);

        assertThrows(IllegalArgumentException.class,
                () -> buildingService.batchSaveBuildings(buildingList),
                "预期抛出租户ID不一致的异常");
    }

    /**
     * 测试批量删除楼栋：成功场景
     */
    @Test
    void testBatchRemoveBuildings_Success() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 模拟校验通过
        doNothing().when(buildingService).validateBuildingIdsBelongToTenant(TENANT_ID, ids);
        // 模拟批量删除成功
        when(buildingMapper.deleteBatchIds(ids)).thenReturn(3);

        boolean result = buildingService.batchRemoveBuildings(ids, TENANT_ID);

        assertTrue(result);
        verify(buildingMapper).deleteBatchIds(ids);
    }

    /**
     * 测试批量删除楼栋：空列表（边界场景）
     */
    @Test
    void testBatchRemoveBuildings_EmptyList() {
        boolean result = buildingService.batchRemoveBuildings(new ArrayList<>(), TENANT_ID);
        assertFalse(result);
    }

    /**
     * 测试校验楼栋ID归属：成功场景
     */
    @Test
    void testValidateBuildingIdsBelongToTenant_Success() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 模拟查询到所有ID都属于当前租户
        when(buildingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

        // 应该不抛出异常
        assertDoesNotThrow(() -> buildingService.validateBuildingIdsBelongToTenant(TENANT_ID, ids));
    }

    /**
     * 测试校验楼栋ID归属：存在非当前租户ID（异常场景）
     */
    @Test
    void testValidateBuildingIdsBelongToTenant_InvalidIds() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 模拟只查询到2个ID属于当前租户
        when(buildingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

        assertThrows(IllegalArgumentException.class,
                () -> buildingService.validateBuildingIdsBelongToTenant(TENANT_ID, ids),
                "预期抛出存在不属于当前租户楼栋ID的异常");
    }

    /**
     * 测试校验楼栋ID归属：空参数（边界场景）
     */
    @Test
    void testValidateBuildingIdsBelongToTenant_EmptyParams() {
        // 空列表应该不抛出异常
        assertDoesNotThrow(() -> buildingService.validateBuildingIdsBelongToTenant(TENANT_ID, new ArrayList<>()));

        // 空租户ID应该不抛出异常
        assertDoesNotThrow(() -> buildingService.validateBuildingIdsBelongToTenant(null, Arrays.asList(1L, 2L)));
    }
}
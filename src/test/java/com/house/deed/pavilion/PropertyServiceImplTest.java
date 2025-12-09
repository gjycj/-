package com.house.deed.pavilion;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.entity.Property;
import com.house.deed.pavilion.service.impl.PropertyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PropertyServiceImpl 单元测试")
public class PropertyServiceImplTest {

    @Mock
    private com.house.deed.pavilion.mapper.PropertyMapper propertyMapper;

    @InjectMocks
    private PropertyServiceImpl propertyService;

    private static final Long TENANT_ID = 1001L;
    private static final Long ANOTHER_TENANT_ID = 1002L;
    private static final Long PROPERTY_ID = 1L;
    private static final Long REGION_ID = 10L;
    private static final Long CREATE_AGENT_ID = 10001L;

    @BeforeEach
    void setUp() throws Exception {
        reset(propertyMapper);

        // 手动设置 baseMapper
        Field baseMapperField = propertyService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(propertyService, propertyMapper);

        // 手动设置 entityClass（虽然现在用QueryWrapper，但Service可能还需要）
        Field entityClassField = propertyService.getClass().getSuperclass().getDeclaredField("entityClass");
        entityClassField.setAccessible(true);
        entityClassField.set(propertyService, com.house.deed.pavilion.entity.Property.class);
    }

    @Test
    @DisplayName("批量删除楼盘 - 楼盘不存在失败")
    void batchRemoveProperties_PropertyNotExists_Fails() {
        // Arrange
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // 只返回两个楼盘，ID为3的不存在
        List<Property> existingProperties = Arrays.asList(
                createProperty(1L, TENANT_ID),
                createProperty(2L, TENANT_ID)
        );

        // 使用 doReturn 而不是 when，避免 Lambda 表达式问题
        doReturn(existingProperties).when(propertyMapper).selectList(any(QueryWrapper.class));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> propertyService.batchRemoveProperties(ids, TENANT_ID));

        assertTrue(exception.getMessage().contains("以下楼盘ID不存在"));
        verify(propertyMapper, times(1)).selectList(any(QueryWrapper.class));
        verify(propertyMapper, never()).deleteBatchIds(any());
    }

    @Test
    @DisplayName("批量删除楼盘 - 成功")
    void batchRemoveProperties_Success() {
        // Arrange
        List<Long> ids = Arrays.asList(1L, 2L);

        List<Property> existingProperties = Arrays.asList(
                createProperty(1L, TENANT_ID),
                createProperty(2L, TENANT_ID)
        );

        doReturn(existingProperties).when(propertyMapper).selectList(any(QueryWrapper.class));
        doReturn(2).when(propertyMapper).deleteBatchIds(ids);

        // Act
        boolean result = propertyService.batchRemoveProperties(ids, TENANT_ID);

        // Assert
        assertTrue(result);
        verify(propertyMapper, times(1)).selectList(any(QueryWrapper.class));
        verify(propertyMapper, times(1)).deleteBatchIds(ids);
    }

    @Test
    @DisplayName("批量删除楼盘 - 无权限操作失败")
    void batchRemoveProperties_NoPermission_Fails() {
        // Arrange
        List<Long> ids = Arrays.asList(1L, 2L);

        // 第二个楼盘属于其他租户
        List<Property> existingProperties = Arrays.asList(
                createProperty(1L, TENANT_ID),
                createProperty(2L, ANOTHER_TENANT_ID) // 其他租户
        );

        doReturn(existingProperties).when(propertyMapper).selectList(any(QueryWrapper.class));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> propertyService.batchRemoveProperties(ids, TENANT_ID));

        assertTrue(exception.getMessage().contains("无权限操作以下楼盘ID"));
        verify(propertyMapper, times(1)).selectList(any(QueryWrapper.class));
        verify(propertyMapper, never()).deleteBatchIds(any());
    }

    @Test
    @DisplayName("批量更新楼盘 - 成功")
    void batchUpdateProperties_Success() {
        // Arrange
        List<Property> propertyList = Arrays.asList(
                createValidPropertyForUpdate(1L, "楼盘1"),
                createValidPropertyForUpdate(2L, "楼盘2")
        );

        // Mock验证楼盘归属
        List<Property> existingProperties = Arrays.asList(
                createProperty(1L, TENANT_ID),
                createProperty(2L, TENANT_ID)
        );

        doReturn(existingProperties).when(propertyMapper).selectList(any(QueryWrapper.class));

        // Mock单个查询（用于名称唯一性检查等）
        Property existingProperty1 = createValidPropertyForUpdate(1L, "旧楼盘1");
        existingProperty1.setCreateTime(LocalDateTime.now().minusDays(1));
        existingProperty1.setUpdateTime(LocalDateTime.now().minusHours(1));

        Property existingProperty2 = createValidPropertyForUpdate(2L, "旧楼盘2");
        existingProperty2.setCreateTime(LocalDateTime.now().minusDays(1));
        existingProperty2.setUpdateTime(LocalDateTime.now().minusHours(1));

        // 需要模拟两次 getPropertyById 调用
        doReturn(existingProperty1).doReturn(existingProperty2)
                .when(propertyMapper).selectOne(any(QueryWrapper.class));

        // Mock名称唯一性检查
        doReturn(0L).when(propertyMapper).selectCount(any(QueryWrapper.class));

        // 创建一个PropertyService的spy，用于模拟updateBatchById方法
        PropertyServiceImpl spyService = spy(propertyService);
        doReturn(true).when(spyService).updateBatchById(anyList());

        // Act
        boolean result = spyService.batchUpdateProperties(propertyList, TENANT_ID);

        // Assert
        assertTrue(result);
        verify(propertyMapper, atLeast(1)).selectList(any(QueryWrapper.class));
        verify(spyService, times(1)).updateBatchById(anyList());
    }


    @Test
    @DisplayName("批量保存楼盘 - 成功")
    void batchSaveProperties_Success() {
        // Arrange
        List<Property> propertyList = Arrays.asList(
                createValidProperty(),
                createValidProperty()
        );

        propertyList.get(0).setPropertyName("楼盘1");
        propertyList.get(0).setId(null);
        propertyList.get(1).setPropertyName("楼盘2");
        propertyList.get(1).setId(null);

        // Mock名称唯一性检查（不存在重复名称）
        doReturn(Collections.emptyList()).when(propertyMapper).selectList(any(QueryWrapper.class));

        // 创建 propertyService 的 spy
        PropertyServiceImpl spyService = spy(propertyService);

        // Mock批量保存 - 使用 spy 对象
        doReturn(true).when(spyService).saveBatch(anyList());

        // Act
        boolean result = spyService.batchSaveProperties(propertyList);

        // Assert
        assertTrue(result);
        verify(propertyMapper, times(1)).selectList(any(QueryWrapper.class));
        verify(spyService, times(1)).saveBatch(anyList());
    }
    @Test
    @DisplayName("保存楼盘 - 成功")
    void saveProperty_Success() {
        // Arrange
        Property property = createValidProperty();
        property.setId(null); // 新增时ID应为null

        // 模拟楼盘名称唯一性检查
        doReturn(0L).when(propertyMapper).selectCount(any(QueryWrapper.class));

        // 模拟插入操作
        doReturn(1).when(propertyMapper).insert(any(Property.class));

        // Act
        boolean result = propertyService.saveProperty(property);

        // Assert
        assertTrue(result);
        verify(propertyMapper, times(1)).selectCount(any(QueryWrapper.class));
        verify(propertyMapper, times(1)).insert(any(Property.class));
    }

    @Test
    @DisplayName("保存楼盘 - 楼盘名称已存在失败")
    void saveProperty_PropertyNameExists_Fails() {
        // Arrange
        Property property = createValidProperty();
        property.setId(null);

        // 模拟楼盘名称已存在
        doReturn(1L).when(propertyMapper).selectCount(any(QueryWrapper.class));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> propertyService.saveProperty(property));

        assertTrue(exception.getMessage().contains("当前租户下楼盘名称已存在"));
        verify(propertyMapper, times(1)).selectCount(any(QueryWrapper.class));
        verify(propertyMapper, never()).insert(any(Property.class));
    }

    @Test
    @DisplayName("更新楼盘 - 修改楼盘名称成功")
    void updatePropertyById_UpdatePropertyName_Success() {
        // Arrange
        Property existingProperty = createValidProperty();
        existingProperty.setId(PROPERTY_ID);
        existingProperty.setPropertyName("原始楼盘名");

        Property updateProperty = new Property();
        updateProperty.setId(PROPERTY_ID);
        updateProperty.setTenantId(TENANT_ID);
        updateProperty.setPropertyName("新楼盘名");
        updateProperty.setAddress("新地址");

        // Mock查询现有楼盘
        doReturn(existingProperty).when(propertyMapper).selectOne(any(QueryWrapper.class));

        // Mock楼盘名称唯一性检查（新名称可用）
        doReturn(0L).when(propertyMapper).selectCount(any(QueryWrapper.class));

        // Mock更新操作
        doReturn(1).when(propertyMapper).updateById(any(Property.class));

        // Act
        boolean result = propertyService.updatePropertyById(updateProperty);

        // Assert
        assertTrue(result);
        verify(propertyMapper, times(1)).selectOne(any(QueryWrapper.class));
        verify(propertyMapper, times(1)).selectCount(any(QueryWrapper.class));
        verify(propertyMapper, times(1)).updateById(any(Property.class));
    }

    @Test
    @DisplayName("删除楼盘 - 成功")
    void removePropertyById_Success() {
        // Arrange
        Property existingProperty = createValidProperty();
        existingProperty.setId(PROPERTY_ID);

        doReturn(existingProperty).when(propertyMapper).selectOne(any(QueryWrapper.class));
        doReturn(1).when(propertyMapper).deleteById(PROPERTY_ID);

        // Act
        boolean result = propertyService.removePropertyById(PROPERTY_ID, TENANT_ID);

        // Assert
        assertTrue(result);
        verify(propertyMapper, times(1)).selectOne(any(QueryWrapper.class));
        verify(propertyMapper, times(1)).deleteById(PROPERTY_ID);
    }

    @Test
    @DisplayName("按建成年份范围查询 - 起始年份大于结束年份失败")
    void listByCompletionYearRange_StartYearGreaterThanEndYear_Fails() {
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> propertyService.listByCompletionYearRange(2020, 2010, TENANT_ID));

        assertEquals("起始年份不能大于结束年份", exception.getMessage());
        verify(propertyMapper, never()).selectList(any());
    }

    // ==================== 新增的辅助方法 ====================

    /**
     * 创建用于更新的Property对象（不包含createTime/updateTime）
     */
    private Property createValidPropertyForUpdate(Long id, String propertyName) {
        Property property = new Property();
        property.setId(id);
        property.setTenantId(TENANT_ID);
        property.setPropertyName(propertyName);
        property.setRegionId(REGION_ID);
        property.setAddress("测试地址123号");
        property.setDeveloper("测试开发商");
        property.setGreenRate(new BigDecimal("30.5"));
        property.setCompletionYear(2020);
        property.setPropertyManagement("测试物业公司");
        property.setCreateAgentId(CREATE_AGENT_ID);
        // 注意：不设置createTime和updateTime，这些应该由数据库自动填充
        return property;
    }

// ==================== 修改原来的辅助方法 ====================

    private Property createValidProperty() {
        Property property = createValidPropertyForUpdate(PROPERTY_ID, "测试楼盘");
        property.setCreateTime(LocalDateTime.now());
        property.setUpdateTime(LocalDateTime.now());
        return property;
    }

    private Property createProperty(Long id, Long tenantId) {
        Property property = new Property();
        property.setId(id);
        property.setTenantId(tenantId);
        property.setPropertyName("楼盘" + id);
        return property;
    }
}
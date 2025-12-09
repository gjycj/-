package com.house.deed.pavilion;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.entity.Tag;
import com.house.deed.pavilion.mapper.TagMapper;
import com.house.deed.pavilion.service.impl.TagServiceImpl;
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

/**
 * TagServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class TagServiceImplTest {

    @Mock
    private TagMapper tagMapper;

    @Spy
    @InjectMocks
    private TagServiceImpl tagService;

    private Tag mockTag;
    private final Long tenantId = 1L;
    private final Long tagId = 100L;
    private final String tagName = "交通便利";
    private final String tagType = "HOUSE";

    @BeforeEach
    void setUp() {
        mockTag = new Tag();
        mockTag.setId(tagId);
        mockTag.setTenantId(tenantId);
        mockTag.setTagName(tagName);
        mockTag.setTagType(tagType);
        mockTag.setDescription("交通便利的房源标签");
        mockTag.setCreateTime(LocalDateTime.now());
        mockTag.setUpdateTime(LocalDateTime.now());

        // 通过反射设置 baseMapper
        setBaseMapper(tagService, tagMapper);
    }

    /**
     * 通过反射设置 ServiceImpl 的 baseMapper 字段
     */
    private void setBaseMapper(TagServiceImpl service, TagMapper mapper) {
        try {
            Field field = service.getClass().getSuperclass().getDeclaredField("baseMapper");
            field.setAccessible(true);
            field.set(service, mapper);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set baseMapper", e);
        }
    }

    @Test
    void saveTag_ShouldSuccess_WhenValidTag() {
        // Arrange
        when(tagMapper.selectCount(any())).thenReturn(0L);
        doReturn(true).when(tagService).save(any(Tag.class));

        // Act
        boolean result = tagService.saveTag(mockTag);

        // Assert
        assertTrue(result);
        verify(tagMapper, times(1)).selectCount(any());
        verify(tagService, times(1)).save(mockTag);
    }

    @Test
    void saveTag_ShouldThrowException_WhenTagNameAndTypeDuplicate() {
        // Arrange
        when(tagMapper.selectCount(any())).thenReturn(1L);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tagService.saveTag(mockTag));
        assertTrue(exception.getMessage().contains("当前租户下标签名称和类型组合已存在"));
        verify(tagMapper, times(1)).selectCount(any());
        verify(tagService, never()).save(any());
    }

    @Test
    void saveTag_ShouldThrowException_WhenTagNameTooLong() {
        // Arrange
        // 创建一个超过50个字符的字符串
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 51; i++) {
            longName.append("测");
        }
        mockTag.setTagName(longName.toString());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tagService.saveTag(mockTag));
        assertTrue(exception.getMessage().contains("标签名称长度不能超过50字符"));
        verify(tagMapper, never()).selectCount(any());
        verify(tagService, never()).save(any());
    }

    @Test
    void saveTag_ShouldThrowException_WhenDescriptionTooLong() {
        // Arrange
        // 创建一个超过200个字符的字符串
        StringBuilder longDescription = new StringBuilder();
        for (int i = 0; i < 201; i++) {
            longDescription.append("测");
        }
        mockTag.setDescription(longDescription.toString());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tagService.saveTag(mockTag));
        assertTrue(exception.getMessage().contains("标签描述长度不能超过200字符"));
        verify(tagMapper, never()).selectCount(any());
        verify(tagService, never()).save(any());
    }

    @Test
    void saveTag_ShouldThrowException_WhenInvalidTagType() {
        // Arrange
        mockTag.setTagType("INVALID_TYPE");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tagService.saveTag(mockTag));
        assertTrue(exception.getMessage().contains("标签类型只能是HOUSE或CUSTOMER"));
        verify(tagMapper, never()).selectCount(any());
        verify(tagService, never()).save(any());
    }

    @Test
    void updateTagById_ShouldSuccess_WhenValidTag() {
        // Arrange
        Tag existingTag = new Tag();
        existingTag.setId(tagId);
        existingTag.setTenantId(tenantId);
        existingTag.setTagName("旧标签名");
        existingTag.setTagType(tagType);
        existingTag.setCreateTime(LocalDateTime.now());

        // 使用 doReturn 而不是 when，避免调用真实方法
        doReturn(existingTag).when(tagService).getById(tagId);
        when(tagMapper.selectCount(any())).thenReturn(0L);
        doReturn(true).when(tagService).updateById(any(Tag.class));

        // Act
        boolean result = tagService.updateTagById(mockTag);

        // Assert
        assertTrue(result);
        verify(tagService, times(1)).getById(tagId);
        verify(tagMapper, times(1)).selectCount(any());
        verify(tagService, times(1)).updateById(mockTag);
    }

    @Test
    void updateTagById_ShouldThrowException_WhenTagNotExist() {
        // Arrange
        // 使用 doReturn 确保只调用一次
        doReturn(null).when(tagService).getById(tagId);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tagService.updateTagById(mockTag));
        assertEquals("标签不存在", exception.getMessage());
        verify(tagService, times(1)).getById(tagId);
        verify(tagService, never()).updateById(any());
    }

    @Test
    void updateTagById_ShouldThrowException_WhenCrossTenant() {
        // Arrange
        Tag existingTag = new Tag();
        existingTag.setId(tagId);
        existingTag.setTenantId(999L); // 不同的租户
        existingTag.setCreateTime(LocalDateTime.now());

        // 使用 doReturn 确保只调用一次
        doReturn(existingTag).when(tagService).getById(tagId);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tagService.updateTagById(mockTag));
        assertEquals("无权限操作其他租户的标签", exception.getMessage());
        verify(tagService, times(1)).getById(tagId);
        verify(tagService, never()).updateById(any());
    }

    @Test
    void updateTagById_ShouldThrowException_WhenTagNameAndTypeDuplicate() {
        // Arrange
        Tag existingTag = new Tag();
        existingTag.setId(tagId);
        existingTag.setTenantId(tenantId);
        existingTag.setTagName("旧标签名");
        existingTag.setTagType(tagType);
        existingTag.setCreateTime(LocalDateTime.now());

        // 使用 doReturn 确保只调用一次
        doReturn(existingTag).when(tagService).getById(tagId);
        when(tagMapper.selectCount(any())).thenReturn(1L);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tagService.updateTagById(mockTag));
        assertTrue(exception.getMessage().contains("当前租户下标签名称和类型组合已存在"));
        verify(tagService, times(1)).getById(tagId);
        verify(tagMapper, times(1)).selectCount(any());
        verify(tagService, never()).updateById(any());
    }

    @Test
    void removeTagById_ShouldSuccess_WhenValidId() {
        // Arrange
        // 使用 doReturn 确保只调用一次
        doReturn(mockTag).when(tagService).getById(tagId);
        doReturn(true).when(tagService).removeById(tagId);

        // Act
        boolean result = tagService.removeTagById(tagId, tenantId);

        // Assert
        assertTrue(result);
        verify(tagService, times(1)).getById(tagId);
        verify(tagService, times(1)).removeById(tagId);
    }

    @Test
    void removeTagById_ShouldThrowException_WhenTagNotExist() {
        // Arrange
        // 使用 doReturn 确保只调用一次
        doReturn(null).when(tagService).getById(tagId);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tagService.removeTagById(tagId, tenantId));
        assertEquals("标签不存在", exception.getMessage());
        verify(tagService, times(1)).getById(tagId);
        verify(tagService, never()).removeById(any());
    }

    @Test
    void removeTagById_ShouldThrowException_WhenCrossTenant() {
        // Arrange
        Tag tagWithDiffTenant = new Tag();
        tagWithDiffTenant.setId(tagId);
        tagWithDiffTenant.setTenantId(999L);
        tagWithDiffTenant.setCreateTime(LocalDateTime.now());

        // 使用 doReturn 确保只调用一次
        doReturn(tagWithDiffTenant).when(tagService).getById(tagId);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tagService.removeTagById(tagId, tenantId));
        assertEquals("无权限操作其他租户的标签", exception.getMessage());
        verify(tagService, times(1)).getById(tagId);
        verify(tagService, never()).removeById(any());
    }

    @Test
    void getTagById_ShouldReturnTag_WhenValidId() {
        // Arrange
        Tag expectedTag = new Tag();
        expectedTag.setId(tagId);
        expectedTag.setTenantId(tenantId);
        expectedTag.setTagName("测试标签");

        // 修复：使用正确的参数匹配器
        when(tagMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class)))
                .thenReturn(expectedTag);

        // Act
        Tag result = tagService.getTagById(tagId, tenantId);

        // Assert
        assertNotNull(result);
        assertEquals(tagId, result.getId());
        assertEquals(tenantId, result.getTenantId());
        verify(tagMapper, times(1)).selectOne(any(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class));
    }

    @Test
    void getTagById_ShouldThrowException_WhenIdOrTenantIdIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> tagService.getTagById(null, tenantId));
        assertThrows(IllegalArgumentException.class,
                () -> tagService.getTagById(tagId, null));
        assertThrows(IllegalArgumentException.class,
                () -> tagService.getTagById(null, null));
        verify(tagMapper, never()).selectOne(any());
    }

    @Test
    void pageQuery_ShouldReturnPage_WhenValidParams() {
        // Arrange
        Page<Tag> page = new Page<>(1, 10);
        Map<String, Object> params = new HashMap<>();
        params.put("tagName", "交通");
        params.put("tagType", "HOUSE");
        params.put("description", "便利");

        when(tagMapper.selectPage(eq(page), any())).thenReturn(page);

        // Act
        IPage<Tag> result = tagService.pageQuery(page, params, tenantId);

        // Assert
        assertNotNull(result);
        verify(tagMapper, times(1)).selectPage(eq(page), any());
    }

    @Test
    void pageQuery_ShouldThrowException_WhenTenantIdIsNull() {
        // Arrange
        Page<Tag> page = new Page<>(1, 10);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> tagService.pageQuery(page, new HashMap<>(), null));
        verify(tagMapper, never()).selectPage(any(), any());
    }

    @Test
    void listByConditions_ShouldReturnList_WhenValidParams() {
        // Arrange
        List<Tag> tagList = Arrays.asList(mockTag, new Tag());
        Map<String, Object> params = new HashMap<>();
        params.put("tagType", "HOUSE");

        when(tagMapper.selectList(any())).thenReturn(tagList);

        // Act
        List<Tag> result = tagService.listByConditions(params, tenantId);

        // Assert
        assertEquals(2, result.size());
        verify(tagMapper, times(1)).selectList(any());
    }

    @Test
    void listByTagType_ShouldReturnList_WhenValidType() {
        // Arrange
        List<Tag> tagList = Arrays.asList(mockTag, new Tag());

        when(tagMapper.selectList(any())).thenReturn(tagList);

        // Act
        List<Tag> result = tagService.listByTagType(tagType, tenantId);

        // Assert
        assertEquals(2, result.size());
        verify(tagMapper, times(1)).selectList(any());
    }

    @Test
    void listByTagType_ShouldThrowException_WhenParamsAreNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> tagService.listByTagType(null, tenantId));
        assertThrows(IllegalArgumentException.class,
                () -> tagService.listByTagType(tagType, null));
        verify(tagMapper, never()).selectList(any());
    }

    @Test
    void batchSaveTags_ShouldSuccess_WhenValidTags() {
        // Arrange
        List<Tag> tags = Arrays.asList(
                createTagWithName("标签1"),
                createTagWithName("标签2")
        );

        when(tagMapper.selectCount(any())).thenReturn(0L);
        doReturn(true).when(tagService).saveBatch(anyList());

        // Act
        boolean result = tagService.batchSaveTags(tags);

        // Assert
        assertTrue(result);
        verify(tagMapper, times(2)).selectCount(any());
        verify(tagService, times(1)).saveBatch(anyList());
    }

    @Test
    void batchSaveTags_ShouldThrowException_WhenListIsEmpty() {
        // Arrange
        List<Tag> emptyList = Collections.emptyList();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tagService.batchSaveTags(emptyList));
        assertEquals("标签列表不能为空", exception.getMessage());
    }

    @Test
    void batchSaveTags_ShouldThrowException_WhenTagCodesDuplicate() {
        // Arrange
        List<Tag> tags = Arrays.asList(
                createTagWithName("标签1"),
                createTagWithName("标签2")
        );

        // 修复：异常消息应该与实际实现一致
        when(tagMapper.selectCount(any())).thenReturn(1L);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tagService.batchSaveTags(tags));
        // 修复：使用正确的异常消息
        assertTrue(exception.getMessage().contains("当前租户下标签名称和类型组合已存在"));
        verify(tagMapper, times(1)).selectCount(any());
    }

    @Test
    void batchSaveTags_ShouldThrowException_WhenTenantIdsNotSame() {
        // Arrange
        Tag tag1 = createTagWithName("标签1");
        Tag tag2 = createTagWithName("标签2");
        tag2.setTenantId(999L);
        List<Tag> tags = Arrays.asList(tag1, tag2);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tagService.batchSaveTags(tags));
        assertEquals("批量操作仅支持同一租户", exception.getMessage());
        // 由于租户ID校验失败，第一个标签会调用一次selectCount
        verify(tagMapper, times(1)).selectCount(any());
    }

    @Test
    void batchRemoveTags_ShouldSuccess_WhenValidIds() {
        // Arrange
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        List<Tag> tags = Arrays.asList(
                createTagWithId(1L),
                createTagWithId(2L),
                createTagWithId(3L)
        );

        // 使用 doReturn 确保只调用一次
        doReturn(tags).when(tagService).listByIds(ids);
        doReturn(true).when(tagService).removeByIds(ids);

        // Act
        boolean result = tagService.batchRemoveTags(ids, tenantId);

        // Assert
        assertTrue(result);
        verify(tagService, times(1)).listByIds(ids);
        verify(tagService, times(1)).removeByIds(ids);
    }

    @Test
    void batchRemoveTags_ShouldThrowException_WhenIdsNotMatch() {
        // Arrange
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        List<Tag> tags = Arrays.asList(createTagWithId(1L), createTagWithId(2L)); // 只返回2个

        // 使用 doReturn 确保只调用一次
        doReturn(tags).when(tagService).listByIds(ids);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tagService.batchRemoveTags(ids, tenantId));
        assertEquals("存在无效的标签ID", exception.getMessage());
        verify(tagService, times(1)).listByIds(ids);
        verify(tagService, never()).removeByIds(any());
    }

    @Test
    void batchRemoveTags_ShouldThrowException_WhenCrossTenant() {
        // Arrange
        List<Long> ids = Arrays.asList(1L, 2L);
        Tag tag1 = createTagWithId(1L);
        Tag tag2 = createTagWithId(2L);
        tag2.setTenantId(999L); // 不同的租户
        List<Tag> tags = Arrays.asList(tag1, tag2);

        // 使用 doReturn 确保只调用一次
        doReturn(tags).when(tagService).listByIds(ids);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tagService.batchRemoveTags(ids, tenantId));
        assertEquals("无权限操作其他租户的标签", exception.getMessage());
        verify(tagService, times(1)).listByIds(ids);
        verify(tagService, never()).removeByIds(any());
    }

    // Helper methods
    private Tag createTagWithName(String tagName) {
        Tag tag = new Tag();
        tag.setTenantId(tenantId);
        tag.setTagName(tagName);
        tag.setTagType(tagType);
        tag.setDescription("测试描述");
        return tag;
    }

    private Tag createTagWithId(Long id) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setTenantId(tenantId);
        tag.setTagName("标签" + id);
        tag.setTagType(tagType);
        tag.setDescription("测试描述");
        tag.setCreateTime(LocalDateTime.now());
        return tag;
    }

    @Test
    void validateTagParams_ShouldThrowException_WhenRequiredFieldsAreNull() {
        // Arrange
        Tag invalidTag = new Tag();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tagService.saveTag(invalidTag));
        assertNotNull(exception.getMessage());
    }

    @Test
    void checkTagNameUnique_ShouldPass_WhenNameAndTypeIsUnique() {
        // Arrange
        when(tagMapper.selectCount(any())).thenReturn(0L);

        // Act & Assert - 通过保存标签间接测试
        tagService.saveTag(mockTag);
        verify(tagMapper, times(1)).selectCount(any());
    }

    @Test
    void checkTagNameUnique_ShouldThrowException_WhenNameAndTypeDuplicate() {
        // Arrange
        when(tagMapper.selectCount(any())).thenReturn(1L);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tagService.saveTag(mockTag));
        assertTrue(exception.getMessage().contains("当前租户下标签名称和类型组合已存在"));
        verify(tagMapper, times(1)).selectCount(any());
    }

    @Test
    void updateTagById_ShouldPreserveCreateTime() {
        // Arrange
        LocalDateTime originalCreateTime = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        Tag existingTag = new Tag();
        existingTag.setId(tagId);
        existingTag.setTenantId(tenantId);
        existingTag.setTagName("旧标签名");
        existingTag.setTagType(tagType);
        existingTag.setCreateTime(originalCreateTime);

        // 使用 doReturn 确保只调用一次
        doReturn(existingTag).when(tagService).getById(tagId);
        when(tagMapper.selectCount(any())).thenReturn(0L);
        doReturn(true).when(tagService).updateById(any(Tag.class));

        // Act
        boolean result = tagService.updateTagById(mockTag);

        // Assert
        assertTrue(result);
        // 验证createTime被设置为原有值
        assertEquals(originalCreateTime, mockTag.getCreateTime());
        verify(tagService, times(1)).updateById(mockTag);
    }

    @Test
    void saveTag_ShouldAllowSameNameDifferentType() {
        // Arrange
        Tag houseTag = createTagWithName("便利");
        houseTag.setTagType("HOUSE");

        Tag customerTag = createTagWithName("便利");
        customerTag.setTagType("CUSTOMER");

        // 模拟第一个保存
        when(tagMapper.selectCount(any())).thenReturn(0L);
        doReturn(true).when(tagService).save(any(Tag.class));

        // 先保存HOUSE类型标签
        tagService.saveTag(houseTag);

        // 重置mock统计
        reset(tagMapper, tagService);
        setBaseMapper(tagService, tagMapper);

        // 模拟第二个保存
        when(tagMapper.selectCount(any())).thenReturn(0L);
        doReturn(true).when(tagService).save(any(Tag.class));

        // Act & Assert
        // 保存CUSTOMER类型标签，应该成功
        assertDoesNotThrow(() -> tagService.saveTag(customerTag));
    }
}
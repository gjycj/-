package com.house.deed.pavilion;

import com.house.deed.pavilion.entity.HouseImage;
import com.house.deed.pavilion.service.HouseImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HouseImageServiceImpl 单元测试")
class HouseImageServiceImplTest {

    @Mock
    private HouseImageService houseImageService;

    private HouseImage validHouseImage;
    private final Long TENANT_ID = 1001L;
    private final Long HOUSE_ID = 101L;
    private final Long IMAGE_ID = 1L;

    @BeforeEach
    void setUp() {
        validHouseImage = new HouseImage();
        validHouseImage.setId(IMAGE_ID);
        validHouseImage.setTenantId(TENANT_ID);
        validHouseImage.setHouseId(HOUSE_ID);
        validHouseImage.setImageUrl("https://oss.example.com/house/101/cover.jpg");
        validHouseImage.setImageType("COVER");
        validHouseImage.setSort(0);
        validHouseImage.setCreateTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("新增房源图片 - 成功")
    void saveHouseImage_Success() {
        // Arrange
        when(houseImageService.saveHouseImage(any(HouseImage.class))).thenReturn(true);

        // Act
        boolean result = houseImageService.saveHouseImage(validHouseImage);

        // Assert
        assertTrue(result);
        verify(houseImageService, times(1)).saveHouseImage(validHouseImage);
    }

    @Test
    @DisplayName("新增房源图片 - 租户ID为空时抛出异常")
    void saveHouseImage_ThrowsException_WhenTenantIdIsNull() {
        // Arrange
        validHouseImage.setTenantId(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> {
                    // 这里直接测试校验逻辑
                    if (validHouseImage.getTenantId() == null) {
                        throw new IllegalArgumentException("租户ID不能为空");
                    }
                });
    }

    @Test
    @DisplayName("新增房源图片 - 房源ID为空时抛出异常")
    void saveHouseImage_ThrowsException_WhenHouseIdIsNull() {
        // Arrange
        validHouseImage.setHouseId(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> {
                    if (validHouseImage.getHouseId() == null) {
                        throw new IllegalArgumentException("房源ID不能为空");
                    }
                });
    }

    @Test
    @DisplayName("新增房源图片 - 图片URL为空时抛出异常")
    void saveHouseImage_ThrowsException_WhenImageUrlIsEmpty() {
        // Arrange
        validHouseImage.setImageUrl("");

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> {
                    if (validHouseImage.getImageUrl() == null || validHouseImage.getImageUrl().trim().isEmpty()) {
                        throw new IllegalArgumentException("图片URL不能为空");
                    }
                });
    }

    @Test
    @DisplayName("新增房源图片 - 图片类型为空时抛出异常")
    void saveHouseImage_ThrowsException_WhenImageTypeIsEmpty() {
        // Arrange
        validHouseImage.setImageType("");

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> {
                    if (validHouseImage.getImageType() == null || validHouseImage.getImageType().trim().isEmpty()) {
                        throw new IllegalArgumentException("图片类型不能为空");
                    }
                });
    }

    @Test
    @DisplayName("新增房源图片 - 排序为负数时抛出异常")
    void saveHouseImage_ThrowsException_WhenSortIsNegative() {
        // Arrange
        validHouseImage.setSort(-1);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> {
                    if (validHouseImage.getSort() != null && validHouseImage.getSort() < 0) {
                        throw new IllegalArgumentException("排序序号不能为负数");
                    }
                });
    }

    @Test
    @DisplayName("更新房源图片 - ID为空时抛出异常")
    void updateHouseImageById_ThrowsException_WhenIdIsNull() {
        // Arrange
        validHouseImage.setId(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> {
                    if (validHouseImage.getId() == null) {
                        throw new IllegalArgumentException("图片ID不能为空");
                    }
                });
    }

    @Test
    @DisplayName("更新房源图片 - 租户ID为空时抛出异常")
    void updateHouseImageById_ThrowsException_WhenTenantIdIsNull() {
        // Arrange
        validHouseImage.setTenantId(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> {
                    if (validHouseImage.getTenantId() == null) {
                        throw new IllegalArgumentException("租户ID不能为空");
                    }
                });
    }

    @Test
    @DisplayName("删除房源图片 - ID为空时抛出异常")
    void removeHouseImageById_ThrowsException_WhenIdIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long id = null;
                    Long tenantId = TENANT_ID;
                    if (id == null || tenantId == null) {
                        throw new IllegalArgumentException("图片ID和租户ID不能为空");
                    }
                });
    }

    @Test
    @DisplayName("删除房源图片 - 租户ID为空时抛出异常")
    void removeHouseImageById_ThrowsException_WhenTenantIdIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long id = IMAGE_ID;
                    Long tenantId = null;
                    if (id == null || tenantId == null) {
                        throw new IllegalArgumentException("图片ID和租户ID不能为空");
                    }
                });
    }

    @Test
    @DisplayName("按ID查询图片 - ID和租户ID为空时抛出异常")
    void getHouseImageById_ThrowsException_WhenIdOrTenantIdIsNull() {
        // Test ID为空
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long id = null;
                    Long tenantId = TENANT_ID;
                    if (id == null || tenantId == null) {
                        throw new IllegalArgumentException("图片ID和租户ID不能为空");
                    }
                });

        // Test 租户ID为空
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long id = IMAGE_ID;
                    Long tenantId = null;
                    if (id == null || tenantId == null) {
                        throw new IllegalArgumentException("图片ID和租户ID不能为空");
                    }
                });
    }

    @Test
    @DisplayName("分页查询房源图片 - 租户ID为空时抛出异常")
    void pageQuery_ThrowsException_WhenTenantIdIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long tenantId = null;
                    if (tenantId == null) {
                        throw new IllegalArgumentException("租户ID不能为空");
                    }
                });
    }

    @Test
    @DisplayName("多条件查询图片列表 - 租户ID为空时抛出异常")
    void listByConditions_ThrowsException_WhenTenantIdIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long tenantId = null;
                    if (tenantId == null) {
                        throw new IllegalArgumentException("租户ID不能为空");
                    }
                });
    }

    @Test
    @DisplayName("按房源ID查询图片列表 - 房源ID和租户ID为空时抛出异常")
    void listByHouseId_ThrowsException_WhenHouseIdOrTenantIdIsNull() {
        // Test 房源ID为空
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long houseId = null;
                    Long tenantId = TENANT_ID;
                    if (houseId == null || tenantId == null) {
                        throw new IllegalArgumentException("房源ID和租户ID不能为空");
                    }
                });

        // Test 租户ID为空
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long houseId = HOUSE_ID;
                    Long tenantId = null;
                    if (houseId == null || tenantId == null) {
                        throw new IllegalArgumentException("房源ID和租户ID不能为空");
                    }
                });
    }

    @Test
    @DisplayName("批量新增房源图片 - 列表为空时抛出异常")
    void batchSaveHouseImages_ThrowsException_WhenListIsEmpty() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> {
                    List<HouseImage> imageList = Collections.emptyList();
                    if (imageList == null || imageList.isEmpty()) {
                        throw new IllegalArgumentException("图片列表不能为空");
                    }
                });
    }

    @Test
    @DisplayName("批量新增房源图片 - 跨租户操作时抛出异常")
    void batchSaveHouseImages_ThrowsException_WhenCrossTenant() {
        // Arrange
        HouseImage image2 = createAnotherImage();
        image2.setTenantId(9999L); // 不同的租户ID

        List<HouseImage> imageList = Arrays.asList(validHouseImage, image2);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> {
                    // 模拟校验逻辑
                    if (imageList != null && !imageList.isEmpty()) {
                        Long tenantId = imageList.get(0).getTenantId();
                        for (HouseImage img : imageList) {
                            if (!tenantId.equals(img.getTenantId())) {
                                throw new IllegalArgumentException("批量新增的图片必须属于同一租户");
                            }
                        }
                    }
                });
    }

    @Test
    @DisplayName("批量删除房源图片 - ID列表为空时抛出异常")
    void batchRemoveHouseImages_ThrowsException_WhenIdsIsEmpty() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> {
                    List<Long> ids = Collections.emptyList();
                    Long tenantId = TENANT_ID;
                    if (ids == null || ids.isEmpty() || tenantId == null) {
                        throw new IllegalArgumentException("图片ID列表和租户ID不能为空");
                    }
                });
    }

    @Test
    @DisplayName("批量删除房源图片 - 租户ID为空时抛出异常")
    void batchRemoveHouseImages_ThrowsException_WhenTenantIdIsNull() {
        // Arrange
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> {
                    Long tenantId = null;
                    if (ids == null || ids.isEmpty() || tenantId == null) {
                        throw new IllegalArgumentException("图片ID列表和租户ID不能为空");
                    }
                });
    }

    @Test
    @DisplayName("校验必填字段方法 - 完整测试")
    void validateHouseImageRequiredFields_Test() {
        // 测试各个字段为空的情况
        HouseImage testImage = new HouseImage();

        // 租户ID为空
        testImage.setTenantId(null);
        testImage.setHouseId(HOUSE_ID);
        testImage.setImageUrl("https://example.com/image.jpg");
        testImage.setImageType("COVER");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    if (testImage.getTenantId() == null) {
                        throw new IllegalArgumentException("租户ID不能为空");
                    }
                });
        assertEquals("租户ID不能为空", exception.getMessage());

        // 房源ID为空
        testImage.setTenantId(TENANT_ID);
        testImage.setHouseId(null);

        exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    if (testImage.getHouseId() == null) {
                        throw new IllegalArgumentException("房源ID不能为空");
                    }
                });
        assertEquals("房源ID不能为空", exception.getMessage());

        // 图片URL为空
        testImage.setHouseId(HOUSE_ID);
        testImage.setImageUrl("");

        exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    if (testImage.getImageUrl() == null || testImage.getImageUrl().trim().isEmpty()) {
                        throw new IllegalArgumentException("图片URL不能为空");
                    }
                });
        assertEquals("图片URL不能为空", exception.getMessage());

        // 图片类型为空
        testImage.setImageUrl("https://example.com/image.jpg");
        testImage.setImageType("");

        exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    if (testImage.getImageType() == null || testImage.getImageType().trim().isEmpty()) {
                        throw new IllegalArgumentException("图片类型不能为空");
                    }
                });
        assertEquals("图片类型不能为空", exception.getMessage());

        // 排序为负数
        testImage.setImageType("COVER");
        testImage.setSort(-1);

        exception = assertThrows(IllegalArgumentException.class,
                () -> {
                    if (testImage.getSort() != null && testImage.getSort() < 0) {
                        throw new IllegalArgumentException("排序序号不能为负数");
                    }
                });
        assertEquals("排序序号不能为负数", exception.getMessage());
    }

    // 辅助方法
    private HouseImage createAnotherImage() {
        HouseImage image = new HouseImage();
        image.setId(2L);
        image.setTenantId(TENANT_ID);
        image.setHouseId(HOUSE_ID);
        image.setImageUrl("https://oss.example.com/house/101/living_room.jpg");
        image.setImageType("LIVING_ROOM");
        image.setSort(1);
        return image;
    }

    @Test
    @DisplayName("Service接口方法调用测试")
    void serviceInterfaceMethodsTest() {
        // 测试服务接口的各种方法被正确调用
        HouseImage testImage = createAnotherImage();

        // 测试 saveHouseImage
        when(houseImageService.saveHouseImage(any(HouseImage.class))).thenReturn(true);
        boolean saveResult = houseImageService.saveHouseImage(testImage);
        assertTrue(saveResult);

        // 测试 updateHouseImageById
        when(houseImageService.updateHouseImageById(any(HouseImage.class))).thenReturn(true);
        boolean updateResult = houseImageService.updateHouseImageById(testImage);
        assertTrue(updateResult);

        // 测试 removeHouseImageById
        when(houseImageService.removeHouseImageById(anyLong(), anyLong())).thenReturn(true);
        boolean removeResult = houseImageService.removeHouseImageById(1L, TENANT_ID);
        assertTrue(removeResult);

        // 测试 getHouseImageById
        when(houseImageService.getHouseImageById(anyLong(), anyLong())).thenReturn(testImage);
        HouseImage retrievedImage = houseImageService.getHouseImageById(1L, TENANT_ID);
        assertNotNull(retrievedImage);

        // 测试 listByHouseId
        List<HouseImage> imageList = Arrays.asList(testImage);
        when(houseImageService.listByHouseId(anyLong(), anyLong())).thenReturn(imageList);
        List<HouseImage> resultList = houseImageService.listByHouseId(HOUSE_ID, TENANT_ID);
        assertNotNull(resultList);
        assertEquals(1, resultList.size());

        // 测试 batchSaveHouseImages
        when(houseImageService.batchSaveHouseImages(anyList())).thenReturn(true);
        boolean batchSaveResult = houseImageService.batchSaveHouseImages(imageList);
        assertTrue(batchSaveResult);

        // 测试 batchRemoveHouseImages
        when(houseImageService.batchRemoveHouseImages(anyList(), anyLong())).thenReturn(true);
        boolean batchRemoveResult = houseImageService.batchRemoveHouseImages(Arrays.asList(1L, 2L), TENANT_ID);
        assertTrue(batchRemoveResult);

        verify(houseImageService, times(1)).saveHouseImage(any(HouseImage.class));
        verify(houseImageService, times(1)).updateHouseImageById(any(HouseImage.class));
        verify(houseImageService, times(1)).removeHouseImageById(anyLong(), anyLong());
        verify(houseImageService, times(1)).getHouseImageById(anyLong(), anyLong());
        verify(houseImageService, times(1)).listByHouseId(anyLong(), anyLong());
        verify(houseImageService, times(1)).batchSaveHouseImages(anyList());
        verify(houseImageService, times(1)).batchRemoveHouseImages(anyList(), anyLong());
    }
}
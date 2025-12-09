package com.house.deed.pavilion;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.entity.Store;
import com.house.deed.pavilion.mapper.StoreMapper;
import com.house.deed.pavilion.service.impl.StoreServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * StoreServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class StoreServiceImplTest {

    @Mock
    private StoreMapper storeMapper;

    @Spy
    @InjectMocks
    private StoreServiceImpl storeService;

    private Store mockStore;
    private final Long tenantId = 1L;
    private final Long storeId = 100L;
    private final String storeCode = "STORE001";

    @BeforeEach
    void setUp() {
        mockStore = new Store();
        mockStore.setId(storeId);
        mockStore.setTenantId(tenantId);
        mockStore.setStoreCode(storeCode);
        mockStore.setStoreName("测试门店");
        mockStore.setRegionId(10L);
        mockStore.setManagerId(20L);
        mockStore.setStatus((byte) 1);
        mockStore.setCreateTime(LocalDateTime.now());
        mockStore.setUpdateTime(LocalDateTime.now());

        // 通过反射设置 baseMapper
        setBaseMapper(storeService, storeMapper);
    }

    /**
     * 通过反射设置 ServiceImpl 的 baseMapper 字段
     */
    private void setBaseMapper(StoreServiceImpl service, StoreMapper mapper) {
        try {
            var field = storeService.getClass().getSuperclass().getDeclaredField("baseMapper");
            field.setAccessible(true);
            field.set(storeService, mapper);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set baseMapper", e);
        }
    }

    @Test
    void saveStore_ShouldSuccess_WhenValidStore() {
        // Arrange
        when(storeMapper.selectCount(any())).thenReturn(0L);
        when(storeMapper.insert(any(Store.class))).thenReturn(1);

        // Act
        boolean result = storeService.saveStore(mockStore);

        // Assert
        assertTrue(result);
        verify(storeMapper, times(1)).selectCount(any());
        verify(storeMapper, times(1)).insert(mockStore);
    }

    @Test
    void saveStore_ShouldThrowException_WhenTenantIdIsNull() {
        // Arrange
        mockStore.setTenantId(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> storeService.saveStore(mockStore));
        assertEquals("租户ID不能为空", exception.getMessage());
        verify(storeMapper, never()).selectCount(any());
        verify(storeMapper, never()).insert(any());
    }

    @Test
    void saveStore_ShouldThrowException_WhenStoreCodeExists() {
        // Arrange
        when(storeMapper.selectCount(any())).thenReturn(1L);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> storeService.saveStore(mockStore));
        assertEquals("当前租户下已存在相同门店编码：" + storeCode, exception.getMessage());
        verify(storeMapper, times(1)).selectCount(any());
        verify(storeMapper, never()).insert(any());
    }

    @Test
    void updateStoreById_ShouldSuccess_WhenValidStore() {
        // Arrange
        Store existingStore = new Store();
        existingStore.setId(storeId);
        existingStore.setTenantId(tenantId);
        existingStore.setStoreCode("OLD_CODE");

        when(storeMapper.selectById(storeId)).thenReturn(existingStore);
        when(storeMapper.selectCount(any())).thenReturn(0L);
        when(storeMapper.updateById(any(Store.class))).thenReturn(1);

        // Act
        boolean result = storeService.updateStoreById(mockStore);

        // Assert
        assertTrue(result);
        verify(storeMapper, times(1)).selectById(storeId);
        verify(storeMapper, times(1)).selectCount(any());
        verify(storeMapper, times(1)).updateById(mockStore);
    }

    @Test
    void updateStoreById_ShouldThrowException_WhenTenantIdOrIdIsNull() {
        // Arrange
        Store storeWithoutTenantId = new Store();
        storeWithoutTenantId.setId(storeId);
        storeWithoutTenantId.setTenantId(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> storeService.updateStoreById(storeWithoutTenantId));
        assertEquals("租户ID和门店ID不能为空", exception.getMessage());
        verify(storeMapper, never()).selectById(any());
    }

    @Test
    void updateStoreById_ShouldThrowException_WhenStoreNotBelongToTenant() {
        // Arrange
        Store existingStore = new Store();
        existingStore.setId(storeId);
        existingStore.setTenantId(999L); // 不同的租户

        when(storeMapper.selectById(storeId)).thenReturn(existingStore);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> storeService.updateStoreById(mockStore));
        assertEquals("门店不存在或无权限操作", exception.getMessage());
        verify(storeMapper, times(1)).selectById(storeId);
        verify(storeMapper, never()).updateById(any());
    }

    @Test
    void removeStoreById_ShouldSuccess_WhenValidId() {
        // Arrange
        when(storeMapper.selectById(storeId)).thenReturn(mockStore);
        when(storeMapper.deleteById(storeId)).thenReturn(1);

        // Act
        boolean result = storeService.removeStoreById(storeId, tenantId);

        // Assert
        assertTrue(result);
        verify(storeMapper, times(1)).selectById(storeId);
        verify(storeMapper, times(1)).deleteById(storeId);
    }

    @Test
    void removeStoreById_ShouldThrowException_WhenIdOrTenantIdIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> storeService.removeStoreById(null, tenantId));
        assertThrows(IllegalArgumentException.class,
                () -> storeService.removeStoreById(storeId, null));
        assertThrows(IllegalArgumentException.class,
                () -> storeService.removeStoreById(null, null));
    }

    @Test
    void getStoreById_ShouldReturnStore_WhenValidId() {
        // Arrange
        when(storeMapper.selectOne(any())).thenReturn(mockStore);

        // Act
        Store result = storeService.getStoreById(storeId, tenantId);

        // Assert
        assertNotNull(result);
        assertEquals(storeId, result.getId());
        assertEquals(tenantId, result.getTenantId());
        verify(storeMapper, times(1)).selectOne(any());
    }

    @Test
    void getStoreById_ShouldReturnNull_WhenIdOrTenantIdIsNull() {
        // Act & Assert
        assertNull(storeService.getStoreById(null, tenantId));
        assertNull(storeService.getStoreById(storeId, null));
        assertNull(storeService.getStoreById(null, null));
        verify(storeMapper, never()).selectOne(any());
    }

    @Test
    void pageQuery_ShouldReturnPage_WhenValidParams() {
        // Arrange
        Page<Store> page = new Page<>(1, 10);
        Map<String, Object> params = new HashMap<>();
        params.put("storeName", "测试");
        params.put("status", 1);

        when(storeMapper.selectPage(eq(page), any())).thenReturn(page);

        // Act
        IPage<Store> result = storeService.pageQuery(page, params, tenantId);

        // Assert
        assertNotNull(result);
        verify(storeMapper, times(1)).selectPage(eq(page), any());
    }

    @Test
    void listByConditions_ShouldReturnList_WhenValidParams() {
        // Arrange
        List<Store> storeList = Arrays.asList(mockStore, new Store());
        Map<String, Object> params = new HashMap<>();
        params.put("regionId", 10L);

        when(storeMapper.selectList(any())).thenReturn(storeList);

        // Act
        List<Store> result = storeService.listByConditions(params, tenantId);

        // Assert
        assertEquals(2, result.size());
        verify(storeMapper, times(1)).selectList(any());
    }

    @Test
    void listByRegionId_ShouldReturnSortedList_WhenValidRegionId() {
        // Arrange
        Long regionId = 10L;
        Store store1 = createStore(1L, "B门店");
        Store store2 = createStore(2L, "A门店");
        List<Store> storeList = Arrays.asList(store1, store2);

        when(storeMapper.selectList(any())).thenReturn(storeList);

        // Act
        List<Store> result = storeService.listByRegionId(regionId, tenantId);

        // Assert
        assertEquals(2, result.size());
        verify(storeMapper, times(1)).selectList(any());
    }

    @Test
    void listByRegionId_ShouldReturnEmptyList_WhenRegionIdOrTenantIdIsNull() {
        // Act & Assert
        assertEquals(0, storeService.listByRegionId(null, tenantId).size());
        assertEquals(0, storeService.listByRegionId(10L, null).size());
        verify(storeMapper, never()).selectList(any());
    }

    @Test
    void batchSaveStores_ShouldSuccess_WhenValidStores() {
        // Arrange
        List<Store> stores = Arrays.asList(
                createStoreWithCode("STORE001"),
                createStoreWithCode("STORE002")
        );

        when(storeMapper.selectCount(any())).thenReturn(0L);
        // 使用 doReturn 来模拟 saveBatch 方法
        doReturn(true).when(storeService).saveBatch(anyList());

        // Act
        boolean result = storeService.batchSaveStores(stores);

        // Assert
        assertTrue(result);
        verify(storeMapper, times(1)).selectCount(any());
        verify(storeService, times(1)).saveBatch(anyList());
    }

    @Test
    void batchSaveStores_ShouldThrowException_WhenListIsEmpty() {
        // Arrange
        List<Store> emptyList = Collections.emptyList();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> storeService.batchSaveStores(emptyList));
        assertEquals("批量新增的门店列表不能为空", exception.getMessage());
    }

    @Test
    void batchSaveStores_ShouldThrowException_WhenStoreCodesDuplicate() {
        // Arrange
        List<Store> stores = Arrays.asList(
                createStoreWithCode("STORE001"),
                createStoreWithCode("STORE002")
        );

        when(storeMapper.selectCount(any())).thenReturn(1L);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> storeService.batchSaveStores(stores));
        assertEquals("批量新增的门店中存在重复编码", exception.getMessage());
        verify(storeMapper, times(1)).selectCount(any());
    }

    @Test
    void batchSaveStores_ShouldThrowException_WhenTenantIdsNotSame() {
        // Arrange
        Store store1 = createStoreWithCode("STORE001");
        Store store2 = createStoreWithCode("STORE002");
        store2.setTenantId(999L);
        List<Store> stores = Arrays.asList(store1, store2);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> storeService.batchSaveStores(stores));
        assertEquals("批量新增的门店必须属于同一租户", exception.getMessage());
        verify(storeMapper, never()).selectCount(any());
    }

    @Test
    void batchUpdateStatus_ShouldSuccess_WhenValidParams() {
        // Arrange
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        Byte status = (byte) 1;

        when(storeMapper.selectCount(any())).thenReturn(3L);
        when(storeMapper.update(any(Store.class), any())).thenReturn(3);

        // Act
        boolean result = storeService.batchUpdateStatus(ids, status, tenantId);

        // Assert
        assertTrue(result);
        verify(storeMapper, times(1)).selectCount(any());
        verify(storeMapper, times(1)).update(any(Store.class), any());
    }

    @Test
    void batchUpdateStatus_ShouldThrowException_WhenParamsAreNull() {
        // Arrange
        List<Long> ids = Arrays.asList(1L, 2L);
        Byte status = (byte) 1;

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> storeService.batchUpdateStatus(null, status, tenantId));
        assertThrows(IllegalArgumentException.class,
                () -> storeService.batchUpdateStatus(ids, null, tenantId));
        assertThrows(IllegalArgumentException.class,
                () -> storeService.batchUpdateStatus(ids, status, null));
        verify(storeMapper, never()).selectCount(any());
    }

    @Test
    void batchUpdateStatus_ShouldThrowException_WhenStatusInvalid() {
        // Arrange
        List<Long> ids = Arrays.asList(1L, 2L);
        Byte invalidStatus = (byte) 2; // 无效状态

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> storeService.batchUpdateStatus(ids, invalidStatus, tenantId));
        assertEquals("状态值只能是0（停业）或1（营业）", exception.getMessage());
        verify(storeMapper, never()).selectCount(any());
    }

    @Test
    void batchUpdateStatus_ShouldThrowException_WhenCountNotMatch() {
        // Arrange
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        Byte status = (byte) 1;

        when(storeMapper.selectCount(any())).thenReturn(2L); // 只找到2个，但传入了3个

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> storeService.batchUpdateStatus(ids, status, tenantId));
        assertEquals("部分门店不存在或无权限操作", exception.getMessage());
        verify(storeMapper, times(1)).selectCount(any());
        verify(storeMapper, never()).update(any(), any());
    }

    // Helper methods
    private Store createStore(Long id, String storeName) {
        Store store = new Store();
        store.setId(id);
        store.setTenantId(tenantId);
        store.setStoreName(storeName);
        store.setRegionId(10L);
        return store;
    }

    private Store createStoreWithCode(String storeCode) {
        Store store = new Store();
        store.setTenantId(tenantId);
        store.setStoreCode(storeCode);
        return store;
    }
}
package com.house.deed.pavilion;

import com.house.deed.pavilion.entity.TenantConfig;
import com.house.deed.pavilion.service.impl.TenantConfigServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TenantConfigServiceImpl 集成测试
 * 使用 Spring Boot Test 和真实数据库
 */
@SpringBootTest
@Transactional  // 测试后自动回滚，不污染数据库
class TenantConfigServiceImplTest {

    @Autowired
    private TenantConfigServiceImpl tenantConfigService;

    @Test
    void saveTenantConfig_Success() {
        // 准备数据
        TenantConfig newConfig = new TenantConfig();
        newConfig.setTenantId(1001L);
        newConfig.setConfigKey("test.save.key");
        newConfig.setConfigValue("testValue");
        newConfig.setConfigDesc("测试保存");
        newConfig.setIsSystem((byte) 0);

        // 执行测试
        boolean result = tenantConfigService.saveTenantConfig(newConfig);

        // 验证
        assertTrue(result);
        assertNotNull(newConfig.getId());

        // 验证数据确实保存到了数据库
        TenantConfig savedConfig = tenantConfigService.getById(newConfig.getId());
        assertNotNull(savedConfig);
        assertEquals("test.save.key", savedConfig.getConfigKey());
        assertEquals("testValue", savedConfig.getConfigValue());
    }

    @Test
    void saveTenantConfig_ConfigKeyExists_ThrowsException() {
        // 准备第一个配置
        TenantConfig config1 = new TenantConfig();
        config1.setTenantId(1001L);
        config1.setConfigKey("duplicate.key");
        config1.setConfigValue("value1");
        config1.setConfigDesc("第一个配置");
        tenantConfigService.saveTenantConfig(config1);

        // 准备重复的配置
        TenantConfig config2 = new TenantConfig();
        config2.setTenantId(1001L);
        config2.setConfigKey("duplicate.key"); // 相同的租户ID和配置键
        config2.setConfigValue("value2");

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tenantConfigService.saveTenantConfig(config2));

        assertTrue(exception.getMessage().contains("配置项已存在"));
    }

    @Test
    void updateTenantConfig_Success() {
        // 先保存一个配置
        TenantConfig config = new TenantConfig();
        config.setTenantId(1001L);
        config.setConfigKey("update.test.key");
        config.setConfigValue("oldValue");
        config.setConfigDesc("旧描述");
        tenantConfigService.saveTenantConfig(config);

        // 准备更新数据
        config.setConfigKey("updated.key");
        config.setConfigValue("newValue");
        config.setConfigDesc("新描述");

        // 执行测试
        boolean result = tenantConfigService.updateTenantConfig(config);

        // 验证
        assertTrue(result);

        // 验证数据已更新
        TenantConfig updatedConfig = tenantConfigService.getById(config.getId());
        assertEquals("updated.key", updatedConfig.getConfigKey());
        assertEquals("newValue", updatedConfig.getConfigValue());
        assertEquals("新描述", updatedConfig.getConfigDesc());
    }

    @Test
    void updateTenantConfig_ConfigNotFound_ThrowsException() {
        // 准备不存在的配置
        TenantConfig config = new TenantConfig();
        config.setId(99999L); // 不存在的ID
        config.setTenantId(1001L);
        config.setConfigKey("not.exist");

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tenantConfigService.updateTenantConfig(config));

        assertTrue(exception.getMessage().contains("配置项不存在"));
    }

    @Test
    void removeTenantConfig_Success() {
        // 先保存一个配置
        TenantConfig config = new TenantConfig();
        config.setTenantId(1001L);
        config.setConfigKey("delete.test.key");
        config.setConfigValue("value");
        tenantConfigService.saveTenantConfig(config);

        Long configId = config.getId();

        // 执行删除
        boolean result = tenantConfigService.removeTenantConfig(configId);

        // 验证
        assertTrue(result);

        // 验证配置已删除
        TenantConfig deletedConfig = tenantConfigService.getById(configId);
        assertNull(deletedConfig);
    }

    @Test
    void removeTenantConfig_ConfigNotFound_ThrowsException() {
        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tenantConfigService.removeTenantConfig(99999L));

        assertTrue(exception.getMessage().contains("配置项不存在"));
    }

    @Test
    void removeTenantConfig_SystemConfig_ThrowsException() {
        // 先保存一个系统内置配置
        TenantConfig systemConfig = new TenantConfig();
        systemConfig.setTenantId(0L);
        systemConfig.setConfigKey("system.test.key");
        systemConfig.setConfigValue("systemValue");
        systemConfig.setIsSystem((byte) 1);
        tenantConfigService.save(systemConfig); // 使用 save 方法，不经过业务逻辑

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tenantConfigService.removeTenantConfig(systemConfig.getId()));

        assertTrue(exception.getMessage().contains("系统内置配置不可删除"));
    }

    @Test
    void batchRemove_Success() {
        // 保存多个配置
        TenantConfig config1 = new TenantConfig();
        config1.setTenantId(1001L);
        config1.setConfigKey("batch.delete.key1");
        config1.setConfigValue("value1");
        tenantConfigService.saveTenantConfig(config1);

        TenantConfig config2 = new TenantConfig();
        config2.setTenantId(1001L);
        config2.setConfigKey("batch.delete.key2");
        config2.setConfigValue("value2");
        tenantConfigService.saveTenantConfig(config2);

        List<Long> ids = Arrays.asList(config1.getId(), config2.getId());

        // 执行批量删除
        int result = tenantConfigService.batchRemove(ids);

        // 验证
        assertEquals(2, result);

        // 验证配置已删除
        for (Long id : ids) {
            assertNull(tenantConfigService.getById(id));
        }
    }

    @Test
    void batchRemove_ContainsSystemConfig_ThrowsException() {
        // 保存一个普通配置
        TenantConfig normalConfig = new TenantConfig();
        normalConfig.setTenantId(1001L);
        normalConfig.setConfigKey("normal.config");
        normalConfig.setConfigValue("value");
        tenantConfigService.saveTenantConfig(normalConfig);

        // 保存一个系统内置配置
        TenantConfig systemConfig = new TenantConfig();
        systemConfig.setTenantId(0L);
        systemConfig.setConfigKey("system.config");
        systemConfig.setConfigValue("systemValue");
        systemConfig.setIsSystem((byte) 1);
        tenantConfigService.save(systemConfig); // 使用 save 方法，不经过业务逻辑

        List<Long> ids = Arrays.asList(normalConfig.getId(), systemConfig.getId());

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tenantConfigService.batchRemove(ids));

        assertTrue(exception.getMessage().contains("包含系统内置配置"));
    }

    @Test
    void queryByConditions_AllConditions() {
        // 保存测试数据
        TenantConfig config1 = new TenantConfig();
        config1.setTenantId(1001L);
        config1.setConfigKey("theme.color");
        config1.setConfigValue("blue");
        config1.setIsSystem((byte) 0);
        tenantConfigService.saveTenantConfig(config1);

        TenantConfig config2 = new TenantConfig();
        config2.setTenantId(1002L);
        config2.setConfigKey("theme.size");
        config2.setConfigValue("large");
        config2.setIsSystem((byte) 0);
        tenantConfigService.saveTenantConfig(config2);

        // 执行查询
        List<TenantConfig> result = tenantConfigService.queryByConditions(
                1001L, "color", (byte) 0);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("theme.color", result.get(0).getConfigKey());
    }

    @Test
    void queryByConditions_NoConditions() {
        // 保存测试数据
        TenantConfig config1 = new TenantConfig();
        config1.setTenantId(1001L);
        config1.setConfigKey("key1");
        config1.setConfigValue("value1");
        tenantConfigService.saveTenantConfig(config1);

        TenantConfig config2 = new TenantConfig();
        config2.setTenantId(1002L);
        config2.setConfigKey("key2");
        config2.setConfigValue("value2");
        tenantConfigService.saveTenantConfig(config2);

        // 执行查询（无任何条件）
        List<TenantConfig> result = tenantConfigService.queryByConditions(
                null, null, null);

        // 验证至少包含我们保存的数据
        assertNotNull(result);
        assertTrue(result.size() >= 2);
    }

    @Test
    void batchSave_Success() {
        // 准备批量数据
        TenantConfig config1 = new TenantConfig();
        config1.setTenantId(1001L);
        config1.setConfigKey("batch.key1");
        config1.setConfigValue("value1");

        TenantConfig config2 = new TenantConfig();
        config2.setTenantId(1001L);
        config2.setConfigKey("batch.key2");
        config2.setConfigValue("value2");

        List<TenantConfig> configs = Arrays.asList(config1, config2);

        // 执行批量保存
        boolean result = tenantConfigService.batchSave(configs);

        // 验证
        assertTrue(result);

        // 验证数据已保存
        for (TenantConfig config : configs) {
            TenantConfig saved = tenantConfigService.getByTenantAndKey(
                    config.getTenantId(), config.getConfigKey());
            assertNotNull(saved);
            assertEquals(config.getConfigValue(), saved.getConfigValue());
        }
    }

    @Test
    void batchSave_DuplicateConfig_ThrowsException() {
        // 先保存一个配置
        TenantConfig existingConfig = new TenantConfig();
        existingConfig.setTenantId(1001L);
        existingConfig.setConfigKey("existing.batch.key");
        existingConfig.setConfigValue("value");
        tenantConfigService.saveTenantConfig(existingConfig);

        // 准备批量数据，包含重复的配置键
        TenantConfig config1 = new TenantConfig();
        config1.setTenantId(1001L);
        config1.setConfigKey("new.batch.key");

        TenantConfig config2 = new TenantConfig();
        config2.setTenantId(1001L);
        config2.setConfigKey("existing.batch.key"); // 重复的配置键

        List<TenantConfig> configs = Arrays.asList(config1, config2);

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tenantConfigService.batchSave(configs));

        assertTrue(exception.getMessage().contains("批量新增失败"));
    }

    @Test
    void batchUpdate_Success() {
        // 先保存两个配置
        TenantConfig config1 = new TenantConfig();
        config1.setTenantId(1001L);
        config1.setConfigKey("update.batch.key1");
        config1.setConfigValue("oldValue1");
        config1.setConfigDesc("旧描述1");
        tenantConfigService.saveTenantConfig(config1);

        TenantConfig config2 = new TenantConfig();
        config2.setTenantId(1001L);
        config2.setConfigKey("update.batch.key2");
        config2.setConfigValue("oldValue2");
        config2.setConfigDesc("旧描述2");
        tenantConfigService.saveTenantConfig(config2);

        // 准备更新数据
        config1.setConfigValue("newValue1");
        config1.setConfigDesc("新描述1");

        config2.setConfigValue("newValue2");
        config2.setConfigDesc("新描述2");

        List<TenantConfig> configs = Arrays.asList(config1, config2);

        // 执行批量更新
        boolean result = tenantConfigService.batchUpdate(configs);

        // 验证
        assertTrue(result);

        // 验证数据已更新
        TenantConfig updated1 = tenantConfigService.getById(config1.getId());
        assertEquals("newValue1", updated1.getConfigValue());
        assertEquals("新描述1", updated1.getConfigDesc());

        TenantConfig updated2 = tenantConfigService.getById(config2.getId());
        assertEquals("newValue2", updated2.getConfigValue());
        assertEquals("新描述2", updated2.getConfigDesc());
    }

    @Test
    void batchUpdate_MissingId_ThrowsException() {
        // 准备没有ID的配置
        TenantConfig config = new TenantConfig();
        config.setConfigValue("newValue");
        config.setConfigDesc("newDesc");

        List<TenantConfig> configs = Collections.singletonList(config);

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tenantConfigService.batchUpdate(configs));

        assertTrue(exception.getMessage().contains("配置ID不能为空"));
    }

    @Test
    void getByTenantAndKey_Success() {
        // 保存一个配置
        TenantConfig config = new TenantConfig();
        config.setTenantId(1001L);
        config.setConfigKey("get.test.key");
        config.setConfigValue("testValue");
        tenantConfigService.saveTenantConfig(config);

        // 执行查询
        TenantConfig result = tenantConfigService.getByTenantAndKey(1001L, "get.test.key");

        // 验证
        assertNotNull(result);
        assertEquals("get.test.key", result.getConfigKey());
        assertEquals("testValue", result.getConfigValue());
    }

    @Test
    void getByTenantAndKey_NotFound_ReturnsNull() {
        // 执行查询不存在的配置
        TenantConfig result = tenantConfigService.getByTenantAndKey(9999L, "not.exist.key");

        // 验证
        assertNull(result);
    }

    @Test
    void batchSave_EmptyList_ReturnsFalse() {
        // 执行测试
        boolean result = tenantConfigService.batchSave(Collections.emptyList());

        // 验证
        assertFalse(result);
    }

    @Test
    void batchUpdate_EmptyList_ReturnsFalse() {
        // 执行测试
        boolean result = tenantConfigService.batchUpdate(Collections.emptyList());

        // 验证
        assertFalse(result);
    }

    @Test
    void batchRemove_EmptyList_ReturnsZero() {
        // 执行测试
        int result = tenantConfigService.batchRemove(Collections.emptyList());

        // 验证
        assertEquals(0, result);
    }

    @Test
    void batchRemove_NullList_ReturnsZero() {
        // 执行测试
        int result = tenantConfigService.batchRemove(null);

        // 验证
        assertEquals(0, result);
    }
}
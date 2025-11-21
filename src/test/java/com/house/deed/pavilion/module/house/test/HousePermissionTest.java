package com.house.deed.pavilion.module.house.test;

import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.AgentContext;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.house.entity.House;
import com.house.deed.pavilion.module.house.service.IHouseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
public class HousePermissionTest {

    @Autowired
    private IHouseService houseService;

    // 测试数据：房源ID=1（创建人=100）、当前经纪人ID=100（正常）/200（越权）
    private final Long HOUSE_ID = 1L;
    private final Long OWNER_AGENT_ID = 100L;
    private final Long OTHER_AGENT_ID = 200L;
    private final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID); // 固定租户
    }

    @AfterEach
    void tearDown() {
        AgentContext.clear(); // 清理上下文
        TenantContext.clear();
    }

    // 测试1：正常访问（查询自己的房源）
    @Test
    void testQueryHouseWithPermission() {
        AgentContext.setAgentId(OWNER_AGENT_ID);
        House house = houseService.getById(HOUSE_ID);
        assertNotNull(house, "正常查询应返回房源数据");
        assertEquals(OWNER_AGENT_ID, house.getCreateAgentId(), "查询结果应为自己创建的房源");
    }

    // 测试2：越权访问（查询他人房源）
    @Test
    void testQueryHouseWithoutPermission() {
        AgentContext.setAgentId(OTHER_AGENT_ID);
        assertThrows(BusinessException.class, () -> {
            houseService.getById(HOUSE_ID);
        }, "越权查询应抛出403异常");
    }

    // 测试3：正常更新自己的房源
    @Test
    void testUpdateHouseWithPermission() {
        AgentContext.setAgentId(OWNER_AGENT_ID);
        House updateHouse = new House();
        updateHouse.setId(HOUSE_ID);
        updateHouse.setPrice(new BigDecimal("2100000"));

        assertDoesNotThrow(() -> {
            houseService.updateById(updateHouse);
        }, "正常更新应无异常");
    }

    // 测试4：越权更新他人房源
    @Test
    void testUpdateHouseWithoutPermission() {
        AgentContext.setAgentId(OTHER_AGENT_ID);
        House updateHouse = new House();
        updateHouse.setId(HOUSE_ID);
        updateHouse.setPrice(new BigDecimal("2100000"));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            houseService.updateById(updateHouse);
        });
        assertEquals(403, exception.getCode(), "越权更新应抛出403异常");
    }
}
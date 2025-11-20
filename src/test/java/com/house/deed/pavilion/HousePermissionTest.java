package com.house.deed.pavilion;

import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.AgentContext;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.house.controller.HouseController;
import com.house.deed.pavilion.module.house.entity.House;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class HousePermissionTest {

    @Resource
    private HouseController houseController;

    @Test
    public void testUpdateHouseWithoutPermission() {
        // 模拟非创建人更新房源
        Long houseId = 1L; // 假设该房源的createAgentId为100
        House updateHouse = new House();
        updateHouse.setId(houseId);
        updateHouse.setPrice(new BigDecimal("2000000"));

        // 模拟当前经纪人ID为200（非创建人）
        AgentContext.setAgentId(200L);
        TenantContext.setTenantId(1L);

        // 预期抛出403异常
        assertThrows(BusinessException.class, () -> {
            houseController.updateHouse(houseId, updateHouse);
        });
    }
}
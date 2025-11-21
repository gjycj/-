package com.house.deed.pavilion.module.maintenanceOrder.test;

import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.AgentContext;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.maintenanceOrder.service.IMaintenanceOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class MaintenanceOrderPermissionTest {

    @Autowired
    private IMaintenanceOrderService maintenanceOrderService;

    // 测试数据：房源ID=1（创建人=100）、当前经纪人=100（正常）/200（越权）
    private final Long HOUSE_ID = 1L;
    private final Long OWNER_AGENT_ID = 100L;
    private final Long OTHER_AGENT_ID = 200L;
    private final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        AgentContext.clear();
        TenantContext.clear();
    }

    // 测试：查询维修工单时，关联房源的创建人权限校验
    @Test
    void testGetByHouseIdWithoutPermission() {
        AgentContext.setAgentId(OTHER_AGENT_ID); // 非房源创建人
        assertThrows(BusinessException.class, () -> {
            maintenanceOrderService.getByHouseId(HOUSE_ID);
        }, "越权查询他人房源的维修工单应抛出403异常");
    }
}
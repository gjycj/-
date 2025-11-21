package com.house.deed.pavilion.module.visitRecord.test;

import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.AgentContext;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.visitRecord.entity.VisitRecord;
import com.house.deed.pavilion.module.visitRecord.service.IVisitRecordService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class VisitRecordPermissionTest {

    @Autowired
    private IVisitRecordService visitRecordService;

    // 测试数据：客户ID=10（创建人=100）、房源ID=1（创建人=100）、当前经纪人=100（正常）/200（越权）
    private final Long CUSTOMER_ID = 10L;
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

    // 测试：创建带看记录时，校验客户和房源归属（越权带看他人客户）
    @Test
    void testCreateVisitRecordWithInvalidCustomer() {
        AgentContext.setAgentId(OTHER_AGENT_ID); // 非客户/房源创建人
        VisitRecord dto = new VisitRecord();
        dto.setCustomerId(CUSTOMER_ID);
        dto.setHouseId(HOUSE_ID);
        dto.setVisitTime(LocalDateTime.now());

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            visitRecordService.save(dto);
        });
        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("无权带看该客户"), "应拦截他人客户的带看操作");
    }
}
package com.house.deed.pavilion.integration.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.house.deed.pavilion.common.util.AgentContext;
import com.house.deed.pavilion.common.util.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // 通用测试数据
    protected final Long TENANT_ID = 1L;
    protected final Long OWNER_AGENT_ID = 100L; // 数据创建人
    protected final Long OTHER_AGENT_ID = 200L; // 其他经纪人

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        AgentContext.clear();
        TenantContext.clear();
    }
}
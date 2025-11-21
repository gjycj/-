package com.house.deed.pavilion.integration.test.customer;

import com.house.deed.pavilion.common.util.AgentContext;
import com.house.deed.pavilion.integration.test.BaseIntegrationTest;
import com.house.deed.pavilion.module.customer.entity.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

public class CustomerApiPermissionTest extends BaseIntegrationTest {

    // 测试客户ID=100（创建人=OWNER_AGENT_ID）
    private final Long CUSTOMER_ID = 100L;

    // 测试1：正常查询自己的客户列表
    @Test
    void testQueryCustomerWithPermission() throws Exception {
        AgentContext.setAgentId(OWNER_AGENT_ID);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/customers")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.records[0].createAgentId").value(OWNER_AGENT_ID));
    }

    // 测试2：越权更新他人客户
    @Test
    void testUpdateCustomerWithoutPermission() throws Exception {
        AgentContext.setAgentId(OTHER_AGENT_ID);
        Customer updateDTO = new Customer();
        updateDTO.setName("篡改客户名称");

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/customers/" + CUSTOMER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(403))
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("无权操作他人创建的资源"));
    }
}
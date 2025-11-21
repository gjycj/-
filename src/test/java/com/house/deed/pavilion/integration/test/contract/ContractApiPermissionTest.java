package com.house.deed.pavilion.integration.test.contract;

import com.house.deed.pavilion.common.util.AgentContext;
import com.house.deed.pavilion.integration.test.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

public class ContractApiPermissionTest extends BaseIntegrationTest {

    // 测试合同ID=5（关联房源ID=1，房源创建人=OWNER_AGENT_ID）
    private final Long CONTRACT_ID = 5L;

    // 测试：越权查询他人房源的合同
    @Test
    void testGetContractWithoutPermission() throws Exception {
        AgentContext.setAgentId(OTHER_AGENT_ID);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/contracts/" + CONTRACT_ID))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(403));
    }
}
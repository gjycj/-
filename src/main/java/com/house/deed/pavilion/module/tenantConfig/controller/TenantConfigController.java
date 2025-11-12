package com.house.deed.pavilion.module.tenantConfig.controller;

import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.module.tenantConfig.entity.TenantConfig;
import com.house.deed.pavilion.module.tenantConfig.service.ITenantConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 租户个性化配置表 前端控制器
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@RestController
@RequestMapping("/module/tenantConfig")
public class TenantConfigController {

    @Autowired
    private ITenantConfigService tenantConfigService;

    // 测试成功响应
    @GetMapping("/{id}")
    public ResultDTO<TenantConfig> getById(@PathVariable Long id) {
        TenantConfig config = tenantConfigService.getById(id);
        System.out.println(config);
        if (config == null) {
            throw new BusinessException(404, "配置不存在");
        }
        return ResultDTO.success(config);
    }

    // 测试异常抛出
    @GetMapping("/testException")
    public ResultDTO<Void> testException() {
        // 模拟业务异常
        throw new BusinessException("测试业务异常");
        // 模拟系统异常（取消注释测试）
    }

}

package com.house.deed.pavilion.config.swagger;

// 新增Swagger配置类
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    // 全局添加tenant-id请求头
    @Bean
    public OperationCustomizer globalHeaderCustomizer() {
        return (operation, handlerMethod) -> {
            operation.addParametersItem(new HeaderParameter()
                    .name("tenant-id")
                    .description("租户ID（必填）")
                    .required(true));
            return operation;
        };
    }

    // 可选：配置API基本信息
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("房屋管理系统API")
                        .version("1.0")
                        .description("租户级房屋管理业务接口文档"));
    }
}
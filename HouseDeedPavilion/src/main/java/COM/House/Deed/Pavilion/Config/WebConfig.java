package COM.House.Deed.Pavilion.Config;

import COM.House.Deed.Pavilion.Converter.EnumConverter;
import COM.House.Deed.Pavilion.Enum.HouseStatusEnum;
import COM.House.Deed.Pavilion.Enum.HouseTypeEnum;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局Web配置：注册枚举转换器
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // 注册HouseTypeEnum转换器
        registry.addConverter(new EnumConverter<>(HouseTypeEnum.class));
        // 注册HouseStatusEnum转换器
        registry.addConverter(new EnumConverter<>(HouseStatusEnum.class));
        // 若有其他枚举需要转换，按此格式添加
    }
}
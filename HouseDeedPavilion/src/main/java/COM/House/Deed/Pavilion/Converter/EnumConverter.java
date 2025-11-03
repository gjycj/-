package COM.House.Deed.Pavilion.Converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;
import java.lang.reflect.Method;

/**
 * 通用枚举转换器：将字符串参数转换为枚举（通过枚举的getByCode方法）
 * 要求枚举类必须有静态方法getByCode(Integer code)
 */
public class EnumConverter<T extends Enum<T>> implements Converter<String, T> {

    private final Class<T> enumType;

    // 构造器：传入枚举类型
    public EnumConverter(Class<T> enumType) {
        this.enumType = enumType;
    }

    @Override
    public T convert(String source) {
        if (!StringUtils.hasText(source)) {
            return null; // 空值不转换
        }
        try {
            // 调用枚举的getByCode方法（传入Integer类型的code）
            Method method = enumType.getMethod("getByCode", Integer.class);
            return (T) method.invoke(null, Integer.parseInt(source));
        } catch (Exception e) {
            // 转换失败（如code无效）
            throw new IllegalArgumentException("无法将值[" + source + "]转换为枚举类型" + enumType.getName(), e);
        }
    }
}
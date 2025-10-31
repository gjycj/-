package COM.House.Deed.Pavilion.Entity.Enum;

import lombok.Getter;

// 房源类型枚举（对应house_type字段）
@Getter
public enum HouseTypeEnum {
    FOR_SALE(1, "出售"),  // 1-出售
    FOR_RENT(2, "出租");  // 2-出租

    private final Integer code;  // 对应数据库存储的数字
    private final String desc;   // 业务描述

    // 构造函数（私有，仅内部定义枚举值）
    HouseTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    // 根据code获取枚举（用于从数据库查询结果转换为枚举）
    public static HouseTypeEnum getByCode(Integer code) {
        for (HouseTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("无效的房源类型编码：" + code);
    }
}
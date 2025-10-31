package COM.House.Deed.Pavilion.Entity.Enum;

// 房源状态枚举（对应status字段）
import jakarta.xml.bind.annotation.XmlEnumValue;
import lombok.Getter;

@Getter
public enum HouseStatusEnum {
    PENDING(1, "待售/待租"),   // 1-待售/待租
    BOOKED(2, "已预订"),       // 2-已预订
    TRANSACTED(3, "已成交"),   // 3-已成交
    OFFLINE(4, "已下架");      // 4-已下架

    private final Integer code;
    private final String desc;

    HouseStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    // 根据code获取枚举
    public static HouseStatusEnum getByCode(Integer code) {
        for (HouseStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的房源状态编码：" + code);
    }
}

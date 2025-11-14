package com.house.deed.pavilion.module.house.repository;

import lombok.Getter;

/**
 * 房源状态枚举类
 * <p>
 * 对应House实体类中的status字段，用于表示房源的各种状态
 * 数据库存储使用code值，前端展示使用desc描述
 * </p>
 *
 * @author 开发者名称
 * @since 项目版本号
 */
@Getter
public enum HouseStatus {

    /**
     * 在售状态：房源处于可售卖/可出租的活跃状态
     */
    ON_SALE("ON_SALE", "在售"),

    /**
     * 已预订状态：房源已被客户预订，暂时无法再次交易
     */
    RESERVED("RESERVED", "已预订"),

    /**
     * 已售状态：房源已完成交易流程，不再参与市场流通
     */
    SOLD("SOLD", "已售"),

    /**
     * 下架状态：房源因某些原因（如信息维护、房东要求等）暂时从市场移除
     */
    OFF_SHELF("OFF_SHELF", "下架");

    /**
     * 数据库存储的状态编码
     */
    private final String code;

    /**
     * 前端展示的状态描述
     */
    private final String desc;

    /**
     * 构造方法：初始化状态编码和描述
     *
     * @param code 数据库存储的状态编码
     * @param desc 前端展示的状态描述
     */
    HouseStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据数据库存储的编码获取对应的枚举实例
     * <p>
     * 用于查询数据时，将数据库字段值转换为枚举对象
     * </p>
     *
     * @param code 数据库中存储的状态编码
     * @return 对应的HouseStatus枚举实例；若编码不存在则返回null
     */
    public static HouseStatus getByCode(String code) {
        for (HouseStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

}
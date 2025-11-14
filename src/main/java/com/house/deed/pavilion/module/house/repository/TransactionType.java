package com.house.deed.pavilion.module.house.repository;

import lombok.Getter;

/**
 * 房源交易类型枚举类
 * <p>
 * 对应House实体类中的transactionType字段，用于表示房源支持的交易方式
 * 数据库存储使用code值，前端展示使用desc描述
 * </p>
 *
 * @author 开发者名称
 * @since 项目版本号
 */
@Getter
public enum TransactionType {

    /**
     * 出售：房源仅支持出售交易
     */
    SALE("SALE", "出售"),

    /**
     * 出租：房源仅支持出租交易
     */
    RENT("RENT", "出租"),

    /**
     * 可售可租：房源同时支持出售和出租交易
     */
    BOTH("BOTH", "可售可租");

    /**
     * 数据库存储的交易类型编码
     */
    private final String code;

    /**
     * 前端展示的交易类型描述
     */
    private final String desc;

    /**
     * 构造方法：初始化交易类型编码和描述
     *
     * @param code 数据库存储的交易类型编码
     * @param desc 前端展示的交易类型描述
     */
    TransactionType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据数据库存储的编码获取对应的枚举实例
     * <p>
     * 用于查询数据时，将数据库字段值转换为枚举对象
     * </p>
     *
     * @param code 数据库中存储的交易类型编码
     * @return 对应的TransactionType枚举实例；若编码不存在则返回null
     */
    public static TransactionType getByCode(String code) {
        for (TransactionType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

}
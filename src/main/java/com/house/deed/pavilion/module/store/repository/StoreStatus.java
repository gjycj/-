package com.house.deed.pavilion.module.store.repository;

import com.house.deed.pavilion.common.exception.BusinessException;
import lombok.Getter;

/**
 * 门店状态枚举类
 * <p>
 * 对应Store实体类中的status字段，数据库存储为tinyint（1-营业，0-停业）
 * </p>
 */
@Getter
public enum StoreStatus {

    /**
     * 营业状态：门店正常营业中
     */
    OPEN((byte) 1, "营业"),

    /**
     * 停业状态：门店暂时或永久停业
     */
    CLOSE((byte) 0, "停业");

    /**
     * 数据库存储的状态值（tinyint）
     */
    private final byte code;

    /**
     * 状态描述
     */
    private final String desc;

    StoreStatus(byte code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据数据库存储的code值获取枚举（核心转换方法）
     * @param code 数据库中的tinyint值（可能为null或无效值）
     * @return 匹配的枚举，或抛出明确异常（避免后续NPE）
     */
    public static StoreStatus getByCode(Byte code) {
        if (code == null) {
            throw new BusinessException(400, "门店状态不能为空");
        }
        for (StoreStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new BusinessException(400, "无效的门店状态值：" + code);
    }
}
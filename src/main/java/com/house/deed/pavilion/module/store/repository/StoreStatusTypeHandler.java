package com.house.deed.pavilion.module.store.repository;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis自定义类型处理器
 * <p>
 * 用于实现StoreStatus枚举与数据库字段的双向转换：
 * 1. 插入/更新时：将StoreStatus枚举转换为数据库存储的tinyint类型状态值
 * 2. 查询时：将数据库存储的tinyint类型状态值转换为StoreStatus枚举
 * </p>
 */
public class StoreStatusTypeHandler extends BaseTypeHandler<StoreStatus> {

    /**
     * 设置非空参数：将StoreStatus枚举转换为数据库存储的tinyint值
     * <p>
     * 用于SQL插入或更新操作时，将枚举参数转换为数据库字段值（1-营业，0-停业）
     * </p>
     *
     * @param ps        PreparedStatement对象
     * @param i         参数索引
     * @param parameter 要设置的StoreStatus枚举实例（非空）
     * @param jdbcType  JDBC数据类型
     * @throws SQLException SQL异常
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, StoreStatus parameter, JdbcType jdbcType) throws SQLException {
        ps.setByte(i, parameter.getCode()); // 枚举转数据库存储的tinyint
    }

    /**
     * 从结果集中根据列名获取值并转换为StoreStatus枚举
     * <p>
     * 用于查询操作时，将数据库tinyint字段值转换为枚举对象
     * </p>
     *
     * @param rs         结果集
     * @param columnName 列名
     * @return 对应的StoreStatus枚举实例；若字段值为null则返回null
     * @throws SQLException SQL异常
     */
    @Override
    public StoreStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return StoreStatus.getByCode(rs.getByte(columnName)); // 数据库tinyint转枚举
    }

    /**
     * 从结果集中根据列索引获取值并转换为StoreStatus枚举
     * <p>
     * 用于查询操作时，将数据库tinyint字段值转换为枚举对象
     * </p>
     *
     * @param rs          结果集
     * @param columnIndex 列索引
     * @return 对应的StoreStatus枚举实例；若字段值为null则返回null
     * @throws SQLException SQL异常
     */
    @Override
    public StoreStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return StoreStatus.getByCode(rs.getByte(columnIndex));
    }

    /**
     * 从存储过程中根据列索引获取值并转换为StoreStatus枚举
     * <p>
     * 用于调用存储过程时，将数据库tinyint字段值转换为枚举对象
     * </p>
     *
     * @param cs          CallableStatement对象
     * @param columnIndex 列索引
     * @return 对应的StoreStatus枚举实例；若字段值为null则返回null
     * @throws SQLException SQL异常
     */
    @Override
    public StoreStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return StoreStatus.getByCode(cs.getByte(columnIndex));
    }
}
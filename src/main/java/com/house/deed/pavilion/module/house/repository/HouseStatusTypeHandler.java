package com.house.deed.pavilion.module.house.repository;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis自定义类型处理器
 * <p>
 * 用于实现HouseStatus枚举与数据库字段的双向转换：
 * 1. 插入/更新时：将HouseStatus枚举转换为数据库存储的code字符串
 * 2. 查询时：将数据库存储的code字符串转换为HouseStatus枚举
 * </p>
 *
 * @author 开发者名称
 * @since 项目版本号
 */
public class HouseStatusTypeHandler extends BaseTypeHandler<HouseStatus> {

    /**
     * 设置非空参数：将HouseStatus枚举转换为数据库存储的code
     * <p>
     * 用于SQL插入或更新操作时，将枚举参数转换为数据库字段值
     * </p>
     *
     * @param ps        PreparedStatement对象
     * @param i         参数索引
     * @param parameter 要设置的HouseStatus枚举实例（非空）
     * @param jdbcType  JDBC数据类型
     * @throws SQLException SQL异常
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, HouseStatus parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.getCode());
    }

    /**
     * 从结果集中根据列名获取值并转换为HouseStatus枚举
     * <p>
     * 用于查询操作时，将数据库字段值转换为枚举对象
     * </p>
     *
     * @param rs         结果集
     * @param columnName 列名
     * @return 对应的HouseStatus枚举实例；若字段值为null则返回null
     * @throws SQLException SQL异常
     */
    @Override
    public HouseStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return HouseStatus.getByCode(rs.getString(columnName));
    }

    /**
     * 从结果集中根据列索引获取值并转换为HouseStatus枚举
     * <p>
     * 用于查询操作时，将数据库字段值转换为枚举对象
     * </p>
     *
     * @param rs          结果集
     * @param columnIndex 列索引
     * @return 对应的HouseStatus枚举实例；若字段值为null则返回null
     * @throws SQLException SQL异常
     */
    @Override
    public HouseStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return HouseStatus.getByCode(rs.getString(columnIndex));
    }

    /**
     * 从存储过程中根据列索引获取值并转换为HouseStatus枚举
     * <p>
     * 用于调用存储过程时，将数据库字段值转换为枚举对象
     * </p>
     *
     * @param cs          CallableStatement对象
     * @param columnIndex 列索引
     * @return 对应的HouseStatus枚举实例；若字段值为null则返回null
     * @throws SQLException SQL异常
     */
    @Override
    public HouseStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return HouseStatus.getByCode(cs.getString(columnIndex));
    }
}
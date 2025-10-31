package COM.House.Deed.Pavilion.Handler;

import COM.House.Deed.Pavilion.Entity.Enum.HouseStatusEnum;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 房源状态枚举处理器：数据库int值 ↔ HouseStatusEnum
 */
public class HouseStatusEnumHandler extends BaseTypeHandler<HouseStatusEnum> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, HouseStatusEnum parameter, JdbcType jdbcType) throws SQLException {
        // 插入/更新时：将枚举的code写入数据库
        ps.setInt(i, parameter.getCode());
    }

    @Override
    public HouseStatusEnum getNullableResult(ResultSet rs, String columnName) throws SQLException {
        // 查询时：从数据库int值转换为枚举
        Integer code = rs.getInt(columnName);
        return rs.wasNull() ? null : HouseStatusEnum.getByCode(code);
    }

    @Override
    public HouseStatusEnum getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Integer code = rs.getInt(columnIndex);
        return rs.wasNull() ? null : HouseStatusEnum.getByCode(code);
    }

    @Override
    public HouseStatusEnum getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Integer code = cs.getInt(columnIndex);
        return cs.wasNull() ? null : HouseStatusEnum.getByCode(code);
    }
}
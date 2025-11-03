package COM.House.Deed.Pavilion.Handler;

// 房源房源类型的处理器
import COM.House.Deed.Pavilion.Enum.HouseTypeEnum;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// 告诉枚举与数据库字段的转换逻辑
public class HouseTypeEnumHandler extends BaseTypeHandler<HouseTypeEnum> {

    // 插入时：将枚举转换为数据库存储的code
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, HouseTypeEnum parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getCode());
    }

    // 查询时：将数据库的code转换为枚举
    @Override
    public HouseTypeEnum getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int code = rs.getInt(columnName);
        return rs.wasNull() ? null : HouseTypeEnum.getByCode(code);
    }

    // 其他两个方法类似（省略，实现getNullableResult的重载）
    @Override
    public HouseTypeEnum getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int code = rs.getInt(columnIndex);
        return rs.wasNull() ? null : HouseTypeEnum.getByCode(code);
    }

    @Override
    public HouseTypeEnum getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int code = cs.getInt(columnIndex);
        return cs.wasNull() ? null : HouseTypeEnum.getByCode(code);
    }
}
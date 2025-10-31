package COM.House.Deed.Pavilion.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 房源查询条件DTO
 * 用于多条件分页查询房源
 */
@Data
@Schema(description = "房源查询条件参数")
public class HouseQueryDTO {

    @Schema(description = "所属楼栋ID（精确匹配）", example = "1001")
    private Long buildingId;

    @Schema(description = "门牌号（模糊匹配，如'101'）", example = "101")
    private String houseNumber;

    @Schema(description = "户型（模糊匹配，如'3室'）", example = "3室")
    private String layout;

    @Schema(description = "房源类型（1-出售，2-出租）", example = "1")
    private Integer houseType;

    @Schema(description = "价格范围-起始价（单位：万元/元）", example = "100.00")
    private BigDecimal priceStart;

    @Schema(description = "价格范围-结束价（单位：万元/元）", example = "200.00")
    private BigDecimal priceEnd;

    @Schema(description = "房源状态（1-待售/待租，2-已预订，3-已成交，4-已下架）", example = "1")
    private Integer status;

    @Schema(description = "所在楼层范围-最低楼层", example = "5")
    private Integer floorStart;

    @Schema(description = "所在楼层范围-最高楼层", example = "20")
    private Integer floorEnd;

    @Schema(description = "负责经纪人ID", example = "2001")
    private Long agentId;

    @Schema(description = "页码（默认1）", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数（默认10，最大100）", example = "10")
    private Integer pageSize = 10;
}

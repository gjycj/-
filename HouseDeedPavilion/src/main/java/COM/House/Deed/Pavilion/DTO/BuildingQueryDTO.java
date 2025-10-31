package COM.House.Deed.Pavilion.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * 楼栋查询条件DTO
 * 用于多条件分页查询楼栋
 */
@Data
@Schema(description = "楼栋查询条件DTO")
public class BuildingQueryDTO {

    @Schema(description = "所属楼盘ID（精确匹配）", example = "1")
    private Long propertyId; // 关联楼盘ID

    @Schema(description = "楼栋名称（模糊查询）", example = "1号")
    private String nameLike;

    @Schema(description = "总层数（最小）", example = "10")
    private Integer totalFloorMin;

    @Schema(description = "总层数（最大）", example = "33")
    private Integer totalFloorMax;

    @Schema(description = "页码（默认1）", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数（默认10）", example = "10")
    private Integer pageSize = 10;
}
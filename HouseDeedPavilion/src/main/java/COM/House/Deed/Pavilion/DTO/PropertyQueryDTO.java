package COM.House.Deed.Pavilion.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 楼盘查询条件DTO
 * 用于多条件分页查询楼盘
 */
@Data
@Schema(description = "楼盘查询条件DTO")
public class PropertyQueryDTO {

    @Schema(description = "楼盘名称（模糊查询）", example = "阳光")
    private String nameLike; // 名称模糊查询

    @Schema(description = "物业类型（精确匹配，如：住宅/商业）", example = "住宅")
    private String propertyType; // 物业类型精确查询

    @Schema(description = "建成年代（起始年份，如：2010）", example = "2010")
    private Integer completionYearStart; // 建成年代起始

    @Schema(description = "建成年代（结束年份，如：2020）", example = "2020")
    private Integer completionYearEnd; // 建成年代结束

    @Schema(description = "开发商（模糊查询）", example = "阳光地产")
    private String developerLike; // 开发商模糊查询

    // 分页参数（复用之前的分页逻辑）
    @Schema(description = "页码（默认1）", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数（默认10）", example = "10")
    private Integer pageSize = 10;
}
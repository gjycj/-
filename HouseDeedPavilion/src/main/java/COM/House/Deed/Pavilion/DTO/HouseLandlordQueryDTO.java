package COM.House.Deed.Pavilion.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 房源-房东关联查询条件DTO
 * 用于查询房源与房东的关联关系
 */
@Data
@Schema(description = "房源-房东关联查询条件参数")
public class HouseLandlordQueryDTO {

    @Schema(description = "房源ID（精确匹配，查询该房源的所有房东）", example = "20001")
    private Long houseId;

    @Schema(description = "房东ID（精确匹配，查询该房东的所有房源）", example = "3001")
    private Integer isMain;

    @Schema(description = "是否主要房东（1-是，0-否，null-查询全部）", example = "1")
    private Long landlordId;

    @Schema(description = "页码（默认1）", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数（默认10，最大100）", example = "10")
    private Integer pageSize = 10;
}

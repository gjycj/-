package COM.House.Deed.Pavilion.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 房源-房东关联备份查询条件DTO
 * 用于查询被删除的关联关系备份
 */
@Data
@Schema(description = "房源-房东关联备份查询条件参数")
public class HouseLandlordBackupQueryDTO {

    @Schema(description = "原关联记录ID（精确匹配）", example = "4001")
    private Long originalId;

    @Schema(description = "房源ID（精确匹配）", example = "20001")
    private Long houseId;

    @Schema(description = "房东ID（精确匹配）", example = "3001")
    private Long landlordId;

    @Schema(description = "是否主要房东（1-是，0-否，null-查询全部）", example = "1")
    private Integer isMain;

    @Schema(description = "删除时间起始（如：2023-11-01 00:00:00）", example = "2023-11-01T00:00:00")
    private LocalDateTime deletedAtStart;

    @Schema(description = "删除时间结束（如：2023-11-30 23:59:59）", example = "2023-11-30T23:59:59")
    private LocalDateTime deletedAtEnd;

    @Schema(description = "页码（默认1）", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数（默认10，最大100）", example = "10")
    private Integer pageSize = 10;
}

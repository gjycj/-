package COM.House.Deed.Pavilion.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 房源状态变更记录查询条件DTO
 * 用于查询房源状态的变更历史
 */
@Data
@Schema(description = "房源状态变更记录查询条件参数")
public class HouseStatusLogQueryDTO {

    @Schema(description = "房源ID（精确匹配，查询指定房源的状态变更记录）", example = "20001")
    private Long houseId;

    @Schema(description = "变更前状态（精确匹配）", example = "1")
    private Integer oldStatus;

    @Schema(description = "变更后状态（精确匹配）", example = "2")
    private Integer newStatus;

    @Schema(description = "操作人ID（精确匹配）", example = "1001")
    private Long operatedBy;

    @Schema(description = "操作时间起始（如：2023-11-01 00:00:00）", example = "2023-11-01T00:00:00")
    private LocalDateTime operatedAtStart;

    @Schema(description = "操作时间结束（如：2023-11-30 23:59:59）", example = "2023-11-30T23:59:59")
    private LocalDateTime operatedAtEnd;

    @Schema(description = "页码（默认1）", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数（默认10，最大100）", example = "10")
    private Integer pageSize = 10;
}

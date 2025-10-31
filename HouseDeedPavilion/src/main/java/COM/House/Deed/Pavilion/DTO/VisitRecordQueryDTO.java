package COM.House.Deed.Pavilion.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 带看记录查询条件DTO
 */
@Data
@Schema(description = "带看记录查询参数")
public class VisitRecordQueryDTO {

    @Schema(description = "带看记录ID（精确匹配）", example = "40001")
    private Long id;

    @Schema(description = "房源ID（精确匹配）", example = "10001")
    private Long houseId;

    @Schema(description = "客户ID（精确匹配）", example = "30001")
    private Long customerId;

    @Schema(description = "经纪人ID（精确匹配）", example = "20001")
    private Long agentId;

    @Schema(description = "带看时间起始（如：2023-11-01 00:00:00）", example = "2023-11-01T00:00:00")
    private LocalDateTime visitTimeStart;

    @Schema(description = "带看时间结束（如：2023-11-30 23:59:59）", example = "2023-11-30T23:59:59")
    private LocalDateTime visitTimeEnd;

    @Schema(description = "页码（默认1）", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数（默认10，最大100）", example = "10")
    private Integer pageSize = 10;
}

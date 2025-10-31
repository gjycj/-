package COM.House.Deed.Pavilion.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 带看记录更新DTO
 */
@Data
@Schema(description = "带看记录更新参数")
public class VisitRecordUpdateDTO {

    @NotNull(message = "带看记录ID不能为空")
    @Schema(description = "带看记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "40001")
    private Long id;

    @Schema(description = "房源ID（不建议更新，如需修改请删除后重建）", example = "10001")
    private Long houseId;

    @Schema(description = "客户ID（不建议更新，如需修改请删除后重建）", example = "30001")
    private Long customerId;

    @Schema(description = "经纪人ID（不建议更新，如需修改请删除后重建建）", example = "20001")
    private Long agentId;

    @Schema(description = "带看时间", example = "2023-11-15T15:00:00")
    private LocalDateTime visitTime;

    @Schema(description = "客户带看后的反馈", example = "房屋朝向满意，价格可谈")
    private String feedback;

    @Schema(description = "后续跟进计划", example = "本周内约谈业主议价")
    private String nextPlan;

    @NotNull(message = "更新人ID不能为空")
    @Schema(description = "更新人用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long updatedBy;
}

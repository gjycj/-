package COM.House.Deed.Pavilion.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 带看记录新增DTO
 */
@Data
@Schema(description = "带看记录新增参数")
public class VisitRecordAddDTO {

    @NotNull(message = "房源ID不能为空")
    @Schema(description = "房源ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10001")
    private Long houseId;

    @NotNull(message = "客户ID不能为空")
    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "30001")
    private Long customerId;

    @NotNull(message = "经纪人ID不能为空")
    @Schema(description = "经纪人ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "20001")
    private Long agentId;

    @NotNull(message = "带看时间不能为空")
    @Schema(description = "带看时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2023-11-15T14:30:00")
    private LocalDateTime visitTime;

    @Schema(description = "客户带看后的反馈", example = "房屋朝向满意，价格偏高")
    private String feedback;

    @Schema(description = "后续跟进计划", example = "下周再带看另一套同户型房源")
    private String nextPlan;

    @NotNull(message = "创建人ID不能为空")
    @Schema(description = "创建人用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long createdBy;
}

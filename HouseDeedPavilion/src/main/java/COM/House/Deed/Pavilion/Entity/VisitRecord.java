package COM.House.Deed.Pavilion.Entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 带看记录实体类（对应visit_record表）
 */
@Data
@Schema(description = "带看记录实体类")
public class VisitRecord {

    /**
     * 带看记录ID（自增主键）
     */
    @Schema(description = "带看记录ID（自动生成）", example = "40001")
    private Long id;

    /**
     * 房源ID（外键关联house表）
     */
    @NotNull(message = "房源ID不能为空")
    @Schema(description = "房源ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10001")
    private Long houseId;

    /**
     * 客户ID（外键关联customer表）
     */
    @NotNull(message = "客户ID不能为空")
    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "30001")
    private Long customerId;

    /**
     * 经纪人ID（外键关联agent表）
     */
    @NotNull(message = "经纪人ID不能为空")
    @Schema(description = "经纪人ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "20001")
    private Long agentId;

    /**
     * 带看时间
     */
    @NotNull(message = "带看时间不能为空")
    @Schema(description = "带看时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2023-11-15T14:30:00")
    private LocalDateTime visitTime;

    /**
     * 客户反馈
     */
    @Schema(description = "客户带看后的反馈", example = "房屋朝向满意，价格偏高")
    private String feedback;

    /**
     * 下一步计划
     */
    @Schema(description = "后续跟进计划", example = "下周再带看同一套同户型房源")
    private String nextPlan;

    /**
     * 创建时间
     */
    @Schema(description = "记录创建时间（自动生成）", example = "2023-11-10T09:00:00")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Schema(description = "记录更新时间（自动生成）", example = "2023-11-10T10:15:00")
    private LocalDateTime updatedAt;

    /**
     * 创建人ID
     */
    @NotNull(message = "创建人ID不能为空")
    @Schema(description = "创建人用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long createdBy;

    /**
     * 更新人ID
     */
    @NotNull(message = "更新人ID不能为空")
    @Schema(description = "更新人用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long updatedBy;
}

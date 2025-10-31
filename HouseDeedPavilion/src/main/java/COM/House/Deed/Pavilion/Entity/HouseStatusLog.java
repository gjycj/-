package COM.House.Deed.Pavilion.Entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 房源状态变更记录实体类（对应house_status_log表）
 * 记录房源状态的所有变更历史
 */
@Data
@Schema(description = "房源状态变更记录实体类")
public class HouseStatusLog {

    /**
     * 记录ID（自增主键）
     */
    @Schema(description = "记录ID（自动生成）", example = "6001")
    private Long id;

    /**
     * 房源ID（外键）
     */
    @NotNull(message = "房源ID不能为空")
    @Schema(description = "关联的房源ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "20001")
    private Long houseId;

    /**
     * 变更前状态
     */
    @NotNull(message = "变更前状态不能为空")
    @Schema(description = "变更前的房源状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer oldStatus;

    /**
     * 变更后状态
     */
    @NotNull(message = "变更后状态不能为空")
    @Schema(description = "变更后的房源状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer newStatus;

    /**
     * 变更原因
     */
    @Schema(description = "状态变更原因（可选）", example = "房源已出租，状态变更为已出租")
    private String reason;

    /**
     * 操作人ID
     */
    @NotNull(message = "操作人ID不能为空")
    @Schema(description = "执行状态变更的用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long operatedBy;

    /**
     * 操作时间
     */
    @Schema(description = "状态变更时间（自动生成）", example = "2023-11-03T09:15:00")
    private LocalDateTime operatedAt;
}

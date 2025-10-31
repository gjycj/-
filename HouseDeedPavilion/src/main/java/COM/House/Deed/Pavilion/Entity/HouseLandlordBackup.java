package COM.House.Deed.Pavilion.Entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 房源-房东关联备份实体类（对应house_landlord_backup表）
 * 记录被删除的关联关系，仅用于备份和查询
 */
@Data
@Schema(description = "房源-房东关联备份信息实体类")
public class HouseLandlordBackup {

    /**
     * 备份记录ID（自增主键）
     */
    @Schema(description = "备份记录ID（自动生成）", example = "5001")
    private Long id;

    /**
     * 原关联记录ID（对应house_landlord表的id）
     */
    @NotNull(message = "原关联记录ID不能为空")
    @Schema(description = "原关联记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "4001")
    private Long originalId;

    /**
     * 房源ID
     */
    @NotNull(message = "房源ID不能为空")
    @Schema(description = "关联的房源ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "20001")
    private Long houseId;

    /**
     * 房东ID
     */
    @NotNull(message = "房东ID不能为空")
    @Schema(description = "关联的房东ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "3001")
    private Long landlordId;

    /**
     * 是否主要房东（1-是，0-否）
     */
    @NotNull(message = "是否主要房东不能为空")
    @Schema(description = "是否主要房东（1-是，0-否）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer isMain;

    /**
     * 删除原因
     */
    @Schema(description = "删除原因（可选）", example = "手动解除房源与房东关联")
    private String deleteReason;

    /**
     * 删除时间（原记录被删除的时间）
     */
    @NotNull(message = "删除时间不能为空")
    @Schema(description = "原记录被删除的时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2023-11-02T15:30:00")
    private LocalDateTime deletedAt;

    /**
     * 操作人ID（执行删除的用户）
     */
    @NotNull(message = "操作人ID不能为空")
    @Schema(description = "执行删除操作的用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long deletedBy;

    /**
     * 备份时间（自动生成）
     */
    @Schema(description = "备份记录生成时间（自动生成）", example = "2023-11-02T15:30:00")
    private LocalDateTime backupTime;
}

package COM.House.Deed.Pavilion.Entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 经纪人备份信息实体类（对应agent_backup表）
 */
@Data
@Schema(description = "经纪人备份信息实体类（用于记录删除的经纪人数据）")
public class AgentBackup {

    /**
     * 备份记录ID（自增主键）
     */
    @Schema(description = "备份记录ID（自增，无需手动设置）", example = "1001")
    private Long id;

    /**
     * 原经纪人ID（关联agent表的id）
     */
    @NotNull(message = "原经纪人ID不能为空")
    @Schema(description = "原经纪人ID（对应agent表的id）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    private Long originalId;

    /**
     * 姓名（与原表一致）
     */
    @NotBlank(message = "姓名不能为空")
    @Schema(description = "经纪人姓名（备份时的原始值）", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String name;

    /**
     * 工号（与原表一致，唯一）
     */
    @NotBlank(message = "工号不能为空")
    @Schema(description = "经纪人工号（备份时的原始值，唯一）", requiredMode = Schema.RequiredMode.REQUIRED, example = "EMP2023001")
    private String employeeId;

    /**
     * 联系电话（与原表一致）
     */
    @NotBlank(message = "联系电话不能为空")
    @Schema(description = "经纪人联系电话（备份时的原始值）", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    private String phone;

    /**
     * 所属门店ID（与原表一致）
     */
    @Schema(description = "所属门店ID（备份时的原始值）", example = "5001")
    private Long storeId;

    /**
     * 职位（与原表一致）
     */
    @Schema(description = "经纪人职位（备份时的原始值）", example = "销售专员")
    private String position;

    /**
     * 删除时的状态（1-在职，0-离职）
     */
    @NotNull(message = "状态不能为空")
    @Schema(description = "删除时的状态（1-在职，0-离职）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

    /**
     * 原经纪人创建时间（保留历史）
     */
    @NotNull(message = "原创建时间不能为空")
    @Schema(description = "原经纪人的创建时间（备份时的原始值）", example = "2023-10-31T09:00:00")
    private LocalDateTime createdAt;

    /**
     * 原经纪人最后更新时间（保留历史）
     */
    @NotNull(message = "原更新时间不能为空")
    @Schema(description = "原经纪人的最后更新时间（备份时的原始值）", example = "2023-10-31T15:30:00")
    private LocalDateTime updatedAt;

    /**
     * 备份创建时间（经纪人被删除时自动记录）
     */
    @Schema(description = "备份记录的创建时间（自动生成，无需传入）", example = "2023-11-01T10:00:00")
    private LocalDateTime backupTime;

    /**
     * 删除原因（如：员工离职/信息错误等）
     */
    @Schema(description = "删除经纪人的原因（可选）", example = "员工已离职")
    private String deleteReason;

    /**
     * 执行删除操作的用户ID
     */
    @NotNull(message = "操作人ID不能为空")
    @Schema(description = "执行删除操作的用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10001")
    private Long deletedBy;

    /**
     * 原记录创建人ID（保留历史）
     */
    @NotNull(message = "原创建人ID不能为空")
    @Schema(description = "原经纪人记录的创建人ID（备份时保留）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long createdBy;

    /**
     * 原记录最后更新人ID（保留历史）
     */
    @NotNull(message = "原更新人ID不能为空")
    @Schema(description = "原经纪人记录的最后更新人ID（备份时保留）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long updatedBy;
}
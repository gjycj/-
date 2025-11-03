package COM.House.Deed.Pavilion.Entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 经纪人信息备份实体（对应agent_backup表）
 * 用于存储被删除的经纪人数据，支持数据追溯和恢复
 */
@Data
@Schema(description = "经纪人信息备份实体")
public class AgentBackup {

    @Schema(description = "备份记录ID（自增主键）", example = "7001")
    private Long id;

    @NotNull(message = "原经纪人ID不能为空")
    @Schema(description = "原经纪人ID（对应agent表的id）", requiredMode = Schema.RequiredMode.REQUIRED, example = "6001")
    private Long originalId; // 对应original_id

    @NotBlank(message = "经纪人姓名不能为空")
    @Schema(description = "经纪人姓名（与原表一致）", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String name;

    @NotBlank(message = "工号不能为空")
    @Schema(description = "经纪人工号（与原表一致，唯一）", requiredMode = Schema.RequiredMode.REQUIRED, example = "EMP2024001")
    private String employeeId; // 对应employee_id

    @NotBlank(message = "联系电话不能为空")
    @Schema(description = "经纪人联系电话（与原表一致）", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    private String phone;

    @Schema(description = "所属门店ID（与原表一致，可为空）", example = "301")
    private Long storeId; // 对应store_id

    @Schema(description = "职位（与原表一致）", example = "高级经纪人")
    private String position;

    @NotNull(message = "删除时的状态不能为空")
    @Schema(description = "删除时的状态：1-在职，0-离职", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

    @NotNull(message = "原创建时间不能为空")
    @Schema(description = "原经纪人记录的创建时间（保留历史）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-15T09:30:00")
    private LocalDateTime createdAt; // 对应created_at

    @NotNull(message = "原更新时间不能为空")
    @Schema(description = "原经纪人记录的最后更新时间（保留历史）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-10-01T14:20:00")
    private LocalDateTime updatedAt; // 对应updated_at

    @NotNull(message = "备份时间不能为空")
    @Schema(description = "备份创建时间（经纪人被删除时自动记录）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-11-08T10:15:00")
    private LocalDateTime backupTime; // 对应backup_time

    @Schema(description = "删除原因（如员工离职、信息错误等）", example = "员工已离职，解除系统权限")
    private String deleteReason; // 对应delete_reason

    @NotNull(message = "删除人ID不能为空")
    @Schema(description = "执行删除操作的用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long deletedBy; // 对应deleted_by
}
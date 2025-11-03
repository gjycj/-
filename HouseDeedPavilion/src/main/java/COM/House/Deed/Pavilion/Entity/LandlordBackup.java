package COM.House.Deed.Pavilion.Entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 房东备份实体类
 * 对应数据库landlord_backup表，存储房东删除时的完整备份
 */
@Data
@Schema(description = "房东备份信息实体")
public class LandlordBackup {

    /**
     * 备份记录ID（自增主键）
     */
    @Schema(description = "备份记录ID（自动生成）", example = "4001")
    private Long id;

    /**
     * 关联原房东ID（对应landlord表的id）
     */
    @NotNull(message = "原房东ID不能为空")
    @Schema(description = "关联的原房东ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "3001")
    private Long originalId; // 对应original_id

    /**
     * 姓名（与原表一致）
     */
    @NotNull(message = "房东姓名不能为空")
    @Schema(description = "房东姓名（备份自原表）", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String name;

    /**
     * 联系电话（与原表一致）
     */
    @NotNull(message = "联系电话不能为空")
    @Schema(description = "联系电话（备份自原表）", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    private String phone;

    /**
     * 身份证号（与原表一致）
     */
    @Schema(description = "身份证号（备份自原表）", example = "110101199001011234")
    private String idCard;

    /**
     * 性别（与原表一致）
     */
    @Schema(description = "性别：1-男，2-女（备份自原表）", example = "1")
    private Integer gender;

    /**
     * 联系地址（与原表一致）
     */
    @Schema(description = "联系地址（备份自原表）", example = "北京市海淀区XX街道")
    private String address;

    /**
     * 备注信息（与原表一致）
     */
    @Schema(description = "备注信息（备份自原表）", example = "此房东有3套房源")
    private String remark;

    /**
     * 原房东创建时间（保留历史）
     */
    @NotNull(message = "原创建时间不能为空")
    @Schema(description = "原房东记录的创建时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-11-05T09:30:00")
    private LocalDateTime createdAt;

    /**
     * 原房东更新时间（保留历史）
     */
    @NotNull(message = "原更新时间不能为空")
    @Schema(description = "原房东记录的最后更新时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-11-05T14:20:00")
    private LocalDateTime updatedAt;

    /**
     * 原房东创建人ID（保留历史）
     */
    @NotNull(message = "原创建人ID不能为空")
    @Schema(description = "原房东记录的创建人ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long createdBy;

    /**
     * 原房东更新人ID（保留历史）
     */
    @NotNull(message = "原更新人ID不能为空")
    @Schema(description = "原房东记录的最后更新人ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long updatedBy;

    /**
     * 备份创建时间（删除时自动记录）
     */
    @NotNull(message = "备份时间不能为空")
    @Schema(description = "备份记录的创建时间（自动生成）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-11-06T10:15:00")
    private LocalDateTime backupTime; // 对应backup_time

    /**
     * 原房东删除原因
     */
    @Schema(description = "原房东被删除的原因", example = "房东注销房源，信息失效")
    private String deleteReason; // 对应delete_reason

    /**
     * 原房东删除人ID
     */
    @Schema(description = "执行删除操作的用户ID", example = "1002")
    private Long deletedBy; // 对应deleted_by
}
package COM.House.Deed.Pavilion.Entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 楼栋备份实体类（原生MyBatis版）
 * 用于存储楼栋删除时的完整备份数据，保留原楼栋的历史信息及删除相关记录
 */
@Data // Lombok注解：自动生成getter、setter、toString、equals等方法
public class BuildingBackup {

    /**
     * 备份记录ID（自增主键）
     */
    @Schema(description = "备份记录ID（自增，无需手动传入）", example = "7001")
    private Long id;

    /**
     * 关联原楼栋ID（对应原building表的id）
     * 用于追溯备份对应的原楼栋，原楼栋删除后仍保留此关联
     */
    @NotNull(message = "原楼栋ID不能为空")
    @Schema(description = "关联的原楼栋ID（备份时必填）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    private Long originalId; // 对应数据库字段original_id

    /**
     * 所属楼盘ID（与原楼栋保持一致）
     * 原楼盘删除时，此字段可被置为NULL（外键ON DELETE SET NULL）
     */
    @Schema(description = "所属楼盘ID（原楼栋关联的楼盘）", example = "101")
    private Long propertyId; // 对应数据库字段property_id

    /**
     * 楼栋名称/编号（与原楼栋保持一致）
     */
    @NotNull(message = "楼栋名称不能为空")
    @Schema(description = "楼栋名称/编号（保留原楼栋信息）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1号楼")
    private String name;

    /**
     * 总层数（与原楼栋保持一致）
     */
    @Schema(description = "楼栋总层数", example = "33")
    private Integer totalFloor; // 对应数据库字段total_floor

    /**
     * 单元数（与原楼栋保持一致）
     */
    @Schema(description = "楼栋单元数量", example = "2")
    private Integer unitCount; // 对应数据库字段unit_count

    /**
     * 楼栋描述（与原楼栋保持一致）
     */
    @Schema(description = "楼栋描述信息（保留原内容）", example = "南北通透，一梯两户")
    private String description;

    /**
     * 原楼栋创建时间（保留历史）
     * 与原楼栋表的created_at一致，备份时自动复制
     */
    @NotNull(message = "原楼栋创建时间不能为空")
    @Schema(description = "原楼栋的创建时间（备份时自动同步）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-05-20T10:30:00")
    private LocalDateTime createdAt; // 对应数据库字段created_at

    /**
     * 原楼栋最后更新时间（保留历史）
     * 与原楼栋表的updated_at一致，备份时自动复制
     */
    @NotNull(message = "原楼栋更新时间不能为空")
    @Schema(description = "原楼栋的最后更新时间（备份时自动同步）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-06-15T14:20:00")
    private LocalDateTime updatedAt; // 对应数据库字段updated_at

    /**
     * 原楼栋创建人ID（保留历史）
     * 与原楼栋表的created_by一致，备份时自动复制
     */
    @NotNull(message = "原楼栋创建人ID不能为空")
    @Schema(description = "原楼栋的创建人ID（备份时自动同步）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long createdBy; // 对应数据库字段created_by

    /**
     * 原楼栋最后更新人ID（保留历史）
     * 与原楼栋表的updated_by一致，备份时自动复制
     */
    @NotNull(message = "原楼栋更新人ID不能为空")
    @Schema(description = "原楼栋的最后更新人ID（备份时自动同步）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1002")
    private Long updatedBy; // 对应数据库字段updated_by

    /**
     * 备份创建时间
     * 记录原楼栋被删除并生成备份的时间，自动生成无需手动传入
     */
    @NotNull(message = "备份时间不能为空")
    @Schema(description = "备份记录的创建时间（自动生成）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-11-02T09:45:35")
    private LocalDateTime backupTime; // 对应数据库字段backup_time

    /**
     * 原楼栋删除原因
     * 备份时手动输入，说明删除原楼栋的原因（如信息错误、重复录入等）
     */
    @Schema(description = "原楼栋被删除的原因（备份时填写）", example = "楼栋信息错误，需重新创建")
    private String deleteReason; // 对应数据库字段delete_reason

    /**
     * 原楼栋删除人ID
     * 记录执行删除操作的用户ID，用于审计追踪
     */
    @Schema(description = "执行原楼栋删除操作的用户ID", example = "1003")
    private Long deletedBy; // 对应数据库字段deleted_by
}
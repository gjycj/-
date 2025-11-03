package COM.House.Deed.Pavilion.Entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 楼盘备份实体类（原生MyBatis版）
 * 存储楼盘删除时的完整备份数据，保留原楼盘的历史信息及删除记录
 */
@Data
public class PropertyBackup {

    /**
     * 备份记录ID（自增主键）
     */
    @Schema(description = "备份记录ID（自增，无需手动传入）", example = "8001")
    private Long id;

    /**
     * 关联原楼盘ID（对应property表的id）
     */
    @NotNull(message = "原楼盘ID不能为空")
    @Schema(description = "关联的原楼盘ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
    private Long originalId; // 对应original_id

    /**
     * 楼盘名称（与原表一致）
     */
    @NotBlank(message = "楼盘名称不能为空")
    @Schema(description = "楼盘名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "阳光花园")
    private String name;

    /**
     * 楼盘地址（与原表一致）
     */
    @NotBlank(message = "楼盘地址不能为空")
    @Schema(description = "楼盘地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "北京市朝阳区XX路100号")
    private String address;

    /**
     * 开发商（与原表一致）
     */
    @Schema(description = "开发商", example = "阳光地产")
    private String developer;

    /**
     * 物业类型（与原表一致）
     */
    @NotBlank(message = "物业类型不能为空")
    @Schema(description = "物业类型（住宅/商业/别墅等）", requiredMode = Schema.RequiredMode.REQUIRED, example = "住宅")
    private String propertyType; // 对应property_type

    /**
     * 建成年代（与原表一致）
     */
    @Schema(description = "建成年代", example = "2020")
    private Integer completionYear; // 对应completion_year

    /**
     * 物业公司（与原表一致）
     */
    @Schema(description = "物业公司", example = "阳光物业")
    private String propertyCompany; // 对应property_company

    /**
     * 物业费（与原表一致）
     */
    @Schema(description = "物业费（元/㎡·月）", example = "2.50")
    private BigDecimal propertyFee; // 对应property_fee

    /**
     * 楼盘描述（与原表一致）
     */
    @Schema(description = "楼盘描述", example = "配套设施完善，交通便利")
    private String description;

    /**
     * 经度（与原表一致）
     */
    @Schema(description = "经度", example = "116.403874")
    private BigDecimal longitude;

    /**
     * 纬度（与原表一致）
     */
    @Schema(description = "纬度", example = "39.914885")
    private BigDecimal latitude;

    /**
     * 原楼盘创建时间（保留历史）
     */
    @NotNull(message = "原创建时间不能为空")
    @Schema(description = "原楼盘的创建时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-05-20T10:30:00")
    private LocalDateTime createdAt; // 对应created_at

    /**
     * 原楼盘更新时间（保留历史）
     */
    @NotNull(message = "原更新时间不能为空")
    @Schema(description = "原楼盘的最后更新时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-06-15T14:20:00")
    private LocalDateTime updatedAt; // 对应updated_at

    /**
     * 原楼盘创建人ID（保留历史）
     */
    @NotNull(message = "原创建人ID不能为空")
    @Schema(description = "原楼盘的创建人ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long createdBy; // 对应created_by

    /**
     * 原楼盘更新人ID（保留历史）
     */
    @NotNull(message = "原更新人ID不能为空")
    @Schema(description = "原楼盘的最后更新人ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1002")
    private Long updatedBy; // 对应updated_by

    /**
     * 备份创建时间
     */
    @NotNull(message = "备份时间不能为空")
    @Schema(description = "备份记录的创建时间（自动生成）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-11-03T09:45:35")
    private LocalDateTime backupTime; // 对应backup_time

    /**
     * 原楼盘删除原因
     */
    @Schema(description = "原楼盘被删除的原因", example = "楼盘信息错误，需重新创建")
    private String deleteReason; // 对应delete_reason

    /**
     * 原楼盘删除人ID
     */
    @Schema(description = "执行原楼盘删除操作的用户ID", example = "1003")
    private Long deletedBy; // 对应deleted_by
}
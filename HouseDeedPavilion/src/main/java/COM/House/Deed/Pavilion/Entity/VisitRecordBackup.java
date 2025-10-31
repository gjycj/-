package COM.House.Deed.Pavilion.Entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 带看记录备份表实体类（对应visit_record_backup表）
 */
@Data
@Schema(description = "带看记录备份表实体类（存储被删除的带看记录）")
public class VisitRecordBackup {

    /**
     * 原带看记录ID（与被删除的记录ID一致）
     */
    @Schema(description = "原带看记录ID", example = "40001")
    private Long id;

    /**
     * 房源ID（原数据）
     */
    @Schema(description = "房源ID", example = "10001")
    private Long houseId;

    /**
     * 客户ID（原数据）
     */
    @Schema(description = "客户ID", example = "30001")
    private Long customerId;

    /**
     * 经纪人ID（原数据）
     */
    @Schema(description = "经纪人ID", example = "20001")
    private Long agentId;

    /**
     * 带看时间（原数据）
     */
    @Schema(description = "带看时间", example = "2023-11-15T14:30:00")
    private LocalDateTime visitTime;

    /**
     * 客户反馈（原数据）
     */
    @Schema(description = "客户反馈", example = "房屋朝向满意，价格偏高")
    private String feedback;

    /**
     * 下一步计划（原数据）
     */
    @Schema(description = "下一步计划", example = "下周再带看同户型房源")
    private String nextPlan;

    /**
     * 原创建时间
     */
    @Schema(description = "原记录创建时间", example = "2023-11-10T09:00:00")
    private LocalDateTime createdAt;

    /**
     * 原更新时间
     */
    @Schema(description = "原记录更新时间", example = "2023-11-10T10:15:00")
    private LocalDateTime updatedAt;

    /**
     * 原创建人ID
     */
    @Schema(description = "原创建人ID", example = "1001")
    private Long createdBy;

    /**
     * 原更新人ID
     */
    @Schema(description = "原更新人ID", example = "1001")
    private Long updatedBy;

    /**
     * 备份时间（执行删除的时间）
     */
    @Schema(description = "备份时间（自动生成）", example = "2023-11-20T16:45:00")
    private LocalDateTime backupTime;

    /**
     * 备份人ID（执行删除操作的用户ID）
     */
    @Schema(description = "执行删除操作的用户ID", example = "1002")
    private Long backupBy;
}

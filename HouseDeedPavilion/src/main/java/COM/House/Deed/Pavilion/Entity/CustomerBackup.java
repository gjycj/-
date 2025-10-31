package COM.House.Deed.Pavilion.Entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户备份表实体类（对应customer_backup表）
 */
@Data
@Schema(description = "客户备份表实体类（存储被删除的客户数据）")
public class CustomerBackup {

    /**
     * 原客户ID（与被删除的客户ID一致）
     */
    @Schema(description = "原客户ID", example = "30001")
    private Long id;

    /**
     * 客户姓名（原数据）
     */
    @Schema(description = "客户姓名", example = "张三")
    private String name;

    /**
     * 联系电话（原数据）
     */
    @Schema(description = "联系电话", example = "13800138000")
    private String phone;

    /**
     * 性别（原数据）
     */
    @Schema(description = "性别（1-男，2-女）", example = "1")
    private Integer gender;

    /**
     * 购房/租房需求（原数据）
     */
    @Schema(description = "购房/租房需求", example = "需要3室2厅，朝南")
    private String demand;

    /**
     * 客户来源（原数据）
     */
    @Schema(description = "客户来源", example = "转介绍")
    private String source;

    /**
     * 客户状态（原数据）
     */
    @Schema(description = "客户状态（1-活跃，2-已成交，3-休眠）", example = "1")
    private Integer status;

    /**
     * 备注信息（原数据）
     */
    @Schema(description = "备注信息", example = "客户希望月底前看房")
    private String remark;

    /**
     * 原创建时间
     */
    @Schema(description = "原记录创建时间", example = "2023-11-05T10:30:00")
    private LocalDateTime createdAt;

    /**
     * 原更新时间
     */
    @Schema(description = "原记录更新时间", example = "2023-11-05T15:20:00")
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
    @Schema(description = "备份时间（自动生成）", example = "2023-11-10T09:15:00")
    private LocalDateTime backupTime;

    /**
     * 备份人ID（执行删除操作的用户ID）
     */
    @Schema(description = "执行删除操作的用户ID", example = "1002")
    private Long backupBy;
}

package COM.House.Deed.Pavilion.Entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户信息实体类（对应customer表）
 */
@Data
@Schema(description = "客户信息实体类")
public class Customer {

    /**
     * 客户ID（自增主键）
     */
    @Schema(description = "客户ID（自动生成）", example = "30001")
    private Long id;

    /**
     * 客户姓名
     */
    @NotBlank(message = "客户姓名不能为空")
    @Schema(description = "客户姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String name;

    /**
     * 联系电话
     */
    @NotBlank(message = "联系电话不能为空")
    @Schema(description = "客户联系电话", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    private String phone;

    /**
     * 性别（1-男，2-女）
     */
    @Schema(description = "性别（1-男，2-女）", example = "1")
    private Integer gender;

    /**
     * 购房/租房需求描述
     */
    @Schema(description = "客户的购房/租房需求", example = "需要3室2厅，朝南，近地铁")
    private String demand;

    /**
     * 客户来源
     */
    @Schema(description = "客户来源渠道", example = "转介绍")
    private String source;

    /**
     * 客户状态（1-活跃，2-已成交，3-休眠）
     */
    @Schema(description = "客户状态（1-活跃，2-已成交，3-休眠）", defaultValue = "1", example = "1")
    private Integer status = 1;

    /**
     * 备注信息
     */
    @Schema(description = "备注信息", example = "客户希望月底前看房")
    private String remark;

    /**
     * 创建时间
     */
    @Schema(description = "记录创建时间（自动生成）", example = "2023-11-05T10:30:00")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Schema(description = "记录更新时间（自动生成）", example = "2023-11-05T15:20:00")
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

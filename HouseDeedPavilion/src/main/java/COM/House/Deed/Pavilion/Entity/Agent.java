package COM.House.Deed.Pavilion.Entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import net.sf.jsqlparser.statement.update.Update;

import java.time.LocalDateTime;

/**
 * 经纪人信息实体类（对应agent表）
 */
@Data
@Schema(description = "经纪人信息实体类")
public class Agent {

    /**
     * 经纪人ID（自增主键）
     */
    @NotNull(message = "经纪人ID不能为空", groups = Update.class)
    @Schema(description = "经纪人ID（更新时必填）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    private Long id;

    /**
     * 姓名
     */
    @NotBlank(message = "经纪人姓名不能为空")
    @Schema(description = "经纪人姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String name;

    /**
     * 工号（唯一）
     */
    @NotBlank(message = "工号不能为空")
    @Schema(description = "经纪人工号（唯一）", requiredMode = Schema.RequiredMode.REQUIRED, example = "EMP2023001")
    private String employeeId;

    /**
     * 联系电话
     */
    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    @Schema(description = "经纪人联系电话（11位手机号）", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    private String phone;

    /**
     * 所属门店ID
     */
    @Schema(description = "所属门店ID（可选）", example = "5001")
    private Long storeId;

    /**
     * 职位
     */
    @Schema(description = "经纪人职位（可选，如经理、专员）", example = "销售专员")
    private String position;

    /**
     * 状态（1-在职，0-离职）
     */
    @NotNull(message = "状态不能为空")
    @Schema(description = "经纪人状态（1-在职，0-离职）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status = 1; // 默认在职

    /**
     * 创建时间
     */
    @Schema(description = "创建时间（自动生成，无需传入）", example = "2023-10-31T09:00:00")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间（自动更新，无需传入）", example = "2023-10-31T15:30:00")
    private LocalDateTime updatedAt;

    /**
     * 创建人ID（操作人）
     */
    @NotNull(message = "创建人ID不能为空")
    @Schema(description = "创建该经纪人记录的操作人ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long createdBy;

    /**
     * 最后更新人ID（操作人）
     */
    @NotNull(message = "更新人ID不能为空")
    @Schema(description = "最后更新该经纪人记录的操作人ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long updatedBy;
}

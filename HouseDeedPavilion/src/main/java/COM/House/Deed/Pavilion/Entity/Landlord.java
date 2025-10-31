package COM.House.Deed.Pavilion.Entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 房东信息实体类（对应landlord表）
 */
@Data
@Schema(description = "房东信息实体类")
public class Landlord {

    /**
     * 房东ID（自增主键）
     */
    @NotNull(message = "房东ID不能为空", groups = Update.class)
    @Schema(description = "房东ID（更新时必填）", requiredMode = Schema.RequiredMode.REQUIRED, example = "3001")
    private Long id;

    /**
     * 姓名
     */
    @NotBlank(message = "房东姓名不能为空")
    @Schema(description = "房东姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    private String name;

    /**
     * 联系电话
     */
    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    @Schema(description = "房东联系电话（11位手机号）", requiredMode = Schema.RequiredMode.REQUIRED, example = "13900139000")
    private String phone;

    /**
     * 身份证号
     */
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$",
            message = "身份证号格式不正确")
    @Schema(description = "房东身份证号（18位，可选）", example = "110101199001011234")
    private String idCard;

    /**
     * 性别（1-男，2-女）
     */
    @Schema(description = "性别（1-男，2-女，可选）", example = "1")
    private Integer gender;

    /**
     * 联系地址
     */
    @Schema(description = "房东联系地址（可选）", example = "北京市朝阳区XX街道")
    private String address;

    /**
     * 备注信息
     */
    @Schema(description = "备注信息（可选）", example = "偏好下午联系")
    private String remark;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间（自动生成，无需传入）", example = "2023-11-01T10:00:00")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间（自动更新，无需传入）", example = "2023-11-01T16:30:00")
    private LocalDateTime updatedAt;

    /**
     * 创建人ID
     */
    @NotNull(message = "创建人ID不能为空")
    @Schema(description = "创建人ID（操作人ID）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long createdBy;

    /**
     * 更新人ID
     */
    @NotNull(message = "更新人ID不能为空")
    @Schema(description = "更新人ID（操作人ID）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long updatedBy;

    /**
     * 分组校验：区分新增和更新场景
     */
    public interface Update {}
}

package COM.House.Deed.Pavilion.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 客户信息新增DTO
 */
@Data
@Schema(description = "客户信息新增参数")
public class CustomerAddDTO {

    @NotBlank(message = "客户姓名不能为空")
    @Schema(description = "客户姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String name;

    @NotBlank(message = "联系电话不能为空")
    @Schema(description = "客户联系电话", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    private String phone;

    @Schema(description = "性别（1-男，2-女）", example = "1")
    private Integer gender;

    @Schema(description = "客户的购房/租房需求", example = "需要3室2厅，朝南，近地铁")
    private String demand;

    @Schema(description = "客户来源渠道", example = "转介绍")
    private String source;

    @Schema(description = "客户状态（1-活跃，2-已成交，3-休眠，默认1）", example = "1")
    private Integer status = 1;

    @Schema(description = "备注信息", example = "客户希望月底前看房")
    private String remark;

    @NotNull(message = "创建人ID不能为空")
    @Schema(description = "创建人用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long createdBy;
}

package COM.House.Deed.Pavilion.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 客户信息更新DTO
 */
@Data
@Schema(description = "客户信息更新参数")
public class CustomerUpdateDTO {

    @NotNull(message = "客户ID不能为空")
    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "30001")
    private Long id;

    @Schema(description = "客户姓名", example = "张三")
    private String name;

    @Schema(description = "客户联系电话", example = "13800138000")
    private String phone;

    @Schema(description = "性别（1-男，2-女）", example = "1")
    private Integer gender;

    @Schema(description = "客户的购房/租房需求", example = "需要3室2厅，朝南，近地铁")
    private String demand;

    @Schema(description = "客户来源渠道", example = "转介绍")
    private String source;

    @Schema(description = "客户状态（1-活跃，2-已成交，3-休眠）", example = "2")
    private Integer status;

    @Schema(description = "备注信息", example = "客户已成交，购买了1号楼302室")
    private String remark;

    @NotNull(message = "更新人ID不能为空")
    @Schema(description = "更新人用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long updatedBy;
}

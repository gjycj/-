package COM.House.Deed.Pavilion.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 房东查询条件DTO
 * 用于多条件分页查询房东信息
 */
@Data
@Schema(description = "房东查询条件参数")
public class LandlordQueryDTO {

    @Schema(description = "房东姓名（模糊匹配）", example = "李")
    private String name;

    @Schema(description = "联系电话（模糊匹配，如'139'）", example = "139")
    private String phone;

    @Schema(description = "身份证号（精确匹配或模糊匹配）", example = "110101")
    private String idCard;

    @Schema(description = "性别（1-男，2-女，null-查询全部）", example = "1")
    private Integer gender;

    @Schema(description = "页码（默认1）", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数（默认10，最大100）", example = "10")
    private Integer pageSize = 10;
}

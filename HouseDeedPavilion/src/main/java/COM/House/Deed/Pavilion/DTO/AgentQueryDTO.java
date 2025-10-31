package COM.House.Deed.Pavilion.DTO;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 经纪人查询条件DTO
 * 用于多条件分页查询经纪人信息
 */
@Data
@Schema(description = "经纪人查询条件参数")
public class AgentQueryDTO {

    @Schema(description = "经纪人姓名（模糊匹配）", example = "张")
    private String name;

    @Schema(description = "工号（精确匹配或模糊匹配）", example = "EMP2023")
    private String employeeId;

    @Schema(description = "联系电话（模糊匹配，如'138'）", example = "138")
    private String phone;

    @Schema(description = "所属门店ID（精确匹配）", example = "5001")
    private Long storeId;

    @Schema(description = "状态（1-在职，0-离职，null-查询全部）", example = "1")
    private Integer status;

    @Schema(description = "页码（默认1）", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数（默认10，最大100）", example = "10")
    private Integer pageSize = 10;
}

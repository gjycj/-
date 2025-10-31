package COM.House.Deed.Pavilion.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户信息查询条件DTO
 */
@Data
@Schema(description = "客户信息查询参数")
public class CustomerQueryDTO {

    @Schema(description = "客户ID（精确匹配）", example = "30001")
    private Long id;

    @Schema(description = "客户姓名（模糊匹配）", example = "张")
    private String name;

    @Schema(description = "联系电话（精确匹配）", example = "13800138000")
    private String phone;

    @Schema(description = "性别（1-男，2-女）", example = "1")
    private Integer gender;

    @Schema(description = "客户状态（1-活跃，2-已成交，3-休眠）", example = "1")
    private Integer status;

    @Schema(description = "客户来源（模糊匹配）", example = "转介绍")
    private String source;

    @Schema(description = "创建时间起始（如：2023-11-01 00:00:00）", example = "2023-11-01T00:00:00")
    private LocalDateTime createdAtStart;

    @Schema(description = "创建时间结束（如：2023-11-30 23:59:59）", example = "2023-11-30T23:59:59")
    private LocalDateTime createdAtEnd;

    @Schema(description = "页码（默认1）", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数（默认10，最大100）", example = "10")
    private Integer pageSize = 10;
}

package COM.House.Deed.Pavilion.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 房源新增参数DTO
 * 接收前端传递的新增房源信息，包含完整校验规则
 */
@Data
@Schema(description = "房源新增参数")
public class HouseAddDTO {

    @NotNull(message = "所属楼栋ID不能为空")
    @Positive(message = "所属楼栋ID必须为正整数")
    @Schema(description = "所属楼栋ID（关联building表）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long buildingId;

    @NotBlank(message = "门牌号不能为空")
    @Schema(description = "房源门牌号（同一楼栋下唯一）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1-101")
    private String houseNumber;

    @NotBlank(message = "户型不能为空")
    @Schema(description = "房源户型（如3室2厅1卫）", requiredMode = Schema.RequiredMode.REQUIRED, example = "3室2厅1卫")
    private String layout;

    @NotNull(message = "建筑面积不能为空")
    @Positive(message = "建筑面积必须大于0（单位：平方米）")
    @Schema(description = "建筑面积（平方米）", requiredMode = Schema.RequiredMode.REQUIRED, example = "120.50")
    private BigDecimal area;

    @PositiveOrZero(message = "套内面积不能为负数（单位：平方米）")
    @Schema(description = "套内面积（平方米，可选）", example = "100.30")
    private BigDecimal innerArea;

    @NotNull(message = "所在楼层不能为空")
    @Positive(message = "所在楼层必须大于0")
    @Schema(description = "房源所在楼层", requiredMode = Schema.RequiredMode.REQUIRED, example = "15")
    private Integer floor;

    @PositiveOrZero(message = "总楼层不能为负数")
    @Schema(description = "楼栋总楼层（可选）", example = "33")
    private Integer totalFloor;

    @Schema(description = "房源朝向（如南北通透，可选）", example = "南北通透")
    private String orientation;

    @Schema(description = "装修情况（如毛坯、简装、精装，可选）", example = "精装")
    private String decoration;

    @NotNull(message = "房源类型不能为空")
    @Schema(description = "房源类型（1-出售，2-出租）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer houseType; // 前端传递枚举code（与HouseTypeEnum对应）

    @NotNull(message = "价格不能为空")
    @Positive(message = "价格必须大于0（出售单位：万元，出租单位：元/月）")
    @Schema(description = "房源价格（出售单位：万元，出租单位：元/月）", requiredMode = Schema.RequiredMode.REQUIRED, example = "150.00")
    private BigDecimal price;

    @NotNull(message = "房源状态不能为空")
    @Schema(description = "房源状态（1-待售/待租，2-已预订，3-已成交，4-已下架）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status; // 前端传递枚举code（与HouseStatusEnum对应）

    @PositiveOrZero(message = "业主底价不能为负数")
    @Schema(description = "业主底价（可选，单位与价格一致）", example = "145.00")
    private BigDecimal ownerPrice;

    @Schema(description = "房源描述（可选，如采光、楼层优势等）", example = "中间楼层，采光充足，配套成熟")
    private String description;

    @NotNull(message = "负责经纪人ID不能为空")
    @Positive(message = "经纪人ID必须为正整数")
    @Schema(description = "负责经纪人ID（关联agent表）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    private Long agentId;

    @NotNull(message = "创建人ID不能为空")
    @Positive(message = "创建人ID必须为正整数")
    @Schema(description = "创建人用户ID（操作人）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long createdBy;
}
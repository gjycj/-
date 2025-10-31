package COM.House.Deed.Pavilion.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 房源更新专用DTO
 * 包含SQL中支持的所有可更新字段及状态变更原因
 */
@Data
@Schema(description = "房源更新参数DTO（包含所有可修改字段）")
public class HouseUpdateDTO {

    /**
     * 房源ID（必须传递）
     */
    @NotNull(message = "房源ID不能为空")
    @Schema(description = "房源ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "20001")
    private Long id;

    /**
     * 楼栋ID（可选更新）
     */
    @Schema(description = "楼栋ID", example = "5001")
    private Long buildingId;

    /**
     * 门牌号（可选更新）
     */
    @Schema(description = "门牌号", example = "1单元302")
    private String houseNumber;

    /**
     * 户型（如：3室2厅）（可选更新）
     */
    @Schema(description = "户型描述", example = "3室2厅1卫")
    private String layout;

    /**
     * 建筑面积（可选更新）
     */
    @Schema(description = "建筑面积（㎡）", example = "120.50")
    private BigDecimal area;

    /**
     * 套内面积（可选更新）
     */
    @Schema(description = "套内面积（㎡）", example = "100.30")
    private BigDecimal innerArea;

    /**
     * 所在楼层（可选更新）
     */
    @Schema(description = "所在楼层", example = "15")
    private Integer floor;

    /**
     * 总楼层（可选更新）
     */
    @Schema(description = "楼栋总楼层", example = "33")
    private Integer totalFloor;

    /**
     * 朝向（如：南北通透）（可选更新）
     */
    @Schema(description = "房屋朝向", example = "南北通透")
    private String orientation;

    /**
     * 装修情况（如：精装修）（可选更新）
     */
    @Schema(description = "装修情况", example = "精装修")
    private String decoration;

    /**
     * 房屋类型编码（可选更新）
     * 对应House实体中houseType的code字段
     */
    @Schema(description = "房屋类型编码", example = "1")
    private Integer houseTypeCode;

    /**
     * 挂牌价格（可选更新）
     */
    @Schema(description = "挂牌价格（元）", example = "2800")
    private BigDecimal price;

    /**
     * 房源状态编码（可选更新）
     * 对应House实体中status的code字段
     */
    @Schema(description = "房源状态编码（如：1-待租，2-已租）", example = "2")
    private Integer statusCode;

    /**
     * 业主报价（可选更新）
     */
    @Schema(description = "业主报价（元）", example = "2700")
    private BigDecimal ownerPrice;

    /**
     * 房源描述（可选更新）
     */
    @Schema(description = "房源详细描述", example = "采光好，拎包入住")
    private String description;

    /**
     * 经纪人ID（可选更新）
     */
    @Schema(description = "负责经纪人ID", example = "1001")
    private Long agentId;

    /**
     * 是否有效（可选更新）
     */
    @Schema(description = "是否有效（1-有效，0-无效）", example = "1")
    private Integer isValid;

    /**
     * 操作人ID（必须传递）
     */
    @NotNull(message = "操作人ID不能为空")
    @Schema(description = "执行更新的用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long updatedBy;

    /**
     * 状态变更原因（仅当更新statusCode时需要传递）
     */
    @Schema(description = "状态变更原因（状态更新时必填）", example = "房源已出租，状态变更为已租")
    private String statusChangeReason;
}

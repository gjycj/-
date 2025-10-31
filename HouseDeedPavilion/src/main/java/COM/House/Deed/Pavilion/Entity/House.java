package COM.House.Deed.Pavilion.Entity;

import COM.House.Deed.Pavilion.Entity.Enum.HouseStatusEnum;
import COM.House.Deed.Pavilion.Entity.Enum.HouseTypeEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房源信息实体类（关联楼栋表）
 */
@Data
@Schema(description = "房源信息实体类")
public class House {

    /**
     * 关联所属楼栋信息（非数据库字段，仅查询时返回）
     */
    @JsonIgnore // JSON序列化时忽略，避免循环引用
    @Schema(description = "所属楼栋详情（关联查询时返回）")
    private Building building;

    /**
     * 房源ID（自增主键）
     */
    @NotNull(message = "房源ID不能为空", groups = Update.class)
    @Schema(description = "房源ID（更新时必填）", requiredMode = Schema.RequiredMode.REQUIRED, example = "3001")
    private Long id;

    /**
     * 所属楼栋ID（外键关联building表）
     */
    @NotNull(message = "所属楼栋ID不能为空")
    @Positive(message = "所属楼栋ID必须为正整数")
    @Schema(description = "所属楼栋ID（关联楼栋表主键）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long buildingId;

    /**
     * 门牌号
     */
    @NotNull(message = "门牌号不能为空")
    @Schema(description = "房源门牌号（如1-101）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1-101")
    private String houseNumber;

    /**
     * 户型
     */
    @NotNull(message = "户型信息不能为空")
    @Schema(description = "房源户型（如3室2厅1卫）", requiredMode = Schema.RequiredMode.REQUIRED, example = "3室2厅1卫")
    private String layout;

    /**
     * 建筑面积(平方米)
     */
    @NotNull(message = "建筑面积不能为空")
    @Positive(message = "建筑面积必须大于0")
    @Schema(description = "房源建筑面积（平方米）", requiredMode = Schema.RequiredMode.REQUIRED, example = "120.50")
    private BigDecimal area;

    /**
     * 套内面积(平方米)
     */
    @PositiveOrZero(message = "套内面积不能为负数")
    @Schema(description = "房源套内面积（平方米，可选）", example = "100.30")
    private BigDecimal innerArea;

    /**
     * 所在楼层
     */
    @NotNull(message = "所在楼层不能为空")
    @Positive(message = "所在楼层必须大于0")
    @Schema(description = "房源所在楼层", requiredMode = Schema.RequiredMode.REQUIRED, example = "15")
    private Integer floor;

    /**
     * 总楼层
     */
    @PositiveOrZero(message = "总楼层不能为负数")
    @Schema(description = "楼栋总楼层（可选）", example = "33")
    private Integer totalFloor;

    /**
     * 朝向
     */
    @Schema(description = "房源朝向（如南北通透，可选）", example = "南北通透")
    private String orientation;

    /**
     * 装修情况
     */
    @Schema(description = "装修情况（如精装，可选）", example = "精装")
    private String decoration;

    /**
     * 房源类型（枚举：1-出售，2-出租）
     */
    @NotNull(message = "房源类型不能为空")
    @Schema(description = "房源类型（1-出售，2-出租）", requiredMode = Schema.RequiredMode.REQUIRED)
    private HouseTypeEnum houseType;

    /**
     * 价格（售价/租金）
     */
    @NotNull(message = "价格不能为空")
    @Positive(message = "价格必须大于0")
    @Schema(description = "房源价格（出售单位：万元，出租单位：元/月）", requiredMode = Schema.RequiredMode.REQUIRED, example = "150.00")
    private BigDecimal price;

    /**
     * 房源状态（枚举：1-待售/待租，2-已预订，3-已成交，4-已下架）
     */
    @NotNull(message = "房源状态不能为空")
    @Schema(description = "房源状态（1-待售/待租，2-已预订，3-已成交，4-已下架）", requiredMode = Schema.RequiredMode.REQUIRED)
    private HouseStatusEnum status;

    /**
     * 业主底价
     */
    @PositiveOrZero(message = "业主底价不能为负数")
    @Schema(description = "业主底价（可选）", example = "145.00")
    private BigDecimal ownerPrice;

    /**
     * 房源描述
     */
    @Schema(description = "房源详细描述（可选）", example = "中间楼层，采光充足，无遮挡")
    private String description;

    /**
     * 负责经纪人ID
     */
    @NotNull(message = "负责经纪人ID不能为空")
    @Positive(message = "经纪人ID必须为正整数")
    @Schema(description = "负责该房源的经纪人ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    private Long agentId;

    /**
     * 是否有效（1-有效，0-无效）
     */
    @Schema(description = "是否有效（1-有效，0-无效，默认1）", example = "1")
    private Integer isValid = 1;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间（自动生成，无需传入）", example = "2023-10-31T15:30:00")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间（自动更新，无需传入）", example = "2023-10-31T16:45:00")
    private LocalDateTime updatedAt;

    /**
     * 创建人ID
     */
    @NotNull(message = "创建人ID不能为空")
    @Positive(message = "创建人ID必须为正整数")
    @Schema(description = "创建人ID（登录用户ID）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long createdBy;

    /**
     * 更新人ID
     */
    @NotNull(message = "更新人ID不能为空")
    @Positive(message = "更新人ID必须为正整数")
    @Schema(description = "更新人ID（新增时与创建人ID一致）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long updatedBy;

    /**
     * 分组校验：区分新增和更新场景
     */
    public interface Update {}

    /**
     * 根据编码设置房屋类型
     */
    public void setHouseTypeByCode(Integer code) {
        if (code != null) {
            this.houseType = HouseTypeEnum.getByCode(code);
        }
    }

    /**
     * 根据编码设置房源状态
     */
    public void setStatusByCode(Integer code) {
        if (code != null) {
            this.status = HouseStatusEnum.getByCode(code);
        }
    }
}

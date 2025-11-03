package COM.House.Deed.Pavilion.Entity;

import COM.House.Deed.Pavilion.Enum.HouseStatusEnum;
import COM.House.Deed.Pavilion.Enum.HouseTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房源备份实体类（对应house_backup表）
 * 存储被物理删除的房源完整数据，用于数据恢复和审计
 */
@Data
@Schema(description = "房源备份信息实体类（存储被删除的房源数据）")
public class HouseBackup {

    /**
     * 备份记录ID（自增主键）
     */
    @Schema(description = "备份记录ID（自动生成）", example = "6001")
    private Long id;

    /**
     * 原房源ID（对应house表的id）
     */
    @NotNull(message = "原房源ID不能为空")
    @Schema(description = "原房源ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "3001")
    private Long originalId;

    /**
     * 所属楼栋ID（原数据）
     */
    @NotNull(message = "所属楼栋ID不能为空")
    @Positive(message = "所属楼栋ID必须为正整数") // 补充精细化校验
    @Schema(description = "所属楼栋ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long buildingId;

    /**
     * 门牌号（原数据）
     */
    @NotNull(message = "门牌号不能为空")
    @Schema(description = "房源门牌号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1-101")
    private String houseNumber;

    /**
     * 户型（原数据）
     */
    @NotNull(message = "户型不能为空")
    @Schema(description = "房源户型", requiredMode = Schema.RequiredMode.REQUIRED, example = "3室2厅1卫")
    private String layout;

    /**
     * 建筑面积(平方米)（原数据）
     */
    @NotNull(message = "建筑面积不能为空")
    @Positive(message = "建筑面积必须大于0") // 补充精细化校验
    @Schema(description = "建筑面积（平方米）", requiredMode = Schema.RequiredMode.REQUIRED, example = "120.50")
    private BigDecimal area;

    /**
     * 套内面积(平方米)（原数据）
     */
    @PositiveOrZero(message = "套内面积不能为负数") // 补充精细化校验
    @Schema(description = "套内面积（平方米，可选）", example = "100.30")
    private BigDecimal innerArea;

    /**
     * 所在楼层（原数据）
     */
    @NotNull(message = "所在楼层不能为空")
    @Positive(message = "所在楼层必须大于0") // 补充精细化校验
    @Schema(description = "所在楼层", requiredMode = Schema.RequiredMode.REQUIRED, example = "15")
    private Integer floor;

    /**
     * 总楼层（原数据）
     */
    @PositiveOrZero(message = "总楼层不能为负数") // 补充精细化校验
    @Schema(description = "楼栋总楼层（可选）", example = "33")
    private Integer totalFloor;

    /**
     * 朝向（原数据）
     */
    @Schema(description = "房源朝向（可选）", example = "南北通透")
    private String orientation;

    /**
     * 装修情况（原数据）
     */
    @Schema(description = "装修情况（可选）", example = "精装")
    private String decoration;

    /**
     * 房源类型（枚举：1-出售，2-出租）（原数据）
     */
    @NotNull(message = "房源类型不能为空")
    @Schema(description = "房源类型（1-出售，2-出租）", requiredMode = Schema.RequiredMode.REQUIRED)
    private HouseTypeEnum houseType; // 调整为枚举类型，与House一致

    /**
     * 价格（原数据）
     */
    @NotNull(message = "价格不能为空")
    @Positive(message = "价格必须大于0") // 补充精细化校验
    @Schema(description = "房源价格（出售单位：万元，出租单位：元/月）", requiredMode = Schema.RequiredMode.REQUIRED, example = "150.00")
    private BigDecimal price;

    /**
     * 房源状态（枚举：1-待售/待租，2-已预订，3-已成交，4-已下架）（原数据）
     */
    @NotNull(message = "房源状态不能为空")
    @Schema(description = "房源状态（1-待售/待租，2-已预订，3-已成交，4-已下架）", requiredMode = Schema.RequiredMode.REQUIRED)
    private HouseStatusEnum status; // 调整为枚举类型，与House一致

    /**
     * 业主底价（原数据）
     */
    @PositiveOrZero(message = "业主底价不能为负数") // 补充精细化校验
    @Schema(description = "业主底价（可选）", example = "145.00")
    private BigDecimal ownerPrice;

    /**
     * 房源描述（原数据）
     */
    @Schema(description = "房源描述（可选）", example = "中间楼层，采光充足")
    private String description;

    /**
     * 负责经纪人ID（原数据）
     */
    @NotNull(message = "经纪人ID不能为空")
    @Positive(message = "经纪人ID必须为正整数") // 补充精细化校验
    @Schema(description = "负责经纪人ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    private Long agentId;

    /**
     * 删除时的有效状态（原数据）
     */
    @NotNull(message = "有效状态不能为空")
    @Schema(description = "删除时的有效状态（1-有效，0-无效）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer isValid; // 移除默认值，严格复制原表状态

    /**
     * 原创建时间
     */
    @NotNull(message = "原创建时间不能为空")
    @Schema(description = "原记录创建时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2023-10-31T15:30:00")
    private LocalDateTime createdAt;

    /**
     * 原更新时间
     */
    @NotNull(message = "原更新时间不能为空")
    @Schema(description = "原记录最后更新时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2023-10-31T16:45:00")
    private LocalDateTime updatedAt;

    /**
     * 原创建人ID
     */
    @NotNull(message = "原创建人ID不能为空")
    @Positive(message = "创建人ID必须为正整数") // 补充精细化校验
    @Schema(description = "原记录创建人ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long createdBy;

    /**
     * 原更新人ID
     */
    @NotNull(message = "原更新人ID不能为空")
    @Positive(message = "更新人ID必须为正整数") // 补充精细化校验
    @Schema(description = "原记录最后更新人ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long updatedBy;

    /**
     * 删除原因
     */
    @Schema(description = "删除原因（可选）", example = "房源信息录入错误")
    private String deleteReason;

    /**
     * 删除操作时间
     */
    @NotNull(message = "删除时间不能为空")
    @Schema(description = "原房源被删除的时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2023-11-05T09:30:00")
    private LocalDateTime deletedAt;

    /**
     * 执行删除的操作人ID
     */
    @NotNull(message = "删除操作人ID不能为空")
    @Positive(message = "删除操作人ID必须为正整数") // 补充精细化校验
    @Schema(description = "执行删除操作的用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1002")
    private Long deletedBy;

    /**
     * 备份记录生成时间（自动生成）
     */
    @Schema(description = "备份记录生成时间（自动生成）", example = "2023-11-05T09:30:00")
    private LocalDateTime backupTime;


    // 补充与House一致的枚举转换方法，确保业务处理兼容
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
package com.house.deed.pavilion.module.house.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.house.deed.pavilion.module.house.repository.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 房源录入请求DTO，用于接收新增房源的基础信息及关联数据（需符合当前租户数据隔离规则）
 */
@Data
@Schema(description = "房源录入请求参数，包含房源基础信息、关联房东、标签及图片列表")
public class HouseAddDTO {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(
            description = "房源唯一标识（自增主键）",
            example = "1001",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long id;

    @Schema(
            description = "所属楼盘ID（关联property表，必须为当前租户下已存在的楼盘）",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long propertyId;

    @Schema(
            description = "所属楼栋ID（关联building表，必须为当前租户下已存在的楼栋，且归属上述楼盘）",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long buildingId;

    @Schema(
            description = "房号（物理标识），格式建议为[单元号]-[房间号]（如1-301、2-502），同一楼栋内唯一",
            example = "1-301",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String houseNo;

    @Schema(
            description = "户型描述（如3室2厅1卫、2室1厅）",
            example = "3室2厅",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String houseType;

    @Schema(
            description = "建筑面积（单位：㎡，保留两位小数）",
            example = "120.50",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private BigDecimal area;

    @Schema(
            description = "套内面积（单位：㎡，保留两位小数，非必填）",
            example = "96.40"
    )
    private BigDecimal insideArea;

    @Schema(
            description = "所在楼层（需小于总楼层数）",
            example = "3",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer floor;

    @Schema(
            description = "楼栋总层数",
            example = "30",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer totalFloor;

    @Schema(
            description = "房屋朝向（如南北通透、朝南、东西向等）",
            example = "南北通透",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String orientation;

    @Schema(
            description = "装修情况",
            example = "精装",
            allowableValues = {"毛坯", "简装", "精装"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String decoration;

    @Schema(
            description = "产权性质",
            example = "商品房",
            allowableValues = {"商品房", "经济适用房", "廉租房", "公租房"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String propertyRight;

    @Schema(
            description = "产权证号（格式依当地政策，如京房权证海字第123456号）",
            example = "123456789012345"
    )
    private String propertyRightCertNo;

    @Schema(
            description = "产权年限（单位：年，如70年、50年）",
            example = "70"
    )
    private Integer propertyRightYears;

    @Schema(
            description = "抵押状态",
            example = "NONE",
            allowableValues = {"NONE", "MORTGAGED"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String mortgageStatus;

    @Schema(
            description = "抵押详情（抵押状态为MORTGAGED时必填，需说明抵押银行、金额等信息）",
            example = "招商银行，抵押金额50万元"
    )
    private String mortgageDetails;

    @Schema(
            description = "挂牌价（单位：万元，保留两位小数）",
            example = "150.00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private BigDecimal price;

    @Schema(
            description = "交易类型（SALE-出售，RENT-出租，BOTH-可售可租）",
            example = "SALE",
            allowableValues = {"SALE", "RENT", "BOTH"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "交易类型不能为空")
    private TransactionType transactionType;

    @Schema(
            description = "房源详细描述（如配套设施、交通情况、房屋亮点等）",
            example = "采光好，近地铁10号线，小区绿化率30%"
    )
    private String description;

    @Schema(
            description = "关联房东ID列表（至少关联一个房东，需为当前租户下的有效房东ID，关联landlord表）",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<Long> landlordIds;

    @Schema(
            description = "关联标签ID列表（可选，需为当前租户下的有效标签ID，关联tag表）"
    )
    private List<Long> tagIds;

    @Schema(
            description = "房源图片列表（可选，包含图片URL、类型及排序信息）"
    )
    private List<HouseImageDTO> imageList;
}
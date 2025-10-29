package COM.House.Deed.Pavilion.Entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 楼盘信息实体类（原生MyBatis版）
 */
@Data // Lombok注解，自动生成getter、setter、toString等方法
public class Property {

    /**
     * 楼盘ID（自增主键）
     */
    @NotNull(message = "楼盘ID不能为空") // 更新时必须传递ID
    @Schema(description = "楼盘ID（更新时必填）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    /**
     * 楼盘名称
     */
    // 楼盘名称：非空校验
    @NotBlank(message = "楼盘名称不能为空")
    @Schema(description = "楼盘名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "阳光花园")
    private String name;

    /**
     * 楼盘地址
     */
    // 楼盘地址：非空校验
    @NotBlank(message = "楼盘地址不能为空")
    @Schema(description = "楼盘地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "北京市朝阳区XX路100号")
    private String address;

    /**
     * 开发商
     */
    @Schema(description = "开发商", example = "阳光地产")
    private String developer;

    /**
     * 物业类型：住宅、商业、别墅等
     */
    // 物业类型：非空校验
    @NotBlank(message = "物业类型不能为空")
    @Schema(description = "物业类型（住宅/商业/别墅等）", requiredMode = Schema.RequiredMode.REQUIRED, example = "住宅")
    private String propertyType; // 对应数据库字段property_type（依赖MyBatis驼峰映射）

    /**
     * 建成年代
     */
    @Schema(description = "建成年代", example = "2020")
    private Integer completionYear; // 对应数据库字段completion_year

    /**
     * 物业公司
     */
    @Schema(description = "物业公司", example = "阳光物业")
    private String propertyCompany; // 对应数据库字段property_company

    /**
     * 物业费
     */
    @Schema(description = "物业费（元/㎡·月）", example = "2.50")
    private BigDecimal propertyFee; // 对应数据库字段property_fee

    /**
     * 楼盘描述
     */
    @Schema(description = "楼盘描述", example = "配套设施完善，交通便利")
    private String description;

    /**
     * 经度
     */
    @Schema(description = "经度", example = "116.403874")
    private BigDecimal longitude;

    /**
     * 纬度
     */
    @Schema(description = "纬度", example = "39.914885")
    private BigDecimal latitude;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间（新增时无需传递，自动生成）", example = "2024-05-20T10:30:00")
    private LocalDateTime createdAt; // 对应数据库字段created_at

    /**
     * 更新时间
     */
    @Schema(description = "更新时间（新增时无需传递，自动生成）", example = "2024-05-20T10:30:00")
    private LocalDateTime updatedAt; // 对应数据库字段updated_at

    /**
     * 创建人ID
     */
    // 创建人ID：非空校验（需从登录用户获取）
    @NotNull(message = "创建人ID不能为空")
    @Schema(description = "创建人ID（需登录用户ID）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long createdBy; // 对应数据库字段created_by

    /**
     * 更新人ID
     */
    // 更新人ID：非空校验（新增时与创建人一致）
    @NotNull(message = "更新人ID不能为空")
    @Schema(description = "更新人ID（新增时与创建人ID一致）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long updatedBy; // 对应数据库字段updated_by
}
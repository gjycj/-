package COM.House.Deed.Pavilion.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlTransient;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;


import lombok.Data;

import java.time.LocalDateTime;
/**
 * 楼栋信息实体类（关联楼盘表）
 */
@Data
@Schema(description = "楼栋信息实体类")
public class Building {

    /**
     * 关联所属楼盘信息（非数据库字段，仅查询时返回）
     */
    @XmlTransient
    @JsonIgnore // 关键：JSON反序列化时忽略该字段（前端传递的property会被无视）
    @Schema(description = "所属楼盘的详细信息（关联查询时返回）")
    private Property property;

    /**
     * 楼栋ID（自增主键）
     */
    @Schema(description = "楼栋ID（新增时无需传递，自动生成）", example = "1")
    private Long id;

    /**
     * 所属楼盘ID（外键关联property表的id）
     */
    @NotNull(message = "所属楼盘ID不能为空")
    @Schema(description = "所属楼盘ID（关联楼盘表的主键）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long propertyId; // 对应数据库字段property_id（驼峰映射）

    /**
     * 楼栋名称/编号
     */
    @NotBlank(message = "楼栋名称不能为空")
    @Schema(description = "楼栋名称或编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1号楼")
    private String name;

    /**
     * 总层数
     */
    @Schema(description = "楼栋总层数", example = "33")
    private Integer totalFloor; // 对应数据库字段total_floor

    /**
     * 单元数
     */
    @Schema(description = "楼栋单元数量", example = "2")
    private Integer unitCount; // 对应数据库字段unit_count

    /**
     * 楼栋描述
     */
    @Schema(description = "楼栋详细描述", example = "南北通透，两梯四户")
    private String description;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间（新增时无需传递，自动生成）", example = "2024-05-20T10:30:00")
    private LocalDateTime createdAt; // 对应数据库字段created_at

    /**
     * 更新时间
     */
    @Schema(description = "更新时间（新增/更新时无需传递，自动生成）", example = "2024-05-20T10:30:00")
    private LocalDateTime updatedAt; // 对应数据库字段updated_at

    /**
     * 创建人ID
     */
    @NotNull(message = "创建人ID不能为空")
    @Schema(description = "创建人ID（需登录用户ID）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long createdBy; // 对应数据库字段created_by

    /**
     * 更新人ID
     */
    @NotNull(message = "更新人ID不能为空")
    @Schema(description = "更新人ID（新增时与创建人ID一致）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long updatedBy; // 对应数据库字段updated_by
}
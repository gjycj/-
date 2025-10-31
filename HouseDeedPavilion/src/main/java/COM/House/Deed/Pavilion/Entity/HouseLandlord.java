package COM.House.Deed.Pavilion.Entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 房源-房东关联实体类（对应house_landlord表）
 * 处理房源与房东的多对多关系
 */
@Data
@Schema(description = "房源-房东关联信息实体类")
public class HouseLandlord {

    /**
     * 关联ID（自增主键）
     */
    @NotNull(message = "关联ID不能为空", groups = Update.class)
    @Schema(description = "关联记录ID（更新/删除时必填）", requiredMode = Schema.RequiredMode.REQUIRED, example = "4001")
    private Long id;

    /**
     * 房源ID（外键）
     */
    @NotNull(message = "房源ID不能为空")
    @Schema(description = "关联的房源ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "20001")
    private Long houseId;

    /**
     * 房东ID（外键）
     */
    @NotNull(message = "房东ID不能为空")
    @Schema(description = "关联的房东ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "3001")
    private Long landlordId;

    /**
     * 是否主要房东（1-是，0-否）
     */
    @NotNull(message = "是否主要房东不能为空")
    @Schema(description = "是否主要房东（1-是，0-否）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer isMain;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间（自动生成，无需传入）", example = "2023-11-01T10:00:00")
    private LocalDateTime createdAt;

    /**
     * 创建人ID
     */
    @NotNull(message = "创建人ID不能为空")
    @Schema(description = "创建人ID（操作人ID）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long createdBy;

    /**
     * 分组校验：区分更新/删除场景
     */
    public interface Update {}
}

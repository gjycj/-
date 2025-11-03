package COM.House.Deed.Pavilion.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 房东备份查询DTO
 * 用于多条件分页查询房东备份
 */
@Data
@Schema(description = "房东备份查询条件DTO")
public class LandlordBackupQueryDTO {

    @Schema(description = "原房东ID")
    private Long originalId;

    @Schema(description = "房东姓名模糊查询")
    private String nameLike;

    @Schema(description = "联系电话精确查询")
    private String phone;

    @Schema(description = "备份时间起始（yyyy-MM-dd HH:mm:ss）")
    private LocalDateTime backupTimeStart;

    @Schema(description = "备份时间结束（yyyy-MM-dd HH:mm:ss）")
    private LocalDateTime backupTimeEnd;

    @Schema(description = "删除人ID")
    private Long deletedBy;

    @Schema(description = "页码（默认1）")
    private Integer pageNum;

    @Schema(description = "页大小（默认10）")
    private Integer pageSize;
}
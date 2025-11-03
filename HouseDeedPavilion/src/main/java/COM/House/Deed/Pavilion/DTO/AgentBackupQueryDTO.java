package COM.House.Deed.Pavilion.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "经纪人备份查询条件DTO")
public class AgentBackupQueryDTO {
    @Schema(description = "姓名模糊查询")
    private String nameLike;

    @Schema(description = "电话精确查询")
    private String phone;

    @Schema(description = "原经纪人ID")
    private Long originalId;

    @Schema(description = "备份开始时间（yyyy-MM-dd HH:mm:ss）")
    private LocalDateTime backupTimeStart;

    @Schema(description = "备份结束时间（yyyy-MM-dd HH:mm:ss）")
    private LocalDateTime backupTimeEnd;

    @Schema(description = "删除时的状态（1-在职，0-离职）")
    private Integer status;

    // 分页参数（复用项目通用分页）
    private Integer pageNum;
    private Integer pageSize;
}
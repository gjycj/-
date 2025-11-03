package COM.House.Deed.Pavilion.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

/**
 * 楼盘备份查询DTO
 * 支持按原楼盘ID、名称、地址、时间范围等条件筛选
 */
@Data
@Schema(description = "楼盘备份记录查询条件参数")
public class PropertyBackupQueryDTO {

    /**
     * 原楼盘ID
     */
    @Schema(description = "原楼盘ID", example = "101")
    private Long originalId;

    /**
     * 楼盘名称（模糊查询）
     */
    @Schema(description = "楼盘名称（模糊匹配）", example = "阳光")
    private String nameLike;

    /**
     * 楼盘地址（模糊查询）
     */
    @Schema(description = "楼盘地址（模糊匹配）", example = "朝阳区")
    private String addressLike;

    /**
     * 备份时间起始
     */
    @Schema(description = "备份时间起始（格式：yyyy-MM-dd HH:mm:ss）", example = "2024-10-01 00:00:00")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime backupTimeStart;

    /**
     * 备份时间截止
     */
    @Schema(description = "备份时间截止（格式：yyyy-MM-dd HH:mm:ss）", example = "2024-10-31 23:59:59")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime backupTimeEnd;

    /**
     * 删除人ID
     */
    @Schema(description = "删除操作人ID", example = "1003")
    private Long deletedBy;

    /**
     * 页码
     */
    @Schema(description = "页码（默认1）", example = "1")
    private Integer pageNum;

    /**
     * 页大小
     */
    @Schema(description = "页大小（默认10）", example = "10")
    private Integer pageSize;
}
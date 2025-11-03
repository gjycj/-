package COM.House.Deed.Pavilion.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

/**
 * 楼栋备份记录多条件查询DTO
 * 用于接收前端传递的查询参数，支持按原楼栋ID、楼盘ID、时间范围等筛选
 */
@Data
@Schema(description = "楼栋备份记录查询条件参数")
public class BuildingBackupQueryDTO {

    // 1. 原楼栋关联条件
    @Schema(description = "原楼栋ID（备份对应的源楼栋ID）", example = "2001")
    private Long originalId; // 对应原building表的id

    @Schema(description = "所属楼盘ID（原楼栋关联的楼盘）", example = "101")
    private Long propertyId; // 对应备份表的property_id

    // 2. 名称模糊查询
    @Schema(description = "楼栋名称（模糊查询，支持部分匹配）", example = "1号")
    private String nameLike; // 用于模糊匹配name字段

    // 3. 时间范围条件（备份时间）
    @Schema(description = "备份时间起始（格式：yyyy-MM-dd HH:mm:ss）", example = "2024-10-01 00:00:00")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") // 支持前端传递的时间格式
    private LocalDateTime backupTimeStart;

    @Schema(description = "备份时间截止（格式：yyyy-MM-dd HH:mm:ss）", example = "2024-10-31 23:59:59")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime backupTimeEnd;

    // 4. 操作人条件
    @Schema(description = "删除操作人ID（原楼栋的删除人）", example = "1003")
    private Long deletedBy; // 对应备份表的deleted_by

    // 5. 分页参数
    @Schema(description = "页码（默认1）", example = "1")
    private Integer pageNum;

    @Schema(description = "页大小（默认10，最大100）", example = "10")
    private Integer pageSize;
}
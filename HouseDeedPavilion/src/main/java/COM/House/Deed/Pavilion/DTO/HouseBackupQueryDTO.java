package COM.House.Deed.Pavilion.DTO;

import COM.House.Deed.Pavilion.Enum.HouseTypeEnum;
import COM.House.Deed.Pavilion.Enum.HouseStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 房源备份查询条件DTO
 */
@Data
@Schema(description = "房源备份记录多条件查询参数")
public class HouseBackupQueryDTO {

    // 基础查询条件
    @Schema(description = "原房源ID（备份对应的源房源ID）", example = "3001")
    private Long originalId;

    @Schema(description = "原楼栋ID", example = "1001")
    private Long buildingId;

    @Schema(description = "房源类型（1-出售，2-出租）", example = "1")
    private HouseTypeEnum houseType; // 枚举类型，对应数据库int值

    @Schema(description = "房源状态（1-有效，2-已预订，3-已成交）", example = "1")
    private HouseStatusEnum status; // 枚举类型，对应数据库int值

    @Schema(description = "门牌号（模糊查询）", example = "302")
    private String houseNumberLike; // 用于模糊匹配门牌号

    // 时间范围条件
    @Schema(description = "删除时间起始（yyyy-MM-dd HH:mm:ss）", example = "2023-11-01 00:00:00")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") // 新增：指定时间格式
    private LocalDateTime deletedAtStart;

    @Schema(description = "删除时间截止（yyyy-MM-dd HH:mm:ss）", example = "2023-11-30 23:59:59")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") // 新增：指定时间格式
    private LocalDateTime deletedAtEnd;

    @Schema(description = "备份时间起始", example = "2023-11-01 00:00:00")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") // 新增：指定时间格式
    private LocalDateTime backupTimeStart;

    @Schema(description = "备份时间截止", example = "2023-11-30 23:59:59")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") // 新增：指定时间格式
    private LocalDateTime backupTimeEnd;

    // 操作人条件
    @Schema(description = "删除操作人ID", example = "1001")
    private Long deletedBy;

    // 分页参数
    @Schema(description = "页码（默认1）", example = "1")
    private Integer pageNum;

    @Schema(description = "页大小（默认10，最大100）", example = "10")
    private Integer pageSize;
}
package COM.House.Deed.Pavilion.Controller;

import COM.House.Deed.Pavilion.DTO.HouseBackupQueryDTO;
import COM.House.Deed.Pavilion.Enum.HouseStatusEnum;
import COM.House.Deed.Pavilion.Enum.HouseTypeEnum;
import COM.House.Deed.Pavilion.Entity.HouseBackup;
import COM.House.Deed.Pavilion.Service.HouseBackupService;
import COM.House.Deed.Pavilion.Utils.PageResult;
import COM.House.Deed.Pavilion.Utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 房源备份记录控制器（与HouseBackupService方法严格对应）
 */
@RestController
@RequestMapping("/houseBackup")
@Tag(name = "房源备份记录管理", description = "房源备份的查询、恢复操作")
public class HouseBackupController {

    @Resource
    private HouseBackupService houseBackupService;

    /**
     * 根据备份ID查询单条备份记录（对应Service.getBackupById）
     */
    @GetMapping("/getById/{backupId}")
    @Operation(
            summary = "查询单个房源备份",
            description = "根据备份记录ID查询房源备份详情",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "备份ID无效或不存在",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统异常",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<HouseBackup> getBackupById(
            @Parameter(description = "备份记录ID", required = true, example = "6001")
            @PathVariable Long backupId
    ) {
        try {
            HouseBackup backup = houseBackupService.getBackupById(backupId);
            return Result.success(backup);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 根据原房源ID分页查询备份记录（对应Service.getBackupsByOriginalId）
     */
    @GetMapping("/listByOriginalId/{originalId}")
    @Operation(
            summary = "按原房源ID查询备份列表",
            description = "查询指定房源的所有历史备份记录（分页）",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "原房源ID无效",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<PageResult<HouseBackup>> getBackupsByOriginalId(
            @Parameter(description = "原房源ID", required = true, example = "3001")
            @PathVariable Long originalId,
            @Parameter(description = "页码（默认1）", example = "1")
            @RequestParam(required = false) Integer pageNum,
            @Parameter(description = "页大小（默认10）", example = "10")
            @RequestParam(required = false) Integer pageSize
    ) {
        try {
            PageResult<HouseBackup> pageResult = houseBackupService.getBackupsByOriginalId(originalId, pageNum, pageSize);
            return Result.success(pageResult);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 多条件分页查询备份记录（对应Service.getBackupsByCondition）
     */
    @GetMapping("/listByCondition")
    @Operation(
            summary = "多条件查询房源备份",
            description = "支持按原房源ID、楼栋ID、操作人等条件分页查询",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "分页参数不合法",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<PageResult<HouseBackup>> getBackupsByCondition(
            // 其他参数...
            @RequestParam(required = false) String houseType,
            @RequestParam(required = false) String status
    ) {
        // 手动转换枚举
        HouseTypeEnum houseTypeEnum = null;
        if (houseType != null) {
            houseTypeEnum = HouseTypeEnum.getByCode(Integer.parseInt(houseType));
        }
        HouseStatusEnum statusEnum = null;
        if (status != null) {
            statusEnum = HouseStatusEnum.getByCode(Integer.parseInt(status));
        }

        // 构建查询DTO
        HouseBackupQueryDTO queryDTO = new HouseBackupQueryDTO();
        queryDTO.setHouseType(houseTypeEnum);
        queryDTO.setStatus(statusEnum);
        // 设置其他参数...

        // 调用Service
        return Result.success(houseBackupService.getBackupsByCondition(queryDTO));
    }

    /**
     * 从备份恢复房源数据（对应Service.recoverFromBackup）
     */
    @PostMapping("/recover/{backupId}")
    @Operation(
            summary = "从备份恢复房源",
            description = "将指定备份记录恢复为有效房源（需校验关联数据有效性）",
            responses = {
                    @ApiResponse(responseCode = "200", description = "恢复成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "备份ID无效或关联数据已删除",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<?> recoverFromBackup(
            @Parameter(description = "备份记录ID", required = true, example = "6001")
            @PathVariable Long backupId,
            @Parameter(description = "操作人ID（当前登录用户）", required = true, example = "1001")
            @RequestParam Long operatorId,
            @Parameter(description = "恢复后是否保留备份（默认true）", example = "true")
            @RequestParam(required = false, defaultValue = "true") boolean keepBackup // 新增参数
    ) {
        try {
            houseBackupService.recoverFromBackup(backupId, operatorId, keepBackup);
            String msg = keepBackup ?
                    "房源恢复成功，备份记录已保留" :
                    "房源恢复成功，备份记录已删除";
            return Result.success(msg);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }
}
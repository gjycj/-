package COM.House.Deed.Pavilion.Controller;

import COM.House.Deed.Pavilion.DTO.BuildingBackupQueryDTO;
import COM.House.Deed.Pavilion.Entity.BuildingBackup;
import COM.House.Deed.Pavilion.Service.BuildingBackupService;
import COM.House.Deed.Pavilion.Utils.Result;
import COM.House.Deed.Pavilion.Utils.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 楼栋备份控制层
 * 提供楼栋备份的查询、恢复、删除等HTTP接口
 */
@RestController
@RequestMapping("/buildingBackup")
@Tag(name = "楼栋备份管理", description = "楼栋备份记录的查询、恢复、删除操作接口")
public class BuildingBackupController {

    @Resource
    private BuildingBackupService buildingBackupService;


    // ========================== 一、备份查询接口 ==========================

    /**
     * 按备份ID查询单条记录
     */
    @GetMapping("/{backupId}")
    @Operation(summary = "查询单个楼栋备份", description = "根据备份ID获取备份详情")
    public Result<BuildingBackup> getBackupById(
            @Parameter(description = "备份记录ID", required = true, example = "7001")
            @PathVariable Long backupId
    ) {
        try {
            BuildingBackup backup = buildingBackupService.getBackupById(backupId);
            if (backup == null) {
                return Result.fail("楼栋备份不存在，ID：" + backupId);
            }
            return Result.success(backup);
        } catch (Exception e) {
            return Result.fail("查询楼栋备份失败：" + e.getMessage());
        }
    }

    /**
     * 按原楼栋ID查询所有备份（分页）
     */
    @GetMapping("/listByOriginalId")
    @Operation(summary = "按原楼栋ID查询备份", description = "查询某楼栋的所有历史备份记录")
    public Result<PageResult<BuildingBackup>> getBackupsByOriginalId(
            @Parameter(description = "原楼栋ID", required = true, example = "2001")
            @RequestParam Long originalId,
            @Parameter(description = "页码（默认1）", example = "1")
            @RequestParam(required = false) Integer pageNum,
            @Parameter(description = "页大小（默认10）", example = "10")
            @RequestParam(required = false) Integer pageSize
    ) {
        try {
            PageResult<BuildingBackup> pageResult = buildingBackupService.getBackupsByOriginalId(originalId, pageNum, pageSize);
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.fail("查询楼栋备份列表失败：" + e.getMessage());
        }
    }

    /**
     * 多条件分页查询备份记录
     */
    @GetMapping("/listByCondition")
    @Operation(summary = "多条件查询备份", description = "支持按楼盘ID、名称、备份时间等条件筛选")
    public Result<PageResult<BuildingBackup>> getBackupsByCondition(
            @Parameter(description = "查询条件（包含分页参数）")
            BuildingBackupQueryDTO queryDTO
    ) {
        try {
            PageResult<BuildingBackup> pageResult = buildingBackupService.getBackupsByCondition(queryDTO);
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.fail("多条件查询楼栋备份失败：" + e.getMessage());
        }
    }


    // ========================== 二、备份恢复接口 ==========================

    /**
     * 从备份恢复楼栋
     */
    @PostMapping("/recover")
    @Operation(summary = "从备份恢复楼栋", description = "将备份数据恢复为原楼栋，支持选择是否保留备份")
    public Result<String> recoverFromBackup(
            @Parameter(description = "备份ID", required = true, example = "7001")
            @RequestParam Long backupId,
            @Parameter(description = "操作人ID（当前登录用户）", required = true, example = "1001")
            @RequestParam Long operatorId,
            @Parameter(description = "是否保留备份（true=保留，false=恢复后删除）", example = "true")
            @RequestParam(defaultValue = "true") boolean keepBackup
    ) {
        try {
            Long recoveredBuildingId = buildingBackupService.recoverFromBackup(backupId, operatorId, keepBackup);
            return Result.success(
                    "楼栋恢复成功，新楼栋ID：" + recoveredBuildingId
            );
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("恢复楼栋失败：" + e.getMessage());
        }
    }

}
package COM.House.Deed.Pavilion.Controller;

import COM.House.Deed.Pavilion.DTO.PropertyBackupQueryDTO;
import COM.House.Deed.Pavilion.Entity.PropertyBackup;
import COM.House.Deed.Pavilion.Service.PropertyBackupService;
import COM.House.Deed.Pavilion.Utils.PageResult;
import COM.House.Deed.Pavilion.Utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 楼盘备份控制器（与Service方法严格对齐）
 */
@RestController
@RequestMapping("/propertyBackup")
@Tag(name = "楼盘备份管理", description = "楼盘备份记录的查询、恢复、删除操作接口")
public class PropertyBackupController {

    @Resource
    private PropertyBackupService propertyBackupService;


    // 1. 按备份ID查询
    @GetMapping("/{backupId}")
    @Operation(summary = "查询单个楼盘备份", description = "根据备份ID获取详情")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "备份ID无效"),
            @ApiResponse(responseCode = "500", description = "系统异常")
    })
    public Result<PropertyBackup> getBackupById(
            @Parameter(description = "备份ID", required = true, example = "8001")
            @PathVariable Long backupId
    ) {
        try {
            PropertyBackup backup = propertyBackupService.getBackupById(backupId);
            if (backup == null) {
                return Result.fail("备份不存在，ID：" + backupId);
            }
            return Result.success(backup);
        } catch (Exception e) {
            return Result.fail("查询失败：" + e.getMessage());
        }
    }


    // 2. 按原楼盘ID查询备份
    @GetMapping("/listByOriginalId")
    @Operation(summary = "按原楼盘ID查询备份", description = "查询某楼盘的所有历史备份")
    public Result<PageResult<PropertyBackup>> getBackupsByOriginalId(
            @Parameter(description = "原楼盘ID", required = true, example = "101")
            @RequestParam Long originalId,
            @Parameter(description = "页码（默认1）", example = "1")
            @RequestParam(required = false) Integer pageNum,
            @Parameter(description = "页大小（默认10）", example = "10")
            @RequestParam(required = false) Integer pageSize
    ) {
        try {
            PageResult<PropertyBackup> pageResult = propertyBackupService.getBackupsByOriginalId(originalId, pageNum, pageSize);
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.fail("查询失败：" + e.getMessage());
        }
    }


    // 3. 多条件查询备份
    @GetMapping("/listByCondition")
    @Operation(summary = "多条件查询备份", description = "支持按名称、地址、时间范围筛选")
    public Result<PageResult<PropertyBackup>> getBackupsByCondition(
            @Parameter(description = "查询条件（含分页参数）")
            PropertyBackupQueryDTO queryDTO
    ) {
        try {
            PageResult<PropertyBackup> pageResult = propertyBackupService.getBackupsByCondition(queryDTO);
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.fail("查询失败：" + e.getMessage());
        }
    }


    // 4. 从备份恢复楼盘
    @PostMapping("/recover")
    @Operation(summary = "恢复楼盘", description = "从备份重建原楼盘，支持保留/删除备份")
    public Result<String> recoverFromBackup(
            @Parameter(description = "备份ID", required = true, example = "8001")
            @RequestParam Long backupId,
            @Parameter(description = "操作人ID", required = true, example = "1001")
            @RequestParam Long operatorId,
            @Parameter(description = "是否保留备份（默认true）", example = "true")
            @RequestParam(defaultValue = "true") boolean keepBackup
    ) {
        try {
            Long recoveredId = propertyBackupService.recoverFromBackup(backupId, operatorId, keepBackup);
            return Result.success("恢复成功，新楼盘ID：" + recoveredId);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("恢复失败：" + e.getMessage());
        }
    }


}
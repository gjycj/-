package COM.House.Deed.Pavilion.Controller;

import COM.House.Deed.Pavilion.Entity.VisitRecordBackup;
import COM.House.Deed.Pavilion.Service.VisitRecordBackupService;
import COM.House.Deed.Pavilion.Utils.PageResult;
import COM.House.Deed.Pavilion.Utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 带看记录备份完整控制器（含查询、恢复）
 */
@RestController
@RequestMapping("/visitRecordBackup")
@Tag(name = "带看记录备份管理", description = "查询、恢复已删除的带看记录备份数据")
public class VisitRecordBackupController {

    @Resource
    private VisitRecordBackupService visitRecordBackupService;


    // -------------------------- 原有接口（保留） --------------------------

    /**
     * 根据原带看记录ID查询备份
     */
    @GetMapping("/{visitRecordId}")
    @Operation(summary = "按带看ID查询备份", description = "查询已删除带看记录的历史数据")
    public Result<VisitRecordBackup> getBackupByVisitRecordId(
            @Parameter(description = "原带看记录ID", required = true, example = "40001")
            @PathVariable Long visitRecordId
    ) {
        try {
            VisitRecordBackup backup = visitRecordBackupService.getBackupByVisitRecordId(visitRecordId);
            return Result.success(backup);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 按客户ID查询带看备份
     */
    @GetMapping("/byCustomer/{customerId}")
    @Operation(summary = "按客户ID查询带看备份", description = "查询指定客户的历史带看记录（客户删除后仍可查）")
    public Result<PageResult<VisitRecordBackup>> getBackupsByCustomerId(
            @Parameter(description = "客户ID", required = true, example = "30001")
            @PathVariable Long customerId,
            @Parameter(description = "页码（默认1）", example = "1")
            @RequestParam(required = false) Integer pageNum,
            @Parameter(description = "每页条数（默认10）", example = "10")
            @RequestParam(required = false) Integer pageSize
    ) {
        try {
            PageResult<VisitRecordBackup> pageResult = visitRecordBackupService.getBackupsByCustomerId(customerId, pageNum, pageSize);
            return Result.success(pageResult);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常");
        }
    }

    /**
     * 分页查询所有带看备份
     */
    @GetMapping("/all")
    @Operation(summary = "查询所有带看备份", description = "按删除时间倒序排列")
    public Result<PageResult<VisitRecordBackup>> getAllVisitBackups(
            @Parameter(description = "页码（默认1）", example = "1")
            @RequestParam(required = false) Integer pageNum,
            @Parameter(description = "每页条数（默认10）", example = "10")
            @RequestParam(required = false) Integer pageSize
    ) {
        try {
            PageResult<VisitRecordBackup> pageResult = visitRecordBackupService.getAllVisitBackups(pageNum, pageSize);
            return Result.success(pageResult);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常");
        }
    }


    // -------------------------- 新增：按时间范围查询 --------------------------

    /**
     * 按备份时间范围查询带看记录备份
     */
    @GetMapping("/byTimeRange")
    @Operation(
            summary = "按时间范围查询带看备份",
            description = "查询指定时间内删除的带看记录（时间格式：yyyy-MM-ddTHH:mm:ss）"
    )
    public Result<PageResult<VisitRecordBackup>> getBackupsByTimeRange(
            @Parameter(description = "开始时间（如：2023-11-01T00:00:00）", example = "2023-11-01T00:00:00")
            @RequestParam(required = false) LocalDateTime startTime,
            @Parameter(description = "结束时间（如：2023-11-30T23:59:59）", example = "2023-11-30T23:59:59")
            @RequestParam(required = false) LocalDateTime endTime,
            @Parameter(description = "页码（默认1）", example = "1")
            @RequestParam(required = false) Integer pageNum,
            @Parameter(description = "每页条数（默认10）", example = "10")
            @RequestParam(required = false) Integer pageSize
    ) {
        try {
            PageResult<VisitRecordBackup> pageResult = visitRecordBackupService.getBackupsByTimeRange(startTime, endTime, pageNum, pageSize);
            return Result.success(pageResult);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常");
        }
    }


    // -------------------------- 新增：恢复带看记录 --------------------------

    /**
     * 从备份恢复带看记录到主表
     */
    @PostMapping("/restore/{visitRecordId}")
    @Operation(
            summary = "恢复带看记录",
            description = "将备份表中的带看记录恢复到主表（主表无此ID时可用）"
    )
    public Result<?> restoreVisitRecord(
            @Parameter(description = "原带看记录ID", required = true, example = "40001")
            @PathVariable Long visitRecordId,
            @Parameter(description = "执行恢复的用户ID", required = true, example = "1001")
            @RequestParam Long operatorId,
            @Parameter(description = "恢复后是否保留备份（默认true）", example = "true")
            @RequestParam(required = false, defaultValue = "true") boolean keepBackup
    ) {
        try {
            visitRecordBackupService.restoreVisitRecord(visitRecordId, operatorId, keepBackup);
            String msg = keepBackup ?
                    "带看记录恢复成功，备份已保留" :
                    "带看记录恢复成功，备份已删除";
            return Result.success(msg);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常");
        }
    }
}

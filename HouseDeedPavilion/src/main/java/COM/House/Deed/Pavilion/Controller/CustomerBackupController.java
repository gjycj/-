package COM.House.Deed.Pavilion.Controller;

import COM.House.Deed.Pavilion.Entity.CustomerBackup;
import COM.House.Deed.Pavilion.Service.CustomerBackupService;
import COM.House.Deed.Pavilion.Utils.PageResult;
import COM.House.Deed.Pavilion.Utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 客户备份记录完整控制器（含查询、恢复功能）
 */
@RestController
@RequestMapping("/customerBackup")
@Tag(name = "客户备份记录管理", description = "查询、恢复已删除客户的备份数据")
public class CustomerBackupController {

    @Resource
    private CustomerBackupService customerBackupService;


    // -------------------------- 原有查询接口（保留） --------------------------

    /**
     * 根据原客户ID查询备份记录
     */
    @GetMapping("/{customerId}")
    @Operation(summary = "根据客户ID查询备份", description = "查询已删除客户的历史数据")
    public Result<CustomerBackup> getBackupByCustomerId(
            @Parameter(description = "原客户ID", example = "30001")
            @PathVariable Long customerId
    ) {
        try {
            CustomerBackup backup = customerBackupService.getBackupByCustomerId(customerId);
            return Result.success(backup);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 分页查询所有客户备份记录
     */
    @GetMapping("/all")
    @Operation(summary = "分页查询所有客户备份", description = "按删除时间倒序排列")
    public Result<PageResult<CustomerBackup>> getAllBackups(
            @Parameter(description = "页码（默认1）", example = "1")
            @RequestParam(required = false) Integer pageNum,
            @Parameter(description = "每页条数（默认10）", example = "10")
            @RequestParam(required = false) Integer pageSize
    ) {
        try {
            PageResult<CustomerBackup> pageResult = customerBackupService.getAllCustomerBackups(pageNum, pageSize);
            return Result.success(pageResult);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 分页查询指定操作人删除的客户备份
     */
    @GetMapping("/byOperator/{operatorId}")
    @Operation(summary = "按操作人查询备份", description = "查询指定用户删除的客户备份记录")
    public Result<PageResult<CustomerBackup>> getBackupsByOperator(
            @Parameter(description = "操作人ID", required = true, example = "1001")
            @PathVariable Long operatorId,
            @Parameter(description = "页码（默认1）", example = "1")
            @RequestParam(required = false) Integer pageNum,
            @Parameter(description = "每页条数（默认10）", example = "10")
            @RequestParam(required = false) Integer pageSize
    ) {
        try {
            PageResult<CustomerBackup> pageResult = customerBackupService.getBackupsByOperator(operatorId, pageNum, pageSize);
            return Result.success(pageResult);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }


    // -------------------------- 新增：按时间范围查询备份 --------------------------

    /**
     * 按备份时间范围查询客户备份记录（查询某段时间内删除的客户）
     */
    @GetMapping("/byTimeRange")
    @Operation(
            summary = "按时间范围查询备份",
            description = "查询指定时间范围内删除的客户备份（startTime和endTime可选，格式：yyyy-MM-ddTHH:mm:ss）"
    )
    public Result<PageResult<CustomerBackup>> getBackupsByTimeRange(
            @Parameter(description = "备份开始时间（如：2023-11-01T00:00:00）", example = "2023-11-01T00:00:00")
            @RequestParam(required = false) LocalDateTime startTime,
            @Parameter(description = "备份结束时间（如：2023-11-30T23:59:59）", example = "2023-11-30T23:59:59")
            @RequestParam(required = false) LocalDateTime endTime,
            @Parameter(description = "页码（默认1）", example = "1")
            @RequestParam(required = false) Integer pageNum,
            @Parameter(description = "每页条数（默认10）", example = "10")
            @RequestParam(required = false) Integer pageSize
    ) {
        try {
            PageResult<CustomerBackup> pageResult = customerBackupService.getBackupsByTimeRange(startTime, endTime, pageNum, pageSize);
            return Result.success(pageResult);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }


    // -------------------------- 新增：从备份恢复客户数据 --------------------------

    /**
     * 从备份表恢复客户数据到主表
     */
    @PostMapping("/restore/{customerId}")
    @Operation(
            summary = "恢复客户数据",
            description = "将备份表中的客户数据恢复到主表（需确保主表中无该客户ID）"
    )
    public Result<?> restoreCustomer(
            @Parameter(description = "原客户ID（备份表ID）", required = true, example = "30001")
            @PathVariable Long customerId,
            @Parameter(description = "执行恢复操作的用户ID", required = true, example = "1001")
            @RequestParam Long operatorId,
            @Parameter(description = "恢复后是否保留备份（默认true）", example = "true")
            @RequestParam(required = false, defaultValue = "true") boolean keepBackup
    ) {
        try {
            customerBackupService.restoreCustomer(customerId, operatorId, keepBackup);
            String msg = keepBackup ?
                    "客户数据恢复成功，备份记录已保留" :
                    "客户数据恢复成功，备份记录已删除";
            return Result.success(msg);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }
}

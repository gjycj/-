package COM.House.Deed.Pavilion.Controller;

import COM.House.Deed.Pavilion.DTO.LandlordBackupQueryDTO;
import COM.House.Deed.Pavilion.Entity.LandlordBackup;
import COM.House.Deed.Pavilion.Service.LandlordBackupService;
import COM.House.Deed.Pavilion.Service.LandlordService;
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
 * 房东备份控制器
 */
@RestController
@RequestMapping("/landlordBackup")
@Tag(name = "房东备份管理", description = "房东备份的查询、恢复、删除接口")
public class LandlordBackupController {

    @Resource
    private LandlordBackupService backupService;

    /**
     * 按备份ID查询单个房东备份
     */
    @GetMapping("/{backupId}")
    @Operation(
            summary = "查询单个房东备份详情",
            description = "根据备份记录ID查询对应的房东备份数据，包含原房东信息及备份审计信息（如删除原因、备份时间）",
            parameters = {
                    @Parameter(
                            name = "backupId",
                            description = "备份记录唯一标识（landlord_backup表的id）",
                            required = true,
                            example = "4001",
                            schema = @Schema(type = "long")
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "查询成功，返回备份详情",
                            content = @Content(schema = @Schema(implementation = LandlordBackup.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "参数错误（如backupId为null或负数）",
                            content = @Content(schema = @Schema(implementation = Result.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "备份记录不存在（该ID的备份未找到）",
                            content = @Content(schema = @Schema(implementation = Result.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "系统错误（如数据库查询异常）",
                            content = @Content(schema = @Schema(implementation = Result.class))
                    )
            }
    )
    public Result<LandlordBackup> getBackup(
            @Parameter(description = "备份ID", required = true) @PathVariable Long backupId
    ) {
        try {
            LandlordBackup backup = backupService.getBackupById(backupId);
            return backup != null ? Result.success(backup) : Result.fail("备份不存在");
        } catch (Exception e) {
            return Result.fail("查询失败：" + e.getMessage());
        }
    }

    /**
     * 按原房东ID查询备份（分页）
     */
    @GetMapping("/listByOriginalId")
    @Operation(
            summary = "按原房东ID分页查询备份记录",
            description = "根据原房东的ID（landlord表的id）查询其所有备份记录，支持分页（默认页码1，页大小10）",
            parameters = {
                    @Parameter(
                            name = "originalId",
                            description = "原房东ID（对应landlord表的id，用于关联备份记录）",
                            required = true,
                            example = "3001",
                            schema = @Schema(type = "long")
                    ),
                    @Parameter(
                            name = "pageNum",
                            description = "页码（可选，默认1，最小1）",
                            required = false,
                            example = "1",
                            schema = @Schema(type = "integer", defaultValue = "1")
                    ),
                    @Parameter(
                            name = "pageSize",
                            description = "页大小（可选，默认10，最大100）",
                            required = false,
                            example = "10",
                            schema = @Schema(type = "integer", defaultValue = "10", maximum = "100")
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "查询成功，返回分页的备份列表",
                            content = @Content(schema = @Schema(implementation = PageResult.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "参数错误（如originalId为null或负数）",
                            content = @Content(schema = @Schema(implementation = Result.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "系统错误（如数据库查询异常）",
                            content = @Content(schema = @Schema(implementation = Result.class))
                    )
            }
    )
    public Result<PageResult<LandlordBackup>> listByOriginalId(
            @Parameter(description = "原房东ID", required = true) @RequestParam Long originalId,
            @Parameter(description = "页码") @RequestParam(required = false) Integer pageNum,
            @Parameter(description = "页大小") @RequestParam(required = false) Integer pageSize
    ) {
        try {
            PageResult<LandlordBackup> result = backupService.getByOriginalId(originalId, pageNum, pageSize);
            return Result.success(result);
        } catch (Exception e) {
            return Result.fail("查询失败：" + e.getMessage());
        }
    }

    /**
     * 多条件查询房东备份
     */
    @GetMapping("/list")
    @Operation(
            summary = "多条件分页查询房东备份",
            description = "支持按原房东ID、姓名模糊查询、备份时间范围、删除人等条件组合查询，分页参数默认页码1、页大小10",
            parameters = {
                    @Parameter(
                            name = "queryDTO",
                            description = "查询条件DTO：" +
                                    "originalId（原房东ID，可选）、" +
                                    "nameLike（姓名模糊查询，可选）、" +
                                    "phone（电话精确查询，可选）、" +
                                    "backupTimeStart（备份开始时间，可选，格式yyyy-MM-dd HH:mm:ss）、" +
                                    "backupTimeEnd（备份结束时间，可选）、" +
                                    "deletedBy（删除人ID，可选）、" +
                                    "pageNum（页码，可选）、" +
                                    "pageSize（页大小，可选）",
                            required = true,
                            schema = @Schema(implementation = LandlordBackupQueryDTO.class)
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "查询成功，返回符合条件的分页备份列表",
                            content = @Content(schema = @Schema(implementation = PageResult.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "参数错误（如时间格式不正确）",
                            content = @Content(schema = @Schema(implementation = Result.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "系统错误（如数据库查询异常）",
                            content = @Content(schema = @Schema(implementation = Result.class))
                    )
            }
    )
    public Result<PageResult<LandlordBackup>> listByCondition(LandlordBackupQueryDTO queryDTO) {
        try {
            PageResult<LandlordBackup> result = backupService.getByCondition(queryDTO);
            return Result.success(result);
        } catch (Exception e) {
            return Result.fail("查询失败：" + e.getMessage());
        }
    }

    /**
     * 从备份恢复房东
     */
    @PostMapping("/recover")
    @Operation(
            summary = "从备份恢复房东数据",
            description = "根据备份ID重建房东记录，恢复后生成新的房东ID（与原ID不同）；" +
                    "若keepBackup为true，备份记录保留；为false，备份记录会被删除",
            parameters = {
                    @Parameter(
                            name = "backupId",
                            description = "要恢复的备份记录ID（landlord_backup表的id）",
                            required = true,
                            example = "4001",
                            schema = @Schema(type = "long")
                    ),
                    @Parameter(
                            name = "operatorId",
                            description = "执行恢复操作的用户ID（记录到新房东的createdBy和updatedBy）",
                            required = true,
                            example = "1001",
                            schema = @Schema(type = "long")
                    ),
                    @Parameter(
                            name = "keepBackup",
                            description = "是否保留备份记录（默认true）",
                            required = false,
                            example = "true",
                            schema = @Schema(type = "boolean", defaultValue = "true")
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "恢复成功，返回新生成的房东ID",
                            content = @Content(schema = @Schema(implementation = Result.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "参数错误（如backupId或operatorId无效）",
                            content = @Content(schema = @Schema(implementation = Result.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "备份记录不存在（该ID的备份未找到）",
                            content = @Content(schema = @Schema(implementation = Result.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "恢复失败（如数据库异常）",
                            content = @Content(schema = @Schema(implementation = Result.class))
                    )
            }
    )
    public Result<String> recover(
            @Parameter(description = "备份ID", required = true) @RequestParam Long backupId,
            @Parameter(description = "操作人ID", required = true) @RequestParam Long operatorId,
            @Parameter(description = "是否保留备份") @RequestParam(defaultValue = "true") boolean keepBackup
    ) {
        try {
            Long landlordId = backupService.recoverFromBackup(backupId, operatorId, keepBackup);
            return Result.success("恢复成功，新房东ID：" + landlordId);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("恢复失败：" + e.getMessage());
        }
    }

}
package COM.House.Deed.Pavilion.Controller;

import COM.House.Deed.Pavilion.DTO.HouseLandlordBackupQueryDTO;
import COM.House.Deed.Pavilion.Entity.HouseLandlordBackup;
import COM.House.Deed.Pavilion.Service.HouseLandlordBackupService;
import COM.House.Deed.Pavilion.Utils.PageResult;
import COM.House.Deed.Pavilion.Utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 房源-房东关联备份控制器
 * 仅提供备份记录的查询接口（插入由系统自动触发）
 */
@RestController
@RequestMapping("/houseLandlordBackup")
@Tag(name = "房源-房东关联备份查询", description = "查询被删除的房源-房东关联关系备份记录")
public class HouseLandlordBackupController {

    @Resource
    private HouseLandlordBackupService backupService;

    /**
     * 根据备份ID查询备份记录详情
     * @param id 备份记录ID
     * @return 统一响应体（含备份详情）
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "根据ID查询备份记录",
            description = "通过备份记录ID查询被删除的房源-房东关联关系详情",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "备份记录ID无效",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或备份记录不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<HouseLandlordBackup> getBackupById(
            @Parameter(description = "备份记录ID", required = true, example = "5001")
            @PathVariable Long id
    ) {
        try {
            HouseLandlordBackup backup = backupService.getBackupById(id);
            return Result.success(backup);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 多条件分页查询备份记录
     * @param queryDTO 包含查询条件（房源ID、房东ID、时间范围等）和分页参数
     * @return 统一响应体（含分页的备份记录列表）
     */
    @GetMapping("/condition")
    @Operation(
            summary = "多条件查询备份记录",
            description = "支持按房源ID、房东ID、删除时间范围等条件查询被删除的关联关系备份",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功（无记录时返回空列表）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "分页参数不合法",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<PageResult<HouseLandlordBackup>> getBackupsByCondition(
            @Parameter(description = "查询条件（所有条件均为可选）", required = false)
            HouseLandlordBackupQueryDTO queryDTO
    ) {
        if (queryDTO == null) {
            queryDTO = new HouseLandlordBackupQueryDTO();
        }

        try {
            PageResult<HouseLandlordBackup> pageResult = backupService.getBackupsByCondition(queryDTO);
            return Result.success(pageResult);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }
}

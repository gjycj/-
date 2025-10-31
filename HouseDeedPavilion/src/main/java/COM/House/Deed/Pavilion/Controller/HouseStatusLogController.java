package COM.House.Deed.Pavilion.Controller;

import COM.House.Deed.Pavilion.DTO.HouseStatusLogQueryDTO;
import COM.House.Deed.Pavilion.Entity.HouseStatusLog;
import COM.House.Deed.Pavilion.Service.HouseStatusLogService;
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

import java.util.List;

/**
 * 房源状态变更记录控制器
 * 提供房源状态变更历史的查询接口
 */
@RestController
@RequestMapping("/houseStatusLog")
@Tag(name = "房源状态变更记录", description = "查询房源状态变更历史记录的接口")
public class HouseStatusLogController {

    @Resource
    private HouseStatusLogService statusLogService;

    /**
     * 根据记录ID查询状态变更详情
     * @param id 状态变更记录ID
     * @return 统一响应体（含记录详情）
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "根据ID查询状态变更记录",
            description = "通过记录ID查询房源状态变更的详细信息",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "记录ID无效",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或记录不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<HouseStatusLog> getStatusLogById(
            @Parameter(description = "状态变更记录ID", required = true, example = "6001")
            @PathVariable Long id
    ) {
        try {
            HouseStatusLog log = statusLogService.getStatusLogById(id);
            return Result.success(log);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 根据房源ID查询所有状态变更记录
     * @param houseId 房源ID
     * @return 统一响应体（含状态变更记录列表）
     */
    @GetMapping("/house/{houseId}")
    @Operation(
            summary = "查询房源的所有状态变更记录",
            description = "根据房源ID获取该房源的完整状态变更历史（按时间倒序）",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功（无变更时返回空列表）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "房源ID无效",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或房源不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<List<HouseStatusLog>> getStatusLogsByHouseId(
            @Parameter(description = "房源ID", required = true, example = "20001")
            @PathVariable Long houseId
    ) {
        try {
            List<HouseStatusLog> logs = statusLogService.getStatusLogsByHouseId(houseId);
            return Result.success(logs);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 多条件分页查询状态变更记录
     * @param queryDTO 包含查询条件（房源ID、状态值、时间范围等）和分页参数
     * @return 统一响应体（含分页的状态变更记录列表）
     */
    @GetMapping("/condition")
    @Operation(
            summary = "多条件查询状态变更记录",
            description = "支持按房源ID、状态值、操作人、时间范围等条件查询状态变更记录",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功（无记录时返回空列表）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "分页参数不合法",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<PageResult<HouseStatusLog>> getStatusLogsByCondition(
            @Parameter(description = "查询条件（所有条件均为可选）", required = false)
            HouseStatusLogQueryDTO queryDTO
    ) {
        if (queryDTO == null) {
            queryDTO = new HouseStatusLogQueryDTO();
        }

        try {
            PageResult<HouseStatusLog> pageResult = statusLogService.getStatusLogsByCondition(queryDTO);
            return Result.success(pageResult);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }
}

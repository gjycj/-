package COM.House.Deed.Pavilion.Controller;

import COM.House.Deed.Pavilion.DTO.BuildingQueryDTO;
import COM.House.Deed.Pavilion.Entity.Building;
import COM.House.Deed.Pavilion.Service.BuildingService;
import COM.House.Deed.Pavilion.Utils.PageResult;
import COM.House.Deed.Pavilion.Utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * 楼栋信息控制器
 */
@RestController("/building")
@Tag(name = "楼栋管理", description = "楼栋的新增、查询、更新等接口")
public class BuildingController {

    @Resource
    private BuildingService buildingService;

    /**
     * 根据ID更新楼栋信息
     * @param building 包含ID和待更新字段的楼栋信息（ID必填）
     * @param bindingResult 参数校验结果
     * @return 统一响应体
     */
    @PutMapping("/buildings")
    @Operation(
            summary = "根据ID更新楼栋信息",
            description = "支持部分字段更新（ID为必填），若更新所属楼盘需确保新楼盘存在",
            responses = {
                    @ApiResponse(responseCode = "200", description = "更新成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "参数错误（如ID为空或楼盘ID无效）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或楼栋/楼盘不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<Void> updateBuilding(
            @Parameter(description = "楼栋信息（ID必填，其他字段选填）", required = true)
            @Valid @RequestBody Building building,
            BindingResult bindingResult
    ) {
        // 1. 参数校验
        if (bindingResult.hasErrors()) {
            String errorMsg = Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage();
            return Result.paramError(errorMsg);
        }

        try {
            buildingService.updateBuildingById(building);
            return Result.success(); // 无数据返回，仅返回成功状态
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 多条件分页查询楼栋
     * @param queryDTO 包含查询条件（可选）和分页参数
     * @return 统一响应体（含分页的楼栋列表）
     */
    @GetMapping("/buildings/condition")
    @Operation(
            summary = "多条件分页查询楼栋",
            description = "支持按楼盘ID、楼栋名称模糊查询、总层数范围等条件组合，默认分页",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功（无符合条件时返回空列表）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "参数错误（如楼盘ID为负数）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或楼盘不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<PageResult<Building>> getBuildingsByCondition(
            @Parameter(description = "查询条件（所有条件均为可选）", required = false)
            BuildingQueryDTO queryDTO
    ) {
        // 避免queryDTO为null（若前端未传递任何参数）
        if (queryDTO == null) {
            queryDTO = new BuildingQueryDTO();
        }

        try {
            PageResult<Building> pageResult = buildingService.getBuildingsByCondition(queryDTO);
            return Result.success(pageResult);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 新增楼栋接口
     * @param building 楼栋信息（包含所属楼盘ID、名称等必填字段）
     * @param bindingResult 参数校验结果
     * @return 统一响应体
     */
    @PostMapping("/addBuilding")
    @Operation(
            summary = "新增楼栋",
            description = "添加新的楼栋信息，需关联已存在的楼盘（必填楼盘ID和楼栋名称）",
            responses = {
                    @ApiResponse(responseCode = "200", description = "新增成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "参数错误（如楼盘ID或名称为空）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或所属楼盘不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<Long> addBuilding(
            @Parameter(description = "楼栋信息（楼盘ID和名称为必填）", required = true)
            @Valid @RequestBody Building building,
            BindingResult bindingResult
    ) {
        // 1. 参数校验
        if (bindingResult.hasErrors()) {
            String errorMsg = Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage();
            return Result.paramError(errorMsg);
        }

        try {
            // 2. 调用Service新增楼栋
            Long buildingId = buildingService.addBuilding(building);
            // 3. 返回成功结果
            return Result.success(buildingId);
        } catch (RuntimeException e) {
            // 4. 捕获业务异常（如楼盘不存在）
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            // 5. 捕获系统异常
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 根据ID查询楼栋详情
     * @param id 楼栋ID
     * @return 统一响应体（含楼栋完整信息）
     */
    @GetMapping("/buildings/{id}")
    @Operation(
            summary = "根据ID查询楼栋详情",
            description = "通过楼栋ID获取完整信息，包括所属楼盘ID、名称、层数等",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "ID无效（如为null或负数）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或楼栋不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )


    public Result<Building> getBuildingById(
            @Parameter(description = "楼栋ID", required = true, example = "1")
            @PathVariable Long id
    ) {
        try {
            Building building = buildingService.getBuildingById(id);
            return Result.success(building);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }
    /**
     * 根据楼盘ID分页查询楼栋列表
     * @param propertyId 所属楼盘ID（必填）
     * @param pageNum 页码（默认1）
     * @param pageSize 每页条数（默认10）
     * @return 统一响应体（含分页的楼栋列表）
     */
    @GetMapping("/buildings/property/{propertyId}")
    @Operation(
            summary = "根据楼盘ID查询楼栋列表（分页）",
            description = "查询指定楼盘下的所有楼栋，支持分页（默认每页10条）",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功（无楼栋时返回空列表）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "楼盘ID无效（如为null或负数）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或楼盘不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<PageResult<Building>> getBuildingsByPropertyId(
            @Parameter(description = "所属楼盘ID", required = true, example = "1")
            @PathVariable Long propertyId,

            @Parameter(description = "页码（默认1）", example = "1", required = false)
            @RequestParam(defaultValue = "1") Integer pageNum,

            @Parameter(description = "每页条数（默认10）", example = "10", required = false)
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        try {
            PageResult<Building> pageResult = buildingService.getBuildingsByPropertyId(propertyId, pageNum, pageSize);
            return Result.success(pageResult);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 根据ID删除楼栋（删除前自动创建备份）
     * @param id 楼栋ID（待删除）
     * @param operatorId 操作人ID（当前登录用户）
     * @param deleteReason 删除原因（必填，用于备份记录）
     * @return 统一响应体（含备份ID，便于后续恢复）
     */
    @DeleteMapping("/buildings/{id}")
    @Operation(
            summary = "删除楼栋（自动备份）",
            description = "删除前会自动创建楼栋备份，支持后续恢复；需提供删除原因用于审计",
            responses = {
                    @ApiResponse(responseCode = "200", description = "删除成功（已创建备份）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "参数错误（如ID为空、删除原因未填写）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或楼栋不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<String> deleteBuilding(
            @Parameter(description = "楼栋ID（待删除）", required = true, example = "2001")
            @PathVariable Long id,

            @Parameter(description = "操作人ID（当前登录用户）", required = true, example = "1001")
            @RequestParam Long operatorId,

            @Parameter(description = "删除原因（必填，如“信息错误”“重复录入”）", required = true, example = "楼栋信息错误，需重新创建")
            @RequestParam String deleteReason
    ) {
        // 1. 参数校验（基础非空校验）
        if (id == null || id <= 0) {
            return Result.paramError("楼栋ID无效（不能为空或负数）");
        }
        if (operatorId == null || operatorId <= 0) {
            return Result.paramError("操作人ID无效");
        }
        if (deleteReason == null || deleteReason.trim().isEmpty()) {
            return Result.paramError("删除原因不能为空");
        }

        try {
            // 2. 调用Service删除（内部会先创建备份）
            Long backupId = buildingService.deleteBuildingWithBackup(id, operatorId, deleteReason);
            return Result.success("楼栋删除成功，已创建备份（备份ID：" + backupId + "，可用于恢复）"
            );
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("删除楼栋失败：系统异常，请联系管理员");
        }
    }

}


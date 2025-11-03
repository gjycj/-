package COM.House.Deed.Pavilion.Controller;

import COM.House.Deed.Pavilion.Constant.MessageConstant;
import COM.House.Deed.Pavilion.DTO.LandlordQueryDTO;
import COM.House.Deed.Pavilion.Entity.Landlord;
import COM.House.Deed.Pavilion.Exception.BusinessException;
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
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * 房东信息控制器
 */
@RestController
@RequestMapping("/landlord")
@Tag(name = "房东管理", description = "房东的新增、查询、更新等接口")
public class LandlordController {

    @Resource
    private LandlordService landlordService;

    /**
     * 新增房东接口
     * @param landlord 房东信息（包含姓名、电话等必填字段）
     * @param bindingResult 参数校验结果
     * @return 统一响应体（含新增房东ID）
     */
    @PostMapping("/addLandlord")
    @Operation(
            summary = "新增房东",
            description = "添加新的房东信息，需填写姓名、联系电话等必填字段",
            responses = {
                    @ApiResponse(responseCode = "200", description = "新增成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "参数错误（如姓名或电话为空）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或电话已被占用",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<Long> addLandlord(
            @Parameter(description = "房东信息（姓名、电话为必填）", required = true)
            @Valid @RequestBody Landlord landlord,
            BindingResult bindingResult
    ) {
        // 1. 参数校验
        if (bindingResult.hasErrors()) {
            String errorMsg = Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage();
            return Result.paramError(errorMsg);
        }

        try {
            // 2. 调用Service新增房东
            Long landlordId = landlordService.addLandlord(landlord);
            // 3. 返回成功结果
            return Result.success(landlordId);
        } catch (RuntimeException e) {
            // 4. 捕获业务异常（如电话重复）
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            // 5. 捕获系统异常
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 根据ID查询房东详情
     * @param id 房东ID
     * @return 统一响应体（含房东完整信息）
     */
    @GetMapping("/landlords/{id}")
    @Operation(
            summary = "根据ID查询房东详情",
            description = "通过房东ID获取完整信息，包括姓名、电话、身份证号等",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "ID无效（如为null或负数）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或房东不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<Landlord> getLandlordById(
            @Parameter(description = "房东ID", required = true, example = "3001")
            @PathVariable Long id
    ) {
        try {
            // 调用Service查询房东
            Landlord landlord = landlordService.getLandlordById(id);
            return Result.success(landlord);
        } catch (RuntimeException e) {
            // 捕获业务异常（如ID无效、房东不存在）
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            // 捕获系统异常
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 根据ID更新房东信息
     * @param landlord 包含ID和待更新字段的房东信息（ID必填）
     * @param bindingResult 参数校验结果
     * @return 统一响应体
     */
    @PutMapping("/landlords")
    @Operation(
            summary = "根据ID更新房东信息",
            description = "支持部分字段更新（ID为必填），若更新电话需确保唯一性",
            responses = {
                    @ApiResponse(responseCode = "200", description = "更新成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "参数错误（如ID为空或电话格式错误）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或房东不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<Void> updateLandlord(
            @Parameter(description = "房东信息（ID必填，其他字段选填）", required = true)
            @Valid @RequestBody Landlord landlord,
            BindingResult bindingResult
    ) {
        // 1. 参数校验
        if (bindingResult.hasErrors()) {
            String errorMsg = Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage();
            return Result.paramError(errorMsg);
        }

        try {
            // 2. 调用Service更新房东
            landlordService.updateLandlordById(landlord);
            return Result.success(); // 无数据返回，仅返回成功状态
        } catch (RuntimeException e) {
            // 3. 捕获业务异常（如房东不存在、电话重复）
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            // 4. 捕获系统异常
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 多条件分页查询房东
     * @param queryDTO 包含查询条件（可选）和分页参数
     * @return 统一响应体（含分页的房东列表）
     */
    @GetMapping("/landlords/condition")
    @Operation(
            summary = "多条件分页查询房东",
            description = "支持按姓名、电话、身份证号、性别等条件组合查询，默认分页",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功（无符合条件时返回空列表）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "参数错误（如分页参数不合法）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<PageResult<Landlord>> getLandlordsByCondition(
            @Parameter(description = "查询条件（所有条件均为可选）", required = false)
            LandlordQueryDTO queryDTO
    ) {
        // 避免queryDTO为null（若前端未传递任何参数）
        if (queryDTO == null) {
            queryDTO = new LandlordQueryDTO();
        }

        try {
            // 调用Service多条件查询房东
            PageResult<Landlord> pageResult = landlordService.getLandlordsByCondition(queryDTO);
            return Result.success(pageResult);
        } catch (RuntimeException e) {
            // 捕获业务异常（如参数范围不合法）
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            // 捕获系统异常
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 根据姓名模糊查询房东列表（分页）
     * @param name 房东姓名（模糊匹配，可为空查询全部）
     * @param pageNum 页码（默认1）
     * @param pageSize 每页条数（默认10）
     * @return 统一响应体（含分页的房东列表）
     */
    @GetMapping("/landlords/name")
    @Operation(
            summary = "根据姓名查询房东列表（分页）",
            description = "模糊匹配房东姓名，支持分页（默认每页10条），姓名为空时查询全部",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功（无房东时返回空列表）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "分页参数不合法",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<PageResult<Landlord>> getLandlordsByName(
            @Parameter(description = "房东姓名（模糊匹配，可选）", example = "李")
            @RequestParam(required = false) String name,

            @Parameter(description = "页码（默认1）", example = "1", required = false)
            @RequestParam(defaultValue = "1") Integer pageNum,

            @Parameter(description = "每页条数（默认10）", example = "10", required = false)
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        try {
            // 调用Service查询姓名匹配的房东
            PageResult<Landlord> pageResult = landlordService.getLandlordsByName(name, pageNum, pageSize);
            return Result.success(pageResult);
        } catch (RuntimeException e) {
            // 捕获业务异常（如分页参数无效）
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            // 捕获系统异常
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 删除房东（自动备份，含关联房源校验）
     * 流程：校验参数 → 调用Service删除（内部完成备份+关联校验）→ 返回结果
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "删除房东信息",
            description = "删除前会自动备份数据到landlord_backup表；若房东关联房源，会阻止删除并提示关联数量",
            parameters = {
                    @Parameter(name = "id", description = "房东ID", required = true, example = "3001"),
                    @Parameter(name = "operatorId", description = "操作人ID（记录删除人）", required = true, example = "1001"),
                    @Parameter(name = "deleteReason", description = "删除原因（可选，记录到备份表）", example = "房东信息错误")
            }
    )
    public Result<String> deleteLandlord(
            @PathVariable Long id,
            @RequestParam Long operatorId,
            @RequestParam(required = false) String deleteReason
    ) {
        try {
            // 1. 基础参数校验（Controller层前置校验）
            if (id == null || id <= 0) {
                return Result.paramError(MessageConstant.LANDLORD_ID_INVALID);
            }
            if (operatorId == null || operatorId <= 0) {
                return Result.paramError("操作人ID无效（必须为正整数）");
            }

            // 2. 调用Service执行删除（含备份和关联校验）
            landlordService.deleteLandlordById(id, operatorId, deleteReason);

            // 3. 返回成功结果
            return Result.success(MessageConstant.DELETE_LANDLORD_SUCCESS);

        } catch (BusinessException e) {
            // 捕获业务异常（如关联房源、备份失败等）
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            // 捕获系统异常（如数据库错误）
            return Result.fail("删除失败：系统异常，请联系管理员");
        }
    }

}

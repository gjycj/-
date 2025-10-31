package COM.House.Deed.Pavilion.Controller;

import COM.House.Deed.Pavilion.DTO.HouseLandlordQueryDTO;
import COM.House.Deed.Pavilion.Entity.HouseLandlord;
import COM.House.Deed.Pavilion.Entity.Landlord;
import COM.House.Deed.Pavilion.Service.HouseLandlordService;
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

import java.util.List;
import java.util.Objects;

/**
 * 房源-房东关联关系控制器
 * 处理房源与房东的多对多关联管理
 */
@RestController
@RequestMapping("/houseLandlord")
@Tag(name = "房源-房东关联管理", description = "房源与房东的关联关系新增、删除、查询等接口")
public class HouseLandlordController {

    @Resource
    private HouseLandlordService houseLandlordService;

    /**
     * 新增房源-房东关联关系
     * @param houseLandlord 关联信息（含房源ID、房东ID、是否主要房东等必填字段）
     * @param bindingResult 参数校验结果
     * @return 统一响应体（含新增关联记录ID）
     */
    @PostMapping("/addHouseLandlord")
    @Operation(
            summary = "新增房源-房东关联",
            description = "建立房源与房东的关联关系，需指定房源ID、房东ID和是否主要房东（1-是，0-否）",
            responses = {
                    @ApiResponse(responseCode = "200", description = "新增成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "参数错误（如房源ID为空或isMain不是0/1）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或关联关系已存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<Long> addHouseLandlord(
            @Parameter(description = "关联信息（房源ID、房东ID、isMain为必填）", required = true)
            @Valid @RequestBody HouseLandlord houseLandlord,
            BindingResult bindingResult
    ) {
        // 参数校验
        if (bindingResult.hasErrors()) {
            String errorMsg = Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage();
            return Result.paramError(errorMsg);
        }

        try {
            Long relationId = houseLandlordService.addHouseLandlord(houseLandlord);
            return Result.success(relationId);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 根据关联记录ID删除关联关系
     * @param id 关联记录ID
     * @return 统一响应体
     */
    @DeleteMapping("/delete/{id}")
    @Operation(
            summary = "根据ID删除关联关系",
            description = "通过关联记录ID解除房源与房东的关联",
            responses = {
                    @ApiResponse(responseCode = "200", description = "删除成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "关联记录ID无效",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或关联记录不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<Void> deleteById(
            @Parameter(description = "关联记录ID", required = true, example = "4001")
            @PathVariable Long id
    ) {
        try {
            houseLandlordService.deleteById(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 根据房源ID和房东ID删除关联关系
     * @param houseId 房源ID
     * @param landlordId 房东ID
     * @return 统一响应体
     */
    @DeleteMapping("/delete")
    @Operation(
            summary = "根据房源和房东ID删除关联",
            description = "通过房源ID和房东ID的组合解除特定关联关系",
            responses = {
                    @ApiResponse(responseCode = "200", description = "删除成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "房源ID或房东ID无效",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或关联关系不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<Void> deleteByHouseAndLandlord(
            @Parameter(description = "房源ID", required = true, example = "20001")
            @RequestParam Long houseId,
            @Parameter(description = "房东ID", required = true, example = "3001")
            @RequestParam Long landlordId
    ) {
        try {
            houseLandlordService.deleteByHouseAndLandlord(houseId, landlordId);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 根据房源ID查询关联的房东列表（含详情）
     * @param houseId 房源ID
     * @return 统一响应体（含房东详情列表）
     */
    @GetMapping("/landlords/house/{houseId}")
    @Operation(
            summary = "查询房源关联的房东详情",
            description = "根据房源ID获取所有关联的房东完整信息（按是否主要房东排序）",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功（无关联时返回空列表）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "房源ID无效",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或房源不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<List<Landlord>> getLandlordsByHouseId(
            @Parameter(description = "房源ID", required = true, example = "20001")
            @PathVariable Long houseId
    ) {
        try {
            List<Landlord> landlords = houseLandlordService.getLandlordsByHouseId(houseId);
            return Result.success(landlords);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 根据房源ID查询关联的房东ID列表（仅返回ID）
     * @param houseId 房源ID
     * @return 统一响应体（含房东ID列表）
     */
    @GetMapping("/landlordIds/house/{houseId}")
    @Operation(
            summary = "查询房源关联的房东ID列表",
            description = "根据房源ID获取所有关联的房东ID（适用于仅需ID的场景）",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功（无关联时返回空列表）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "房源ID无效",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或房源不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<List<Long>> getLandlordIdsByHouseId(
            @Parameter(description = "房源ID", required = true, example = "20001")
            @PathVariable Long houseId
    ) {
        try {
            List<Long> landlordIds = houseLandlordService.getLandlordIdsByHouseId(houseId);
            return Result.success(landlordIds);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 多条件分页查询关联关系
     * @param queryDTO 包含查询条件（房源ID、房东ID、是否主要房东等）和分页参数
     * @return 统一响应体（含分页的关联记录列表）
     */
    @GetMapping("/condition")
    @Operation(
            summary = "多条件分页查询关联关系",
            description = "支持按房源ID、房东ID、是否主要房东等条件组合查询关联记录，默认分页",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功（无记录时返回空列表）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "分页参数不合法",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<PageResult<HouseLandlord>> getHouseLandlordsByCondition(
            @Parameter(description = "查询条件（所有条件均为可选）", required = false)
            HouseLandlordQueryDTO queryDTO
    ) {
        if (queryDTO == null) {
            queryDTO = new HouseLandlordQueryDTO();
        }

        try {
            PageResult<HouseLandlord> pageResult = houseLandlordService.getHouseLandlordsByCondition(queryDTO);
            return Result.success(pageResult);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }
}

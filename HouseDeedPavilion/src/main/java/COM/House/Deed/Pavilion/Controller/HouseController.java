package COM.House.Deed.Pavilion.Controller;

import COM.House.Deed.Pavilion.DTO.HouseQueryDTO;
import COM.House.Deed.Pavilion.DTO.HouseUpdateDTO;
import COM.House.Deed.Pavilion.Entity.House;
import COM.House.Deed.Pavilion.Service.HouseService;
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
 * 房源信息控制器
 */
@RestController("/house")
@Tag(name = "房源管理", description = "房源的新增、查询、更新等接口")
public class HouseController {

    @Resource
    private HouseService houseService;

    /**
     * 删除房源（自动备份）
     */
    @PostMapping("/delete/{id}")
    @Operation(
            summary = "删除房源并自动备份",
            description = "删除前会备份到house_backup表，需传递操作人ID和删除原因"
    )
    public Result<?> deleteHouse(
            @Parameter(description = "房源ID", required = true, example = "20001")
            @PathVariable Long id,
            @Parameter(description = "操作人ID", required = true, example = "1001")
            @RequestParam Long operatorId,
            @Parameter(description = "删除原因（可选）", example = "房源已出售")
            @RequestParam(required = false) String deleteReason
    ) {
        try {
            houseService.deleteHouse(id, operatorId, deleteReason);
            return Result.success("房源删除成功（已自动备份）");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常");
        }
    }

    /**
     * 新增房源接口
     * @param house 房源信息（包含所属楼栋ID、门牌号等必填字段）
     * @param bindingResult 参数校验结果
     * @return 统一响应体（含新增房源ID）
     */
    @PostMapping("/addHouse")
    @Operation(
            summary = "新增房源",
            description = "添加新的房源信息，需关联已存在的楼栋（必填楼栋ID、门牌号、户型等信息）",
            responses = {
                    @ApiResponse(responseCode = "200", description = "新增成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "参数错误（如楼栋ID或门牌号为空）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或所属楼栋/经纪人不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<Long> addHouse(
            @Parameter(description = "房源信息（楼栋ID、门牌号、户型为必填）", required = true)
            @Valid @RequestBody House house,
            BindingResult bindingResult
    ) {
        // 1. 参数校验
        if (bindingResult.hasErrors()) {
            String errorMsg = Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage();
            return Result.paramError(errorMsg);
        }

        try {
            // 2. 调用Service新增房源
            Long houseId = houseService.addHouse(house);
            // 3. 返回成功结果
            return Result.success(houseId);
        } catch (RuntimeException e) {
            // 4. 捕获业务异常（如楼栋不存在、门牌号重复）
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            // 5. 捕获系统异常
            return Result.fail("系统异常，请联系管理员");
        }
    }


    /**
     * 根据ID查询房源详情
     * @param id 房源ID
     * @return 统一响应体（含房源完整信息）
     */
    @GetMapping("/houses/{id}")
    @Operation(
            summary = "根据ID查询房源详情",
            description = "通过房源ID获取完整信息，包括所属楼栋ID、门牌号、户型、价格等",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "ID无效（如为null或负数）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或房源不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<House> getHouseById(
            @Parameter(description = "房源ID", required = true, example = "3001")
            @PathVariable Long id
    ) {
        try {
            // 调用Service查询房源
            House house = houseService.getHouseById(id);
            return Result.success(house);
        } catch (RuntimeException e) {
            // 捕获业务异常（如ID无效、房源不存在）
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            // 捕获系统异常
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 根据ID更新房源信息
     * @param updateDTO 包含ID和待更新字、房源状态变更记录的房源信息（ID必填）
     * @param bindingResult 参数校验结果
     * @return 统一响应体
     */
    @PutMapping("/updateHouses")
    @Operation(
            summary = "根据ID更新房源信息",
            description = "支持部分字段更新（ID为必填），若更新所属楼栋需确保楼栋存在，门牌号更新需保证唯一性",
            responses = {
                    @ApiResponse(responseCode = "200", description = "更新成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "参数错误（如ID为空或楼栋ID无效）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或房源/楼栋不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<Void> updateHouse(
            @Parameter(description = "房源信息（ID必填，其他字段选填）", required = true)
            @Valid @RequestBody HouseUpdateDTO updateDTO,
            BindingResult bindingResult
    ) {
        // 1. 参数校验
        if (bindingResult.hasErrors()) {
            String errorMsg = Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage();
            return Result.paramError(errorMsg);
        }

        try {
            // 2. 调用Service更新房源
            houseService.updateHouseById(updateDTO);
            return Result.success(); // 无数据返回，仅返回成功状态
        } catch (RuntimeException e) {
            // 3. 捕获业务异常（如房源不存在、门牌号重复）
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            // 4. 捕获系统异常
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 根据楼栋ID分页查询房源列表
     * @param buildingId 所属楼栋ID（必填）
     * @param pageNum 页码（默认1）
     * @param pageSize 每页条数（默认10）
     * @return 统一响应体（含分页的房源列表）
     */
    @GetMapping("/houses/building/{buildingId}")
    @Operation(
            summary = "根据楼栋ID查询房源列表（分页）",
            description = "查询指定楼栋下的所有房源，支持分页（默认每页10条），按门牌号升序排列",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功（无房源时返回空列表）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "楼栋ID无效（如为null或负数）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或楼栋不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<PageResult<House>> getHousesByBuildingId(
            @Parameter(description = "所属楼栋ID", required = true, example = "1001")
            @PathVariable Long buildingId,

            @Parameter(description = "页码（默认1）", example = "1", required = false)
            @RequestParam(defaultValue = "1") Integer pageNum,

            @Parameter(description = "每页条数（默认10）", example = "10", required = false)
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        try {
            // 调用Service查询楼栋下的房源
            PageResult<House> pageResult = houseService.getHousesByBuildingId(buildingId, pageNum, pageSize);
            return Result.success(pageResult);
        } catch (RuntimeException e) {
            // 捕获业务异常（如楼栋不存在、参数无效）
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            // 捕获系统异常
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 多条件分页查询房源
     * @param queryDTO 包含查询条件（可选）和分页参数
     * @return 统一响应体（含分页的房源列表）
     */
    @GetMapping("/houses/condition")
    @Operation(
            summary = "多条件分页查询房源",
            description = "支持按楼栋ID、门牌号模糊查询、户型、价格范围、楼层范围等条件组合，默认分页",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功（无符合条件时返回空列表）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "参数错误（如价格/楼层范围不合法）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或关联数据不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<PageResult<House>> getHousesByCondition(
            @Parameter(description = "查询条件（所有条件均为可选）", required = false)
            HouseQueryDTO queryDTO
    ) {
        // 避免queryDTO为null（若前端未传递任何参数）
        if (queryDTO == null) {
            queryDTO = new HouseQueryDTO();
        }

        try {
            // 调用Service多条件查询房源
            PageResult<House> pageResult = houseService.getHousesByCondition(queryDTO);
            return Result.success(pageResult);
        } catch (RuntimeException e) {
            // 捕获业务异常（如参数范围不合法）
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            // 捕获系统异常
            return Result.fail("系统异常，请联系管理员");
        }
    }

}

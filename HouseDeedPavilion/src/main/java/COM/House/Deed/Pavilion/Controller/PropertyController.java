package COM.House.Deed.Pavilion.Controller;

import COM.House.Deed.Pavilion.DTO.PropertyQueryDTO;
import COM.House.Deed.Pavilion.Entity.Property;
import COM.House.Deed.Pavilion.Service.PropertyService;
import COM.House.Deed.Pavilion.Utils.PageResult;
import COM.House.Deed.Pavilion.Utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController("/property")
@Tag(name = "楼盘管理", description = "楼盘的新增、查询、更新等接口")
public class PropertyController {

    @Resource
    private PropertyService propertyService;

    @GetMapping("/properties/condition")
    @Operation(
            summary = "根据多条件查询楼盘",
            description = "支持名称模糊查询、物业类型精确匹配、建成年代范围等条件组合，默认分页",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<PageResult<Property>> getPropertyByCondition(
            @Parameter(description = "查询条件和分页参数（所有条件均为可选）")
            PropertyQueryDTO queryDTO
    ) {
        try {
            PageResult<Property> pageResult = propertyService.getPropertyByCondition(queryDTO);
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.fail("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/properties/{id}")
    @Operation(
            summary = "根据ID查询楼盘详情",
            description = "通过楼盘ID获取完整信息（包括名称、地址、物业类型等）",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "ID无效（如为null或负数）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或楼盘不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<Property> getPropertyById(
            @Parameter(description = "楼盘ID", required = true, example = "1")
            @PathVariable Long id
    ) {
        try {
            Property property = propertyService.getPropertyById(id);
            return Result.success(property);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 新增楼盘接口
     *
     * @param property      楼盘信息（包含必填字段）
     * @param bindingResult 参数校验结果
     * @return 统一响应体
     */
    @PostMapping("/addProperty")
    @Operation(
            summary = "新增楼盘",
            description = "添加新的楼盘信息，需传入必填字段（名称、地址、物业类型等）",
            responses = {
                    @ApiResponse(responseCode = "200", description = "新增成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "参数错误",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<Long> addProperty(
            @Parameter(description = "楼盘信息（包含名称、地址等必填字段）", required = true)
            @Valid @RequestBody Property property, // @Valid触发参数校验
            BindingResult bindingResult
    ) {
        // 1. 校验参数是否合法
        if (bindingResult.hasErrors()) {
            // 获取第一个错误信息返回
            String errorMsg = Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage();
            return Result.paramError(errorMsg);
        }

        try {
            // 2. 调用Service新增楼盘
            Long propertyId = propertyService.addProperty(property);
            // 3. 返回成功结果（包含新增的楼盘ID）
            return Result.success(propertyId);
        } catch (RuntimeException e) {
            // 4. 捕获业务异常，返回失败信息
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            // 5. 捕获其他异常（如数据库异常）
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 分页查询楼盘列表
     *
     * @param pageNum      页码（默认1）
     * @param pageSize     每页条数（默认10）
     * @param propertyType 物业类型（可选）
     * @return 分页结果
     */
    @GetMapping("/properties")
    @Operation(
            summary = "分页查询楼盘",
            description = "支持按物业类型筛选，默认查询第1页，每页10条数据",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<PageResult<Property>> getProperties(
            @Parameter(description = "页码（默认1）", example = "1", required = false)
            @RequestParam(defaultValue = "1") Integer pageNum,

            @Parameter(description = "每页条数（默认10）", example = "10", required = false)
            @RequestParam(defaultValue = "10") Integer pageSize,

            @Parameter(description = "物业类型（可选，如：住宅/商业，不填则查询全部）", example = "住宅", required = false)
            @RequestParam(required = false) String propertyType
    ) {
        try {
            PageResult<Property> pageResult = propertyService.getPropertyByPage(pageNum, pageSize, propertyType);
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.fail("查询失败：" + e.getMessage());
        }
    }

    @PutMapping("/updateProperty")
    @Operation(
            summary = "更新楼盘信息",
            description = "根据ID更新楼盘信息，只需传递需要修改的字段（ID为必填）",
            responses = {
                    @ApiResponse(responseCode = "200", description = "更新成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "参数错误（如ID为空）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或楼盘不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<Void> updateProperty(
            @Parameter(description = "楼盘信息（ID必填，其他字段选填）", required = true)
            @Valid @RequestBody Property property,
            BindingResult bindingResult
    ) {
        // 参数校验
        if (bindingResult.hasErrors()) {
            String errorMsg = Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage();
            return Result.paramError(errorMsg);
        }

        try {
            propertyService.updateProperty(property);
            return Result.success(); // 无数据返回，仅返回成功状态
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 删除楼盘（删除前自动创建备份）
     * 注意：若楼盘下有关联楼栋，需先删除楼栋（或配置级联删除）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除楼盘（自动备份）", description = "删除前自动创建备份，支持后续恢复；需先删除关联楼栋")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功（已备份）"),
            @ApiResponse(responseCode = "400", description = "参数错误或存在关联楼栋"),
            @ApiResponse(responseCode = "500", description = "系统异常")
    })
    public Result<String> deleteProperty(
            @Parameter(description = "楼盘ID（待删除）", required = true, example = "101")
            @PathVariable Long id,
            @Parameter(description = "操作人ID", required = true, example = "1001")
            @RequestParam Long operatorId,
            @Parameter(description = "删除原因（必填）", required = true, example = "楼盘信息错误")
            @RequestParam String deleteReason
    ) {
        // 参数校验
        if (id == null || id <= 0) {
            return Result.paramError("楼盘ID无效");
        }
        if (operatorId == null || operatorId <= 0) {
            return Result.paramError("操作人ID无效");
        }
        if (deleteReason == null || deleteReason.trim().isEmpty()) {
            return Result.paramError("删除原因不能为空");
        }

        try {
            // 调用Service：先备份，再删除
            Long backupId = propertyService.deletePropertyWithBackup(id, operatorId, deleteReason);
            return Result.success("楼盘删除成功，备份ID：" + backupId);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage()); // 如“存在关联楼栋，无法删除”
        } catch (Exception e) {
            return Result.fail("删除失败：系统异常");
        }
    }
}
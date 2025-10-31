package COM.House.Deed.Pavilion.Controller;

import COM.House.Deed.Pavilion.Utils.PageResult;
import COM.House.Deed.Pavilion.Utils.Result;
import COM.House.Deed.Pavilion.DTO.VisitRecordAddDTO;
import COM.House.Deed.Pavilion.DTO.VisitRecordQueryDTO;
import COM.House.Deed.Pavilion.DTO.VisitRecordUpdateDTO;
import COM.House.Deed.Pavilion.Entity.VisitRecord;
import COM.House.Deed.Pavilion.Service.VisitRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 带看记录控制器
 */
@RestController
@RequestMapping("/visitRecord")
@Tag(name = "带看记录管理", description = "带看记录的增删改查接口")
public class VisitRecordController {

    @Resource
    private VisitRecordService visitRecordService;

    /**
     * 新增带看记录
     */
    @PostMapping("/addVisitRecord")
    @Operation(
            summary = "新增带看记录",
            description = "创建带看记录，需关联存在的房源、客户和经纪人",
            responses = {
                    @ApiResponse(responseCode = "200", description = "新增成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "参数错误或关联数据不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<?> addVisitRecord(
            @Parameter(description = "带看记录新增参数", required = true)
            @Valid @RequestBody VisitRecordAddDTO addDTO
    ) {
        try {
            Long addVisitRecord = visitRecordService.addVisitRecord(addDTO);
            return Result.success("新增" + addVisitRecord + "带看记录成功");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 根据ID查询带看记录
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "根据ID查询带看记录详情",
            description = "通过带看记录ID获取完整信息",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "记录ID无效或不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<VisitRecord> getVisitRecordById(
            @Parameter(description = "带看记录ID", required = true, example = "40001")
            @PathVariable Long id
    ) {
        try {
            VisitRecord visitRecord = visitRecordService.getVisitRecordById(id);
            return Result.success(visitRecord);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 多条件分页查询带看记录
     */
    @GetMapping("/list")
    @Operation(
            summary = "多条件查询带看记录列表",
            description = "支持按房源、客户、经纪人、带看时间范围分页查询",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<PageResult<VisitRecord>> getVisitRecordList(
            @Parameter(description = "查询条件（可选）")
            VisitRecordQueryDTO queryDTO
    ) {
        try {
            PageResult<VisitRecord> pageResult = visitRecordService.getVisitRecordByCondition(queryDTO);
            return Result.success(pageResult);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 更新带看记录
     */
    @PostMapping("/updateVisitRecord")
    @Operation(
            summary = "更新带看记录",
            description = "支持更新带看时间、反馈、计划等，更新关联ID需确保存在性",
            responses = {
                    @ApiResponse(responseCode = "200", description = "更新成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "参数错误或记录不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<?> updateVisitRecord(
            @Parameter(description = "带看记录更新参数", required = true)
            @Valid @RequestBody VisitRecordUpdateDTO updateDTO
    ) {
        try {
            visitRecordService.updateVisitRecord(updateDTO);
            return Result.success("更新带看记录成功");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 删除带看记录（自动备份）
     */
    @PostMapping("/delete/{id}")
    @Operation(
            summary = "根据ID删除带看记录（自动备份）",
            description = "删除前会自动备份到visit_record_backup表，需传递操作人ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "删除成功"),
                    @ApiResponse(responseCode = "400", description = "参数错误或备份失败")
            }
    )
    public Result<?> deleteVisitRecord(
            @Parameter(description = "带看记录ID", required = true, example = "40001")
            @PathVariable Long id,
            @Parameter(description = "执行删除操作的用户ID", required = true, example = "1001")
            @RequestParam Long operatorId
    ) {
        try {
            visitRecordService.deleteVisitRecord(id, operatorId);
            return Result.success("删除带看记录成功（已自动备份）");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }
}

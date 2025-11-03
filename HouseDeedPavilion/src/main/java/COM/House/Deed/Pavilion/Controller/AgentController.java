package COM.House.Deed.Pavilion.Controller;

import COM.House.Deed.Pavilion.DTO.AgentQueryDTO;
import COM.House.Deed.Pavilion.Entity.Agent;
import COM.House.Deed.Pavilion.Service.AgentService;
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
 * 经纪人信息控制器
 */
@RestController
@RequestMapping("/")
@Tag(name = "经纪人管理", description = "经纪人的新增、查询、更新等接口")
public class AgentController {

    @Resource
    private AgentService agentService;

    /**
     * 新增经纪人接口
     *
     * @param agent         经纪人信息（包含姓名、工号等必填字段）
     * @param operator      操作人ID
     * @param bindingResult 参数校验结果
     * @return 统一响应体（含新增经纪人ID）
     */
    @PostMapping("/addAgent")
    @Operation(
            summary = "新增经纪人",
            description = "添加新的经纪人信息，需填写姓名、工号、联系电话等必填字段",
            responses = {
                    @ApiResponse(responseCode = "200", description = "新增成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "参数错误（如工号或电话为空、操作人无效）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或工号已存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<Long> addAgent(
            @Parameter(description = "经纪人信息（姓名、工号、电话为必填）", required = true)
            @Valid @RequestBody Agent agent,
            @Parameter(description = "操作人ID（系统用户ID）", required = true, example = "10001")
            @RequestParam Long operator,
            BindingResult bindingResult
    ) {
        // 1. 参数校验
        if (bindingResult.hasErrors()) {
            String errorMsg = Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage();
            return Result.paramError(errorMsg);
        }
        // 操作人参数校验
        if (operator == null || operator <= 0) {
            return Result.paramError("操作人ID无效（必须为正整数）");
        }

        try {
            // 2. 调用Service新增经纪人（需同步修改Service层方法接收operator参数）
            Long agentId = agentService.addAgent(agent, operator);
            // 3. 返回成功结果
            return Result.success(agentId);
        } catch (RuntimeException e) {
            // 4. 捕获业务异常（如工号重复）
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            // 5. 捕获系统异常
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 根据ID更新经纪人信息
     *
     * @param agent         包含ID和待更新字段的经纪人信息（ID必填）
     * @param operator      操作人ID
     * @param bindingResult 参数校验结果
     * @return 统一响应体
     */
    @PutMapping("/agents")
    @Operation(
            summary = "根据ID更新经纪人信息",
            description = "支持部分字段更新（ID为必填），若更新工号需确保唯一性",
            responses = {
                    @ApiResponse(responseCode = "200", description = "更新成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "参数错误（如ID为空、电话格式错误、操作人无效）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或经纪人不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<Void> updateAgent(
            @Parameter(description = "经纪人信息（ID必填，其他字段选填）", required = true)
            @Valid @RequestBody Agent agent,
            @Parameter(description = "操作人ID（系统用户ID）", required = true, example = "10001")
            @RequestParam Long operator,
            BindingResult bindingResult
    ) {
        // 1. 参数校验
        if (bindingResult.hasErrors()) {
            String errorMsg = Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage();
            return Result.paramError(errorMsg);
        }
        // 操作人参数校验
        if (operator == null || operator <= 0) {
            return Result.paramError("操作人ID无效（必须为正整数）");
        }

        try {
            // 2. 调用Service更新经纪人（需同步修改Service层方法接收operator参数）
            agentService.updateAgentById(agent, operator);
            return Result.success(); // 无数据返回，仅返回成功状态
        } catch (RuntimeException e) {
            // 3. 捕获业务异常（如经纪人不存在、工号重复）
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            // 4. 捕获系统异常
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 根据ID查询经纪人详情
     *
     * @param id 经纪人ID
     * @return 统一响应体（含经纪人完整信息）
     */
    @GetMapping("/agents/{id}")
    @Operation(
            summary = "根据ID查询经纪人详情",
            description = "通过经纪人ID获取完整信息，包括姓名、工号、联系电话等",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "ID无效（如为null或负数）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误或经纪人不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<Agent> getAgentById(
            @Parameter(description = "经纪人ID", required = true, example = "2001")
            @PathVariable Long id
    ) {
        try {
            // 调用Service查询经纪人
            Agent agent = agentService.getAgentById(id);
            return Result.success(agent);
        } catch (RuntimeException e) {
            // 捕获业务异常（如ID无效、经纪人不存在）
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            // 捕获系统异常
            return Result.fail("系统异常，请联系管理员");
        }
    }


    /**
     * 多条件分页查询经纪人
     *
     * @param queryDTO 包含查询条件（可选）和分页参数
     * @return 统一响应体（含分页的经纪人列表）
     */
    @GetMapping("/agents/condition")
    @Operation(
            summary = "多条件分页查询经纪人",
            description = "支持按姓名模糊查询、工号、电话、状态、所属门店等条件组合，默认分页",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功（无符合条件时返回空列表）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "参数错误（如分页参数不合法）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<PageResult<Agent>> getAgentsByCondition(
            @Parameter(description = "查询条件（所有条件均为可选）", required = false)
            AgentQueryDTO queryDTO
    ) {
        // 避免queryDTO为null（若前端未传递任何参数）
        if (queryDTO == null) {
            queryDTO = new AgentQueryDTO();
        }

        try {
            // 调用Service多条件查询经纪人
            PageResult<Agent> pageResult = agentService.getAgentsByCondition(queryDTO);
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
     * 根据ID删除经纪人（含自动备份）
     *
     * @param id       经纪人ID
     * @param operator 操作人ID
     * @return 统一响应体
     */
    @DeleteMapping("/agents/{id}")
    @Operation(
            summary = "根据ID删除经纪人",
            description = "删除前会自动备份数据到agent_backup表，删除后不可恢复原记录",
            responses = {
                    @ApiResponse(responseCode = "200", description = "删除成功（含备份）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "ID无效（如为null或负数）或操作人无效",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "备份失败或系统错误",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<String> deleteAgent(
            @Parameter(description = "经纪人ID", required = true, example = "2001")
            @PathVariable Long id,
            @Parameter(description = "删除原因", required = true, example = "离职")
            @RequestParam String deleteReason,
            @Parameter(description = "操作人ID（系统用户ID）", required = true, example = "10001")
            @RequestParam Long operator
    ) {
        try {
            // 操作人参数校验
            if (operator == null || operator <= 0) {
                return Result.paramError("操作人ID无效（必须为正整数）");
            }
            // 调用Service层删除方法（内含备份逻辑，需传入操作人信息）
            agentService.deleteAgentById(id, deleteReason, operator);
            return Result.success("经纪人已删除并备份");
        } catch (RuntimeException e) {
            // 捕获业务异常（如ID无效、备份失败、记录不存在）
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            // 捕获系统异常
            return Result.fail("删除失败，请联系管理员");
        }
    }

    /**
     * 根据门店ID分页查询经纪人列表
     *
     * @param storeId  所属门店ID（必填）
     * @param pageNum  页码（默认1）
     * @param pageSize 每页条数（默认10）
     * @return 统一响应体（含分页的经纪人列表）
     */
    @GetMapping("/agents/store/{storeId}")
    @Operation(
            summary = "根据门店ID查询经纪人列表（分页）",
            description = "查询指定门店下的所有经纪人，支持分页（默认每页10条）",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功（无经纪人时返回空列表）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "门店ID无效（如为null或负数）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<PageResult<Agent>> getAgentsByStoreId(
            @Parameter(description = "所属门店ID", required = true, example = "5001")
            @PathVariable Long storeId,

            @Parameter(description = "页码（默认1）", example = "1", required = false)
            @RequestParam(defaultValue = "1") Integer pageNum,

            @Parameter(description = "每页条数（默认10）", example = "10", required = false)
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        try {
            // 调用Service查询门店下的经纪人
            PageResult<Agent> pageResult = agentService.getAgentsByStoreId(storeId, pageNum, pageSize);
            return Result.success(pageResult);
        } catch (RuntimeException e) {
            // 捕获业务异常（如门店ID无效）
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            // 捕获系统异常
            return Result.fail("系统异常，请联系管理员");
        }
    }
}

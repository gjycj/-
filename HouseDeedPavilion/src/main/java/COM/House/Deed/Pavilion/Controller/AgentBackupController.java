package COM.House.Deed.Pavilion.Controller;

import COM.House.Deed.Pavilion.Constant.MessageConstant;
import COM.House.Deed.Pavilion.DTO.AgentBackupQueryDTO;
import COM.House.Deed.Pavilion.Entity.AgentBackup;
import COM.House.Deed.Pavilion.Exception.BusinessException;
import COM.House.Deed.Pavilion.Service.AgentBackupService;
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
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeParseException;

/**
 * 经纪人备份管理控制器
 * 负责经纪人删除备份、备份查询、从备份恢复等操作，与agent表操作形成数据安全闭环
 */
@RestController
@RequestMapping("/agentBackup") // 补充API版本前缀，便于后续迭代
@Tag(
        name = "经纪人备份管理",
        description = "包含经纪人删除自动备份、备份记录多条件查询、从备份恢复经纪人等接口；" +
                "删除经纪人时自动生成备份，支持误删后恢复，保障数据可追溯"
)
public class AgentBackupController {

    @Resource
    private AgentBackupService agentBackupService;


    /**
     * 1. 删除经纪人并自动备份（核心业务接口）
     * 完善点：补充参数长度校验、响应携带关键信息、明确业务约束
     */
    @DeleteMapping("/delete/{agentId}")
    @Operation(
            summary = "删除经纪人并生成备份",
            description = "1. 删除前自动查询经纪人信息并备份到agent_backup表；" +
                    "2. 支持记录删除原因（限500字内）；" +
                    "3. 删除后关联表（如house.agent_id）会按外键规则置空；" +
                    "4. 操作人ID需为系统已存在的用户ID",
            parameters = {
                    @Parameter(
                            name = "agentId",
                            description = "待删除的经纪人ID（对应agent表的id）",
                            required = true,
                            example = "6001",
                            schema = @Schema(type = "long", minimum = "1")
                    ),
                    @Parameter(
                            name = "operatorId",
                            description = "执行删除操作的用户ID（记录到备份表的deleted_by字段）",
                            required = true,
                            example = "1001",
                            schema = @Schema(type = "long", minimum = "1")
                    ),
                    @Parameter(
                            name = "deleteReason",
                            description = "删除原因（如'员工离职'、'信息录入错误'，可选，限500字）",
                            required = false,
                            example = "员工已离职，解除系统权限",
                            schema = @Schema(type = "string", maxLength = 500)
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "删除成功，返回备份记录ID",
                            content = @Content(
                                    schema = @Schema(implementation = Result.class, example = "{\n" +
                                            "  \"success\": true,\n" +
                                            "  \"code\": 200,\n" +
                                            "  \"message\": \"经纪人[张三，工号EMP2024001]已删除，备份记录ID：7001\",\n" +
                                            "  \"data\": 7001\n" +
                                            "}"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "参数错误（如ID为负、删除原因过长）",
                            content = @Content(schema = @Schema(implementation = Result.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "经纪人不存在（该ID的经纪人未找到）",
                            content = @Content(schema = @Schema(implementation = Result.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "系统错误（如备份插入失败、数据库异常）",
                            content = @Content(schema = @Schema(implementation = Result.class))
                    )
            }
    )
    public Result<String> deleteAndBackup(
            @PathVariable Long agentId,
            @RequestParam Long operatorId,
            @RequestParam(required = false) String deleteReason
    ) {
        try {
            // 1. 精细化参数校验
            if (agentId == null || agentId <= 0) {
                return Result.paramError("经纪人ID必须为正整数");
            }
            if (operatorId == null || operatorId <= 0) {
                return Result.paramError("操作人ID必须为正整数");
            }
            if (StringUtils.hasText(deleteReason) && deleteReason.length() > 500) {
                return Result.paramError("删除原因长度不能超过500字");
            }

            // 2. 调用服务层（补充返回备份ID，提升响应价值）
            Long backupId = agentBackupService.deleteAndBackup(agentId, operatorId, deleteReason);

            // 3. 补充关键信息到响应（便于前端展示和日志记录）
            AgentBackup backup = agentBackupService.getBackupById(backupId);
            String successMsg = String.format(
                    "经纪人[%s，工号%s]已删除，备份记录ID：%s",
                    backup.getName(), backup.getEmployeeId(), backupId
            );
            return Result.success(successMsg);

        } catch (BusinessException e) {
            // 业务异常：如经纪人不存在、备份失败
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            // 系统异常：统一封装提示
            return Result.fail("删除失败：系统异常，请联系管理员（" + e.getClass().getSimpleName() + "）");
        }
    }


    /**
     * 2. 从备份恢复经纪人（核心业务接口）
     * 完善点：补充工号冲突提示、保留备份的逻辑说明、响应携带新经纪人信息
     */
    @PostMapping("/recover")
    @Operation(
            summary = "从备份恢复经纪人",
            description = "1. 恢复前校验备份存在性、工号唯一性（工号重复会导致恢复失败）；" +
                    "2. 恢复后生成新的经纪人ID（不复用原ID），创建时间为恢复时间；" +
                    "3. keepBackup=true时保留备份记录，false时恢复后删除备份；" +
                    "4. 操作人ID需为系统已存在的管理员ID",
            parameters = {
                    @Parameter(
                            name = "backupId",
                            description = "待恢复的备份记录ID（对应agent_backup表的id）",
                            required = true,
                            example = "7001",
                            schema = @Schema(type = "long", minimum = "1")
                    ),
                    @Parameter(
                            name = "operatorId",
                            description = "执行恢复操作的用户ID（记录到新经纪人的created_by字段）",
                            required = true,
                            example = "1001",
                            schema = @Schema(type = "long", minimum = "1")
                    ),
                    @Parameter(
                            name = "keepBackup",
                            description = "是否保留备份记录（true=保留，false=恢复后删除）",
                            required = false,
                            example = "true",
                            schema = @Schema(type = "boolean", defaultValue = "true")
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "恢复成功，返回新经纪人ID和关键信息",
                            content = @Content(
                                    schema = @Schema(implementation = Result.class, example = "{\n" +
                                            "  \"success\": true,\n" +
                                            "  \"code\": 200,\n" +
                                            "  \"message\": \"经纪人[张三，工号EMP2024001]恢复成功，新ID：6002\",\n" +
                                            "  \"data\": {\n" +
                                            "    \"newAgentId\": 6002,\n" +
                                            "    \"name\": \"张三\",\n" +
                                            "    \"employeeId\": \"EMP2024001\",\n" +
                                            "    \"keepBackup\": true\n" +
                                            "  }\n" +
                                            "}"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "参数错误（如备份ID为负）",
                            content = @Content(schema = @Schema(implementation = Result.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "备份记录不存在（该ID的备份未找到）",
                            content = @Content(schema = @Schema(implementation = Result.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "工号冲突（该工号的经纪人已存在）",
                            content = @Content(schema = @Schema(implementation = Result.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "系统错误（如恢复插入失败）",
                            content = @Content(schema = @Schema(implementation = Result.class))
                    )
            }
    )
    public Result<String> recoverFromBackup(
            @RequestParam Long backupId,
            @RequestParam Long operatorId,
            @RequestParam(defaultValue = "true") boolean keepBackup
    ) {
        try {
            // 1. 精细化参数校验
            if (backupId == null || backupId <= 0) {
                return Result.paramError("备份ID必须为正整数");
            }
            if (operatorId == null || operatorId <= 0) {
                return Result.paramError("操作人ID必须为正整数");
            }

            // 2. 调用服务层恢复
            Long newAgentId = agentBackupService.recoverFromBackup(backupId, operatorId, keepBackup);

            // 3. 组装响应数据（携带关键信息，减少前端二次查询）
            AgentBackup backup = agentBackupService.getBackupById(backupId);
            String successMsg = String.format(
                    "经纪人[%s，工号%s]恢复成功，新ID：%s",
                    backup.getName(), backup.getEmployeeId(), newAgentId
            );
            return Result.success(successMsg);
        } catch (BusinessException e) {
            // 业务异常：如备份不存在、工号重复
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("恢复失败：系统异常，请联系管理员");
        }
    }


    /**
     * 3. 根据备份ID查询单条备份（基础查询接口）
     * 完善点：补充响应示例、明确字段含义
     */
    @GetMapping("/{backupId}")
    @Operation(
            summary = "查询单个经纪人备份详情",
            description = "返回备份记录的完整信息，包括原经纪人业务字段和备份审计字段（删除人、删除原因等）",
            parameters = {
                    @Parameter(
                            name = "backupId",
                            description = "备份记录ID（对应agent_backup表的id）",
                            required = true,
                            example = "7001",
                            schema = @Schema(type = "long", minimum = "1")
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "查询成功，返回备份详情",
                            content = @Content(
                                    schema = @Schema(implementation = AgentBackup.class, example = "{\n" +
                                            "  \"id\": 7001,\n" +
                                            "  \"originalId\": 6001,\n" +
                                            "  \"name\": \"张三\",\n" +
                                            "  \"employeeId\": \"EMP2024001\",\n" +
                                            "  \"phone\": \"13800138000\",\n" +
                                            "  \"storeId\": 301,\n" +
                                            "  \"position\": \"高级经纪人\",\n" +
                                            "  \"status\": 1,\n" +
                                            "  \"createdAt\": \"2024-01-15T09:30:00\",\n" +
                                            "  \"updatedAt\": \"2024-10-01T14:20:00\",\n" +
                                            "  \"backupTime\": \"2024-11-08T10:15:00\",\n" +
                                            "  \"deleteReason\": \"员工已离职\",\n" +
                                            "  \"deletedBy\": 1001\n" +
                                            "}"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "备份ID无效（如为负或null）",
                            content = @Content(schema = @Schema(implementation = Result.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "备份记录不存在",
                            content = @Content(schema = @Schema(implementation = Result.class))
                    )
            }
    )
    public Result<AgentBackup> getBackupById(@PathVariable Long backupId) {
        try {
            AgentBackup backup = agentBackupService.getBackupById(backupId);
            if (backup == null) {
                return Result.fail(String.format(MessageConstant.AGENT_BACKUP_NOT_FOUND, backupId));
            }
            return Result.success(backup);
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("查询失败：系统异常，请联系管理员");
        }
    }


    /**
     * 4. 按原经纪人ID查询备份列表（关联查询接口）
     * 完善点：补充分页默认值、结果排序说明
     */
    @GetMapping("/list/original/{originalAgentId}")
    @Operation(
            summary = "按原经纪人ID查询备份列表",
            description = "1. 查询某经纪人的所有历史备份记录（一个经纪人可能多次删除/备份）；" +
                    "2. 结果按备份时间倒序排列（最新备份在前）；" +
                    "3. 分页参数默认页码1，页大小10，最大页大小100",
            parameters = {
                    @Parameter(
                            name = "originalAgentId",
                            description = "原经纪人ID（对应agent表的id）",
                            required = true,
                            example = "6001",
                            schema = @Schema(type = "long", minimum = "1")
                    ),
                    @Parameter(
                            name = "pageNum",
                            description = "页码（默认1，最小1）",
                            required = false,
                            example = "1",
                            schema = @Schema(type = "integer", defaultValue = "1", minimum = "1")
                    ),
                    @Parameter(
                            name = "pageSize",
                            description = "页大小（默认10，最大100）",
                            required = false,
                            example = "10",
                            schema = @Schema(type = "integer", defaultValue = "10", minLength = 10, maxLength = 100)
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "查询成功，返回分页备份列表",
                            content = @Content(schema = @Schema(implementation = PageResult.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "原经纪人ID无效",
                            content = @Content(schema = @Schema(implementation = Result.class))
                    )
            }
    )
    public Result<PageResult<AgentBackup>> getBackupsByOriginalId(
            @PathVariable Long originalAgentId,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        try {
            // 补充分页参数默认值（避免Service层重复处理）
            pageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
            pageSize = pageSize == null || pageSize < 1 || pageSize > 100 ? 10 : pageSize;

            // 构造查询DTO（复用多条件查询逻辑，保持一致性）
            AgentBackupQueryDTO queryDTO = new AgentBackupQueryDTO();
            queryDTO.setOriginalId(originalAgentId);
            queryDTO.setPageNum(pageNum);
            queryDTO.setPageSize(pageSize);

            PageResult<AgentBackup> pageResult = agentBackupService.queryBackups(queryDTO);
            return Result.success(pageResult);
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("查询失败：系统异常，请联系管理员");
        }
    }


    /**
     * 5. 多条件分页查询经纪人备份（高级查询接口）
     * 完善点：补充参数格式说明、空条件处理逻辑
     */
    @GetMapping("/list/condition")
    @Operation(
            summary = "多条件分页查询经纪人备份",
            description = "1. 支持组合条件查询（如“姓名含张+备份时间2024年10月后”）；" +
                    "2. 时间格式需为yyyy-MM-dd HH:mm:ss（如2024-10-01 00:00:00）；" +
                    "3. 状态值说明：1=在职（删除时状态为在职），0=离职（删除时状态为离职）；" +
                    "4. 空条件时返回全量备份（分页）",
            parameters = {
                    @Parameter(
                            name = "queryDTO",
                            description = "查询条件DTO",
                            required = true,
                            schema = @Schema(implementation = AgentBackupQueryDTO.class)
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "查询成功，返回分页结果",
                            content = @Content(schema = @Schema(implementation = PageResult.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "参数错误（如时间格式不对、页大小超限）",
                            content = @Content(schema = @Schema(implementation = Result.class))
                    )
            }
    )
    public Result<PageResult<AgentBackup>> queryBackups(@Valid AgentBackupQueryDTO queryDTO) {
        try {
            // 补充参数校验：时间格式合法性（避免Service层处理格式异常）
            try {
                if (queryDTO.getBackupTimeStart() != null) {
                    // 触发LocalDateTime格式校验（若前端传错格式，此处会抛DateTimeParseException）
                    queryDTO.getBackupTimeStart().toString();
                }
                if (queryDTO.getBackupTimeEnd() != null) {
                    queryDTO.getBackupTimeEnd().toString();
                }
            } catch (DateTimeParseException e) {
                return Result.paramError("时间格式错误，请使用yyyy-MM-dd HH:mm:ss格式");
            }

            // 补充分页参数默认值
            if (queryDTO.getPageNum() == null || queryDTO.getPageNum() < 1) {
                queryDTO.setPageNum(1);
            }
            if (queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1 || queryDTO.getPageSize() > 100) {
                queryDTO.setPageSize(10);
            }

            PageResult<AgentBackup> pageResult = agentBackupService.queryBackups(queryDTO);
            return Result.success(pageResult);
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("查询失败：系统异常，请联系管理员");
        }
    }


    /**
     * 内部DTO：恢复结果封装（用于返回更丰富的恢复信息）
     */
    @Setter
    @Getter
    @Schema(description = "经纪人恢复结果DTO")
    private static class RecoveryResultDTO {
        // Getter + Setter（Lombok的@Data也可，此处显式定义便于Swagger识别）
        @Schema(description = "恢复后新生成的经纪人ID", example = "6002")
        private Long newAgentId;

        @Schema(description = "经纪人姓名", example = "张三")
        private String name;

        @Schema(description = "经纪人工号（唯一）", example = "EMP2024001")
        private String employeeId;

        @Schema(description = "是否保留备份记录", example = "true")
        private boolean keepBackup;

    }
}
package COM.House.Deed.Pavilion.Controller;

import COM.House.Deed.Pavilion.Entity.Agent;
import COM.House.Deed.Pavilion.Entity.AgentBackup;
import COM.House.Deed.Pavilion.Service.AgentBackupService;
import COM.House.Deed.Pavilion.Service.AgentService;
import COM.House.Deed.Pavilion.Utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 经纪人备份控制器（提供备份查询和数据恢复功能）
 */
@RestController
@RequestMapping("/agentBackup")
@Tag(name = "经纪人备份管理", description = "经纪人备份记录的查询与数据恢复接口")
public class AgentBackupController {

    @Resource
    private AgentBackupService agentBackupService;

    @Resource
    private AgentService agentService;

    /**
     * 根据备份ID查询备份记录
     *
     * @param backupId 备份记录ID（agent_backup表主键）
     * @return 备份记录详情
     */
    @GetMapping("/{backupId}")
    @Operation(
            summary = "根据备份ID查询备份记录",
            description = "获取指定ID的经纪人备份详情，包含原经纪人信息和删除时间",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功",
                            content = @Content(schema = @Schema(implementation = AgentBackup.class))),
                    @ApiResponse(responseCode = "400", description = "备份ID无效",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "备份记录不存在或系统错误",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<AgentBackup> getBackupById(
            @Parameter(description = "备份记录ID", required = true, example = "1001")
            @PathVariable Long backupId
    ) {
        try {
            AgentBackup backup = agentBackupService.getBackupById(backupId);
            return Result.success(backup);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("查询备份失败，请联系管理员");
        }
    }

    /**
     * 根据原经纪人ID查询备份记录
     *
     * @param originalAgentId 原经纪人ID（agent表的id）
     * @return 对应的备份记录（若存在）
     */
    @GetMapping("/original/{originalAgentId}")
    @Operation(
            summary = "根据原经纪人ID查询备份",
            description = "查询指定经纪人ID对应的备份记录（通常用于确认是否有可恢复的数据）",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功（无备份时返回null）",
                            content = @Content(schema = @Schema(implementation = AgentBackup.class))),
                    @ApiResponse(responseCode = "400", description = "原经纪人ID无效",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "系统错误",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<AgentBackup> getBackupByOriginalId(
            @Parameter(description = "原经纪人ID", required = true, example = "2001")
            @PathVariable Long originalAgentId
    ) {
        try {
            AgentBackup backup = agentBackupService.getBackupByOriginalId(originalAgentId);
            return Result.success(backup);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("查询备份失败，请联系管理员");
        }
    }

    /**
     * 从备份恢复经纪人数据（支持控制是否删除备份）
     *
     * @param backupId              备份记录ID
     * @param operatorId            操作人（执行恢复操作的用户）
     * @param deleteBackupAfterRestore 恢复后是否删除备份记录（true-删除，false-保留，默认false）
     * @return 恢复结果
     */
    @PostMapping("/restore/{backupId}")
    @Transactional
    @Operation(
            summary = "从备份恢复经纪人数据",
            description = "将备份记录中的数据恢复到agent表，可指定是否删除原备份记录",
            responses = {
                    @ApiResponse(responseCode = "200", description = "恢复成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "参数无效（如备份ID无效、操作人为空）",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "500", description = "恢复失败（如原记录已存在、备份损坏等）",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<String> restoreFromBackup(
            @Parameter(description = "备份记录ID（agent_backup表主键）", required = true, example = "1001")
            @PathVariable Long backupId,
            @Parameter(description = "操作人ID（执行恢复的用户ID）", required = true, example = "1001")
            @RequestParam Long operatorId,
            @Parameter(description = "恢复后是否保留备份记录（默认false）", example = "false")
            @RequestParam(required = false, defaultValue = "false") boolean deleteBackupAfterRestore) {

        // 1. 校验操作人及备份记录存在性
        if (operatorId == null || operatorId <= 0) {
            throw new RuntimeException("操作人ID无效（必须为正整数）");
        }
        AgentBackup backup = agentBackupService.getBackupById(backupId);
        if (backup == null) {
            throw new RuntimeException("备份记录不存在（ID：" + backupId + "）");
        }

        // 2. 校验原经纪人ID是否已存在（避免ID冲突）
        Long originalId = backup.getOriginalId();
        boolean existingAgent = agentService.existsAgentById(originalId);
        if (existingAgent) {
            throw new RuntimeException("原经纪人（ID：" + originalId + "）已存在，无法恢复");
        }

        // 3. 校验工号是否已被占用（避免工号冲突）
        String employeeId = backup.getEmployeeId();
        Long empIdCount = agentService.countByEmployeeId(employeeId);
        if (empIdCount > 0) {
            throw new RuntimeException("工号【" + employeeId + "】已被占用，无法恢复");
        }

        // 4. 执行恢复（复制备份数据到agent表）
        Agent agent = new Agent();
        agent.setId(originalId); // 恢复原ID
        agent.setName(backup.getName());
        agent.setEmployeeId(employeeId);
        agent.setPhone(backup.getPhone());
        agent.setStoreId(backup.getStoreId());
        agent.setPosition(backup.getPosition());
        agent.setStatus(backup.getStatus());
        agent.setCreatedAt(backup.getCreatedAt()); // 保留原创建时间
        agent.setUpdatedAt(LocalDateTime.now()); // 更新时间设为恢复时间
        agent.setCreatedBy(backup.getCreatedBy()); // 保留原创建人
        agent.setUpdatedBy(operatorId); // 恢复操作人作为最新更新人

        Long restoredId = agentService.addAgent(agent, operatorId);
        if (restoredId == null || !restoredId.equals(originalId)) {
            throw new RuntimeException("恢复经纪人失败，ID不匹配");
        }

        // 5. 根据参数决定是否删除备份记录
        if (!deleteBackupAfterRestore) {
            agentBackupService.deleteBackupById(backupId);
            return Result.success("经纪人（ID：" + originalId + "）已从备份恢复，备份记录已删除");
        } else {
            return Result.success("经纪人（ID：" + originalId + "）已从备份恢复，备份记录已保留");
        }
    }
}
package com.house.deed.pavilion.module.store.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.module.store.entity.Store;
import com.house.deed.pavilion.module.store.repository.StoreStatus;
import com.house.deed.pavilion.module.store.service.IStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * <p>
 * 门店信息表（租户级数据） 前端控制器
 * 负责门店的新增、分页查询、信息更新及状态管理
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Tag(name = "门店管理", description = "门店信息CRUD及状态管理接口")
@RestController
@RequestMapping("/module/store")
@Slf4j
public class StoreController {

    @Resource
    private IStoreService storeService;

    /**
     * 新增门店
     */
    @PostMapping
    @Operation(summary = "新增门店", description = "创建门店信息，包含编码、名称、区域等基础信息，自动关联当前租户")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功", content = @Content(schema = @Schema(implementation = Long.class))),
            @ApiResponse(responseCode = "400", description = "参数错误（如编码重复、必填字段为空）"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResultDTO<Long> addStore(
            @Parameter(description = "门店信息（需包含编码、名称、区域ID等必填项）", required = true)
            @Valid @RequestBody Store store) {
        Long tenantId = TenantContext.getTenantId();
        store.setTenantId(tenantId);
        store.setCreateTime(LocalDateTime.now());
        store.setUpdateTime(LocalDateTime.now());

        // 校验门店编码唯一性（租户内唯一）
        if (storeService.checkStoreCodeUnique(tenantId, store.getStoreCode(), null)) {
            throw new BusinessException(400, "门店编码已存在");
        }

        // 校验店长合法性（必须是当前租户下的经纪人）
        storeService.validateManager(tenantId, store.getManagerId());

        // 校验区域ID合法性（必须是当前租户下的区域）
        storeService.validateRegion(tenantId, store.getRegionId());

        storeService.save(store);
        return ResultDTO.success(store.getId());
    }

    /**
     * 分页查询门店
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询门店", description = "按租户隔离查询门店列表，支持按门店名称模糊搜索")
    @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = Page.class)))
    public ResultDTO<IPage<Store>> getStorePage(
            @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数，默认10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "门店名称（模糊查询，可为空）") @RequestParam(required = false) String storeName) {

        Long tenantId = TenantContext.getTenantId();
        Page<Store> page = new Page<>(pageNum, pageSize);
        IPage<Store> storePage = storeService.lambdaQuery()
                .eq(Store::getTenantId, tenantId)
                .like(storeName != null, Store::getStoreName, storeName)
                .orderByDesc(Store::getCreateTime)
                .page(page);
        return ResultDTO.success(storePage);
    }

    /**
     * 更新门店信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新门店信息", description = "全量更新门店信息，ID需在路径和请求体中保持一致")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "ID不匹配或编码重复"),
            @ApiResponse(responseCode = "404", description = "门店不存在")
    })
    public ResultDTO<Boolean> updateStore(
            @Parameter(description = "门店ID", required = true) @PathVariable Long id,
            @Parameter(description = "更新后的门店信息", required = true) @Valid @RequestBody Store store) {

        if (!id.equals(store.getId())) {
            throw new BusinessException(400, "ID不匹配");
        }
        Long tenantId = TenantContext.getTenantId();
        // 校验门店是否属于当前租户
        Store existingStore = storeService.getById(id);
        if (existingStore == null || !existingStore.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "门店不存在");
        }

        store.setTenantId(tenantId);
        store.setUpdateTime(LocalDateTime.now());

        // 校验门店编码唯一性（排除当前ID）
        if (storeService.checkStoreCodeUnique(tenantId, store.getStoreCode(), id)) {
            throw new BusinessException(400, "门店编码已存在");
        }

        // 校验店长和区域合法性
        storeService.validateManager(tenantId, store.getManagerId());
        storeService.validateRegion(tenantId, store.getRegionId());

        boolean success = storeService.updateById(store);
        return ResultDTO.success(success);
    }

    /**
     * 禁用/启用门店
     */
    @PatchMapping("/status/{id}")
    @Operation(summary = "更新门店状态", description = "修改门店营业状态（OPEN-营业，CLOSE-停业）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "状态更新成功"),
            @ApiResponse(responseCode = "400", description = "无效状态值"),
            @ApiResponse(responseCode = "404", description = "门店不存在或无权访问")
    })
    public ResultDTO<Boolean> updateStoreStatus(
            @Parameter(description = "门店ID", required = true) @PathVariable Long id,
            @Parameter(description = "状态值（OPEN-营业，CLOSE-停业）", required = true) @RequestParam StoreStatus status) {

        Long tenantId = TenantContext.getTenantId();
        Store store = storeService.getById(id);

        // 校验门店存在性及租户归属（多租户隔离）
        if (store == null || !store.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "门店不存在或无权访问");
        }

        // 状态未变更则直接返回成功（避免无效更新）
        if (store.getStatus() == status) {
            return ResultDTO.success(true);
        }

        // 记录原状态用于日志（可选，根据业务是否需要审计）
        StoreStatus oldStatus = store.getStatus();

        // 更新状态及时间
        store.setStatus(status);
        store.setUpdateTime(LocalDateTime.now());
        boolean success = storeService.updateById(store);

        // 可选：记录状态变更日志（参考house_status_log表设计）
        if (success) {
            log.info("门店状态变更：门店ID={}, 租户ID={}, 原状态={}, 新状态={}",
                    id, tenantId, oldStatus.getDesc(), status.getDesc());
            // 如需持久化日志，可调用日志服务：storeStatusLogService.recordLog(store, oldStatus, status, operatorId);
        }

        return ResultDTO.success(success);
    }

    /**
     * 查询单个门店详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询门店详情", description = "根据ID查询门店完整信息，包含所属区域、店长等关联数据")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = Store.class))),
            @ApiResponse(responseCode = "404", description = "门店不存在")
    })
    public ResultDTO<Store> getStoreById(
            @Parameter(description = "门店ID", required = true) @PathVariable Long id) {

        Long tenantId = TenantContext.getTenantId();
        Store store = storeService.getById(id);
        if (store == null || !store.getTenantId().equals(tenantId)) {
            throw new BusinessException(404, "门店不存在");
        }
        return ResultDTO.success(store);
    }
}
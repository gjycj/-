package com.house.deed.pavilion.module.house.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.module.house.entity.House;
import com.house.deed.pavilion.module.house.service.IHouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 房源信息表（租户核心数据） 前端控制器
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@RestController
@RequestMapping("/module/house")
public class HouseController {

    @Autowired
    private IHouseService houseService;

    /**
     * 新增房源（关联房东）
     * @param house 房源信息
     * @param landlordIds 关联的房东ID列表
     */
    @PostMapping
    public ResultDTO<Boolean> addHouse(@RequestBody House house, @RequestParam List<Long> landlordIds) {
        if (landlordIds == null || landlordIds.isEmpty()) {
            throw new BusinessException(400, "请至少关联一个房东");
        }
        boolean success = houseService.addHouse(house, landlordIds);
        return ResultDTO.success(success);
    }

    /**
     * 根据ID查询房源
     */
    @GetMapping("/{id}")
    public ResultDTO<House> getHouseById(@PathVariable Long id) {
        House house = houseService.getById(id);
        if (house == null) {
            throw new BusinessException(404, "房源不存在");
        }
        return ResultDTO.success(house);
    }

    /**
     * 分页查询房源列表（支持按房号和状态筛选）
     */
    @GetMapping("/page")
    public ResultDTO<Page<House>> getHousePage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String houseNo,
            @RequestParam(required = false) String status) {
        Page<House> page = new Page<>(pageNum, pageSize);
        Page<House> resultPage = houseService.getHousePage(page, houseNo, status);
        return ResultDTO.success(resultPage);
    }

    /**
     * 更新房源信息
     */
    @PutMapping("/{id}")
    public ResultDTO<Boolean> updateHouse(@PathVariable Long id, @RequestBody House house) {
        if (!id.equals(house.getId())) {
            throw new BusinessException(400, "ID不匹配");
        }
        // 校验房源是否存在
        if (!houseService.existsById(id)) {
            throw new BusinessException(404, "房源不存在");
        }
        boolean success = houseService.updateById(house);
        return ResultDTO.success(success);
    }

    /**
     * 删除房源（逻辑删除可根据需求改为物理删除）
     */
    @DeleteMapping("/{id}")
    public ResultDTO<Boolean> deleteHouse(@PathVariable Long id) {
        if (!houseService.existsById(id)) {
            throw new BusinessException(404, "房源不存在");
        }
        boolean success = houseService.removeById(id);
        return ResultDTO.success(success);
    }
}
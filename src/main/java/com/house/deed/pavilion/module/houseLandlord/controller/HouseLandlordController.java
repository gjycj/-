package com.house.deed.pavilion.module.houseLandlord.controller;

import com.house.deed.pavilion.common.dto.ResultDTO;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.module.houseLandlord.entity.HouseLandlord;
import com.house.deed.pavilion.module.houseLandlord.service.IHouseLandlordService;
import com.house.deed.pavilion.module.house.service.IHouseService;
import com.house.deed.pavilion.module.landlord.service.ILandlordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 房源与房东关联控制器
 */
@RestController
@RequestMapping("/module/houseLandlord")
public class HouseLandlordController {

    @Autowired
    private IHouseLandlordService houseLandlordService;
    @Autowired
    private IHouseService houseService;
    @Autowired
    private ILandlordService landlordService;

    /**
     * 新增房源-房东关联
     */
    @PostMapping
    public ResultDTO<Boolean> addRelation(@RequestBody HouseLandlord relation) {
        // 校验房源和房东是否存在
        if (!houseService.existsById(relation.getHouseId())) {
            throw new BusinessException(404, "房源不存在");
        }
        if (!landlordService.existsById(relation.getLandlordId())) {
            throw new BusinessException(404, "房东不存在");
        }
        boolean success = houseLandlordService.save(relation);
        return ResultDTO.success(success);
    }

    /**
     * 查询房源关联的所有房东
     */
    @GetMapping("/house/{houseId}")
    public ResultDTO<List<HouseLandlord>> getByHouseId(@PathVariable Long houseId) {
        List<HouseLandlord> relations = houseLandlordService.getByHouseId(houseId);
        return ResultDTO.success(relations);
    }

    /**
     * 解除房源-房东关联
     */
    @DeleteMapping
    public ResultDTO<Boolean> removeRelation(
            @RequestParam Long houseId,
            @RequestParam Long landlordId) {
        boolean success = houseLandlordService.removeByHouseAndLandlord(houseId, landlordId);
        return ResultDTO.success(success);
    }
}
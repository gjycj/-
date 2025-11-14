package com.house.deed.pavilion.module.house.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.common.exception.BusinessException;
import com.house.deed.pavilion.common.util.BeanConvertUtil;
import com.house.deed.pavilion.common.util.TenantContext;
import com.house.deed.pavilion.common.util.ValidateUtil;
import com.house.deed.pavilion.module.agent.entity.Agent;
import com.house.deed.pavilion.module.agent.service.IAgentService;
import com.house.deed.pavilion.module.building.entity.Building;
import com.house.deed.pavilion.module.building.service.IBuildingService;
import com.house.deed.pavilion.module.house.dto.HouseAddDTO;
import com.house.deed.pavilion.module.house.entity.House;
import com.house.deed.pavilion.module.house.mapper.HouseMapper;
import com.house.deed.pavilion.module.house.service.IHouseService;
import com.house.deed.pavilion.module.houseImage.entity.HouseImage;
import com.house.deed.pavilion.module.houseImage.service.IHouseImageService;
import com.house.deed.pavilion.module.houseLandlord.entity.HouseLandlord;
import com.house.deed.pavilion.module.houseLandlord.service.IHouseLandlordService;
import com.house.deed.pavilion.module.houseStatusLog.entity.HouseStatusLog;
import com.house.deed.pavilion.module.houseStatusLog.service.IHouseStatusLogService;
import com.house.deed.pavilion.module.houseTag.entity.HouseTag;
import com.house.deed.pavilion.module.houseTag.service.IHouseTagService;
import com.house.deed.pavilion.module.landlord.entity.Landlord;
import com.house.deed.pavilion.module.landlord.service.ILandlordService;
import com.house.deed.pavilion.module.operationLog.entity.OperationLog;
import com.house.deed.pavilion.module.operationLog.service.IOperationLogService;
import com.house.deed.pavilion.module.property.entity.Property;
import com.house.deed.pavilion.module.property.service.IPropertyService;
import com.house.deed.pavilion.module.tag.entity.Tag;
import com.house.deed.pavilion.module.tag.service.ITagService;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;



/**
 * <p>
 * 房源信息表（租户核心数据） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
@Slf4j
public class HouseServiceImpl extends ServiceImpl<HouseMapper, House> implements IHouseService {

    @Autowired
    private IBuildingService buildingService;

    @Autowired
    private IPropertyService propertyService;

    @Autowired
    private IAgentService agentService;

    @Autowired
    private ILandlordService landlordService;

    @Autowired
    private IHouseLandlordService houseLandlordService;

    @Autowired
    private ITagService tagService;

    @Autowired
    private IHouseTagService houseTagService;

    @Autowired
    private IHouseImageService houseImageService;

    @Autowired
    private IOperationLogService operationLogService;

    @Autowired
    private IHouseStatusLogService houseStatusLogService;

    /**
     * 房源录入核心方法（事务保证原子性）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addHouse(HouseAddDTO dto, Long currentAgentId) {
        log.info("开始录入房源：经纪人ID={}，房号={}", currentAgentId, dto.getHouseNo());

        // 1. 基础参数校验（复用通用工具）
        validateBaseParams(dto);

        // 2. 关联数据有效性校验
        validateRelatedData(dto, currentAgentId);

        // 3. DTO转实体（复用通用转换工具，自定义字段填充）
        House house = convertToHouse(dto, currentAgentId);

        // 4. 保存房源主数据
        boolean saveSuccess = this.save(house);
        if (!saveSuccess) {
            log.error("房源录入失败：保存主数据失败，房号={}", dto.getHouseNo());
            throw new BusinessException(500, "房源录入失败");
        }

        // 5. 同步关联数据（房东、标签、图片）
        syncRelatedData(house.getId(), dto);

        // 6. 记录操作日志与状态变更日志
        recordLogs(house, currentAgentId);

        log.info("房源录入成功：房源ID={}，房号={}", house.getId(), dto.getHouseNo());
        return house.getId();
    }

    /**
     * 步骤1：基础参数校验（复用通用工具）
     */
    private void validateBaseParams(HouseAddDTO dto) {
        // 非空校验
        ValidateUtil.notNull(dto.getPropertyId(), "楼盘ID不能为空");
        ValidateUtil.notNull(dto.getBuildingId(), "楼栋ID不能为空");
        ValidateUtil.notNull(dto.getHouseNo(), "房号不能为空");
        ValidateUtil.notNull(dto.getHouseType(), "户型不能为空");
        ValidateUtil.notNull(dto.getArea(), "建筑面积不能为空");
        ValidateUtil.notNull(dto.getFloor(), "所在楼层不能为空");
        ValidateUtil.notNull(dto.getTotalFloor(), "总楼层不能为空");
        ValidateUtil.notNull(dto.getPropertyRight(), "产权性质不能为空");
        ValidateUtil.notNull(dto.getPrice(), "挂牌价不能为空");
        ValidateUtil.notNull(dto.getTransactionType(), "交易类型不能为空");

        // 数值合理性校验
        ValidateUtil.greaterThanZero(dto.getArea(), "建筑面积必须大于0");
        ValidateUtil.greaterThanZero(dto.getPrice(), "挂牌价必须大于0");
        ValidateUtil.greaterThanZero(dto.getFloor(), "所在楼层必须大于0");
        ValidateUtil.greaterThanZero(dto.getTotalFloor(), "总楼层必须大于0");
        ValidateUtil.lessThanOrEqual(dto.getFloor(), dto.getTotalFloor(), "所在楼层不能超过总楼层");
        if (dto.getPropertyRightYears() != null) {
            ValidateUtil.greaterThanZero(dto.getPropertyRightYears(), "产权年限必须大于0");
        }

        // 格式校验
        ValidateUtil.validateHouseNo(dto.getHouseNo());
        ValidateUtil.validateCertNo(dto.getPropertyRightCertNo());

        // 房号重复校验（当前楼栋内唯一）
        Long tenantId = TenantContext.getTenantId();
        Integer duplicateCount = baseMapper.checkHouseNoUnique(dto.getBuildingId(), dto.getHouseNo());
        if (duplicateCount != null && duplicateCount > 0) {
            throw new BusinessException(400, "该楼栋下房号已存在，请勿重复录入");
        }
    }

    /**
     * 步骤2：关联数据有效性校验
     */
    private void validateRelatedData(HouseAddDTO dto, Long currentAgentId) {
        Long tenantId = TenantContext.getTenantId();

        // 校验楼盘（存在且属于当前租户）
        Property property = propertyService.getById(dto.getPropertyId());
        if (property == null || !property.getTenantId().equals(tenantId)) {
            throw new BusinessException(400, "楼盘不存在或无权访问");
        }

        // 校验楼栋（存在且属于当前租户、关联当前楼盘）
        Building building = buildingService.getById(dto.getBuildingId());
        if (building == null || !building.getTenantId().equals(tenantId) || !building.getPropertyId().equals(dto.getPropertyId())) {
            throw new BusinessException(400, "楼栋不存在或与楼盘不匹配");
        }

        // 校验录入经纪人（存在且在职、属于当前租户）
        Agent agent = agentService.getById(currentAgentId);
        if (agent == null || !agent.getTenantId().equals(tenantId) || agent.getStatus() != 1) {
            throw new BusinessException(400, "经纪人不存在或已离职");
        }

        // 校验房东（若传入则需存在且属于当前租户）
        if (dto.getLandlordId() != null) {
            Landlord landlord = landlordService.getById(dto.getLandlordId());
            if (landlord == null || !landlord.getTenantId().equals(tenantId)) {
                throw new BusinessException(400, "房东不存在或无权访问");
            }
        }

        // 校验标签（若传入则需全部存在且属于当前租户）
        if (!dto.getTagIds().isEmpty()) {
            List<Tag> tagList = tagService.listByIds(dto.getTagIds());
            if (tagList.size() != dto.getTagIds().size()) {
                throw new BusinessException(400, "部分标签不存在");
            }
            boolean allTenantMatch = tagList.stream().allMatch(tag -> tag.getTenantId().equals(tenantId));
            if (!allTenantMatch) {
                throw new BusinessException(400, "部分标签无权访问");
            }
        }
    }

    /**
     * 步骤3：DTO转实体（复用通用转换工具）
     */
    private House convertToHouse(HouseAddDTO dto, Long currentAgentId) {
        Long tenantId = TenantContext.getTenantId();

        // 基础字段转换（复用通用工具）
        House house = BeanConvertUtil.convert(dto, House.class, target -> {
            // 自定义字段填充：多租户ID
            target.setTenantId(tenantId);
            // 自定义字段填充：录入经纪人ID
            target.setCreateAgentId(currentAgentId);
            // 自定义字段填充：创建/更新时间
            target.setCreateTime(LocalDateTime.now());
            target.setUpdateTime(LocalDateTime.now());
            // 自定义字段填充：默认套内面积（建筑面积*0.8）
            if (target.getInsideArea() == null) {
                target.setInsideArea(dto.getArea().multiply(new BigDecimal("0.8")));
            }
            // 自定义字段填充：默认朝向
            if (StringUtils.isBlank(target.getOrientation())) {
                target.setOrientation("未知");
            }
            // 自定义字段填充：默认装修情况
            if (StringUtils.isBlank(target.getDecoration())) {
                target.setDecoration("毛坯");
            }
            // 自定义字段填充：房源状态（根据交易类型初始化）
            if ("RENT".equals(dto.getTransactionType())) {
                target.setStatus("ON_RENT"); // 仅租→待租
            } else {
                target.setStatus("ON_SALE"); // 出售/可售可租→在售
            }
            return target;
        });

        return house;
    }

    /**
     * 步骤5：同步关联数据（房东、标签、图片）
     */
    private void syncRelatedData(Long houseId, HouseAddDTO dto) {
        Long tenantId = TenantContext.getTenantId();

        // 1. 关联房东
        if (dto.getLandlordId() != null) {
            HouseLandlord houseLandlord = new HouseLandlord();
            houseLandlord.setTenantId(tenantId);
            houseLandlord.setHouseId(houseId);
            houseLandlord.setLandlordId(dto.getLandlordId());
            houseLandlord.setOwnership("100%"); // 默认100%所有权
            houseLandlordService.save(houseLandlord);
        }

        // 2. 关联标签
        if (!dto.getTagIds().isEmpty()) {
            List<HouseTag> houseTagList = dto.getTagIds().stream().map(tagId -> {
                HouseTag houseTag = new HouseTag();
                houseTag.setTenantId(tenantId);
                houseTag.setHouseId(houseId);
                houseTag.setTagId(tagId);
                return houseTag;
            }).collect(Collectors.toList());
            houseTagService.saveBatch(houseTagList);
        }

        // 3. 关联图片
        if (!dto.getImageList().isEmpty()) {
            List<HouseImage> houseImageList = dto.getImageList().stream().map(imageDTO -> {
                HouseImage houseImage = new HouseImage();
                houseImage.setTenantId(tenantId);
                houseImage.setHouseId(houseId);
                houseImage.setImageUrl(imageDTO.getImageUrl());
                // 默认首张为封面，无类型则设为OTHER
                houseImage.setImageType(StringUtils.isBlank(imageDTO.getImageType()) ? "OTHER" : imageDTO.getImageType());
                // 排序默认0
                houseImage.setSort(imageDTO.getSort() == null ? 0 : imageDTO.getSort());
                return houseImage;
            }).collect(Collectors.toList());
            houseImageService.saveBatch(houseImageList);
        }
    }

    /**
     * 步骤6：记录日志
     */
    private void recordLogs(House house, Long currentAgentId) {
        Long tenantId = TenantContext.getTenantId();
        Agent agent = agentService.getById(currentAgentId);
        String agentName = agent == null ? "未知" : agent.getName();

        // 1. 记录系统操作日志
        OperationLog operationLog = new OperationLog();
        operationLog.setTenantId(tenantId);
        operationLog.setModule("HOUSE_MANAGE");
        operationLog.setOperationType("ADD");
        operationLog.setOperationContent(String.format("录入房源：房号=%s，楼盘=%s，挂牌价=%.2f万元",
                house.getHouseNo(), propertyService.getById(house.getBuildingId()).getPropertyName(), house.getPrice()));
        operationLog.setOperatorId(currentAgentId);
        operationLog.setOperatorName(agentName);
        operationLog.setIpAddress(getCurrentIp()); // 实际项目中从请求上下文获取IP
        operationLogService.save(operationLog);

        // 2. 记录房源状态变更日志
        HouseStatusLog statusLog = new HouseStatusLog();
        statusLog.setTenantId(tenantId);
        statusLog.setHouseId(house.getId());
        statusLog.setStatusBefore(null);
        statusLog.setStatusAfter(house.getStatus());
        statusLog.setChangeReason("初始录入");
        statusLog.setOperatorId(currentAgentId);
        statusLog.setOperatorName(agentName);
        houseStatusLogService.save(statusLog);
    }

    /**
     * 获取当前请求IP（实际项目中从HttpServletRequest获取，此处简化）
     */
    private String getCurrentIp() {
        // 实际实现：return request.getRemoteAddr();
        return "127.0.0.1";
    }


    // 查询租户的房源列表（插件自动添加tenant_id条件）
    @Override
    public Page<House> getHousePage(Page<House> page, String houseNo, String status) {
        return lambdaQuery()
                .like(StrUtil.isNotBlank(houseNo), House::getHouseNo, houseNo)
                .eq(StrUtil.isNotBlank(status), House::getStatus, status)
                .page(page);
    }

    @Override
    public boolean existsById(Long id) {
        return this.exists(Wrappers.<House>lambdaQuery().eq(House::getId, id));
    }

}

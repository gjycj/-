package com.house.deed.pavilion.module.house.service.impl;

import cn.hutool.core.bean.BeanUtil;
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
import com.house.deed.pavilion.module.house.repository.HouseStatus;
import com.house.deed.pavilion.module.house.repository.TransactionType;
import com.house.deed.pavilion.module.house.service.IHouseService;
import com.house.deed.pavilion.module.houseBackup.entity.HouseBackup;
import com.house.deed.pavilion.module.houseBackup.service.IHouseBackupService;
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
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * <p>
 * 房源信息表（租户核心数据）服务实现类
 * 负责房源的录入、查询、删除备份等核心业务逻辑
 * 涉及关联表：楼栋表、房东关联表、标签关联表、图片表、状态日志表等
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
@Slf4j
public class HouseServiceImpl extends ServiceImpl<HouseMapper, House> implements IHouseService {

    @Resource
    private IBuildingService buildingService;

    @Resource
    private IPropertyService propertyService;

    @Resource
    private IAgentService agentService;

    @Resource
    private ILandlordService landlordService;

    @Resource
    private IHouseLandlordService houseLandlordService;

    @Resource
    private ITagService tagService;

    @Resource
    private IHouseTagService houseTagService;

    @Resource
    private IHouseImageService houseImageService;

    @Resource
    private IOperationLogService operationLogService;

    @Resource
    private IHouseStatusLogService houseStatusLogService;

    @Resource
    private IHouseBackupService houseBackupService;  // 房源删除备份服务


    /**
     * 房源录入核心方法（事务保证原子性）
     * 包含参数校验、关联数据校验、数据转换、主数据保存、关联数据同步及日志记录
     *
     * @param dto            房源新增DTO，包含房源基本信息及关联数据ID
     * @param currentAgentId 当前操作经纪人ID（用于权限校验及日志记录）
     * @return 新增房源的ID
     * @throws BusinessException 当参数无效、关联数据不存在或保存失败时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addHouse(HouseAddDTO dto, Long currentAgentId) {
        log.info("开始录入房源：经纪人ID={}，房号={}", currentAgentId, dto.getHouseNo());

        // 1. 基础参数校验（非空、格式、合理性）
        validateBaseParams(dto);

        // 2. 关联数据有效性校验（楼盘、楼栋、经纪人、房东等）
        validateRelatedData(dto, currentAgentId);

        // 3. DTO转实体（填充租户ID、默认值等）
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
     * 基础参数校验
     * 校验房源必填字段、数值合理性、格式规范性及唯一性
     *
     * @param dto 房源新增DTO
     * @throws BusinessException 当参数不符合要求时抛出
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

        // 交易类型有效性校验（枚举匹配）
        if (dto.getTransactionType() == null) {
            throw new BusinessException(400, "交易类型不能为空");
        }
        boolean isValidType = Arrays.stream(TransactionType.values())
                .anyMatch(type -> type.equals(dto.getTransactionType()));
        if (!isValidType) {
            throw new BusinessException(400, "无效的交易类型");
        }

        // 数值合理性校验
        ValidateUtil.greaterThanZero(dto.getArea(), "建筑面积必须大于0");
        ValidateUtil.greaterThanZero(dto.getPrice(), "挂牌价必须大于0");
        ValidateUtil.greaterThanZero(dto.getFloor(), "所在楼层必须大于0");
        ValidateUtil.greaterThanZero(dto.getTotalFloor(), "总楼层必须大于0");
        ValidateUtil.lessThanOrEqual(dto.getFloor(), dto.getTotalFloor(), "所在楼层不能超过总楼层");
        if (dto.getPropertyRightYears() != null) {
            ValidateUtil.greaterThanZero(dto.getPropertyRightYears(), "产权年限必须大于0");
        }

        // 格式校验（房号、产权证号）
        ValidateUtil.validateHouseNo(dto.getHouseNo());
        ValidateUtil.validateCertNo(dto.getPropertyRightCertNo());

        // 房号唯一性校验（当前楼栋内唯一）
        Long tenantId = TenantContext.getTenantId();
        Integer duplicateCount = baseMapper.checkHouseNoUnique(dto.getBuildingId(), dto.getHouseNo(), tenantId);
        if (duplicateCount != null && duplicateCount > 0) {
            throw new BusinessException(400, "该楼栋下房号已存在，请勿重复录入");
        }
    }


    /**
     * 关联数据有效性校验
     * 校验楼盘、楼栋、经纪人、房东、标签等关联数据的存在性及租户权限
     *
     * @param dto            房源新增DTO
     * @param currentAgentId 当前操作经纪人ID
     * @throws BusinessException 当关联数据不存在或无权访问时抛出
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

        // 校验房东（至少关联一个，且均存在、属于当前租户）
        if (dto.getLandlordIds() == null || dto.getLandlordIds().isEmpty()) {
            throw new BusinessException(400, "请至少关联一个房东");
        }
        for (Long landlordId : dto.getLandlordIds()) {
            Landlord landlord = landlordService.getById(landlordId);
            if (landlord == null || !landlord.getTenantId().equals(tenantId)) {
                throw new BusinessException(400, "房东ID=" + landlordId + "不存在或无权访问");
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
     * DTO转换为实体对象
     * 基于通用转换工具实现基础字段映射，补充租户ID、默认值等业务字段
     *
     * @param dto            房源新增DTO
     * @param currentAgentId 当前操作经纪人ID
     * @return 转换后的House实体
     */
    private House convertToHouse(HouseAddDTO dto, Long currentAgentId) {
        Long tenantId = TenantContext.getTenantId();

        // 基础字段转换 + 自定义字段填充
        return BeanConvertUtil.convert(dto, House.class, target -> {
            target.setTenantId(tenantId); // 多租户ID（从上下文获取）
            target.setCreateAgentId(currentAgentId); // 录入经纪人ID
            target.setCreateTime(LocalDateTime.now()); // 创建时间
            target.setUpdateTime(LocalDateTime.now()); // 更新时间

            // 套内面积默认值（建筑面积*0.8，若未传入）
            if (target.getInsideArea() == null) {
                target.setInsideArea(dto.getArea().multiply(new BigDecimal("0.8")));
            }

            // 朝向默认值（未知）
            if (StringUtils.isBlank(target.getOrientation())) {
                target.setOrientation("未知");
            }

            // 装修情况默认值（毛坯）
            if (StringUtils.isBlank(target.getDecoration())) {
                target.setDecoration("毛坯");
            }

            // 基于交易类型设置初始状态
            // 出售/可售可租→在售
            target.setStatus(HouseStatus.ON_SALE); // 出租→待租
            return target;
        });
    }


    /**
     * 同步关联数据
     * 批量处理房源与房东、标签、图片的关联关系，确保主数据与关联数据一致性
     *
     * @param houseId 房源ID（主数据ID）
     * @param dto     房源新增DTO（包含关联数据ID列表）
     */
    private void syncRelatedData(Long houseId, HouseAddDTO dto) {
        Long tenantId = TenantContext.getTenantId();

        // 1. 关联房东（批量创建房源-房东关联关系）
        List<HouseLandlord> houseLandlords = dto.getLandlordIds().stream().map(landlordId -> {
            HouseLandlord houseLandlord = new HouseLandlord();
            houseLandlord.setTenantId(tenantId);
            houseLandlord.setHouseId(houseId);
            houseLandlord.setLandlordId(landlordId);
            houseLandlord.setOwnership("100%"); // 默认100%所有权
            return houseLandlord;
        }).collect(Collectors.toList());
        houseLandlordService.saveBatch(houseLandlords);

        // 2. 关联标签（批量创建房源-标签关联关系）
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

        // 3. 关联图片（批量创建房源图片记录）
        if (!dto.getImageList().isEmpty()) {
            List<HouseImage> houseImageList = dto.getImageList().stream().map(imageDTO -> {
                HouseImage houseImage = new HouseImage();
                houseImage.setTenantId(tenantId);
                houseImage.setHouseId(houseId);
                houseImage.setImageUrl(imageDTO.getImageUrl());
                houseImage.setImageType(StringUtils.isBlank(imageDTO.getImageType()) ? "OTHER" : imageDTO.getImageType());
                houseImage.setSort(imageDTO.getSort() == null ? 0 : imageDTO.getSort());
                return houseImage;
            }).collect(Collectors.toList());
            houseImageService.saveBatch(houseImageList);
        }
    }


    /**
     * 记录操作日志与状态变更日志
     * 用于追踪房源创建操作及初始状态，支持后续审计与溯源
     *
     * @param house          新增的房源实体
     * @param currentAgentId 当前操作经纪人ID
     */
    private void recordLogs(House house, Long currentAgentId) {
        Long tenantId = TenantContext.getTenantId();
        Agent agent = agentService.getById(currentAgentId);
        String agentName = agent == null ? "未知" : agent.getName();

        // 1. 系统操作日志（记录录入行为）
        OperationLog operationLog = new OperationLog();
        operationLog.setTenantId(tenantId);
        operationLog.setModule("HOUSE_MANAGE");
        operationLog.setOperationType("ADD");
        operationLog.setOperationContent(String.format("录入房源：房号=%s，楼盘=%s，挂牌价=%.2f万元",
                house.getHouseNo(), propertyService.getById(house.getBuildingId()).getPropertyName(), house.getPrice()));
        operationLog.setOperatorId(currentAgentId);
        operationLog.setOperatorName(agentName);
        operationLog.setIpAddress(getCurrentIp()); // 操作IP地址
        operationLogService.save(operationLog);

        // 2. 房源状态变更日志（记录初始状态）
        HouseStatusLog statusLog = new HouseStatusLog();
        statusLog.setTenantId(tenantId);
        statusLog.setHouseId(house.getId());
        statusLog.setStatusBefore(null); // 初始状态无前置状态
        statusLog.setStatusAfter(house.getStatus().toString());
        statusLog.setChangeReason("初始录入");
        statusLog.setOperatorId(currentAgentId);
        statusLog.setOperatorName(agentName);
        houseStatusLogService.save(statusLog);
    }


    /**
     * 获取当前请求的真实IP地址（支持代理场景）
     * 优先从代理头获取，兼容多种代理服务器（Squid、Apache、WebLogic等）
     *
     * @return 客户端真实IP地址，非Web环境返回默认值127.0.0.1
     */
    private String getCurrentIp() {
        // 从请求上下文获取HttpServletRequest
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "127.0.0.1"; // 非Web环境默认值
        }
        HttpServletRequest request = attributes.getRequest();

        String ip;
        // 依次尝试从代理头获取真实IP
        ip = getIpFromHeader(request, "X-Forwarded-For");
        if (ip == null) ip = getIpFromHeader(request, "Proxy-Client-IP");
        if (ip == null) ip = getIpFromHeader(request, "WL-Proxy-Client-IP");
        if (ip == null) ip = getIpFromHeader(request, "HTTP_CLIENT_IP");
        if (ip == null) ip = getIpFromHeader(request, "HTTP_X_FORWARDED_FOR");

        // 若代理头无有效IP，使用请求远程地址
        if (ip == null) {
            ip = request.getRemoteAddr();
        }

        // 处理多IP场景（取第一个有效IP）
        if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * 从请求头中提取IP（过滤unknown及空值）
     *
     * @param request    请求对象
     * @param headerName 头名称
     * @return 有效IP或null
     */
    private String getIpFromHeader(HttpServletRequest request, String headerName) {
        String ip = request.getHeader(headerName);
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            return null;
        }
        return ip;
    }


    /**
     * 分页查询房源列表（支持按房号模糊查询和状态筛选）
     * 自动通过多租户插件添加租户ID条件，确保数据隔离
     *
     * @param page    分页参数（页码、每页条数）
     * @param houseNo 房号（模糊查询，可为null）
     * @param status  房源状态（精确匹配，可为null）
     * @return 分页查询结果（包含房源列表及分页信息）
     */
    @Override
    public Page<House> getHousePage(Page<House> page, String houseNo, String status) {
        return lambdaQuery()
                .like(StrUtil.isNotBlank(houseNo), House::getHouseNo, houseNo)
                .eq(StrUtil.isNotBlank(status), House::getStatus,
                        HouseStatus.getByCode(status) != null ? Objects.requireNonNull(HouseStatus.getByCode(status)).getCode() : status)
                .page(page);
    }


    /**
     * 校验房源是否存在
     *
     * @param id 房源ID
     * @return 存在返回true，否则返回false
     */
    @Override
    public boolean existsById(Long id) {
        return this.exists(Wrappers.<House>lambdaQuery().eq(House::getId, id));
    }


    /**
     * 删除房源并备份（事务保证原子性）
     * 先将房源信息复制到备份表，再删除原房源，确保数据可追溯
     *
     * @param id       房源ID
     * @param operator 删除操作人名称
     * @return 操作成功返回true，否则返回false
     * @throws RuntimeException 当备份失败时抛出，触发事务回滚
     */
    @Transactional
    @Override
    public boolean deleteAndBackup(Long id, String operator) {
        // 1. 查询原房源信息
        House house = getById(id);
        if (house == null) {
            return false;
        }

        // 2. 复制房源信息到备份表
        HouseBackup backup = new HouseBackup();
        BeanUtil.copyProperties(house, backup); // 复制共同字段
        backup.setOriginalId(house.getId());    // 记录原房源ID
        backup.setDeleteTime(LocalDateTime.now()); // 记录删除时间
        backup.setDeleteOperator(operator);     // 记录删除人

        // 3. 先保存备份，失败则取消删除
        boolean backupSuccess = houseBackupService.save(backup);
        if (!backupSuccess) {
            throw new RuntimeException("房源备份失败，取消删除操作");
        }

        // 4. 删除原房源
        return removeById(id);
    }


}
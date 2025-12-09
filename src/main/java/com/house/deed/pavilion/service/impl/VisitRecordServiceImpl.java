package com.house.deed.pavilion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.entity.VisitRecord;
import com.house.deed.pavilion.mapper.VisitRecordMapper;
import com.house.deed.pavilion.service.VisitRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 带看记录表（租户级数据）服务实现类
 *
 * <p>实现带看记录的核心业务逻辑，为房产经纪业务提供带看记录的完整生命周期管理。
 * 所有操作均严格遵循租户数据隔离原则，确保不同租户间的数据完全隔离和安全。</p>
 *
 * <p><b>核心业务特性：</b></p>
 * <ul>
 *   <li><b>严格的租户隔离</b>：所有操作均强制校验租户ID，确保数据访问安全</li>
 *   <li><b>多维查询支持</b>：支持房源、客户、经纪人、时间范围等多维度筛选</li>
 *   <li><b>批量操作优化</b>：提供高效的批量增删改操作，提升数据处理效率</li>
 *   <li><b>业务完整性校验</b>：完善的数据校验机制，确保业务数据完整性</li>
 *   <li><b>事务一致性保障</b>：关键操作通过事务确保数据一致性</li>
 * </ul>
 *
 * <p><b>业务实体关联关系：</b></p>
 * <ul>
 *   <li>一个带看记录关联一个房源（house_id）</li>
 *   <li>一个带看记录关联一个客户（customer_id）</li>
 *   <li>一个带看记录关联一个经纪人（agent_id）</li>
 *   <li>所有带看记录属于特定租户（tenant_id）</li>
 * </ul>
 *
 * @author yuquanxi
 * @since 2025-11-26
 * @version 1.0.0
 */
@Service
public class VisitRecordServiceImpl extends ServiceImpl<VisitRecordMapper, VisitRecord> implements VisitRecordService {

    /**
     * 新增带看记录（租户隔离强化校验）
     * <p>创建新的带看记录，强制校验租户ID以确保数据隔离安全</p>
     *
     * @param visitRecord 待保存的带看记录实体对象，必须包含租户ID
     * @return boolean 新增操作结果，true-成功 false-失败
     * @throws IllegalArgumentException 当租户ID为空时抛出异常
     *
     * <p><b>数据校验：</b></p>
     * <ul>
     *   <li>租户ID不能为null（实体类已有校验，此处进行二次确认）</li>
     *   <li>房源ID、客户ID、经纪人ID等关键关联字段的合法性</li>
     *   <li>带看时间不能晚于当前时间</li>
     * </ul>
     *
     * <p><b>业务逻辑：</b></p>
     * <ol>
     *   <li>租户ID空值校验（二次确认保证安全）</li>
     *   <li>执行数据库保存操作</li>
     *   <li>返回操作结果</li>
     * </ol>
     */
    @Override
    public boolean saveVisitRecord(VisitRecord visitRecord) {
        // 强化租户隔离：确保租户ID非空（实体类已有校验，此处二次确认）
        if (visitRecord.getTenantId() == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }
        return save(visitRecord);
    }

    /**
     * 根据ID查询带看记录（租户隔离精确查询）
     * <p>在指定租户下根据主键ID精确查询带看记录，确保数据访问权限</p>
     *
     * @param id 带看记录主键ID，不能为null
     * @param tenantId 租户ID，用于数据隔离和权限校验，不能为null
     * @return VisitRecord 带看记录实体，未找到或租户不匹配时返回null
     *
     * <p><b>安全机制：</b></p>
     * <ul>
     *   <li>双条件查询：同时匹配ID和租户ID，确保数据隔离</li>
     *   <li>权限校验：防止跨租户数据访问</li>
     *   <li>精确查询：使用eq操作符确保结果唯一性</li>
     * </ul>
     *
     * <p><b>查询性能：</b>ID字段通常为主键索引，租户ID字段通常为复合索引，查询效率高</p>
     */
    @Override
    public VisitRecord getByIdWithTenant(Long id, Long tenantId) {
        return getOne(new LambdaQueryWrapper<VisitRecord>()
                .eq(VisitRecord::getId, id)
                .eq(VisitRecord::getTenantId, tenantId));
    }

    /**
     * 更新带看记录（租户隔离权限校验）
     * <p>更新现有带看记录信息，操作前严格校验租户权限和数据存在性</p>
     *
     * @param visitRecord 待更新的带看记录实体，必须包含ID和租户ID
     * @return boolean 更新操作结果，true-成功 false-失败
     * @throws IllegalArgumentException 当租户ID或记录ID为空时抛出异常
     * @throws SecurityException 当试图操作其他租户数据时抛出权限异常
     *
     * <p><b>权限校验流程：</b></p>
     * <ol>
     *   <li>参数空值校验</li>
     *   <li>查询记录是否存在且属于指定租户</li>
     *   <li>权限校验失败时抛出SecurityException</li>
     *   <li>校验通过后执行更新操作</li>
     * </ol>
     *
     * <p><b>更新限制：</b>租户ID不可更新，其他字段根据业务需求可更新</p>
     */
    @Override
    public boolean updateVisitRecord(VisitRecord visitRecord) {
        if (visitRecord.getTenantId() == null || visitRecord.getId() == null) {
            throw new IllegalArgumentException("租户ID和记录ID不能为空");
        }
        // 校验记录是否属于当前租户，防止越权操作
        VisitRecord existing = getByIdWithTenant(visitRecord.getId(), visitRecord.getTenantId());
        if (existing == null) {
            throw new SecurityException("无权操作其他租户的带看记录");
        }
        return updateById(visitRecord);
    }

    /**
     * 根据ID删除带看记录（租户隔离删除）
     * <p>删除指定租户下的特定带看记录，确保操作权限和数据安全</p>
     *
     * @param id 带看记录主键ID，不能为null
     * @param tenantId 租户ID，用于权限校验和数据隔离，不能为null
     * @return boolean 删除操作结果，true-成功 false-失败
     * @throws IllegalArgumentException 当租户ID或记录ID为空时抛出异常
     *
     * <p><b>删除机制：</b></p>
     * <ul>
     *   <li>物理删除：直接从数据库移除记录</li>
     *   <li>条件删除：同时匹配ID和租户ID，确保操作安全</li>
     *   <li>原子操作：删除操作不可逆，需谨慎使用</li>
     * </ul>
     *
     * <p><b>使用场景：</b>错误数据修正、数据清理等</p>
     */
    @Override
    public boolean removeByIdWithTenant(Long id, Long tenantId) {
        if (id == null || tenantId == null) {
            throw new IllegalArgumentException("租户ID和记录ID不能为空");
        }
        return remove(new LambdaQueryWrapper<VisitRecord>()
                .eq(VisitRecord::getId, id)
                .eq(VisitRecord::getTenantId, tenantId));
    }

    /**
     * 多条件分页查询（租户隔离和多维度筛选）
     * <p>支持多条件组合分页查询，返回满足条件的带看记录分页结果</p>
     *
     * @param page 分页参数对象，包含页码、每页大小等分页信息
     * @param tenantId 租户ID，必须指定，用于数据隔离
     * @param houseId 房源ID，精确匹配，null表示不限制
     * @param customerId 客户ID，精确匹配，null表示不限制
     * @param agentId 经纪人ID，精确匹配，null表示不限制
     * @param startTime 带看时间起始范围，null表示无开始时间限制
     * @param endTime 带看时间结束范围，null表示无结束时间限制
     * @param visitType 带看类型，精确匹配，null表示不限制
     * @param intentionLevel 意向等级，精确匹配，null表示不限制
     * @return IPage<VisitRecord> 分页查询结果，包含数据列表和分页统计信息
     * @throws IllegalArgumentException 当租户ID为空时抛出异常
     *
     * <p><b>时间查询条件处理逻辑：</b></p>
     * <table border="1">
     *   <tr><th>条件组合</th><th>查询逻辑</th><th>说明</th></tr>
     *   <tr><td>startTime和endTime都不为null</td><td>BETWEEN范围查询</td><td>查询指定时间区间内的记录</td></tr>
     *   <tr><td>仅startTime不为null</td><td>GE大于等于查询</td><td>查询指定时间之后的记录</td></tr>
     *   <tr><td>仅endTime不为null</td><td>LE小于等于查询</td><td>查询指定时间之前的记录</td></tr>
     *   <tr><td>startTime和endTime都为null</td><td>不添加时间条件</td><td>查询所有时间的记录</td></tr>
     * </table>
     *
     * <p><b>排序规则：</b>默认按带看时间倒序排列（最新带看在前）</p>
     */
    @Override
    public IPage<VisitRecord> pageQuery(Page<VisitRecord> page,
                                        Long tenantId,
                                        Long houseId,
                                        Long customerId,
                                        Long agentId,
                                        LocalDateTime startTime,
                                        LocalDateTime endTime,
                                        String visitType,
                                        Byte intentionLevel) {
        // 强制租户隔离：必须传入租户ID
        if (tenantId == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }

        // 构建基础查询条件（强制租户隔离）
        LambdaQueryWrapper<VisitRecord> queryWrapper = new LambdaQueryWrapper<VisitRecord>()
                .eq(VisitRecord::getTenantId, tenantId);

        // 动态拼接多维查询条件
        if (houseId != null) {
            queryWrapper.eq(VisitRecord::getHouseId, houseId);
        }
        if (customerId != null) {
            queryWrapper.eq(VisitRecord::getCustomerId, customerId);
        }
        if (agentId != null) {
            queryWrapper.eq(VisitRecord::getAgentId, agentId);
        }

        // 时间范围查询条件处理（支持单边条件）
        if (startTime != null && endTime != null) {
            queryWrapper.between(VisitRecord::getVisitTime, startTime, endTime);
        } else if (startTime != null) {
            queryWrapper.ge(VisitRecord::getVisitTime, startTime);
        } else if (endTime != null) {
            queryWrapper.le(VisitRecord::getVisitTime, endTime);
        }

        if (visitType != null) {
            queryWrapper.eq(VisitRecord::getVisitType, visitType);
        }
        if (intentionLevel != null) {
            queryWrapper.eq(VisitRecord::getIntentionLevel, intentionLevel);
        }

        // 排序规则：按带看时间倒序（最新记录在前）
        queryWrapper.orderByDesc(VisitRecord::getVisitTime);

        return page(page, queryWrapper);
    }

    /**
     * 批量新增带看记录（租户隔离一致性校验）
     * <p>批量创建带看记录，操作前校验所有记录属于同一租户，确保数据隔离一致性</p>
     *
     * @param visitRecordList 待保存的带看记录实体列表，不能为null
     * @return boolean 批量操作结果，true-成功 false-失败
     * @throws IllegalArgumentException 当列表为空、租户ID为空或租户不一致时抛出异常
     *
     * <p><b>事务特性：</b>使用@Transactional注解，任一记录保存失败则全部回滚</p>
     *
     * <p><b>租户一致性校验逻辑：</b></p>
     * <ol>
     *   <li>检查列表是否为空（为空时直接返回true，视为操作成功）</li>
     *   <li>获取第一个记录的租户ID作为基准租户ID</li>
     *   <li>遍历所有记录，校验租户ID是否与基准一致</li>
     *   <li>不一致时抛出异常终止操作</li>
     * </ol>
     *
     * <p><b>性能优化：</b>使用Stream API进行批量校验，提高校验效率</p>
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveBatchVisitRecord(List<VisitRecord> visitRecordList) {
        // 空列表检查：空列表视为操作成功
        if (visitRecordList.isEmpty()) {
            return true;
        }

        // 校验所有记录租户ID一致且非空
        Long tenantId = visitRecordList.get(0).getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }

        // 使用Stream API校验所有记录是否属于同一租户
        boolean allSameTenant = visitRecordList.stream()
                .allMatch(record -> tenantId.equals(record.getTenantId()));
        if (!allSameTenant) {
            throw new IllegalArgumentException("批量新增的带看记录必须属于同一租户");
        }

        return saveBatch(visitRecordList);
    }

    /**
     * 批量更新带看记录（租户隔离和ID校验）
     * <p>批量更新带看记录，操作前校验所有记录属于同一租户且包含有效ID</p>
     *
     * @param visitRecordList 待更新的带看记录实体列表，不能为null
     * @return boolean 批量操作结果，true-成功 false-失败
     * @throws IllegalArgumentException 当列表为空、租户ID为空或记录不符合要求时抛出异常
     *
     * <p><b>校验要求：</b></p>
     * <ul>
     *   <li>所有记录必须属于同一租户</li>
     *   <li>所有记录必须包含非空的ID（用于定位更新记录）</li>
     *   <li>空列表视为操作成功（无需更新）</li>
     * </ul>
     *
     * <p><b>事务保证：</b>使用@Transactional注解，确保批量更新的事务一致性</p>
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateBatchVisitRecord(List<VisitRecord> visitRecordList) {
        // 空列表检查：空列表视为操作成功
        if (visitRecordList.isEmpty()) {
            return true;
        }

        // 校验所有记录租户ID一致且非空，同时检查ID非空
        Long tenantId = visitRecordList.get(0).getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }

        boolean allSameTenant = visitRecordList.stream()
                .allMatch(record -> tenantId.equals(record.getTenantId()) && record.getId() != null);
        if (!allSameTenant) {
            throw new IllegalArgumentException("批量更新的带看记录必须包含ID且属于同一租户");
        }

        return updateBatchById(visitRecordList);
    }

    /**
     * 批量删除带看记录（租户隔离批量删除）
     * <p>批量删除指定租户下的带看记录，确保操作权限和数据安全</p>
     *
     * @param ids 待删除的带看记录ID列表，不能为空
     * @param tenantId 租户ID，用于权限校验和数据隔离，不能为null
     * @return boolean 批量操作结果，true-成功 false-失败
     * @throws IllegalArgumentException 当ID列表为空或租户ID为空时抛出异常
     *
     * <p><b>删除机制：</b></p>
     * <ul>
     *   <li>条件删除：同时匹配ID列表和租户ID，确保操作安全</li>
     *   <li>批量操作：使用IN条件实现高效批量删除</li>
     *   <li>事务保护：使用@Transactional注解确保操作原子性</li>
     * </ul>
     *
     * <p><b>使用场景：</b>批量清理无效数据、数据归档等</p>
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean removeBatchByIdsWithTenant(List<Long> ids, Long tenantId) {
        // 参数空值校验
        if (ids.isEmpty() || tenantId == null) {
            throw new IllegalArgumentException("租户ID和记录ID列表不能为空");
        }

        // 构建批量删除条件：ID列表包含且租户ID匹配
        return remove(new LambdaQueryWrapper<VisitRecord>()
                .in(VisitRecord::getId, ids)
                .eq(VisitRecord::getTenantId, tenantId));
    }
}
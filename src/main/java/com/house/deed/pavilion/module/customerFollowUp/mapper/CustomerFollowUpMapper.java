package com.house.deed.pavilion.module.customerFollowUp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.house.deed.pavilion.module.customerFollowUp.entity.CustomerFollowUp;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * <p>
 * 客户跟进记录表（租户级数据） Mapper 接口
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
public interface CustomerFollowUpMapper extends BaseMapper<CustomerFollowUp> {

    /**
     * 查询客户最近一次跟进时间
     */
    @Select("SELECT MAX(follow_time) FROM customer_follow_up WHERE customer_id = #{customerId} AND tenant_id = #{tenantId}")
    LocalDateTime selectLastFollowTime(@Param("customerId") Long customerId, @Param("tenantId") Long tenantId);


}

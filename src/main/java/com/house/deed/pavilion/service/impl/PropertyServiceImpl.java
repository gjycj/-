package com.house.deed.pavilion.service.impl;

import com.house.deed.pavilion.entity.Property;
import com.house.deed.pavilion.mapper.PropertyMapper;
import com.house.deed.pavilion.service.PropertyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 楼盘信息表（租户级数据隔离） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class PropertyServiceImpl extends ServiceImpl<PropertyMapper, Property> implements PropertyService {

}

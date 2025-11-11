package com.house.deed.pavilion.module.property.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.module.property.entity.Property;
import com.house.deed.pavilion.module.property.mapper.PropertyMapper;
import com.house.deed.pavilion.module.property.service.IPropertyService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 楼盘信息表（租户级数据隔离） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class PropertyServiceImpl extends ServiceImpl<PropertyMapper, Property> implements IPropertyService {

}

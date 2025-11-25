package com.house.deed.pavilion.service.impl;

import com.house.deed.pavilion.entity.Tag;
import com.house.deed.pavilion.mapper.TagMapper;
import com.house.deed.pavilion.service.TagService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 标签表（租户级数据隔离） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

}

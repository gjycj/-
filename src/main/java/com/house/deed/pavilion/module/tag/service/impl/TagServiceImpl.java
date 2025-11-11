package com.house.deed.pavilion.module.tag.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.module.tag.mapper.TagMapper;
import com.house.deed.pavilion.module.tag.service.ITagService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 标签表（租户级数据隔离） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements ITagService {

}

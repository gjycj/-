package com.house.deed.pavilion.module.store.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.module.store.entity.Store;
import com.house.deed.pavilion.module.store.mapper.StoreMapper;
import com.house.deed.pavilion.module.store.service.IStoreService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 门店信息表（租户级数据） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class StoreServiceImpl extends ServiceImpl<StoreMapper, Store> implements IStoreService {

}

package com.house.deed.pavilion.service.impl;

import com.house.deed.pavilion.entity.OperationLog;
import com.house.deed.pavilion.mapper.OperationLogMapper;
import com.house.deed.pavilion.service.OperationLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 系统操作日志表（租户级审计） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-26
 */
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

}

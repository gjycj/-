package com.house.deed.pavilion.module.operationLog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.house.deed.pavilion.module.operationLog.entity.OperationLog;
import com.house.deed.pavilion.module.operationLog.mapper.OperationLogMapper;
import com.house.deed.pavilion.module.operationLog.service.IOperationLogService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 系统操作日志表（租户级审计） 服务实现类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements IOperationLogService {

}

package com.house.deed.pavilion.module.electronicSign.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.module.electronicSign.entity.ElectronicSign;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 电子签约信息表（租户级数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
public interface IElectronicSignService extends IService<ElectronicSign> {

    // 新增：电子签作废方法
    boolean invalidSign(Long signId);

    /**
     * 创建电子签约记录（生成签约链接）
     * @param contractId 合同ID
     * @param signPlatform 签约平台（e签宝/法大大）
     * @return 电子签记录（含签约链接）
     */
    ElectronicSign createElectronicSign(Long contractId, String signPlatform);

    /**
     * 更新电子签状态+记录签名时间
     * @param signId 电子签ID
     * @param customerSign 客户是否已签
     * @param landlordSign 房东是否已签
     * @param customerSignTime 客户签名时间（第三方提供）
     * @param landlordSignTime 房东签名时间（第三方提供）
     * @return 新状态
     */
    String updateSignStatus(Long signId, boolean customerSign, boolean landlordSign,
                            LocalDateTime customerSignTime, LocalDateTime landlordSignTime);

    /**
     * 新增：通过合同ID查询电子签记录（关联查询用）
     * @param contractId 合同ID
     * @return 电子签记录
     */
    ElectronicSign getByContractId(Long contractId);

    /**
     * 新增：批量通过合同ID查询电子签记录
     * @param contractIds 合同ID列表
     * @return 合同ID -> 电子签记录的映射
     */
    Map<Long, ElectronicSign> getBatchByContractIds(List<Long> contractIds);

}

package com.house.deed.pavilion.module.electronicSign.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.house.deed.pavilion.module.electronicSign.entity.ElectronicSign;

/**
 * <p>
 * 电子签约信息表（租户级数据） 服务类
 * </p>
 *
 * @author yuquanxi
 * @since 2025-11-07
 */
public interface IElectronicSignService extends IService<ElectronicSign> {

    /**
     * 创建电子签约记录（生成签约链接）
     * @param contractId 合同ID
     * @param signPlatform 签约平台（e签宝/法大大）
     * @return 电子签记录（含签约链接）
     */
    ElectronicSign createElectronicSign(Long contractId, String signPlatform);

    /**
     * 更新签约状态（回调接口）
     * @param signId 电子签ID
     * @param customerSign 是否客户已签
     * @param landlordSign 是否房东已签
     * @return 更新后的状态
     */
    String updateSignStatus(Long signId, boolean customerSign, boolean landlordSign);

}

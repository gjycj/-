package com.house.deed.pavilion.module.contract.vo;

import com.house.deed.pavilion.module.contract.entity.Contract;
import com.house.deed.pavilion.module.contractAttachment.entity.ContractAttachment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 合同详情VO（含附件列表）
 */
@Data
@Schema(description = "合同详情（含关联附件）")
public class ContractDetailVO extends Contract { // 继承Contract保留基本字段
    @Schema(description = "合同关联的附件列表")
    private List<ContractAttachment> attachments;
}
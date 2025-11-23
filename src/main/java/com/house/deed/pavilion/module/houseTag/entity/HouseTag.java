package com.house.deed.pavilion.module.houseTag.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("house_tag")
@Schema(description = "房源-标签关联表")
public class HouseTag {

    @TableId(type = IdType.AUTO)
    @Schema(description = "关联ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    @Schema(description = "租户ID（归属租户）", accessMode = Schema.AccessMode.READ_ONLY)
    private Long tenantId;

    @TableField("house_id")
    @Schema(description = "房源ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long houseId;

    @TableField("tag_id")
    @Schema(description = "标签ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long tagId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createTime;
}
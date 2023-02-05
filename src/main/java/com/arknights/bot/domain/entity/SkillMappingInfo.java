package com.arknights.bot.domain.entity;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 干员-技能信息映射
 * Created by wangzhen on 2023/2/5 23:21
 * @author 14869
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SkillMappingInfo {
    private Long id;
    @ApiModelProperty(value = "干员唯一标识")
    private String keyCode;
    @ApiModelProperty(value = "技能id")
    private String skillCode;
    @ApiModelProperty(value = "技能序号（比如技能一，技能二）")
    private Integer skillOrder;
    @ApiModelProperty(value = "技能解锁条件")
    private String openLevel;
}

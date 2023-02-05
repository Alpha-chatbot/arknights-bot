package com.arknights.bot.domain.entity;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 干员技能信息
 * Created by wangzhen on 2023/1/30 11:21
 * @author 14869
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SkillLevelInfo {

    @ApiModelProperty(value = "主键id")
    private Long id;
    @ApiModelProperty(value = "技能id")
    private String skillCode;
    @ApiModelProperty(value = "技能名称")
    private String skillName;
    @ApiModelProperty(value = "技力类型(自动回复等)")
    private String powerType;
    @ApiModelProperty(value = "触发类型(自动触发等)")
    private String triggerType;
    @ApiModelProperty(value = "等级(专精等级以8,9,10代替)")
    private Long skillLevel;
    @ApiModelProperty(value = "技能描述")
    private String description;
    @ApiModelProperty(value = "初始技力")
    private Long initialValue;
    @ApiModelProperty(value = "消耗技力")
    private Long consumeValue;
    @ApiModelProperty(value = "技能持续时间")
    private Long span;
    @ApiModelProperty(value = "备注")
    private String remarks;
    @ApiModelProperty(value = "技能序号（比如技能一，技能二）")
    private Integer skillOrder;
    @ApiModelProperty(value = "技能解锁条件")
    private String openLevel;
    @ApiModelProperty(value = "附加字段1")
    private String attribute1;
}

package com.arknights.bot.domain.entity;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 寻访记录对应实体
 *
 * Created by wangzhen on 2023/1/23 10:15
 * @author 14869
 */
@Data
public class GaChaInfo {
    @ApiModelProperty(value = "主键id")
    private Long id;
    @ApiModelProperty(value = "唯一性id，时间戳")
    private Long ts;
    @ApiModelProperty(value = "干员名称")
    private String operatorsName;
    @ApiModelProperty(value = "稀有度,默认需要+1")
    private Integer rarity;
    @ApiModelProperty(value = "卡池名")
    private String pool;
    @ApiModelProperty(value = "是否为第一次获取")
    private Boolean isNew;
    @ApiModelProperty(value = "qq号")
    private Long qq;
    @ApiModelProperty(value = "批次号")
    private Long processId;
}

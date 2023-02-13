package com.arknights.bot.api.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 寻访类实体
 *
 * Created by wangzhen on 2023/1/23 14:24
 * @author 14869
 */
@Data
public class EmployeesDto {

    @ApiModelProperty(value = "干员名称")
    private String name;
    @ApiModelProperty(value = "稀有度,默认需要+1")
    private Integer rarity;
    @ApiModelProperty(value = "是否为第一次获取")
    private Boolean isNew;
}

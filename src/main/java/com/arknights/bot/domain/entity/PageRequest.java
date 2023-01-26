package com.arknights.bot.domain.entity;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Created by wangzhen on 2023/1/23 10:36
 * @author 14869
 */
@Data
public class PageRequest {
    @ApiModelProperty(value = "当前页数，从1开始")
    private int current;
    @ApiModelProperty(value = "总页数")
    private int total;
}

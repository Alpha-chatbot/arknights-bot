package com.arknights.bot.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

/**
 * 干员base表
 *
 * Created by wangzhen on 2023/2/1 16:47
 * @author 14869
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("干员基本信息表")
public class OperatorBaseInfo {
    @ApiModelProperty(value = "主键id")
    private Long id;
    @ApiModelProperty(value = "json key")
    private String key;
    @ApiModelProperty(value = "干员中文名")
    private String zhName;
    @ApiModelProperty(value = "干员英文名")
    private String enName;
    @ApiModelProperty(value = "招聘合同")
    private String itemUsage;
}

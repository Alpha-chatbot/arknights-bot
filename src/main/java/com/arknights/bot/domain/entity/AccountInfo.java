package com.arknights.bot.domain.entity;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 账户信息
 *
 * Created by wangzhen on 2023/1/24 19:13
 * @author 14869
 */
@Data
public class AccountInfo {

    @ApiModelProperty(value = "主键id")
    private Long id;
    @ApiModelProperty(value = "token")
    private String token;
    @ApiModelProperty(value = "qq号")
    private Long qq;
}

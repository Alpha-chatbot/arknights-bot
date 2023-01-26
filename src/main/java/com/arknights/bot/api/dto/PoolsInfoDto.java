package com.arknights.bot.api.dto;

import com.arknights.bot.domain.entity.PageRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * Created by wangzhen on 2023/1/23 14:27
 * @author 14869
 */
@Data
public class PoolsInfoDto {
    @ApiModelProperty(value = "唯一性id")
    private Long ts;
    @ApiModelProperty(value = "卡池名")
    private String pool;
    @ApiModelProperty(value = "寻访干员列表，若十连则列表中会有10条，单抽则只有一条")
    private List<EmployeesDto> chars;

    private PageRequest pageRequest;

}

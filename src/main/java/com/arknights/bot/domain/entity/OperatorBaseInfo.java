package com.arknights.bot.domain.entity;

import lombok.Builder;
import lombok.Data;

/**
 * 干员base表
 *
 * Created by wangzhen on 2023/2/1 16:47
 * @author 14869
 */
@Builder
@Data
public class OperatorBaseInfo {
    private Long id;
    private Long operatorId;
    private String name;
}

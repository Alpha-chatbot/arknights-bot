package com.arknights.bot.domain.entity;

import lombok.Data;

/**
 * OPQ WebAPI返回参数
 * Created by wangzhen on 2023/1/19 16:38
 * @author 14869
 */
@Data
public class SendMsgRespInfo {
    private String Msg;
    private Integer Ret;
}
